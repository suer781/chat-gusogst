import type { ModelConfig, Persona, Message } from './types'

export type StreamEvent =
  | { type: 'token'; content: string }
  | { type: 'error'; message: string }
  | { type: 'done' }

export async function* chatStream(
  config: ModelConfig,
  persona: Persona,
  history: Message[]
): AsyncGenerator<StreamEvent> {
  const messages = [
    { role: 'system', content: persona.systemPrompt },
    ...history.map(m => ({ role: m.role, content: m.content })),
  ]

  const url = `${config.apiHost.replace(/\/+$/, '')}/v1/chat/completions`
  const res = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${config.apiKey}`,
    },
    body: JSON.stringify({
      model: config.model,
      messages,
      temperature: config.temperature,
      max_tokens: config.maxTokens,
      stream: true,
    }),
  })

  if (!res.ok) {
    const text = await res.text()
    yield { type: 'error', message: `API ${res.status}: ${text}` }
    return
  }

  const reader = res.body?.getReader()
  if (!reader) { yield { type: 'error', message: 'No response body' }; return }

  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() ?? ''
    for (const line of lines) {
      if (!line.startsWith('data: ')) continue
      const data = line.slice(6).trim()
      if (data === '[DONE]') { yield { type: 'done' }; return }
      try {
        const parsed = JSON.parse(data)
        const delta = parsed.choices?.[0]?.delta?.content
        if (delta) yield { type: 'token', content: delta }
      } catch {}
    }
  }
  yield { type: 'done' }
}