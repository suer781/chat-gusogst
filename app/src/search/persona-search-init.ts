// ============================================
// 人设搜索引擎智能初始化
// 首次拿到系统提示词时，AI 分析人设特征
// 推断出最佳搜索引擎配置 + 模型采样参数
// ============================================

import type { PersonaSearchConfig } from '../shared/types'
import { ALL_ENGINES } from './engines/registry'
import type { EngineMeta } from './engines/registry'

// ── 模型采样配置 ──────────────────────

export interface PersonaSamplingConfig {
  /** 温度 (0.0-2.0)：越低越确定，越高越随机 */
  temperature: number
  /** 核采样 (0.0-1.0)：越低越保守 */
  topP: number
  /** 存在惩罚 (-2.0~2.0)：鼓励谈论新话题 */
  presencePenalty: number
  /** 频率惩罚 (-2.0~2.0)：减少重复用词 */
  frequencyPenalty: number
  /** 回复长度偏好 */
  maxTokens: number
}

// ── 完整分析结果 ──────────────────────

export interface PersonaAnalysis {
  /** 人设画像标签 */
  tags: string[]
  /** 搜索场景描述 */
  scenarios: string[]
  /** 推荐的引擎优先级 */
  recommendedEngines: string[]
  /** 各推荐引擎的理由 */
  engineReasons: Record<string, string>
  /** 推荐的并发数 */
  concurrency: number
  /** 是否启用联网搜索 */
  enableSearch: boolean
  /** 是否启用时间过滤 */
  enableTimeRange: boolean
  /** 推荐的模型采样参数 */
  sampling: PersonaSamplingConfig
}

// ── 引擎特征库 ──────────────────────

interface EngineProfile {
  id: string
  strengths: string[]
  weaknesses: string[]
  languages: 'zh' | 'en' | 'both'
  qualityScore: number
  speedScore: number
}

const ENGINE_PROFILES: EngineProfile[] = [
  { id: 'duckduckgo',    strengths: ['隐私','国际','技术','英文内容','开源','学术'], weaknesses: [], languages: 'both', qualityScore: 8, speedScore: 7 },
  { id: 'bing',          strengths: ['通用','技术','国际','英文内容','学术'], weaknesses: [], languages: 'both', qualityScore: 7, speedScore: 8 },
  { id: 'baidu',         strengths: ['中文','通用','国内','百科','生活'], weaknesses: ['英文内容','隐私'], languages: 'zh', qualityScore: 6, speedScore: 8 },
  { id: 'brave',         strengths: ['隐私','技术','独立','开源'], weaknesses: ['中文内容'], languages: 'en', qualityScore: 7, speedScore: 7 },
  { id: 'so360',         strengths: ['国内','安全','中文'], weaknesses: ['英文内容','隐私'], languages: 'zh', qualityScore: 5, speedScore: 7 },
  { id: 'sogou',         strengths: ['中文','微信','国内','生活'], weaknesses: ['英文内容'], languages: 'zh', qualityScore: 6, speedScore: 7 },
  { id: 'shenma',        strengths: ['移动端','中文','小说','影视'], weaknesses: ['技术','学术'], languages: 'zh', qualityScore: 5, speedScore: 8 },
  { id: 'toutiao',       strengths: ['新闻','资讯','热点','中文'], weaknesses: ['学术','技术'], languages: 'zh', qualityScore: 5, speedScore: 8 },
  { id: 'chinaso',       strengths: ['权威','官方','中文'], weaknesses: ['国际'], languages: 'zh', qualityScore: 6, speedScore: 6 },
  { id: 'quark',         strengths: ['移动端','中文','智能','生活'], weaknesses: [], languages: 'zh', qualityScore: 6, speedScore: 8 },
  { id: 'baidu_dev',     strengths: ['技术','编程','开发','代码','GitHub'], weaknesses: ['非技术'], languages: 'zh', qualityScore: 8, speedScore: 7 },
  { id: 'zhihu',         strengths: ['问答','深度','经验','知乎','观点','讨论'], weaknesses: ['实时性'], languages: 'zh', qualityScore: 8, speedScore: 6 },
  { id: 'weixin',        strengths: ['微信','公众号','深度','行业'], weaknesses: ['英文'], languages: 'zh', qualityScore: 7, speedScore: 5 },
  { id: 'baidu_academic', strengths: ['学术','论文','科研','专业'], weaknesses: ['通俗内容'], languages: 'both', qualityScore: 8, speedScore: 5 },
  { id: 'smzdm',         strengths: ['购物','优惠','评测','消费','比价'], weaknesses: ['非购物'], languages: 'zh', qualityScore: 7, speedScore: 7 },
  { id: '58',            strengths: ['本地','生活服务','招聘','租房'], weaknesses: ['技术','学术'], languages: 'zh', qualityScore: 5, speedScore: 7 },
  { id: 'bocha',         strengths: ['AI搜索','中文','综合'], weaknesses: [], languages: 'zh', qualityScore: 7, speedScore: 7 },
  { id: 'pansou',        strengths: ['网盘','资源','下载','电影','软件'], weaknesses: ['实时性'], languages: 'zh', qualityScore: 6, speedScore: 7 },
  { id: 'panclub',       strengths: ['网盘','资源','下载'], weaknesses: ['实时性'], languages: 'zh', qualityScore: 6, speedScore: 7 },
  { id: 'xiongdipan',    strengths: ['网盘','资源'], weaknesses: ['实时性'], languages: 'zh', qualityScore: 5, speedScore: 7 },
]

