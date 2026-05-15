// ── 消息 ─────────────────────────────────────────
export interface Message {
  role: 'system' | 'user' | 'assistant' | 'tool'
  content: string | null
  tool_calls?: ToolCall[]
  tool_call_id?: string
  name?: string
  timestamp?: number
}

export interface ToolCall {
  id: string
  type: 'function'
  function: { name: string; arguments: string }
}

export interface ToolResult {
  tool_call_id: string
  name: string
  content: string
  is_error?: boolean
}

// ── 模型 ─────────────────────────────────────────
export interface ModelConfig {
  provider: string
  model: string
  apiKey: string
  apiHost?: string
  temperature?: number
  maxTokens?: number
  systemPrompt?: string
  topP?: number
  contextWindow?: number
}

// ── Provider ────────────────────────────────────
export interface ProviderAdapter {
  readonly name: string
  chat(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): Promise<Message>
  chatStream(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): AsyncGenerator<string>
}

export interface ToolDefinition {
  type: 'function'
  function: {
    name: string
    description: string
    parameters: Record<string, unknown>
  }
}


// ── 模型采样配置 ────────────────────────────────
export interface PersonaSamplingConfig {
  temperature: number
  topP: number
  presencePenalty: number
  frequencyPenalty: number
  maxTokens: number
}

// ── 人设搜索配置 ────────────────────────────────
export interface PersonaSearchConfig {
  engines: string[]
  engineWeights?: Record<string, number>
  concurrency?: number
  enableTimeRange?: boolean
  enableSearch?: boolean
}

// ── 渠道 ─────────────────────────────────────────
export type Channel = 'app' | 'wechat' | 'qq' | 'feishu' | 'custom'

export interface ChannelStyle {
  id: Channel
  name: string
  /** 拼进 system prompt 的风格指令 */
  instruction: string
}

// ── 人设 ─────────────────────────────────────────
export interface Persona {
  id: string
  name: string
  systemPrompt: string
  avatar?: string
  tags: string[]
  isDefault?: boolean
  /** 搜索引擎配置 */
  searchConfig?: PersonaSearchConfig
  /** 模型采样配置（温度、topP 等） */
  samplingConfig?: PersonaSamplingConfig
  /** AI 智能分析开关，true=自动分析，false=手动 */
  autoAnalyzeSearch?: boolean
}

// ── Agent 配置 ───────────────────────────────────
export interface AgentConfig {
  model: ModelConfig
  persona: Persona
  memoryEnabled: boolean
  maxRounds: number
  maxHistoryTokens: number
  searchEnabled: boolean
  searchEngine: 'tavily' | 'duckduckgo' | 'baidu' | 'bing' | 'quark' | 'brave'
  searchApiKey?: string
  /** 当前对话渠道，决定输出风格 */
  channel?: Channel
}

// ── Agent 事件 ───────────────────────────────────
export type AgentEvent =
  | { type: 'token'; content: string }
  | { type: 'tool_call'; name: string; args: Record<string, unknown> }
  | { type: 'tool_result'; name: string; content: string }
  | { type: 'done'; message: Message }
  | { type: 'error'; error: string }
  | { type: 'memory_saved'; count: number }
