import { Agent } from './agent/core/agent'
import type { AgentConfig, AgentEvent, MCPServerConfig } from './shared/agent-types'
import type { AppSettings } from './ui/types'

// StreamEvent format that ChatView consumes
export type StreamEvent =
  | { type: 'text_delta'; data: string }
  | { type: 'thinking'; data: string }
  | { type: 'tool_use'; data: { tool: string; input: any } }
  | { type: 'tool_result'; data: { tool: string; output: string; id?: string; isError?: boolean } }
  | { type: 'error'; data: string }
  | { type: 'done'; message?: any }

// Convert AppSettings to AgentConfig for the Agent
function settingsToAgentConfig(s: AppSettings): AgentConfig {
  return {
    model: {
      provider: s.model.provider,
      model: s.model.model,
      apiKey: s.model.apiKey,
      baseUrl: s.model.baseUrl || undefined,
      apiHost: s.model.apiHost || undefined,
      temperature: s.model.temperature,
      maxTokens: s.model.maxTokens,
    },
    persona: s.persona,
    memory: { enabled: s.memoryEnabled },
    search: s.searchEnabled
      ? {
          engine: s.searchEngine as 'tavily' | 'duckduckgo' | 'auto',
          tavilyApiKey: s.searchApiKey || undefined,
        }
      : undefined,
    mcpServers: s.mcpServers?.filter(m => m.enabled !== false) || undefined,
    maxHistoryTokens: s.maxHistoryTokens,
  }
}

class Bridge {
  private agent: Agent | null = null
  private _abortController: AbortController | null = null
  private _initialized = false

  /** Initialize agent from AppSettings */
  init(settings: AppSettings): boolean {
    try {
      const agentConfig = settingsToAgentConfig(settings)
      this.agent = new Agent(agentConfig)
      this._initialized = true
      console.log('[Bridge] Agent initialized:', settings.model.provider + '/' + settings.model.model)
      return true
    } catch (e) {
      console.error('[Bridge] init failed:', e)
      this.agent = null
      this._initialized = false
      return false
    }
  }

  /** Re-init if settings changed (compare key fields) */
  private _lastKey = ''
  ensureInit(settings: AppSettings): boolean {
    const key = [settings.model.provider, settings.model.model, settings.model.apiKey,
      settings.persona?.id, settings.memoryEnabled, settings.searchEnabled,
      JSON.stringify(settings.mcpServers)].join('|')
    if (key !== this._lastKey || !this._initialized) {
      this._lastKey = key
      return this.init(settings)
    }
    return true
  }

  /** Stream chat events */
  async *chat(content: string, settings?: AppSettings): AsyncGenerator<StreamEvent> {
    // Auto-init if settings provided
    if (settings) {
      if (!this.ensureInit(settings)) {
        yield { type: 'error', data: 'Agent initialization failed. Check your API key and model settings.' }
        return
      }
    }

    if (!this.agent) {
      yield { type: 'error', data: 'Agent not initialized. Please configure your model in settings.' }
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
                input: typeof event.arguments === 'string'
                  ? JSON.parse(event.arguments || '{}')
                  : (event.arguments || {}),
              },
            }
            break
          case 'tool_result':
            yield {
              type: 'tool_result',
              data: { tool: event.name, output: event.result || '', id: event.id, isError: (event as any).is_error },
            }
            break
          case 'error':
            yield { type: 'error', data: event.error }
            break
          case 'done':
            yield { type: 'done', message }
            break
        }
      }
    } catch (e: any) {
      if (!this._abortController.signal.aborted) {
        yield { type: 'error', data: e.message || 'Unknown error' }
      }
    } finally {
      this._abortController = null
    }
  }

  abort() {
    this._abortController?.abort()
    this.agent?.abort()
  }

  isReady() { return this._initialized && this.agent !== null }
  getAgent() { return this.agent }
}

export const bridge = new Bridge()