// ── 标签提取 ──────────────────────

function extractTags(prompt: string): string[] {
  const tags: string[] = []
  const p = prompt.toLowerCase()

  if (/编程|代码|开发|程序员|技术|bug|github|api|框架|算法|backend|frontend|devops|java|python|typescript|rust|golang|react|vue/.test(p)) tags.push('技术')
  if (/前端|后端|全栈|运维|sre|架构/.test(p)) tags.push('编程')
  if (/ai|机器学习|深度学习|llm|大模型|nlp|人工智能/.test(p)) tags.push('AI')
  if (/硬件|嵌入式|iot|单片机|电路/.test(p)) tags.push('硬件')
  if (/学术|论文|科研|研究|文献|期刊|sci/.test(p)) tags.push('学术')
  if (/考研|考公|考试|学习|教育|辅导/.test(p)) tags.push('学习')
  if (/购物|优惠|比价|消费|买|推荐商品/.test(p)) tags.push('购物')
  if (/美食|菜谱|做饭|烹饪|吃货|餐厅/.test(p)) tags.push('美食')
  if (/旅行|旅游|攻略|景点|酒店|机票/.test(p)) tags.push('旅行')
  if (/健身|运动|减肥|健康|养生/.test(p)) tags.push('健康')
  if (/电影|影视|剧集|动漫|综艺|追剧/.test(p)) tags.push('影视')
  if (/音乐|歌曲|歌手|乐评/.test(p)) tags.push('音乐')
  if (/游戏|攻略|手游|steam/.test(p)) tags.push('游戏')
  if (/投资|股票|基金|理财|金融|财经/.test(p)) tags.push('金融')
  if (/新闻|时事|热点|资讯/.test(p)) tags.push('新闻')
  if (/装修|家居|家电/.test(p)) tags.push('家居')
  if (/育儿|亲子|宝宝/.test(p)) tags.push('育儿')
  if (/宠物|猫|狗/.test(p)) tags.push('宠物')
  if (/穿搭|时尚|美妆|护肤/.test(p)) tags.push('时尚')
  if (/汽车|车型|驾驶|评测/.test(p)) tags.push('汽车')
  if (/网盘|资源|下载|种子|torrent/.test(p)) tags.push('网盘资源')
  if (/软件|工具|app|插件/.test(p)) tags.push('软件')
  if (/问答|解答|百科|知识/.test(p)) tags.push('问答')
  if (/写作|文案|创作|编辑/.test(p)) tags.push('写作')
  if (/翻译|translate|多语言/.test(p)) tags.push('翻译')
  if (/情感|恋爱|倾诉|陪伴|聊天|安慰|心理咨询/.test(p)) tags.push('情感')
  if (/社交|人脉|沟通/.test(p)) tags.push('社交')
  if (/[\u4e00-\u9fff]/.test(p)) tags.push('中文')
  if (/[a-zA-Z]{5,}/.test(p)) tags.push('英文')

  // 语气风格检测（影响采样参数）
  if (/温柔|轻声|体贴|关心|温暖|柔情/.test(p)) tags.push('温柔语气')
  if (/傲娇|嫌弃|毒舌|暴躁|凶/.test(p)) tags.push('傲娇语气')
  if (/活泼|元气|开朗|热情|兴奋|正能量/.test(p)) tags.push('元气语气')
  if (/冷静|理性|客观|严谨|专业|严肃/.test(p)) tags.push('理性语气')
  if (/幽默|搞笑|段子|调侃|逗/.test(p)) tags.push('幽默语气')
  if (/文艺|诗意|浪漫|唯美|细腻/.test(p)) tags.push('文艺语气')
  if (/深夜|安静|沉思|冥想|放空/.test(p)) tags.push('深夜语气')

  return [...new Set(tags)]
}

