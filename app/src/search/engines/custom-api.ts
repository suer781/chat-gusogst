// ============================================
// 自定义 API 搜索引擎
// 用户填 endpoint + header + body 模板 + 响应路径
// 支持任意 JSON API
// ============================================

import type { SearchEngine, SearchResult, SearchOptions, EngineConfig } from '../types'

/** 用户自定义 API 引擎配置 */
export interface CustomApiConfig {
  id: string
  name: string
  /**
   * API 端点 URL，支持占位符：
   * {{query}} — URL 编码后的搜索词
   * {{queryRaw}} — 原始搜索词（不编码）
   * {{count}} — 请求结果数
   * {{apiKey}} — 用户配置的 API Key
   */
  endpoint: string
  /** HTTP 方法，默认 POST */
  method?: 'GET' | 'POST'
  /** 请求头，支持同样的占位符 */
  headers?: Record<string, string>
  /**
   * 请求体模板（POST 时使用），JSON 字符串，支持占位符。
   * 不填则自动用 {"query": "{{query}}"}
   */
  bodyTemplate?: string
  /**
   * 响应 JSON 路径映射，告诉引擎从响应的哪里提取数据：
   * resultsPath — 结果数组的 JSONPath（如 "data.results"、"items"、"results"）
   * titleField — 标题字段名（如 "title"、"name"）
   * urlField — 链接字段名（如 "url"、"link"）
   * snippetField — 摘要字段名（如 "snippet"、"description"、"content"）
   */
  responseMapping: {
    resultsPath: string
    titleField: string
    urlField: string
    snippetField?: string
    /** AI 摘要字段（可选，如 Tavily 的 answer） */
    answerField?: string
  }
}

/** 自定义 API 引擎的状态（持久化用） */
export interface CustomApiState {
  configs: CustomApiConfig[]
}

export class CustomApiEngine implements SearchEngine {
  readonly id: string
  readonly name: string
  private config: CustomApiConfig

  constructor(config: CustomApiConfig) {
    this.id = config.id
    this.name = config.name
    this.config = config
  }

  get capabilities() {
    return {
      hasApiMode: true,
      hasScrapeMode: false,
      requiresKey: true,
      supportsTimeRange: false,
      supportsLanguage: false,
      supportsRegion: false,
    }
  }

  isAvailable(_config: EngineConfig): boolean {
    return true // 自定义 API 只要配了就可用
  }

  async search(query: string, options?: SearchOptions, apiKey?: string): Promise<SearchResult[]> {
    const { endpoint, method = 'POST', headers = {}, bodyTemplate, responseMapping } = this.config
    const count = options?.count || 10

    // 替换占位符
    const placeholders: Record<string, string> = {
      '{{query}}': encodeURIComponent(query),
      '{{queryRaw}}': query,
      '{{count}}': String(count),
      '{{apiKey}}': apiKey || '',
    }

    const url = this.replacePlaceholders(endpoint, placeholders)
    const resolvedHeaders: Record<string, string> = {
      'Content-Type': 'application/json',
      ...Object.fromEntries(
        Object.entries(headers).map(([k, v]) => [k, this.replacePlaceholders(v, placeholders)])
      ),
    }

    const fetchOptions: RequestInit = {
      method,
      headers: resolvedHeaders,
      signal: AbortSignal.timeout(15000),
    }

    if (method === 'POST') {
      const body = bodyTemplate
        ? this.replacePlaceholders(bodyTemplate, placeholders)
        : JSON.stringify({ query, count })
      fetchOptions.body = body
    }

    const resp = await fetch(url, fetchOptions)
    if (!resp.ok) {
      const errText = await resp.text().catch(() => '')
      throw new Error(`${this.name} API ${resp.status}: ${errText.substring(0, 200)}`)
    }

    const data = await resp.json()
    return this.parseResponse(data, responseMapping, query)
  }

  private replacePlaceholders(template: string, placeholders: Record<string, string>): string {
    let result = template
    for (const [key, value] of Object.entries(placeholders)) {
      result = result.replaceAll(key, value)
    }
    return result
  }

  /** 按 JSONPath 简单提取（支持 a.b.c 形式） */
  private getNestedValue(obj: unknown, path: string): unknown {
    return path.split('.').reduce((current, key) => {
      if (current == null || typeof current !== 'object') return undefined
      return (current as Record<string, unknown>)[key]
    }, obj)
  }

