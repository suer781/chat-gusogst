/**
 * Anthropic Claude Provider
 */
import type { Message, ModelConfig, ProviderAdapter, ToolDefinition } from '../../shared/types'

export class AnthropicProvider implements ProviderAdapter {
  readonly name: string
  readonly defaultHost: string

  constructor(name = 'anthropic', defaultHost = 'https://api.anthropic.com') {
    this.name = name
    this.defaultHost = defaultHost
  }

  private getEndpoint(config: ModelConfig): string {
    const host = (config.apiHost || this.defaultHost).replace(/\/+$/, '')
    return `${host}/v1/messages`
  }

  private convertMessages(messages: Message[]): { system?: string; messages: unknown[] } {
    let system: string | undefined
    const converted: unknown[] = []
    for (const m of messages) {
      if (m.role === 'system') {
        system = m.content ?? undefined
        continue
      }
      if (m.role === 'tool') {
        converted.push({
          role: 'user',
          content: [{ type: 'tool_result', tool_use_id: m.tool_call_id, content: m.content }],
        })
        continue
      }
      const msg: Record<string, unknown> = { role: m.role }
      if (m.tool_calls && m.tool_calls.length > 0) {
        const parts: unknown[] = []
        if (m.content) parts.push({ type: 'text', text: m.content })
        for (const tc of m.tool_calls) {
          parts.push({
            type: 'tool_use', id: tc.id, name: tc.function.name,
            input: JSON.parse(tc.function.arguments),
          })
        }
        msg.content = parts
      } else {
        msg.content = m.content
      }
      converted.push(msg)
    }
    return { system, messages: converted }
  }

  private convertTools(tools?: ToolDefinition[]): unknown[] | undefined {
    if (!tools?.length) return undefined
    return tools.map(t => ({
      name: t.function.name,
      description: t.function.description,
      input_schema: t.function.parameters,
    }))
  }

  async chat(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): Promise<Message> {
    const { system, messages: msgs } = this.convertMessages(messages)
    const body: Record<string, unknown> = {
      model: config.model,
      messages: msgs,
      max_tokens: config.maxTokens ?? 8192,
    }
    if (system) body.system = system
    if (config.temperature != null) body.temperature = config.temperature
    const convertedTools = this.convertTools(tools)
    if (convertedTools) body.tools = convertedTools

    const resp = await fetch(this.getEndpoint(config), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-api-key': config.apiKey,
        'anthropic-version': '2023-06-01',
      },
      body: JSON.stringify(body),
    })
    if (!resp.ok) throw new Error(`[anthropic] ${resp.status}: ${await resp.text()}`)
    const data = await resp.json()
    const contentBlocks = data.content || []
    let text = ''
    const toolCalls: Message['tool_calls'] = []
    for (const block of contentBlocks) {
      if (block.type === 'text') text += block.text
      if (block.type === 'tool_use') {
        toolCalls!.push({
          id: block.id, type: 'function',
          function: { name: block.name, arguments: JSON.stringify(block.input) },
        })
      }
    }
    return { role: 'assistant', content: text || null, tool_calls: toolCalls?.length ? toolCalls : undefined, timestamp: Date.now() }
  }

  async *chatStream(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): AsyncGenerator<string> {
    const endpoint = this.getEndpoint(config)
    const body: Record<string, unknown> = {
      model: config.model,
      max_tokens: config.maxTokens ?? 4096,
      messages: this.convertMessages(messages),
      stream: true
    }
    if (config.temperature !== undefined) body.temperature = config.temperature
    if (config.systemPrompt) body.system = config.systemPrompt
    if (tools && tools.length > 0) body.tools = this.convertTools(tools)

    const headers: Record<string, string> = { 'Content-Type': 'application/json', 'anthropic-version': '2023-06-01' }
    if (config.apiKey) headers['x-api-key'] = config.apiKey

    const resp = await fetch(endpoint, { method: 'POST', headers, body: JSON.stringify(body) })
    if (!resp.ok) throw new Error(`Anthropic stream error ${resp.status}: ${await resp.text()}`)
    if (!resp.body) throw new Error('No response body for streaming')

    const reader = resp.body.getReader()
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
          if (!line.startsWith('data: ')) continue
          const data = line.slice(6).trim()
          if (data === '[DONE]') return
          try {
            const evt = JSON.parse(data)
            if (evt.type === 'content_block_delta' && evt.delta?.type === 'text_delta') {
              yield evt.delta.text
            }
          } catch { /* skip malformed SSE lines */ }
        }
      }
    } finally {
      reader.releaseLock()
    }
  }
}