function extractScenarios(prompt: string): string[] {
  const scenarios: string[] = []
  if (/搜索|查|找|看|了解/.test(prompt)) scenarios.push('信息检索')
  if (/新闻|最新|实时|热点/.test(prompt)) scenarios.push('实时资讯')
  if (/推荐|比较|评测|对比/.test(prompt)) scenarios.push('产品评测')
  if (/教程|学习|入门|进阶/.test(prompt)) scenarios.push('学习教程')
  if (/解决|问题|报错|bug|排查/.test(prompt)) scenarios.push('问题解决')
  if (/灵感|创意|头脑风暴/.test(prompt)) scenarios.push('创意灵感')
  return scenarios
}

// ── 采样参数推断 ──────────────────────

function inferSampling(tags: string[]): PersonaSamplingConfig {
  // 基准值
  let temperature = 0.7
  let topP = 0.9
  let presencePenalty = 0.3
  let frequencyPenalty = 0.3
  let maxTokens = 800

  // ── 语气风格调整 ──

  // 温柔型：稳定、温暖、少随机
  if (tags.includes('温柔语气')) {
    temperature = 0.55
    topP = 0.85
    presencePenalty = 0.4  // 多关心不同方面
    frequencyPenalty = 0.2 // 允许重复温馨用词
    maxTokens = 600
  }

  // 傲娇型：需要变化感，不能太死板
  if (tags.includes('傲娇语气')) {
    temperature = 0.85
    topP = 0.92
    presencePenalty = 0.5  // 多换花样怼
    frequencyPenalty = 0.4 // 减少重复句式
    maxTokens = 500        // 傲娇话不多
  }

  // 元气型：活泼多变
  if (tags.includes('元气语气')) {
    temperature = 0.9
    topP = 0.95
    presencePenalty = 0.6  // 话题跳跃
    frequencyPenalty = 0.3
    maxTokens = 700
  }

  // 理性型：精确、一致
  if (tags.includes('理性语气')) {
    temperature = 0.3
    topP = 0.8
    presencePenalty = 0.1
    frequencyPenalty = 0.1
    maxTokens = 1200
  }

  // 幽默型：需要创意
  if (tags.includes('幽默语气')) {
    temperature = 1.0
    topP = 0.95
    presencePenalty = 0.7
    frequencyPenalty = 0.5
    maxTokens = 600
  }

  // 文艺型：细腻但有想象力
  if (tags.includes('文艺语气')) {
    temperature = 0.8
    topP = 0.9
    presencePenalty = 0.5
    frequencyPenalty = 0.2  // 允许诗意重复
    maxTokens = 900
  }

  // 深夜型：沉稳、有深度
  if (tags.includes('深夜语气')) {
    temperature = 0.6
    topP = 0.85
    presencePenalty = 0.3
    frequencyPenalty = 0.2
    maxTokens = 1000
  }

  // ── 内容领域调整 ──

  // 技术/学术：需要精确
  if (tags.includes('技术') || tags.includes('学术') || tags.includes('编程')) {
    temperature = Math.min(temperature, 0.5)
    topP = Math.min(topP, 0.85)
    frequencyPenalty = Math.min(frequencyPenalty, 0.2)
    maxTokens = Math.max(maxTokens, 1200)
  }

  // 学习/辅导：准确为主
  if (tags.includes('学习')) {
    temperature = Math.min(temperature, 0.6)
    maxTokens = Math.max(maxTokens, 1000)
  }

  // 写作/创作：需要创意空间
  if (tags.includes('写作')) {
    temperature = Math.max(temperature, 0.8)
    presencePenalty = Math.max(presencePenalty, 0.5)
    maxTokens = Math.max(maxTokens, 1500)
  }

  // 情感/陪伴：自然流畅
  if (tags.includes('情感')) {
    temperature = Math.max(temperature, 0.6)
    frequencyPenalty = Math.max(frequencyPenalty, 0.3)
    maxTokens = Math.min(maxTokens, 600)
  }

  // 金融/投资：严谨
  if (tags.includes('金融')) {
    temperature = Math.min(temperature, 0.4)
    frequencyPenalty = Math.min(frequencyPenalty, 0.2)
    maxTokens = Math.max(maxTokens, 1000)
  }

  // ── 限制范围 ──
  return {
    temperature: Math.round(Math.max(0, Math.min(2, temperature)) * 100) / 100,
    topP: Math.round(Math.max(0, Math.min(1, topP)) * 100) / 100,
    presencePenalty: Math.round(Math.max(-2, Math.min(2, presencePenalty)) * 100) / 100,
    frequencyPenalty: Math.round(Math.max(-2, Math.min(2, frequencyPenalty)) * 100) / 100,
    maxTokens: Math.round(Math.max(100, Math.min(4096, maxTokens))),
  }
}

