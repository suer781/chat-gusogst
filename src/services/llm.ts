import { Message, ModelProvider } from '../types';

interface StreamCallbacks {
  onToken: (token: string) => void;
  onComplete: (fullText: string) => void;
  onError: (error: string) => void;
}

export async function chatStream(
  provider: ModelProvider,
  messages: { role: string; content: string }[],
  callbacks: StreamCallbacks
) {
  try {
    const url = provider.apiUrl.replace(/\/$/, '') + '/v1/chat/completions';
    const resp = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${provider.apiKey}`,
      },
      body: JSON.stringify({
        model: provider.model,
        messages,
        stream: true,
        temperature: 0.8,
        max_tokens: 2048,
      }),
    });

    if (!resp.ok) {
      const err = await resp.text();
      callbacks.onError(`API 错误 ${resp.status}: ${err}`);
      return;
    }

    const reader = resp.body?.getReader();
    if (!reader) { callbacks.onError('无法读取响应'); return; }

    const decoder = new TextDecoder();
    let full = '';
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (!line.startsWith('data: ')) continue;
        const data = line.slice(6).trim();
        if (data === '[DONE]') break;
        try {
          const json = JSON.parse(data);
          const token = json.choices?.[0]?.delta?.content;
          if (token) {
            full += token;
            callbacks.onToken(token);
          }
        } catch {}
      }
    }
    callbacks.onComplete(full);
  } catch (e: any) {
    callbacks.onError(e.message || '网络错误');
  }
}

export async function chatSync(
  provider: ModelProvider,
  messages: { role: string; content: string }[]
): Promise<string> {
  const url = provider.apiUrl.replace(/\/$/, '') + '/v1/chat/completions';
  const resp = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${provider.apiKey}`,
    },
    body: JSON.stringify({
      model: provider.model,
      messages,
      temperature: 0.8,
      max_tokens: 2048,
    }),
  });
  const json = await resp.json();
  return json.choices?.[0]?.message?.content || '';
}
