import type { SearchResult } from './types'

interface CacheEntry {
  results: SearchResult[]
  timestamp: number
}

/**
 * LRU 搜索结果缓存
 * 默认 TTL 5 分钟，最多 50 条
 */
export class SearchCache {
  private cache = new Map<string, CacheEntry>()
  private readonly ttl: number
  private readonly maxSize: number

  constructor(ttlMs = 5 * 60 * 1000, maxSize = 50) {
    this.ttl = ttlMs
    this.maxSize = maxSize
  }

  get(query: string): SearchResult[] | null {
    const key = this.normalize(query)
    const entry = this.cache.get(key)
    if (!entry) return null
    if (Date.now() - entry.timestamp > this.ttl) {
      this.cache.delete(key)
      return null
    }
    // 移到最后（LRU）
    this.cache.delete(key)
    this.cache.set(key, entry)
    return entry.results
  }

  set(query: string, results: SearchResult[]): void {
    const key = this.normalize(query)
    if (this.cache.size >= this.maxSize) {
      const oldest = this.cache.keys().next().value
      if (oldest) this.cache.delete(oldest)
    }
    this.cache.set(key, { results, timestamp: Date.now() })
  }

  clear(): void {
    this.cache.clear()
  }

  get size(): number {
    return this.cache.size
  }

  private normalize(query: string): string {
    return query.trim().toLowerCase().replace(/\s+/g, ' ')
  }
}
