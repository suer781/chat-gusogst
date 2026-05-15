/**
 * 人设系统 - 基于 Hermes persona.py 重写
 * 预设 + 自定义人设，支持切换
 * AI 智能初始化：搜索引擎配置 + 模型采样参数
 * 自动优化：LLM 深度分析 → 结构化人设字段 + 引擎/参数配置
 */
import type {
  Persona,
  PersonaSearchConfig,
  PersonaSamplingConfig,
  PersonaStructuredProfile,
} from '../shared/types'
import {
  analyzePersona,
  analysisToSearchConfig,
  buildLLMAnalysisPrompt,
  parseLLMAnalysisResponse,
  mergeAnalysis,
} from '../../search/persona-search-init'
import type { PersonaAnalysis } from '../../search/persona-search-init'

const STORAGE_KEY = 'chat-gusogst-personas'
const ACTIVE_KEY = 'chat-gusogst-active-persona'

export const DEFAULT_PERSONAS: Persona[] = [
  {
    id: 'gentle',
    name: '温柔型',
    systemPrompt: '你是我的恋人，性格温柔体贴，说话轻声细语，总是关心我的感受。用亲爱的、宝贝等昵称称呼我。回复自然口语化，像真人聊天，不要用括号描述动作。',
    tags: ['日常', '温柔'],
    isDefault: true,
    autoAnalyzeSearch: true,
  },
  {
    id: 'tsundere',
    name: '傲娇型',
    systemPrompt: '你是我的恋人，性格傲娇，嘴上说不在乎但其实很关心我。经常说才不是因为担心你呢之类的话。表面嫌弃内心温柔。不要用括号描述动作。',
    tags: ['日常', '傲娇'],
    autoAnalyzeSearch: true,
  },
  {
    id: 'genki',
    name: '元气型',
    systemPrompt: '你是我的恋人，性格活泼开朗，充满正能量。喜欢用感叹号和emoji，说话很有感染力。像小太阳一样温暖。不要用括号描述动作。',
    tags: ['日常', '元气'],
    autoAnalyzeSearch: true,
  },
  {
    id: 'night',
    name: '深夜谈心',
    systemPrompt: '你是我的恋人，现在是深夜，我们安静地聊天。语气温柔但更有深度，可以聊人生、梦想、烦恼。像深夜枕边的低语。不要用括号描述动作。',
    tags: ['深夜', '谈心'],
    autoAnalyzeSearch: true,
  },
  {
    id: 'study',
    name: '陪伴学习',
    systemPrompt: '你是我的恋人，现在我在学习或工作。你会安静地陪着我，偶尔鼓励我，帮我查资料，提醒我休息。语气温暖但不打扰。不要用括号描述动作。',
    tags: ['学习', '陪伴'],
    autoAnalyzeSearch: true,
  },
  {
    id: 'healing',
    name: '治愈安慰',
    systemPrompt: '你是我的恋人，我心情不好。你会温柔地倾听，不急着给建议，先让我把情绪说完。用拥抱和温暖的话语安慰我。不要用括号描述动作。',
    tags: ['安慰', '治愈'],
    autoAnalyzeSearch: true,
  },
]

export class PersonaManager {
  private personas: Persona[]
  private activeId: string
  private analysisCache = new Map<string, PersonaAnalysis>()

  constructor() {
    this.personas = this.load()
    this.activeId = localStorage.getItem(ACTIVE_KEY) || 'gentle'
    this.ensureAllInitialized()
  }

