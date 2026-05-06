import { ModelProvider, StreamCallbacks } from '../types';

/** OpenAI 兼容接口 — 流式聊天（Chatbox multi-provider pattern） */
export async function chatStream(
  provider: ModelProvider,
  messages: { role: string; content: string }[],
  callbacks: StreamCallbacks,
  signal?: AbortSignal,
) {
  try {
    const url = provider.apiUrl.replace(/\/$/, '') + '/v1/chat/completions';
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + provider.apiKey,
      },
      body: JSON.stringify({
        model: provider.model,
        messages,
        stream: true,
        temperature: 0.8,
        max_tokens: 2048,
      }),
      signal,
    });

    if (!resp.ok) {
      const err = await resp.text();
      callbacks.onError('API ' + resp.status + ': ' + err.slice(0, 200));
      return;
    }

    const reader = resp.body?.getReader();
    if (!reader) { callbacks.onError('无法读取响应'); return; }

    const decoder = new TextDecoder();
    let full = '';
    let buffer = '';
    let tokens = 0;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('
');
      buffer = lines.pop() || '';
      for (const line of lines) {
        if (!line.startsWith('data: ')) continue;
        const data = line.slice(6).trim();
        if (data === '[DONE]') break;
        try {
          const json = JSON.parse(data);
          const delta = json.choices?.[0]?.delta?.content;
          if (delta) { full += delta; tokens++; callbacks.onToken(delta); }
        } catch {}
      }
    }
    callbacks.onComplete(full, tokens);
  } catch (e: any) {
    if (e.name === 'AbortError') return;
    callbacks.onError(e.message || '网络错误');
  }
}

/** 同步调用（用于记忆摘要等后台任务） */
export async function chatSync(
  provider: ModelProvider,
  messages: { role: string; content: string }[],
  opts?: { temperature?: number; maxTokens?: number },
): Promise<{ content: string; tokens?: number }> {
  const url = provider.apiUrl.replace(/\/$/, '') + '/v1/chat/completions';
  const resp = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + provider.apiKey },
    body: JSON.stringify({
      model: provider.model,
      messages,
      temperature: opts?.temperature ?? 0.5,
      max_tokens: opts?.maxTokens ?? 1024,
    }),
  });
  if (!resp.ok) throw new Error('API ' + resp.status);
  const data = await resp.json();
  return { content: data.choices?.[0]?.message?.content || '', tokens: data.usage?.total_tokens };
}

/** 测试连接（Chatbox-style: provider_settings 中的测试按钮） */
export async function testConnection(provider: ModelProvider): Promise<boolean> {
  try {
    const r = await chatSync(provider, [{ role: 'user', content: 'ping' }], { maxTokens: 10 });
    return r.content.length > 0;
  } catch { return false; }
}
