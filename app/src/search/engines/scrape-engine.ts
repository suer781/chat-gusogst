// ============================================
// 通用抓取搜索引擎
// 给定 URL 模板 + 解析策略，自动抓取并解析结果
// 支持动态添加新引擎，零代码：只需在 registry.ts 加一行
// ============================================

import type { SearchEngine, SearchResult, SearchOptions, EngineConfig } from '../types'
import type { EngineMeta } from './registry'
import { stripHtml } from './base'

export class ScrapeEngine implements SearchEngine {
  readonly id: string
  readonly name: string
  private meta: EngineMeta

  constructor(meta: EngineMeta) {
    this.id = meta.id
    this.name = meta.name
    this.meta = meta
  }

  get capabilities() {
    return {
      hasApiMode: false,
      hasScrapeMode: true,
      requiresKey: false,
      supportsTimeRange: this.meta.supportsTimeRange,
      supportsLanguage: this.meta.supportsLanguage,
      supportsRegion: false,
    }
  }

  isAvailable(_config: EngineConfig): boolean {
    return true
  }

  async search(query: string, options?: SearchOptions, _apiKey?: string): Promise<SearchResult[]> {
    const url = this.buildUrl(query, options)
    const headers = this.meta.headers || {
      'User-Agent': 'Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36',
      'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
      'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
    }

    const resp = await fetch(url, {
      headers,
      signal: AbortSignal.timeout(12000),
      redirect: 'follow',
    })
    if (!resp.ok) throw new Error(`HTTP ${resp.status} ${resp.statusText}`)

    const html = await resp.text()
    const results = this.parseResults(html)
    if (results.length === 0) {
      throw new Error(`${this.name}: 未解析到结果（页面可能需要 JS 渲染）`)
    }
    return results
  }

  // ── 构建搜索 URL ──────────────────────

  private buildUrl(query: string, options?: SearchOptions): string {
    const encoded = encodeURIComponent(query)
    let url = this.meta.searchUrl.replace('%s', encoded)

    // 时间范围参数（只对支持的引擎生效）
    if (options?.timeRange && this.meta.supportsTimeRange) {
      url = this.appendTimeRange(url, options.timeRange)
    }
    return url
  }

  private appendTimeRange(url: string, timeRange: string): string {
    const u = new URL(url)
    // 百度用 gpc 参数
    if (this.meta.id === 'baidu' || this.meta.id === 'baidu_dev' || this.meta.id === 'baidu_academic') {
      // 百度时间范围比较复杂，这里用搜索工具参数
      const lmMap: Record<string, string> = { day: '1', week: '2', month: '3', year: '' }
      const lm = lmMap[timeRange]
      if (lm) u.searchParams.set('lm', lm)
    }
    // DDG 用 df 参数
    if (this.meta.id === 'duckduckgo') {
      const dfMap: Record<string, string> = { day: 'd', week: 'w', month: 'm', year: 'y' }
      u.searchParams.set('df', dfMap[timeRange] || '')
    }
    // Bing 用 filters 参数
    if (this.meta.id === 'bing') {
      const fMap: Record<string, string> = { day: 'ex1:"ez1"', week: 'ex1:"ez2"', month: 'ex1:"ez3"' }
      const f = fMap[timeRange]
      if (f) u.searchParams.set('filters', f)
    }
    // Brave 用 tf 参数
    if (this.meta.id === 'brave') {
      const pdMap: Record<string, string> = { day: 'pd', week: 'pw', month: 'pm', year: 'py' }
      u.searchParams.set('tf', pdMap[timeRange] || '')
    }
    // 搜狗用 f 参数
    if (this.meta.id === 'sogou') {
      const fMap: Record<string, string> = { day: '1', week: '2', month: '3' }
      const f = fMap[timeRange]
      if (f) u.searchParams.set('tsn', f)
    }
    // 知乎用 time_sort
    if (this.meta.id === 'zhihu') {
      const tMap: Record<string, string> = { day: 'd', week: 'w', month: 'm', year: 'y' }
      u.searchParams.set('time_sort', tMap[timeRange] || '')
    }
    return u.toString()
  }

