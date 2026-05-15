// ============================================
// 核心数据类型定义
// ============================================

// ── 消息 ─────────────────────────────────

export type MessageRole = 'system' | 'user' | 'assistant' | 'tool'

export interface Message {
  id: string
  role: MessageRole
  content: string
  timestamp: number
  /** 工具调用（assistant 发出） */
  tool_calls?: ToolCall[]
  /** 工具调用 ID（tool role 用） */
  tool_call_id?: string
  /** 搜索来源 */
  sources?: SearchResult[]
  /** 人设 ID */
  personaId?: string
}

export interface ToolCall {
  id: string
  type: 'function'
  function: {
    name: string
    arguments: string
  }
}

// ── 搜索结果 ──────────────────────────────

export interface SearchResult {
  title: string
  url: string
  snippet: string
  engine: string
  score?: number
  publishedDate?: string
}

// ── 模型配置 ──────────────────────────────

export type ModelProvider = 'openai' | 'anthropic' | 'custom' | 'ollama'

export interface ModelConfig {
  provider: ModelProvider
  model: string
  apiKey: string
  baseUrl: string
  /** API 主机（用于 providers 调用） */
  apiHost?: string
  temperature?: number
  topP?: number
  maxTokens?: number
  /** 完整系统提示词（用户自由填写） */
  systemPrompt?: string
}

// ── 渠道风格 ──────────────────────────────

export type ChannelId = 'app' | 'wechat' | 'qq' | 'telegram' | 'feishu' | 'custom'

export interface ChannelStyle {
  id: ChannelId
  name: string
  instruction: string
  /** 是否精简回复（适合 IM） */
  compact: boolean
  /** 消息长度限制 */
  maxLength?: number
  /** 是否启用 emoji */
  enableEmoji?: boolean
}

// ── 搜索配置 ──────────────────────────────

/** 人设专属搜索配置 */
export interface PersonaSearchConfig {
  /** 推荐的引擎 ID 列表 */
  engines: string[]
  /** 引擎权重 { engineId: weight } */
  engineWeights?: Record<string, number>
  /** 最大并发数 */
  concurrency?: number
  /** 是否启用时间范围 */
  enableTimeRange?: boolean
  /** 是否启用搜索 */
  enableSearch?: boolean
}

// ── 采样配置 ──────────────────────────────

/** 人设专属采样配置 */
export interface PersonaSamplingConfig {
  temperature: number
  topP: number
  presencePenalty: number
  frequencyPenalty: number
  maxTokens: number
}

// ── 结构化人设档案（自动优化产出） ──────────

/** 自动优化提取的结构化人设字段 */
export interface PersonaStructuredProfile {
  /** 语气风格：温柔、傲娇、元气、冷酷、理性、幽默、文艺、深夜 等 */
  tone: string
  /** 性格特征：体贴、活泼、毒舌、成熟、可爱 等 */
  personality: string[]
  /** 口头禅 / 常用表达（数组） */
  verbalHabits: string[]
  /** 说话风格：简短直接、长句抒情、emoji丰富、口语化 等 */
  speakingStyle: string
  /** 关系设定：恋人、朋友、老师、助手 等 */
  relationship: string
  /** 场景：日常陪伴、深夜谈心、学习辅导、情绪安慰 等 */
  scenario: string
  /** 称呼方式：亲爱的、宝贝、主人、大佬 等 */
  nickname: string
  /** 用户称呼：对方怎么称呼用户 */
  userNickname: string
  /** 禁忌：不要做什么 */
  restrictions: string[]
}

// ── 工具定义 ──────────────────────────────

export interface ToolParameter {
  name: string
  type: 'string' | 'number' | 'boolean' | 'object' | 'array'
  description: string
  required: boolean
  default?: any
  enum?: any[]
}

export interface ToolDefinition {
  name: string
  description: string
  parameters: ToolParameter[]
  execute: (args: Record<string, any>) => Promise<any>
}

// ── Provider 适配器 ───────────────────────

