export type Message = { id: string; role: 'user' | 'assistant' | 'system'; content: string; timestamp?: number }
export type Persona = { id: string; name: string; systemPrompt: string; tags: string[] }
export type AgentConfig = {
  model: { provider: string; model: string; apiKey: string; baseUrl: string; apiHost: string; temperature: number; maxTokens: number }
  persona: Persona; searchEnabled: boolean; searchEngine?: string; searchApiKey?: string
  channel: string; maxRounds: number; memoryEnabled: boolean; maxHistoryTokens: number
}