  // ── 解析搜索结果 ──────────────────────

  private parseResults(html: string): SearchResult[] {
    // 按引擎优先尝试对应的解析策略
    // 通用策略按优先级排列
    const strategies = [
      () => this.parseBaiduLike(html),
      () => this.parseBingLike(html),
      () => this.parseDDGLike(html),
      () => this.parseBraveLike(html),
      () => this.parseStructuredCards(html),
      () => this.parseGenericLinks(html),
    ]

    for (const strategy of strategies) {
      const results = strategy()
      if (results.length > 0) return results
    }
    return []
  }

  // ── 解析策略 1: 百度/搜狗/360 格式 ──────────────────
  private parseBaiduLike(html: string): SearchResult[] {
    const results: SearchResult[] = []
    // <h3><a href="...">标题</a></h3> + 后续摘要文本
    const regex = /<h3[^>]*>[\s\S]*?<a[^>]*href="([^"]*)"[^>]*>([\s\S]*?)<\/a>[\s\S]*?<\/h3>/gi
    const matches: { url: string; title: string; pos: number }[] = []
    let m
    while ((m = regex.exec(html)) !== null && matches.length < 15) {
      const title = stripHtml(m[2])
      if (title.length >= 4) {
        matches.push({ url: m[1], title, pos: m.index })
      }
    }
    // 对每个 h3，找它后面的最近一段文本作为摘要
    for (let i = 0; i < matches.length && results.length < 10; i++) {
      const start = matches[i].pos
      const end = i + 1 < matches.length ? matches[i + 1].pos : start + 3000
      const chunk = html.substring(start, end)
      // 摘要：取 </h3> 后的第一个 <span> 或 <div> 中的文本
      const snippetMatch = chunk.match(/<\/h3>[\s\S]*?(?:content-right[^"]*"|c-abstract[^"]*"|c-span-last[^"]*"|)[^>]*>([\s\S]*?)<\//i)
      let snippet = ''
      if (snippetMatch) {
        snippet = stripHtml(snippetMatch[1])
      } else {
        // 降级：取 </h3> 后面 200 字符的纯文本
        const after = chunk.substring(chunk.indexOf('</h3>') + 5, chunk.indexOf('</h3>') + 600)
        snippet = stripHtml(after).substring(0, 200)
      }
      if (!this.isExcluded(matches[i].url)) {
        results.push({
          title: matches[i].title,
          url: matches[i].url,
          snippet,
          source: this.meta.id,
        })
      }
    }
    return results
  }

  // ── 解析策略 2: Bing 格式 ──────────────────
  private parseBingLike(html: string): SearchResult[] {
    const results: SearchResult[] = []
    // Bing: <li class="b_algo"><h2><a href="...">标题</a></h2><div class="b_caption"><p>摘要</p></div>
    const regex = /<li[^>]*class="b_algo"[^>]*>[\s\S]*?<h2[^>]*>[\s\S]*?<a[^>]*href="([^"]*)"[^>]*>([\s\S]*?)<\/a>[\s\S]*?<\/h2>[\s\S]*?(?:<p[^>]*>([\s\S]*?)<\/p>)?/gi
    let m
    while ((m = regex.exec(html)) !== null && results.length < 10) {
      const url = m[1]
      if (this.isExcluded(url)) continue
      results.push({
        title: stripHtml(m[2]),
        url,
        snippet: m[3] ? stripHtml(m[3]).substring(0, 200) : '',
        source: this.meta.id,
      })
    }
    return results
  }

  // ── 解析策略 3: DDG HTML 格式 ──────────────────
  private parseDDGLike(html: string): SearchResult[] {
    const results: SearchResult[] = []
    const regex = /<a[^>]*class="result__a"[^>]*href="([^"]*)"[^>]*>([\s\S]*?)<\/a>[\s\S]*?<a[^>]*class="result__snippet"[^>]*>([\s\S]*?)<\/a>/gi
    let m
    while ((m = regex.exec(html)) !== null && results.length < 10) {
      results.push({
        title: stripHtml(m[2]),
        url: m[1],
        snippet: stripHtml(m[3]),
        source: this.meta.id,
      })
    }
    return results
  }

  // ── 解析策略 4: Brave 格式 ──────────────────
  private parseBraveLike(html: string): SearchResult[] {
    const results: SearchResult[] = []
    const regex = /snippet-title[^>]*>([\s\S]*?)<\/span>[\s\S]*?snippet-description[^>]*>([\s\S]*?)<\/p>/gi
    // 找对应的链接
    const linkRegex = /<a[^>]*href="([^"]+)"[^>]*>[\s\S]*?snippet-title/g
    const titles: string[] = []
    const snippets: string[] = []
    const urls: string[] = []

    let m
    while ((m = linkRegex.exec(html)) !== null && urls.length < 10) {
      urls.push(m[1])
    }
    while ((m = regex.exec(html)) !== null && titles.length < 10) {
      titles.push(stripHtml(m[1]))
      snippets.push(stripHtml(m[2]))
    }
    for (let i = 0; i < Math.min(titles.length, urls.length) && results.length < 10; i++) {
      if (!this.isExcluded(urls[i])) {
        results.push({
          title: titles[i],
          url: urls[i],
          snippet: snippets[i] || '',
          source: this.meta.id,
        })
      }
    }
    return results
  }

  // ── 解析策略 5: 结构化卡片（垂直搜索引擎常用） ──────────────────
  private parseStructuredCards(html: string): SearchResult[] {
    const results: SearchResult[] = []
    // 找包含标题+摘要的卡片结构
    const cardRegex = /<div[^>]*class="[^"]*(?:card|item|result|entry)[^"]*"[^>]*>([\s\S]*?)<\/div>/gi
    let m
    while ((m = cardRegex.exec(html)) !== null && results.length < 10) {
      const card = m[1]
      // 提取链接和标题
      const linkMatch = card.match(/<a[^>]*href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/)
      if (!linkMatch) continue
      const title = stripHtml(linkMatch[2])
      if (title.length < 4) continue
      const url = linkMatch[1]
      if (this.isExcluded(url)) continue
      // 提取摘要
      const snippetMatch = card.match(/<p[^>]*>([\s\S]*?)<\/p>/) || card.match(/<span[^>]*>([\s\S]*?)<\/span>/)
      const snippet = snippetMatch ? stripHtml(snippetMatch[1]).substring(0, 200) : ''
      results.push({ title, url, snippet, source: this.meta.id })
    }
    return results
  }

  // ── 解析策略 6: 通用链接提取（最终降级） ──────────────────
  private parseGenericLinks(html: string): SearchResult[] {
    const results: SearchResult[] = []
    const seen = new Set<string>()
    const regex = /<a[^>]*href="(https?:\/\/[^"]+)"[^>]*>([\s\S]*?)<\/a>/gi
    let m
    while ((m = regex.exec(html)) !== null && results.length < 10) {
      const url = m[1]
      const title = stripHtml(m[2])
      if (title.length < 5 || seen.has(url)) continue
      if (this.isExcluded(url)) continue
      seen.add(url)
      results.push({ title, url, snippet: '', source: this.meta.id })
    }
    return results
  }

  // ── 域名黑名单过滤 ──────────────────
  private isExcluded(url: string): boolean {
    if (!this.meta.excludeDomains) return false
    try {
      const hostname = new URL(url).hostname
      return this.meta.excludeDomains.some(d => hostname.includes(d))
    } catch {
      return false
    }
  }
}
