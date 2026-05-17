export type ToolCall = { id: string; tool: string; input: any; output?: string; status: 'running' | 'done' | 'error' }

export type ThinkingBlock = { id: string; content: string; collapsed: boolean }

export type Message = {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp?: number
  thinking?: ThinkingBlock[]
  toolCalls?: ToolCall[]
  error?: string
}

export type Persona = {
  id: string; name: string; systemPrompt: string; tags: string[]
  emoji?: string; personality?: Record<string, number>
  modelParamsConfig?: { mode: 'rule' | 'llm' | 'off'; manual?: { temperature?: number; topP?: number; maxTokens?: number } }
}

export type AgentConfig = {
  model: { provider: string; model: string; apiKey: string; baseUrl: string; apiHost: string; temperature: number; maxTokens: number }
  persona: Persona; searchEnabled: boolean; searchEngine?: string; searchApiKey?: string
  channel: string; maxRounds: number; memoryEnabled: boolean; maxHistoryTokens: number
  showThinking: boolean; showToolCalls: boolean; showMemoryHints: boolean
  showSearchSources: boolean; showErrorDetails: boolean
}
