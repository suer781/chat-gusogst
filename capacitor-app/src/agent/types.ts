// Agent 核心类型定义

// ========== Agent 事件流 ==========
export type AgentEventType =
  | 'thinking'      // AI 正在思考
  | 'tool_call'     // 调用工具
  | 'tool_result'   // 工具返回结果
  | 'token'         // 流式文本 token
  | 'memory_saved'  // 记忆已保存
  | 'error'         // 错误
  | 'done'          // 完成

export interface AgentEvent {
  type: AgentEventType
  content?: string
  toolName?: string
  toolArgs?: Record<string, unknown>
  toolResult?: string
  error?: string
  timestamp: number
}

// ========== 工具定义 ==========
export interface ToolParameter {
  type: string
  description: string
  required?: boolean
  enum?: string[]
}

export interface ToolDefinition {
  name: string
  description: string
  parameters: Record<string, ToolParameter>
  execute: (args: Record<string, unknown>) => Promise<string>
}

// ========== 记忆 ==========
export interface MemoryEntry {
  id: string
  type: 'fact' | 'preference' | 'event' | 'relationship'
  content: string
  timestamp: number
  importance: number  // 0-1
  tags: string[]
}

export interface MemorySearchResult {
  entry: MemoryEntry
  score: number
}

// ========== Agent 配置 ==========
export interface AgentConfig {
  maxIterations: number    // 最大循环次数，防止无限循环
  maxTokens: number        // 单次回复最大 token
  temperature: number
  enableMemory: boolean    // 是否启用记忆
  enableTools: boolean     // 是否启用工具
  systemPromptOverride?: string  // 覆盖人设的系统提示词
}

export const DEFAULT_AGENT_CONFIG: AgentConfig = {
  maxIterations: 10,
  maxTokens: 4096,
  temperature: 0.7,
  enableMemory: true,
  enableTools: true,
}

// ========== Agent 状态 ==========
export interface AgentState {
  isRunning: boolean
  iteration: number
  currentTool?: string
  thinkingContent: string
  responseContent: string
}