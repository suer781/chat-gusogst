/**
 * OpenAI 兼容 Provider
 * 支持: OpenAI, Deepseek, Groq, Together, 硅基流动, 自定义端点
 */
import type { Message, ModelConfig, ProviderAdapter, ToolDefinition } from '../../shared/types'

export class OpenAIProvider implements ProviderAdapter {
  readonly displayName = 'OpenAI'
  readonly apiMode = 'chat_completions' as const
  readonly authType = 'api_key' as const
  readonly name: string
  readonly baseUrl: string

  constructor(name = 'openai', baseUrl = 'https://api.openai.com') {
    this.name = name
    this.baseUrl = baseUrl
  }
  _lastStreamToolCalls?: Array<{id: string; type: string; function: {name: string; arguments: string}}>

  private getEndpoint(config: ModelConfig): string {
    const host = (config.apiHost || this.baseUrl).replace(/\/+$/, '')
    return `${host}/v1/chat/completions`
  }

  private buildBody(messages: Message[], config: ModelConfig, tools?: ToolDefinition[], stream = false) {
    const body: Record<string, unknown> = {
      model: config.model,
      messages: messages.map(m => {
        const msg: Record<string, unknown> = { role: m.role, content: m.content }
        if (m.tool_calls) msg.tool_calls = m.tool_calls
        if (m.tool_call_id) msg.tool_call_id = m.tool_call_id
        return msg
      }),
      stream,
    }
    if (config.temperature != null) body.temperature = config.temperature
    if (config.maxTokens != null) body.max_tokens = config.maxTokens
    if (config.topP != null) body.top_p = config.topP
    if (tools && tools.length > 0) body.tools = tools
    return body
  }

  private parseSSELine(line: string): { content?: string; toolCalls?: Array<{index: number; id?: string; type?: string; function?: {name?: string; arguments?: string}}> } | null {
    if (line.startsWith(': ping')) return null
    if (!line.startsWith('data: ')) return null
    const data = line.slice(6).trim()
    if (data === '[DONE]') return null
    try {
      const json = JSON.parse(data)
      const delta = json.choices?.[0]?.delta
      if (!delta) return null
      const result: any = {}
      if (delta.content) result.content = delta.content
      if (delta.tool_calls) result.toolCalls = delta.tool_calls
      return Object.keys(result).length > 0 ? result : null
    } catch { return null }
  }
  async chat(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): Promise<Message> {
    const resp = await fetch(this.getEndpoint(config), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${config.apiKey}`,
      },
      body: JSON.stringify(this.buildBody(messages, config, tools, false)),
    })
    if (!resp.ok) {
      const err = await resp.text()
      throw new Error(`[${this.name}] ${resp.status}: ${err}`)
    }
    const data = await resp.json()
    const choice = data.choices?.[0]
    if (!choice) throw new Error('No choice in response')
    return {
      id: crypto.randomUUID(),
          role: 'assistant',
      content: choice.message?.content ?? null,
      tool_calls: choice.message?.tool_calls,
      timestamp: Date.now(),
    }
  }

  async *chatStream(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): AsyncGenerator<string> {
    const toolCallsMap: Map<number, any> = new Map()
    this._lastStreamToolCalls = undefined

    const resp = await fetch(this.getEndpoint(config), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${config.apiKey}`,
      },
      body: JSON.stringify(this.buildBody(messages, config, tools, true)),
    })
    if (!resp.ok) {
      const err = await resp.text()
      throw new Error(`[${this.name}] ${resp.status}: ${err}`)
    }
    const reader = resp.body?.getReader()
    if (!reader) throw new Error('No response body')
    const decoder = new TextDecoder()
    let buffer = ''
    try {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''
        for (const line of lines) {
          const parsed = this.parseSSELine(line)
          if (!parsed) continue
          if (parsed.content) yield parsed.content
          if (parsed.toolCalls) {
            for (const tc of parsed.toolCalls) {
              const existing = toolCallsMap.get(tc.index) ?? {} as any
              if (tc.id) existing.id = tc.id
              if (tc.type) existing.type = tc.type
              if (tc.function) {
                existing.function = existing.function ?? {} as any
                if (tc.function.name) existing.function.name = (existing.function.name ?? '') + tc.function.name
                if (tc.function.arguments) existing.function.arguments = (existing.function.arguments ?? '') + tc.function.arguments
              }
              toolCallsMap.set(tc.index, existing)
            }
          }
        }
      }
    } finally {
      reader.releaseLock()
    }

    if (toolCallsMap.size > 0) {
      this._lastStreamToolCalls = [...toolCallsMap.entries()].sort(([a],[b]) => a-b).map(([, tc]) => ({
        id: tc.id ?? '',
        type: tc.type ?? 'function',
        function: { name: tc.function?.name ?? '', arguments: tc.function?.arguments ?? '{}' }
      }))
    }
  }
}