export interface ProviderAdapter {
  name: string
  displayName: string
  description?: string
  apiMode: 'chat_completions' | 'responses' | 'anthropic_messages' | 'gemini' | 'baidu_ernie' | 'dashscope' | 'ollama_chat' | 'ollama_generate' | 'hf_inference' | 'openai_compatible'
  baseUrl: string
  modelsUrl?: string
  authType: 'api_key' | 'oauth_device_code' | 'none'
  envVars?: string[]
  defaultHeaders?: Record<string, string>
  fallbackModels?: string[]
  defaultMaxTokens?: number
  fixedTemperature?: number
  supportsHealthCheck?: boolean
  supportsStreaming?: boolean
  supportsToolCalling?: boolean
  supportsVision?: boolean
  signupUrl?: string
  aliases?: string[]
}

// ── 记忆条目 ──────────────────────────────

export type MemoryCategory = 'personal' | 'preference' | 'event' | 'relationship'

export interface MemoryEntry {
  id: string
  content: string
  category: MemoryCategory
  confidence: number
  sourceMessageId?: string
  sourceRole?: 'user' | 'assistant'
  confirmedCount: number
  contradictedCount: number
  createdAt: number
  lastAccessed: number
  /** 标签 */
  tags?: string[]
  /** 情感分数 (-1 ~ 1) */
  sentiment?: number
  /** 重要性 (0 ~ 1) */
  importance?: number
}

// ── 人设 ──────────────────────────────────

export interface Persona {
  id: string
  name: string
  /** 完整系统提示词（用户自由填写，自动优化的输入源） */
  systemPrompt: string
  avatar?: string
  tags: string[]
  isDefault?: boolean
  /** 搜索引擎配置（自动优化产出或手动设置） */
  searchConfig?: PersonaSearchConfig
  /** 采样参数配置（自动优化产出或手动设置） */
  samplingConfig?: PersonaSamplingConfig
  /** 结构化人设档案（自动优化产出，用于 UI 展示和手动微调） */
  structured?: PersonaStructuredProfile
  /** AI 智能分析开关：true=自动优化，false=手动 */
  autoAnalyzeSearch?: boolean
}

// ── Agent 配置 ─────────────────────────────

export interface AgentConfig {
  model: ModelConfig
  persona: Persona
  searchEnabled: boolean
  searchEngine?: 'tavily' | 'duckduckgo' | 'baidu' | 'bing' | 'quark' | 'brave'
  searchApiKey?: string
  /** 当前对话渠道 */
  channel?: ChannelId
  /** 最大轮次（防止无限循环） */
  maxRounds?: number
  /** 是否启用记忆 */
  memoryEnabled?: boolean
  /** 最大历史 token 数 */
  maxHistoryTokens?: number
}

// ── 事件 ──────────────────────────────────

export type AgentEventType =
  | 'token'
  | 'tool_call'
  | 'tool_result'
  | 'done'
  | 'error'
  | 'search_start'
  | 'search_done'

export interface AgentEvent {
  type: AgentEventType
  data: any
  /** 用于 token 事件 */
  content?: string
  /** 用于 error 事件 */
  error?: string
  /** 用于 tool_call 事件 */
  name?: string
  /** 用于 tool_result 事件 */
  message?: string
}

// ── 对话 ──────────────────────────────────

export interface Conversation {
  id: string
  title: string
  personaId: string
  messages: Message[]
  createdAt: number
  updatedAt: number
}

// ── 模型预设 ──────────────────────────────

export interface ModelPreset {
  id: string
  name: string
  provider: ModelProvider
  model: string
  baseUrl: string
  description: string
  /** 是否需要 API key */
  needsKey: boolean
  /** 默认采样参数 */
  defaultSampling?: Partial<PersonaSamplingConfig>
}

// ── 主题 ──────────────────────────────────

export type ThemeMode = 'light' | 'dark' | 'auto'

export interface ThemeConfig {
  mode: ThemeMode
  primaryColor: string
  fontSize: 'small' | 'medium' | 'large'
}

// ── 全局设置 ──────────────────────────────

export interface AppSettings {
  theme: ThemeConfig
  model: ModelConfig
  activePersonaId: string
  searchEnabled: boolean
  channel: ChannelId
  /** 是否首次启动 */
  firstLaunch: boolean
  /** 自动优化默认开启 */
  autoOptimizeDefault: boolean
}
