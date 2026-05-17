// Canonical type definitions for the Agent system
// Ground truth: agent/core/agent.ts uses these types
// All modules should import from here, never redefine locally

// --- Message (OpenAI format) ---
export interface ToolCall {
  id: string
  type: 'function'
  function: { name: string; arguments: string }
}

export interface Message {
  id?: string
  role: 'system' | 'user' | 'assistant' | 'tool'
  content: string
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

// --- Model & Config ---
export interface ModelConfig {
  provider: string
  model: string
  apiKey: string
  apiHost?: string
  baseUrl?: string
  temperature?: number
  maxTokens?: number
  topP?: number
}

export interface SearchConfig {
  engine: 'tavily' | 'duckduckgo' | 'auto'
  tavilyApiKey?: string
}

export interface MemoryConfig {
  enabled: boolean
}

export interface MCPServerConfig {
  name: string
  url: string
  headers?: Record<string, string>
  enabled?: boolean
  timeout?: number
}

export interface AgentConfig {
  model: ModelConfig
  persona?: Persona
  provider?: ProviderAdapter
  memory?: MemoryConfig
  mcpServers?: MCPServerConfig[]
  search?: SearchConfig
  maxHistoryTokens?: number
}

// --- Agent Events ---
export type AgentEvent =
  | { type: 'token'; content: string }
  | { type: 'thinking'; content: string }
  | { type: 'tool_call'; id: string; name: string; arguments: any }
  | { type: 'tool_result'; id: string; name: string; result: string }
  | { type: 'error'; error: string }
  | { type: 'done'; message: Message }

// --- Provider ---
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

// --- Memory ---
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

// --- Persona ---
export interface Persona {
  id: string
  name: string
  systemPrompt: string
  avatar?: string
  emoji?: string
  tags: string[]
  isDefault?: boolean
  builtIn?: boolean
  personality?: Record<string, any>
  modelParamsConfig?: Record<string, any>
}

// --- Provider Definitions (for settings UI) ---
export type TransportType = 'openai_chat' | 'anthropic_messages' | 'codex_responses'
export type AuthType = 'api_key' | 'oauth_device_code' | 'oauth_external' | 'external_process' | 'aws_sdk'

export interface ProviderDef {
  id: string
  name: string
  transport: TransportType
  apiKeyEnvVars: string[]
  baseUrl?: string
  baseUrlEnvVar?: string
  authType: AuthType
  isAggregator: boolean
  aliases: string[]
  doc?: string
  source: 'hermes' | 'models-dev' | 'user-config'
}