// ── 核心分析函数 ──────────────────────

export function analyzePersona(prompt: string): PersonaAnalysis {
  const tags = extractTags(prompt)
  const scenarios = extractScenarios(prompt)

  // 搜索引擎打分
  const scores: { id: string; score: number; reason: string }[] = []
  for (const profile of ENGINE_PROFILES) {
    let score = 0
    const reasons: string[] = []
    for (const tag of tags) {
      if (profile.strengths.some(s => tag.includes(s) || s.includes(tag))) {
        score += 3
        reasons.push(`擅长${tag}`)
      }
    }
    for (const tag of tags) {
      if (profile.weaknesses.some(w => tag.includes(w) || w.includes(tag))) {
        score -= 2
      }
    }
    score += profile.qualityScore * 0.3 + profile.speedScore * 0.1
    if (tags.includes('中文') && profile.languages === 'zh') score += 1
    if (tags.includes('英文') && profile.languages === 'en') score += 1
    if (tags.includes('网盘资源') && /pansou|panclub|xiongdipan/.test(profile.id)) {
      score += 5
      reasons.push('网盘搜索')
    }
    if (score > 0) scores.push({ id: profile.id, score, reason: reasons.join('、') || '通用匹配' })
  }

  scores.sort((a, b) => b.score - a.score)
  const topEngines = scores.slice(0, 6)
  const recommendedIds = topEngines.map(e => e.id)
  if (!recommendedIds.includes('duckduckgo')) recommendedIds.push('duckduckgo')
  if (!recommendedIds.includes('baidu')) recommendedIds.push('baidu')

  const engineReasons: Record<string, string> = {}
  for (const e of topEngines) engineReasons[e.id] = e.reason

  const needSearch = !(/情感|倾诉|陪伴|聊天$/.test(prompt) && tags.every(t => ['情感','社交','中文'].includes(t)))

  return {
    tags,
    scenarios,
    recommendedEngines: recommendedIds,
    engineReasons,
    concurrency: tags.length > 3 ? 3 : 2,
    enableSearch: needSearch,
    enableTimeRange: tags.some(t => ['新闻','实时资讯','热点'].includes(t)),
    sampling: inferSampling(tags),
  }
}