  private parseResponse(
    data: unknown,
    mapping: CustomApiConfig['responseMapping'],
    query: string
  ): SearchResult[] {
    const results: SearchResult[] = []

    // 提取 AI 摘要
    if (mapping.answerField) {
      const answer = this.getNestedValue(data, mapping.answerField)
      if (typeof answer === 'string' && answer.trim()) {
        results.push({
          title: `${query} — AI 摘要`,
          url: '',
          snippet: answer,
          source: this.id,
        })
      }
    }

    // 提取结果列表
    const items = this.getNestedValue(data, mapping.resultsPath)
    if (!Array.isArray(items)) {
      throw new Error(`${this.name}: 响应中未找到 ${mapping.resultsPath} 数组`)
    }

    for (const item of items) {
      const title = this.getNestedValue(item, mapping.titleField)
      const url = this.getNestedValue(item, mapping.urlField)
      if (!title || !url) continue

      const snippet = mapping.snippetField
        ? this.getNestedValue(item, mapping.snippetField)
        : ''

      results.push({
        title: String(title),
        url: String(url),
        snippet: typeof snippet === 'string' ? snippet.substring(0, 300) : '',
        source: this.id,
      })
    }

    return results
  }
}

// ── 预置的 API 引擎模板（用户可以一键导入，也可以自己填） ────────────

export const API_TEMPLATES: { name: string; description: string; config: Omit<CustomApiConfig, 'id'> }[] = [
  {
    name: 'Tavily',
    description: 'AI 优化搜索，免费 1000 次/月',
    config: {
      name: 'Tavily',
      endpoint: 'https://api.tavily.com/search',
      method: 'POST',
      headers: {},
      bodyTemplate: '{"api_key":"{{apiKey}}","query":"{{queryRaw}}","search_depth":"basic","max_results":{{count}},"include_answer":true}',
      responseMapping: {
        resultsPath: 'results',
        titleField: 'title',
        urlField: 'url',
        snippetField: 'content',
        answerField: 'answer',
      },
    },
  },
  {
    name: 'SerpAPI (Google)',
    description: 'Google 搜索 API，免费 100 次/月',
    config: {
      name: 'SerpAPI',
      endpoint: 'https://serpapi.com/search.json?q={{query}}&api_key={{apiKey}}&num={{count}}',
      method: 'GET',
      headers: {},
      responseMapping: {
        resultsPath: 'organic_results',
        titleField: 'title',
        urlField: 'link',
        snippetField: 'snippet',
      },
    },
  },
  {
    name: 'SearXNG',
    description: '自托管元搜索引擎，支持多种后端',
    config: {
      name: 'SearXNG',
      endpoint: 'https://searx.be/search?q={{queryRaw}}&format=json&pageno=1',
      method: 'GET',
      headers: {},
      responseMapping: {
        resultsPath: 'results',
        titleField: 'title',
        urlField: 'url',
        snippetField: 'content',
      },
    },
  },
  {
    name: 'Perplexity API',
    description: 'AI 搜索引擎 API，需付费',
    config: {
      name: 'Perplexity',
      endpoint: 'https://api.perplexity.ai/chat/completions',
      method: 'POST',
      headers: {
        'Authorization': 'Bearer {{apiKey}}',
      },
      bodyTemplate: '{"model":"llama-3.1-sonar-small-128k-online","messages":[{"role":"user","content":"{{queryRaw}}"}],"max_tokens":1000}',
      responseMapping: {
        resultsPath: 'choices',
        titleField: 'message.content',
        urlField: 'message.content',
        snippetField: 'message.content',
      },
    },
  },
  {
    name: '博查 AI 搜索',
    description: '国内 AI 搜索 API，适合中文场景',
    config: {
      name: '博查 AI',
      endpoint: 'https://api.bochaai.com/v1/web-search',
      method: 'POST',
      headers: {
        'Authorization': 'Bearer {{apiKey}}',
      },
      bodyTemplate: '{"query":"{{queryRaw}}","freshness":"noLimit","summary":true,"count":{{count}}}',
      responseMapping: {
        resultsPath: 'data.webPages.value',
        titleField: 'name',
        urlField: 'url',
        snippetField: 'snippet',
        answerField: 'data.summary',
      },
    },
  },
  {
    name: 'Firecrawl',
    description: '网页抓取和搜索 API',
    config: {
      name: 'Firecrawl',
      endpoint: 'https://api.firecrawl.dev/v1/search',
      method: 'POST',
      headers: {
        'Authorization': 'Bearer {{apiKey}}',
      },
      bodyTemplate: '{"query":"{{queryRaw}}","limit":{{count}},"scrapeOptions":{"formats":["markdown"]}}',
      responseMapping: {
        resultsPath: 'data',
        titleField: 'title',
        urlField: 'url',
        snippetField: 'markdown',
      },
    },
  },
]
