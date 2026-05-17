// Agent 核心类型 — 独立文件，不与 chatbox 原有类型冲突
// 所有类型自包含，不从 chatbox types 导入

export interface ToolCall {
  id: string
  type: 'function'
  function: { name: string; arguments: string }
}

export interface Message {
  id?: string
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
  apiKey?: string
  apiHost?: string
  baseUrl?: string
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
  emoji?: string
  tags: string[]
  isDefault?: boolean
  builtIn?: boolean
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
  searchEngine: 'tavily' | 'duckduckgo' | 'baidu' | string
  searchApiKey?: string
  searchMaxResults?: number
  searchWithSummary?: boolean
  theme?: 'light' | 'dark' | 'auto' | string
  fontSize?: number | string
  language?: string
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
/**
 * 模型供应商类型定义
 * 数据来源：hermes-backend hermes_cli/providers.py + agent/models_dev.py
 * 提取自 109+ models.dev 供应商 + hermes 自有覆盖层
 */

/** 传输协议 */
export type TransportType =
  | 'openai_chat'        // OpenAI Chat Completions 兼容
  | 'anthropic_messages' // Anthropic Messages API
  | 'codex_responses'    // Codex Responses API (OpenAI Codex / xAI)

/** 认证类型 */
export type AuthType =
  | 'api_key'            // 标准 API Key
  | 'oauth_device_code'  // OAuth 设备码流程
  | 'oauth_external'     // OAuth 外部认证
  | 'external_process'   // 外部进程认证（如 GitHub Copilot）
  | 'aws_sdk'            // AWS SDK 认证（Bedrock）

/** 供应商定义 */
export interface ProviderDef {
  /** 唯一标识符（小写、连字符分隔） */
  id: string
  /** 显示名称 */
  name: string
  /** 传输协议，默认 openai_chat */
  transport: TransportType
  /** API Key 环境变量列表（任一即可） */
  apiKeyEnvVars: string[]
  /** 基础 URL（部分供应商有默认值） */
  baseUrl?: string
  /** 基础 URL 环境变量（允许用户自定义） */
  baseUrlEnvVar?: string
  /** 认证类型，默认 api_key */
  authType: AuthType
  /** 是否为聚合器（如 OpenRouter、HuggingFace） */
  isAggregator: boolean
  /** 别名列表（用于模糊匹配用户输入） */
  aliases: string[]
  /** 文档 URL */
  doc?: string
  /** 数据来源 */
  source: 'hermes' | 'models-dev' | 'user-config'
}

