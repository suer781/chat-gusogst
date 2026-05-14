// ============================================
// 搜索引擎类型定义
// ============================================

/** 搜索选项 */
export interface SearchOptions {
  count?: number
  language?: string       // 'zh' | 'en' | ...
  region?: string         // 'cn' | 'us' | ...
  timeRange?: 'day' | 'week' | 'month' | 'year'
}

/** 单条搜索结果 */
export interface SearchResult {
  title: string
  url: string
  snippet: string
  source?: string         // 来源引擎 id
  publishedAt?: string
}

/** 搜索引擎能力标记 */
export interface EngineCapabilities {
  hasApiMode: boolean
  hasScrapeMode: boolean
  requiresKey: boolean
  supportsTimeRange: boolean
  supportsLanguage: boolean
  supportsRegion: boolean
}

/** 引擎配置（用户在设置页填写，持久化到 localStorage） */
export interface EngineConfig {
  enabled: boolean
  mode: 'api' | 'scrape'
  apiKey?: string
  customHeaders?: Record<string, string>
}\n/** 所有引擎配置的集合，key 是引擎 id */
export type EngineConfigMap = Record<string, EngineConfig>

/** 搜索引擎接口 */
export interface SearchEngine {
  readonly id: string
  readonly name: string
  readonly capabilities: EngineCapabilities

  search(query: string, options?: SearchOptions, apiKey?: string): Promise<SearchResult[]>
  isAvailable(config: EngineConfig): boolean
  validateKey?(key: string): Promise<boolean>
}