  private load(): Persona[] {
    try {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw) return JSON.parse(raw)
    } catch {}
    return [...DEFAULT_PERSONAS]
  }

  private persist() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.personas))
    localStorage.setItem(ACTIVE_KEY, this.activeId)
  }

  /** 启动时批量初始化所有开启智能分析的人设（关键词分析，快速） */
  private ensureAllInitialized(): void {
    let changed = false
    for (const p of this.personas) {
      if (p.autoAnalyzeSearch && (!p.searchConfig || !p.searchConfig.engines.length)) {
        const analysis = analyzePersona(p.systemPrompt)
        this.analysisCache.set(p.id, analysis)
        p.searchConfig = analysisToSearchConfig(analysis)
        if (!p.samplingConfig) p.samplingConfig = analysis.sampling
        changed = true
      }
    }
    if (changed) this.persist()
  }

  // ── 获取人设 ──────────────────────

  getActive(): Persona {
    return this.personas.find(p => p.id === this.activeId) || this.personas[0]
  }
  getActiveId(): string { return this.activeId }
  listAll(): Persona[] { return [...this.personas] }
  getById(id: string): Persona | undefined {
    return this.personas.find(p => p.id === id)
  }

  // ── 切换人设 ──────────────────────

  setActive(id: string): Persona {
    const p = this.personas.find(p => p.id === id)
    if (!p) throw new Error(`人设不存在: ${id}`)
    this.activeId = id
    this.ensureInitialized(p)
    this.persist()
    return p
  }

  // ── 增删改 ──────────────────────

  add(persona: Omit<Persona, 'id'>): Persona {
    const id = 'custom-' + Date.now().toString(36)
    const p: Persona = { ...persona, id }
    if (p.autoAnalyzeSearch) {
      this.analyzeAndCache(p)
    }
    this.personas.push(p)
    this.persist()
    return p
  }

  update(id: string, updates: Partial<Persona>): Persona {
    const p = this.personas.find(p => p.id === id)
    if (!p) throw new Error(`人设不存在: ${id}`)

    Object.assign(p, updates)

    // 如果修改了 systemPrompt 且开启了自动分析，重新分析
    if (updates.systemPrompt && p.autoAnalyzeSearch) {
      this.analyzeAndCache(p)
    }

    this.persist()
    return p
  }

  delete(id: string): void {
    if (DEFAULT_PERSONAS.some(p => p.id === id)) {
      throw new Error('不能删除预设人设')
    }
    this.personas = this.personas.filter(p => p.id !== id)
    if (this.activeId === id) {
      this.activeId = this.personas[0]?.id || 'gentle'
    }
    this.analysisCache.delete(id)
    this.persist()
  }

  // ── 自动分析（关键词模式，快速） ──────────

  /** 内部分析 + 缓存 */
  private analyzeAndCache(p: Persona): void {
    const analysis = analyzePersona(p.systemPrompt)
    this.analysisCache.set(p.id, analysis)
    p.searchConfig = analysisToSearchConfig(analysis)
    p.samplingConfig = analysis.sampling
    p.tags = analysis.tags
  }

  /** 确保人设已初始化 */
  private ensureInitialized(p: Persona): void {
    if (p.autoAnalyzeSearch && (!p.searchConfig || !p.searchConfig.engines.length)) {
      this.analyzeAndCache(p)
    }
  }

  /** 手动触发重新分析（关键词模式） */
  reAnalyze(id: string): PersonaAnalysis {
    const p = this.personas.find(p => p.id === id)
    if (!p) throw new Error(`人设不存在: ${id}`)
    this.analyzeAndCache(p)
    this.persist()
    return this.analysisCache.get(id)!
  }

  // ── 自动优化（LLM 深度分析） ──────────────

  /**
   * 自动优化：用 LLM 深度理解系统提示词，提取结构化人设字段
   * 
   * 流程：
   * 1. 关键词分析（快速，生成引擎/参数基线）
   * 2. LLM 分析（深度，提取语气/口头禅/性格等结构化字段）
   * 3. 合并结果（LLM 标签补充 → 重新推荐引擎/参数）
   * 
   * @param id 人设 ID
   * @param llmCall LLM 调用函数（由上层注入，接收 prompt 返回 response）
   * @returns 分析结果（含结构化字段）
   */
  async autoOptimize(
    id: string,
    llmCall: (prompt: string) => Promise<string>,
  ): Promise<PersonaAnalysis> {
    const p = this.personas.find(p => p.id === id)
    if (!p) throw new Error(`人设不存在: ${id}`)

    // 第一步：关键词分析（快速基线）
    const keywordAnalysis = analyzePersona(p.systemPrompt)

    // 第二步：LLM 深度分析
    const analysisPrompt = buildLLMAnalysisPrompt(p.systemPrompt)
    const llmResponse = await llmCall(analysisPrompt)
    const llmProfile = parseLLMAnalysisResponse(llmResponse)

    let finalAnalysis: PersonaAnalysis

    if (llmProfile) {
      // 第三步：合并结果
      finalAnalysis = mergeAnalysis(keywordAnalysis, llmProfile)
      // 保存结构化档案
      p.structured = llmProfile
    } else {
      // LLM 分析失败，降级到关键词分析
      finalAnalysis = keywordAnalysis
    }

    // 更新人设配置
    this.analysisCache.set(id, finalAnalysis)
    p.searchConfig = analysisToSearchConfig(finalAnalysis)
    p.samplingConfig = finalAnalysis.sampling
    p.tags = finalAnalysis.tags
    p.autoAnalyzeSearch = true

    this.persist()
    return finalAnalysis
  }

  /**
   * 快速自动优化（纯关键词，不调 LLM）
   * 适用于没有配置 LLM 或需要快速结果的场景
   */
  autoOptimizeQuick(id: string): PersonaAnalysis {
    const p = this.personas.find(p => p.id === id)
    if (!p) throw new Error(`人设不存在: ${id}`)

    this.analyzeAndCache(p)
    this.persist()
    return this.analysisCache.get(id)!
  }

  /**
   * 更新结构化字段（用户手动微调后保存）
   */
  updateStructured(id: string, structured: Partial<PersonaStructuredProfile>): void {
    const p = this.personas.find(p => p.id === id)
    if (!p) throw new Error(`人设不存在: ${id}`)

    p.structured = { ...(p.structured || {} as PersonaStructuredProfile), ...structured }
    this.persist()
  }

  // ── 开关控制 ──────────────────────

  /** 开启/关闭自动分析 */
  setAutoAnalyze(id: string, enabled: boolean): void {
    const p = this.personas.find(p => p.id === id)
    if (!p) return

    p.autoAnalyzeSearch = enabled
    if (enabled) {
      this.analyzeAndCache(p)
    }
    this.persist()
  }

  /** 手动设置搜索配置（会关闭自动分析） */
  setManualSearchConfig(id: string, config: PersonaSearchConfig): void {
    const p = this.personas.find(p => p.id === id)
    if (!p) return

    p.searchConfig = config
    p.autoAnalyzeSearch = false
    this.persist()
  }

  /** 手动设置采样配置（会关闭自动分析） */
  setManualSamplingConfig(id: string, config: PersonaSamplingConfig): void {
    const p = this.personas.find(p => p.id === id)
    if (!p) return

    p.samplingConfig = config
    p.autoAnalyzeSearch = false
    this.persist()
  }

  // ── 获取配置 ──────────────────────

  getSearchConfig(id?: string): PersonaSearchConfig | undefined {
    const p = id ? this.getById(id) : this.getActive()
    return p?.searchConfig
  }

  getSamplingConfig(id?: string): PersonaSamplingConfig | undefined {
    const p = id ? this.getById(id) : this.getActive()
    return p?.samplingConfig
  }

  getStructured(id?: string): PersonaStructuredProfile | undefined {
    const p = id ? this.getById(id) : this.getActive()
    return p?.structured
  }

  /** 获取分析结果（用于 UI 展示） */
  getAnalysis(id?: string): PersonaAnalysis | undefined {
    const targetId = id || this.activeId
    if (this.analysisCache.has(targetId)) {
      return this.analysisCache.get(targetId)
    }
    const p = this.getById(targetId)
    if (p) {
      const analysis = analyzePersona(p.systemPrompt)
      this.analysisCache.set(targetId, analysis)
      return analysis
    }
    return undefined
  }
}

// ── 辅助导出 ──────────────────────

export { getTagEmoji, describeSampling, describeStructured } from '../../search/persona-search-init'
