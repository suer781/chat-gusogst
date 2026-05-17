import { Agent } from './agent/core/agent'
import type { AgentConfig, AgentEvent, Message } from './agent/core/agent'
import type { SearchEngine } from './agent/tools/search'

export interface BridgeConfig {
  model: {
    provider: string
    model: string
    apiKey: string
    apiHost?: string
    temperature?: number
    maxTokens?: number
  }
  persona?: {
    name: string
    systemPrompt: string
    avatar?: string
  }
  memory?: {
    enabled: boolean
  }
  search?: {
    engine: SearchEngine
    tavilyApiKey?: string
  }
  mcpServers?: any[]
}

// Match ChatView's expected event format
export type StreamEvent =
  | { type: 'text_delta'; data: string }
  | { type: 'thinking'; data: string }
  | { type: 'tool_use'; data: { tool: string; input: any } }
  | { type: 'tool_result'; data: { tool: string; output: string } }
  | { type: 'error'; data: string }
  | { type: 'done' }

class Bridge {
  private agent: Agent | null = null
  private _abortController: AbortController | null = null

  init(config: BridgeConfig) {
    try {
      const agentConfig: AgentConfig = {
        model: config.model,
        persona: config.persona ? {
          id: 'default',
          name: config.persona.name,
          systemPrompt: config.persona.systemPrompt,
          avatar: config.persona.avatar || '',
          tags: [],
          isDefault: false,
        } : undefined,
        memory: config.memory || { enabled: true },
        search: config.search || { engine: 'auto' },
        mcpServers: config.mcpServers,
      }
      this.agent = new Agent(agentConfig)
      return true
    } catch (e) {
      console.error('Bridge init failed:', e)
      this.agent = null
      return false
    }
  }

  async *chat(content: string, cfg?: any): AsyncGenerator<StreamEvent> {
    // If cfg is passed, re-init with it
    if (cfg && cfg.model) {
      this.init(cfg)
    }

    if (!this.agent) {
      yield { type: 'error', data: 'Agent not initialized' }
      return
    }

    this._abortController = new AbortController()

    try {
      for await (const event of this.agent.sendMessage(content)) {
        if (this._abortController.signal.aborted) break

        switch (event.type) {
          case 'token':
            yield { type: 'text_delta', data: event.content }
            break
          case 'thinking':
            yield { type: 'thinking', data: event.content }
            break
          case 'tool_call':
            yield {
              type: 'tool_use',
              data: {
                tool: event.name,
                input: typeof event.arguments === 'string' ? JSON.parse(event.arguments || '{}') : event.arguments,
              },
            }
            break
          case 'tool_result':
            yield {
              type: 'tool_result',
              data: { tool: event.name, output: event.result },
            }
            break
          case 'error':
            yield { type: 'error', data: event.error }
            break
          case 'done':
            yield { type: 'done' }
            break
        }
      }
    } catch (e: any) {
      if (!this._abortController.signal.aborted) {
        yield { type: 'error', data: e.message }
      }
    } finally {
      this._abortController = null
    }
  }

  abort() {
    this._abortController?.abort()
    this.agent?.abort()
  }

  getAgent() { return this.agent }
}

export const bridge = new Bridge()
