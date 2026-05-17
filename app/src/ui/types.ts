// Re-export agent types as the canonical source
export type {
  Message,
  ToolCall,
  ToolResult,
  Persona,
  ModelConfig,
  AgentConfig,
  AgentEvent,
  MemoryEntry,
  ToolDefinition,
} from '../shared/agent-types'

// UI-extended types
import type { Message as _MSG } from "../shared/agent-types"

export interface UIToolCall {
  id: string
  tool: string
  input: any
  output?: string
  status: "running" | "done" | "error"
}

export interface UIMessage extends _MSG {
  thinking?: ThinkingBlock[]
  error?: string
  toolCalls?: UIToolCall[]
}

// UI-specific display types
export interface ThinkingBlock {
  id: string
  content: string
  collapsed: boolean
}

export type ThinkingMap = Record<string, ThinkingBlock[]>

// UI AgentConfig extends the base with display preferences
import type { AgentConfig as BaseAgentConfig, Persona } from '../shared/agent-types'

export interface AppSettings {
  // Agent config (passed to bridge)
  model: {
    provider: string
    model: string
    apiKey: string
    baseUrl: string
    apiHost: string
    temperature: number
    maxTokens: number
  }
  persona: Persona
  searchEnabled: boolean
  searchEngine: string
  searchApiKey: string
  channel: string
  maxRounds: number
  memoryEnabled: boolean
  maxHistoryTokens: number
  // UI display prefs (not passed to agent)
  showThinking: boolean
  showToolCalls: boolean
  showMemoryHints: boolean
  showSearchSources: boolean
  showErrorDetails: boolean
}

