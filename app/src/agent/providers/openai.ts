import type { ProviderAdapter, ModelConfig, Message, ToolDefinition } from '../../shared/types'

export class OpenAIProvider implements ProviderAdapter {
  name = 'openai'

  private getEndpoint(config: ModelConfig): string {
    const host = (config.apiHost || 'https://api.openai.com').replace(/\/$/, '')
    return host + '/v1/chat/completions'
  }

  private buildBody(messages: Message[], config: ModelConfig, tools?: ToolDefinition[], stream = false) {
    const body: any = {
      model: config.model,
      messages: messages.map(m => {
        const msg: any = { role: m.role, content: m.content }
        if (m.tool_calls) msg.tool_calls = m.tool_calls
        if (m.tool_call_id) msg.tool_call_id = m.tool_call_id
        if (m.name && m.role === 'tool') msg.name = m.name
        return msg
      }),
      stream,
    }
    if (config.temperature != null) body.temperature = config.temperature
    if (config.maxTokens) body.max_tokens = config.maxTokens
    if (config.topP != null) body.top_p = config.topP
    if (tools && tools.length > 0) {
      body.tools = tools.map(t => ({
        type: 'function',
        function: { name: t.function.name, description: t.function.description, parameters: t.function.parameters },
      }))
      body.tool_choice = 'auto'
    }
    return body
  }

  async chat(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): Promise<Message> {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 60000)
    try {
      const resp = await fetch(this.getEndpoint(config), {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + config.apiKey,
        },
        body: JSON.stringify(this.buildBody(messages, config, tools, false)),
      })
    } catch (e) {
      if (controller.signal.aborted) throw new Error('Request timeout after 60000ms')
      throw e
    } finally {
      clearTimeout(timeoutId)
    }
    if (!resp.ok) {
      const err = await resp.text().catch(() => '')
      throw new Error('OpenAI error ' + resp.status + ': ' + err)
    }
    const data = await resp.json()
    const choice = data.choices?.[0]
    if (!choice) throw new Error('No response from OpenAI')

    const result: Message = {
      role: 'assistant',
      content: choice.message?.content || '',
      timestamp: Date.now(),
    }

    // Parse tool calls
    if (choice.message?.tool_calls && choice.message.tool_calls.length > 0) {
      result.tool_calls = choice.message.tool_calls.map((tc: any) => ({
        id: tc.id || 'call_' + Date.now(),
        type: 'function' as const,
        function: {
          name: tc.function?.name || '',
          arguments: tc.function?.arguments || '{}',
        },
      }))
    }

    return result
  }

  async *chatStream(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): AsyncGenerator<string> {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 120000)
    try {
      const resp = await fetch(this.getEndpoint(config), {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + config.apiKey,
        },
        body: JSON.stringify(this.buildBody(messages, config, tools, true)),
      })
    } catch (e) {
      if (controller.signal.aborted) throw new Error('Request timeout after 120000ms')
      throw e
    } finally {
      clearTimeout(timeoutId)
    }
    if (!resp.ok) throw new Error('OpenAI stream error: ' + resp.status)

    const reader = resp.body?.getReader()
    if (!reader) throw new Error('No response body')

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line.startsWith('data: ')) continue
        const data = line.slice(6).trim()
        if (data === '[DONE]') return

        try {
          const parsed = JSON.parse(data)
          const delta = parsed.choices?.[0]?.delta
          if (delta?.content) yield delta.content
        } catch {}
      }
    }
  }
}
