/**
 * 人设系统 - 基于 Hermes persona.py 重写
 * 预设 + 自定义人设，支持切换
 * AI 智能初始化：搜索引擎配置 + 模型采样参数
 */
import type { Persona, PersonaSearchConfig, PersonaSamplingConfig } from '../../shared/types'
import { analyzePersona, analysisToSearchConfig } from '../../search/persona-search-init'
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

  /** 启动时批量初始化所有开启智能分析的人设 */
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

  switchTo(id: string): Persona {
    const p = this.personas.find(x => x.id === id)
    if (!p) throw new Error(`Persona not found: ${id}`)
    this.activeId = id
    this.persist()
    return p
  }

  // ── 增删改 ──────────────────────

  add(persona: Omit<Persona, 'id'>): Persona {
    const p: Persona = {
      ...persona,
      id: `custom_${Date.now()}`,
      autoAnalyzeSearch: persona.autoAnalyzeSearch ?? true,
    }
    if (p.autoAnalyzeSearch && !p.searchConfig) {
      const analysis = this.analyzeAndCache(p)
      p.searchConfig = analysisToSearchConfig(analysis)
      if (!p.samplingConfig) p.samplingConfig = analysis.sampling
    }
    this.personas.push(p)
    this.persist()
    return p
  }

  update(id: string, patch: Partial<Persona>): boolean {
    const idx = this.personas.findIndex(p => p.id === id)
    if (idx < 0) return false
    const updated = { ...this.personas[idx], ...patch }
    if (patch.systemPrompt && updated.autoAnalyzeSearch) {
      const analysis = this.analyzeAndCache(updated)
      updated.searchConfig = analysisToSearchConfig(analysis)
      updated.samplingConfig = analysis.sampling
    }
    this.personas[idx] = updated
    this.persist()
    return true
  }

  delete(id: string): boolean {
    if (DEFAULT_PERSONAS.some(p => p.id === id)) return false
    this.personas = this.personas.filter(p => p.id !== id)
    if (this.activeId === id) this.activeId = 'gentle'
    this.analysisCache.delete(id)
    this.persist()
    return true
  }

  // ── 智能初始化 ──────────────────────

  private analyzeAndCache(persona: Persona): PersonaAnalysis {
    const analysis = analyzePersona(persona.systemPrompt)
    this.analysisCache.set(persona.id, analysis)
    return analysis
  }

  /** 获取分析结果（UI 展示标签、推荐理由等） */
  getAnalysis(personaId: string): PersonaAnalysis | null {
    const cached = this.analysisCache.get(personaId)
    if (cached) return cached
    const persona = this.getById(personaId)
    if (!persona) return null
    return this.analyzeAndCache(persona)
  }

  /** 手动重新分析（搜索+采样一起重新生成） */
  reAnalyze(personaId: string): PersonaAnalysis | null {
    const persona = this.getById(personaId)
    if (!persona) return null
    this.analysisCache.delete(personaId)
    const analysis = this.analyzeAndCache(persona)
    this.update(personaId, {
      searchConfig: analysisToSearchConfig(analysis),
      samplingConfig: analysis.sampling,
    })
    return analysis
  }

  /** 切换智能分析开关 */
  setAutoAnalyze(personaId: string, enabled: boolean): void {
    const persona = this.getById(personaId)
    if (!persona) return
    if (enabled) {
      const analysis = this.analyzeAndCache(persona)
      this.update(personaId, {
        autoAnalyzeSearch: true,
        searchConfig: analysisToSearchConfig(analysis),
        samplingConfig: analysis.sampling,
      })
    } else {
      this.update(personaId, { autoAnalyzeSearch: false })
    }
  }

  // ── 搜索配置手动控制 ──────────────────────

  setManualSearchConfig(personaId: string, config: PersonaSearchConfig): void {
    this.update(personaId, {
      searchConfig: config,
      autoAnalyzeSearch: false,
    })
  }

  getSearchConfig(personaId?: string): PersonaSearchConfig | null {
    const id = personaId || this.activeId
    const persona = this.getById(id)
    return persona?.searchConfig || null
  }

  // ── 采样配置手动控制 ──────────────────────

  setManualSamplingConfig(personaId: string, config: PersonaSamplingConfig): void {
    this.update(personaId, { samplingConfig: config })
  }

  getSamplingConfig(personaId?: string): PersonaSamplingConfig | null {
    const id = personaId || this.activeId
    const persona = this.getById(id)
    return persona?.samplingConfig || null
  }

  /** 首次激活人设时确保已初始化 */
  ensureInitialized(personaId: string): void {
    const persona = this.getById(personaId)
    if (!persona) return
    if (persona.autoAnalyzeSearch && (!persona.searchConfig || !persona.searchConfig.engines.length)) {
      const analysis = this.analyzeAndCache(persona)
      this.update(personaId, {
        searchConfig: analysisToSearchConfig(analysis),
        samplingConfig: analysis.sampling,
      })
    }
  }
}

export { getTagEmoji, describeSampling } from '../../search/persona-search-init'