// ── 转换函数 ──────────────────────

export function analysisToSearchConfig(analysis: PersonaAnalysis): PersonaSearchConfig {
  const engineWeights: Record<string, number> = {}
  analysis.recommendedEngines.forEach((id, index) => {
    engineWeights[id] = Math.max(1, 10 - index)
  })
  return {
    engines: analysis.recommendedEngines,
    engineWeights,
    concurrency: analysis.concurrency,
    enableTimeRange: analysis.enableTimeRange,
    enableSearch: analysis.enableSearch,
  }
}

// ── 标签 emoji（UI 展示用）──────────────────────

export function getTagEmoji(tag: string): string {
  const map: Record<string, string> = {
    '技术': '💻', '编程': '⌨️', 'AI': '🤖', '硬件': '🔌',
    '学术': '📚', '学习': '📖', '购物': '🛒', '美食': '🍜',
    '旅行': '✈️', '健康': '💪', '影视': '🎬', '音乐': '🎵',
    '游戏': '🎮', '金融': '📈', '新闻': '📰', '家居': '🏠',
    '育儿': '👶', '宠物': '🐱', '时尚': '👗', '汽车': '🚗',
    '软件': '🔧', '问答': '💡', '写作': '✍️', '翻译': '🌐',
    '情感': '❤️', '社交': '👥', '网盘资源': '📁',
    '中文': '🇨🇳', '英文': '🇬🇧',
    '温柔语气': '💕', '傲娇语气': '💢', '元气语气': '☀️',
    '理性语气': '🧠', '幽默语气': '😂', '文艺语气': '🌸', '深夜语气': '🌙',
  }
  return map[tag] || '🏷️'
}

/** 采样参数的中文说明 */
export function describeSampling(config: PersonaSamplingConfig): string {
  const parts: string[] = []
  if (config.temperature <= 0.4) parts.push('精确稳定')
  else if (config.temperature <= 0.7) parts.push('均衡自然')
  else if (config.temperature <= 1.0) parts.push('灵活多变')
  else parts.push('高度创意')

  if (config.presencePenalty >= 0.5) parts.push('话题丰富')
  if (config.frequencyPenalty >= 0.4) parts.push('表达多样')
  if (config.maxTokens >= 1000) parts.push('详尽回复')
  else if (config.maxTokens <= 500) parts.push('简洁回复')

  return parts.join(' · ')
}

// ── LLM 辅助分析（可选）──────────────────────

export function buildLLMAnalysisPrompt(systemPrompt: string, availableEngines: { id: string; name: string; description: string }[]): string {
  const engineList = availableEngines.map(e => `- ${e.id}: ${e.name} — ${e.description}`).join('\n')
  return `根据以下 AI 助手人设，选出最适合的搜索引擎（4-6个）并建议模型采样参数。

人设：${systemPrompt}

可用引擎：${engineList}

输出 JSON：{"recommendedEngines":[],"engineReasons":{},"sampling":{"temperature":0.7,"topP":0.9,"presencePenalty":0.3,"frequencyPenalty":0.3,"maxTokens":800},"concurrency":2,"enableSearch":true,"enableTimeRange":false}`
}

export function parseLLMAnalysisResponse(llmResponse: string, originalTags: string[]): PersonaAnalysis {
  try {
    const jsonMatch = llmResponse.match(/\{[\s\S]*\}/)
    if (!jsonMatch) throw new Error('No JSON')
    const data = JSON.parse(jsonMatch[0])
    return {
      tags: originalTags,
      scenarios: [],
      recommendedEngines: data.recommendedEngines || ['duckduckgo', 'baidu'],
      engineReasons: data.engineReasons || {},
      concurrency: data.concurrency || 2,
      enableSearch: data.enableSearch !== false,
      enableTimeRange: data.enableTimeRange || false,
      sampling: data.sampling || inferSampling(originalTags),
    }
  } catch {
    return analyzePersona(originalTags.join(' '))
  }
}
