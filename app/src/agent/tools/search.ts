/**
 * 搜索工具 — 支持 Tavily / DuckDuckGo / 百度
 * 基于 Hermes websearch.py 重写
 */
import { ToolRegistry } from './registry'

export function registerSearchTools(registry: ToolRegistry, config: {
  engine: string
  apiKey?: string
}) {
  registry.register(
    {
      type: 'function',
      function: {
        name: 'web_search',
        description: '搜索互联网获取实时信息。当用户询问新闻、实时数据、你不了解的信息时使用。',
        parameters: {
          type: 'object',
          properties: {
            query: { type: 'string', description: '搜索关键词' },
            max_results: { type: 'number', description: '返回结果数量，默认5' },
          },
          required: ['query'],
        },
      },
    },
    async (_name: string, args: Record<string, unknown>) => {
      const query = args.query as string
      const maxResults = (args.max_results as number) || 5

      try {
        if (config.engine === 'tavily' && config.apiKey) {
          return await searchTavily(query, config.apiKey, maxResults)
        }
        return await searchDuckDuckGo(query, maxResults)
      } catch (err: any) {
        return JSON.stringify({ error: err.message })
      }
    },
  )
}

async function searchTavily(query: string, apiKey: string, maxResults: number): Promise<string> {
  const resp = await fetch('https://api.tavily.com/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      api_key: apiKey,
      query,
      max_results: maxResults,
      include_answer: true,
    }),
  })
  if (!resp.ok) throw new Error(`Tavily error: ${resp.status}`)
  const data = await resp.json()
  const results = (data.results || []).map((r: any) => ({
    title: r.title,
    url: r.url,
    snippet: r.content?.slice(0, 200),
  }))
  return JSON.stringify({ answer: data.answer, results })
}

async function searchDuckDuckGo(query: string, maxResults: number): Promise<string> {
  // DuckDuckGo Instant Answer API (免费，无需 key)
  const params = new URLSearchParams({ q: query, format: 'json', no_html: '1', skip_disambig: '1' })
  const resp = await fetch(`https://api.duckduckgo.com/?${params}`)
  if (!resp.ok) throw new Error(`DuckDuckGo error: ${resp.status}`)
  const data = await resp.json()
  const results: Array<{ title: string; snippet: string }> = []
  if (data.Abstract) {
    results.push({ title: data.Heading || query, snippet: data.Abstract })
  }
  for (const topic of (data.RelatedTopics || []).slice(0, maxResults)) {
    if (topic.Text) {
      results.push({ title: topic.Text?.slice(0, 80), snippet: topic.Text })
    }
  }
  return JSON.stringify({ results })
}
