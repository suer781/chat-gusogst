import { ToolRegistry } from './registry'

interface SearchResult {
  title: string
  url: string
  snippet: string
}

interface SearchResponse {
  answer?: string
  results: SearchResult[]
}

export type SearchEngine = 'tavily' | 'duckduckgo' | 'auto'

export interface SearchConfig {
  engine: SearchEngine
  tavilyApiKey?: string
}

async function searchTavily(query: string, apiKey: string, maxResults = 5): Promise<SearchResponse> {
  const resp = await fetch('https://api.tavily.com/search', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ api_key: apiKey, query, max_results: maxResults, include_answer: true, search_depth: 'basic' }),
  })
  if (!resp.ok) throw new Error('Tavily error: ' + resp.status)
  const data = await resp.json()
  return {
    answer: data.answer,
    results: (data.results || []).map((r: any) => ({ title: r.title || '', url: r.url || '', snippet: r.content || '' })),
  }
}

async function searchDuckDuckGo(query: string, maxResults = 5): Promise<SearchResponse> {
  const params = new URLSearchParams({ q: query, format: 'json', no_html: '1', skip_disambig: '1' })
  const resp = await fetch('https://api.duckduckgo.com/?' + params)
  if (!resp.ok) throw new Error('DuckDuckGo error: ' + resp.status)
  const data = await resp.json()
  const results: SearchResult[] = []
  if (data.Abstract) {
    results.push({ title: data.Heading || query, url: data.AbstractURL || '', snippet: data.Abstract })
  }
  for (const topic of (data.RelatedTopics || []).slice(0, maxResults)) {
    if (topic.Text && topic.FirstURL) {
      results.push({ title: topic.Text.split(' - ')[0]?.substring(0, 80) || '', url: topic.FirstURL, snippet: topic.Text })
    }
  }
  if (results.length === 0) return await searchDuckDuckGoHTML(query, maxResults)
  return { results: results.slice(0, maxResults) }
}

async function searchDuckDuckGoHTML(query: string, maxResults = 5): Promise<SearchResponse> {
  const params = new URLSearchParams({ q: query })
  const resp = await fetch('https://html.duckduckgo.com/html/?' + params)
  if (!resp.ok) throw new Error('DDG HTML error: ' + resp.status)
  const html = await resp.text()
  const results: SearchResult[] = []
  const re = /<a[^>]+class="result__a"[^>]*href="([^"]*)"[^>]*>([^<]*)<\/a>[\s\S]*?<a[^>]+class="result__snippet"[^>]*>([\s\S]*?)<\/a>/g
  let m
  while ((m = re.exec(html)) && results.length < maxResults) {
    results.push({ title: (m[2]||'').trim(), url: m[1]||'', snippet: (m[3]||'').replace(/<[^>]+>/g,'').trim() })
  }
  return { results }
}

export async function search(query: string, config: SearchConfig, maxResults = 5): Promise<SearchResponse> {
  if ((config.engine === 'tavily' || config.engine === 'auto') && config.tavilyApiKey) {
    try { return await searchTavily(query, config.tavilyApiKey, maxResults) }
    catch (e) { if (config.engine === 'tavily') throw e }
  }
  return await searchDuckDuckGo(query, maxResults)
}

export function registerSearchTools(registry: ToolRegistry, config: SearchConfig) {
  registry.register(
    {
      type: 'function',
      function: {
        name: 'web_search',
        description: 'Search the internet for real-time information including news, weather, facts, and more',
        parameters: {
          type: 'object',
          properties: {
            query: { type: 'string', description: 'Search keywords' },
            max_results: { type: 'number', description: 'Max results, default 5' },
          },
          required: ['query'],
        },
      },
    },
    async (_name: string, args: any) => {
      const result = await search(args.query, config, args.max_results || 5)
      if (result.answer) return 'Answer: ' + result.answer + '\n\nSources:\n' + result.results.map((r,i)=>(i+1)+'. '+r.title+'\n   '+r.url+'\n   '+r.snippet).join('\n')
      if (result.results.length === 0) return 'No results found'
      return result.results.map((r,i)=>(i+1)+'. '+r.title+'\n   '+r.url+'\n   '+r.snippet).join('\n')
    },
  )
}
