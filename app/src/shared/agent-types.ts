// Agent 核心类型 — 独立文件，不与 chatbox 原有类型冲突
// 所有类型自包含，不从 chatbox types 导入

export interface ToolCall {
  id: string
  type: 'function'
  function: { name: string; arguments: string }
}

export interface Message {
  role: 'system' | 'user' | 'assistant' | 'tool'
  content: string | null
  tool_calls?: ToolCall[]
  tool_call_id?: string
  name?: string
  timestamp?: number
}

export interface ToolResult {
  tool_call_id: string
  name: string
  content: string
  is_error?: boolean
}

export interface ModelConfig {
  provider: string
  model: string
  apiKey: string
  apiHost?: string
  temperature?: number
  maxTokens?: number
  topP?: number
  contextWindow?: number
}

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

export interface MemoryEntry {
  id: string
  content: string
  type: 'conversation' | 'fact' | 'preference' | 'emotion'
  importance: number
  created_at: string
  last_accessed: string
  access_count: number
  tags: string[]
  embedding?: number[]
}

export interface Persona {
  id: string
  name: string
  systemPrompt: string
  avatar?: string
  tags: string[]
  isDefault?: boolean
}

export interface AgentConfig {
  model: ModelConfig
  persona: Persona
  provider: ProviderAdapter
  memory: { enabled: boolean; maxEntries: number }
  memoryEnabled: boolean
  maxRounds: number
  maxHistoryTokens: number
  searchEnabled: boolean
  searchEngine: 'tavily' | 'duckduckgo' | 'baidu'
  searchApiKey?: string
}

export type AgentEvent =
  | { type: 'token'; content: string }
  | { type: 'message'; content: Message }
  | { type: 'tool_start'; toolCalls: ToolCall[] }
  | { type: 'tool_call'; name: string; args: Record<string, unknown> }
  | { type: 'tool_result'; name: string; content: string }
  | { type: 'done'; message: Message }
  | { type: 'error'; error: string }
  | { type: 'memory_saved'; count: number }
