/**
 * Bridge — 连接 UI 和 Agent Core
 * UI 调用 bridge.chat() → Agent.sendMessage() → StreamEvent
 */
import { Agent } from './agent/core/agent'
import type { AgentConfig, AgentEvent } from './shared/agent-types'

// UI 侧的事件类型（ChatView 消费）
type StreamEvent =
  | { type: 'text_delta'; data: string }
  | { type: 'thinking'; data: string }
  | { type: 'tool_use'; data: { tool: string; input: any } }
  | { type: 'tool_result'; data: { tool: string; output: string } }
  | { type: 'error'; data: string }
  | { type: 'done'; data?: any }

class Bridge {
  private agent: Agent | null = null
  private _abortController: AbortController | null = null

  async init(config: any, toolRegistry?: any) {
    try {
      // UI config → AgentConfig 映射
      const agentConfig: AgentConfig = {
        model: {
          provider: config.model?.provider ?? 'openai',
          model: config.model?.model ?? 'gpt-4o',
          apiKey: config.model?.apiKey ?? '',
          apiHost: config.model?.apiHost ?? config.model?.baseUrl,
          baseUrl: config.model?.baseUrl,
          temperature: config.model?.temperature ?? 0.7,
          maxTokens: config.model?.maxTokens ?? 4096,
        },
        persona: config.persona ?? { id: 'default', name: 'Assistant', systemPrompt: 'You are a helpful assistant.', tags: [] },
        provider: null as any, // Agent.resolveProvider() 会自动解析
        memory: { enabled: config.memoryEnabled ?? true, maxEntries: config.maxHistoryTokens ?? 50 },
        memoryEnabled: config.memoryEnabled ?? true,
        maxRounds: config.maxRounds ?? 5,
        maxHistoryTokens: config.maxHistoryTokens ?? 8000,
        searchEnabled: config.searchEnabled ?? false,
        searchEngine: config.searchEngine ?? 'duckduckgo',
        searchApiKey: config.searchApiKey,
      }
      this.agent = new Agent(agentConfig)
      console.log('[bridge] Agent initialized')
    } catch (err: any) {
      console.error('[bridge] init failed:', err)
      this.agent = null
    }
  }

  async *chat(content: string, config?: any): AsyncGenerator<StreamEvent> {
    if (!this.agent) {
      yield { type: 'error', data: 'Agent not initialized. Please configure a model provider first.' }
      return
    }
    this._abortController = new AbortController()
    try {
      for await (const evt of this.agent.sendMessage(content)) {
        if (this._abortController?.signal.aborted) break
        // AgentEvent → StreamEvent 映射
        switch (evt.type) {
          case 'token':
            yield { type: 'text_delta', data: evt.content }
            break
          case 'tool_call':
            yield { type: 'tool_use', data: { tool: evt.name, input: evt.args } }
            break
          case 'tool_result':
            yield { type: 'tool_result', data: { tool: evt.name, output: evt.content } }
            break
          case 'error':
            yield { type: 'error', data: evt.error }
            break
          case 'done':
            yield { type: 'done' }
            break
        }
      }
    } catch (err: any) {
      yield { type: 'error', data: err.message || 'Agent error' }
    }
  }

  abort() {
    this._abortController?.abort()
    this._abortController = null
  }
}

export const bridge = new Bridge()
