/**
 * OpenAI 兼容 Provider
 * 支持: OpenAI, Deepseek, Groq, Together, 硅基流动, 自定义端点
 */
import type { Message, ModelConfig, ProviderAdapter, ToolDefinition } from '../../shared/types'

export class OpenAIProvider implements ProviderAdapter {
  readonly name = 'openai'

  private getEndpoint(config: ModelConfig): string {
    const host = (config.apiHost || 'https://api.openai.com').replace(/\/+$/, '')
    return `${host}/v1/chat/completions`
  }

  private buildBody(messages: Message[], config: ModelConfig, tools?: ToolDefinition[], stream = false) {
    const body: Record<string, unknown> = {
      model: config.model,
      messages: messages.map(m => {
        const msg: Record<string, unknown> = { role: m.role, content: m.content }
        if (m.tool_calls) msg.tool_calls = m.tool_calls
        if (m.tool_call_id) msg.tool_call_id = m.tool_call_id
        if (m.name) msg.name = m.name
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

  private parseSSELine(line: string): string | null {
    if (!line.startsWith('data: ')) return null
    const data = line.slice(6).trim()
    if (data === '[DONE]') return null
    try {
      const parsed = JSON.parse(data)
      return parsed.choices?.[0]?.delta?.content ?? null
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
      role: 'assistant',
      content: choice.message?.content ?? null,
      tool_calls: choice.message?.tool_calls,
      timestamp: Date.now(),
    }
  }

  async *chatStream(messages: Message[], config: ModelConfig, tools?: ToolDefinition[]): AsyncGenerator<string> {
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
          const token = this.parseSSELine(line)
          if (token !== null) yield token
        }
      }
    } finally {
      reader.releaseLock()
    }
  }
}
