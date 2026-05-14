// ============================================
// Tavily 搜索引擎（纯 API，需要 key）
// ============================================

import type { SearchEngine, SearchResult, SearchOptions, EngineConfig } from '../types'

interface TavilyResponse {
  results: { title: string; url: string; content: string }[]
  answer?: string
}

export class TavilyEngine implements SearchEngine {
  readonly id = 'tavily'
  readonly name = 'Tavily'

  get capabilities() {
    return {
      hasApiMode: true,
      hasScrapeMode: false,
      requiresKey: true,
      supportsTimeRange: true,
      supportsLanguage: true,
      supportsRegion: false,
    }
  }

  isAvailable(config: EngineConfig): boolean {
    return config.mode === 'api' && !!config.apiKey
  }

  async search(query: string, options?: SearchOptions, apiKey?: string): Promise<SearchResult[]> {
    if (!apiKey) throw new Error('Tavily API key 未配置')

    const body: Record<string, unknown> = {
      api_key: apiKey,
      query,
      search_depth: 'basic',
      max_results: options?.count || 5,
      include_answer: true,
      include_raw_content: false,
    }
    if (options?.timeRange) {
      body.days = { day: 1, week: 7, month: 30, year: 365 }[options.timeRange]
    }

    const resp = await fetch('https://api.tavily.com/search', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal: AbortSignal.timeout(15000),
    })

    if (!resp.ok) {
      const err = await resp.text()
      throw new Error(`Tavily API ${resp.status}: ${err}`)
    }

    const data: TavilyResponse = await resp.json()
    const results: SearchResult[] = (data.results || []).map(r => ({
      title: r.title,
      url: r.url,
      snippet: r.content,
      source: 'tavily',
    }))

    if (data.answer) {
      results.unshift({
        title: `${query} — AI 摘要`,
        url: '',
        snippet: data.answer,
        source: 'tavily',
      })
    }

    return results
  }

  async validateKey(key: string): Promise<boolean> {
    try {
      await this.search('test', { count: 1 }, key)
      return true
    } catch {
      return false
    }
  }
}
