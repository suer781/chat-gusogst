import type { ProviderAdapter, ModelConfig, Message, ToolDefinition } from '../../shared/types'

export class AnthropicProvider implements ProviderAdapter {
  name = 'anthropic'

  private getEndpoint(config: ModelConfig): string {
    const host = (config.apiHost || 'https://api.anthropic.com').replace(/\/$/, '')
    return host + '/v1/messages'
  }

  private convertMessages(messages: Message[]): { system?: string; messages: any[] } {
    let system: string | undefined
    const converted: any[] = []

    for (const msg of messages) {
      if (msg.role === 'system') {
        system = (system ? system + '\n' : '') + msg.content
        continue
      }
      if (msg.role === 'tool') {
        converted.push({
          role: 'user',
          content: [{
            type: 'tool_result',
            tool_use_id: msg.tool_call_id,
            content: msg.content,
          }],
        })
        continue
      }
      if (msg.role === 'assistant' && msg.tool_calls) {
        const content: any[] = []
        if (msg.content) content.push({ type: 'text', text: msg.content })
        for (const tc of msg.tool_calls) {
          let parsed: any
          try { parsed = typeof tc.function.arguments === 'string' ? JSON.parse(tc.function.arguments) : tc.function.arguments }
          catch { parsed = {} }
          content.push({ type: 'tool_use', id: tc.id, name: tc.function.name, input: parsed })
        }
        converted.push({ role: 'assistant', content })
        continue
      }
      converted.push({ role: msg.role, content: msg.content })
    }

    return { system, messages: converted }
  }

  private convertTools(tools?: ToolDefinition[]): any[] | undefined {
    if (!tools || tools.length === 0) return undefined
    return tools.map(t => ({
      name: t.function.name,
      description: t.function.description,
      input_schema: t.function.parameters,
    }))
  }

  async chat(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): Promise<Message> {
    const { system, messages: convertedMsgs } = this.convertMessages(messages)
    const body: any = {
      model: config.model,
      messages: convertedMsgs,
      max_tokens: config.maxTokens || 4096,
    }
    if (system) body.system = system
    if (config.temperature != null) body.temperature = config.temperature
    const convertedTools = this.convertTools(tools)
    if (convertedTools) body.tools = convertedTools

    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 120000)
    try {
      const resp = await fetch(this.getEndpoint(config), {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          'x-api-key': config.apiKey || '',
          'anthropic-version': '2023-06-01',
        } as any,
        body: JSON.stringify(body),
      })
    } catch (e) {
      if (controller.signal.aborted) throw new Error('Request timeout after 120000ms')
      throw e
    } finally {
      clearTimeout(timeoutId)
    }
    if (!resp.ok) {
      const err = await resp.text().catch(() => '')
      throw new Error('Anthropic error ' + resp.status + ': ' + err)
    }
    const data = await resp.json()

    const result: Message = {
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
    }

    const contentBlocks = data.content || []
    const toolCalls: any[] = []

    for (const block of contentBlocks) {
      if (block.type === 'text') {
        result.content += block.text
      } else if (block.type === 'tool_use') {
        toolCalls.push({
          id: block.id,
          type: 'function',
          function: {
            name: block.name,
            arguments: JSON.stringify(block.input),
          },
        })
      }
    }

    if (toolCalls.length > 0) result.tool_calls = toolCalls
    return result
  }

  async *chatStream(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): AsyncGenerator<string> {
    // Anthropic streaming is complex, use non-streaming for now
    const result = await this.chat(messages, config, tools)
    if (result.content) yield result.content
  }
}
