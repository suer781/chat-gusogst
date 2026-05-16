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
  provider: string       // openai | anthropic | gemini | custom
  model: string          // gpt-4o | claude-3.5-sonnet | ...
  apiKey: string
  apiHost?: string
  temperature?: number
  maxTokens?: number
  topP?: number
  contextWindow?: number
}

// ── Provider（模型适配器）────────────────────────
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

// ── 记忆 ─────────────────────────────────────────
export interface MemoryEntry {
  id: string
  content: string
  type: 'conversation' | 'fact' | 'preference' | 'emotion'
  importance: number       // 0-1
  created_at: string
  last_accessed: string
  access_count: number
  tags: string[]
  embedding?: number[]
}

// ── 人设 ─────────────────────────────────────────
export interface Persona {
  id: string
  name: string
  systemPrompt: string
  avatar?: string
  tags: string[]
  isDefault?: boolean
}

// ── Agent 配置 ───────────────────────────────────
export interface AgentConfig {
  model: ModelConfig
  persona: Persona
  memoryEnabled: boolean
  maxRounds: number         // 最大 tool call 轮次
  maxHistoryTokens: number  // 历史上限
  searchEnabled: boolean
  searchEngine: 'tavily' | 'duckduckgo' | 'baidu'
  searchApiKey?: string
}

// ── Agent 事件 ───────────────────────────────────
export type AgentEvent =
  | { type: 'token'; content: string }
  | { type: 'tool_call'; name: string; args: Record<string, unknown> }
  | { type: 'tool_result'; name: string; content: string }
  | { type: 'done'; message: Message }
  | { type: 'error'; error: string }
  | { type: 'memory_saved'; count: number }
