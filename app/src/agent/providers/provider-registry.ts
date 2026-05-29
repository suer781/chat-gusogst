// ============================================================
// provider-registry.ts — 四合一供应商字典 + 向量搜索
// 字典 = 向量库 = 字典 = 评分载体
// ============================================================

import type { ProviderEntry, ModelInfo } from './endpoint-types'
import { vectorStore } from '../memory/vectorStore'

// 原始供应商数据（providers-registry.json）
import providersRaw from '../../data/providers-registry.json'

// ---- 供应商字典（内存中） ----
let providers: ProviderEntry[] = []
let initialized = false

/**
 * 从 API URL 提取域名
 * "https://nano-gpt.com/api/v1" -> "nano-gpt.com"
 */
function extractDomain(apiUrl: string): string {
  try {
    return new URL(apiUrl).hostname
  } catch {
    return ''
  }
}

/**
 * 从 API URL 推断端点路径
 * "https://api.openai.com/v1" -> ["/v1/chat/completions"]
 */
function inferEndpoints(apiUrl: string): string[] {
  try {
    const url = new URL(apiUrl)
    const path = url.pathname.replace(/\/$/, '')
    return [path + '/chat/completions']
  } catch {
    return ['/v1/chat/completions']
  }
}

/**
 * 构建搜索文本：name + domain + 所有 model name/id
 * 用于向量匹配
 */
function buildSearchText(raw: any, domain: string): string {
  const parts = [
    raw.name || '',
    raw.id || '',
    domain,
  ]
  if (raw.models && Array.isArray(raw.models)) {
    for (const m of raw.models) {
      if (m.name) parts.push(m.name)
      if (m.id) parts.push(m.id)
    }
  }
  return parts.filter(Boolean).join(' ')
}

/**
 * 初始化：加载供应商数据到内存 + 构建向量索引
 */
export async function initProviderRegistry(): Promise<void> {
  if (initialized) return

  // 1. 解析原始数据
  providers = (providersRaw as any[]).map(raw => {
    const api = raw.api || raw.base_url || ''
    const domain = extractDomain(api)
    const endpoints = inferEndpoints(api)
    const models: ModelInfo[] = (raw.models || []).map((m: any) => ({
      id: m.id || '',
      name: m.name || m.id || '',
      context_length: m.context_length || 0,
      max_output: m.max_output || 0,
      cost_input: m.cost_input || 0,
      cost_output: m.cost_output || 0,
    }))

    return {
      id: raw.id,
      name: raw.name,
      domain,
      api,
      endpoints,
      models,
      envKey: raw.env_key || [],
      doc: raw.doc,
      searchText: buildSearchText(raw, domain),
    }
  }).filter(p => p.api) // 过滤掉没有 API URL 的

  // 2. 构建向量索引（利用现有 VectorStore）
  await vectorStore.clear()
  for (const p of providers) {
    await vectorStore.add('provider:' + p.id, p.searchText)
  }

  initialized = true
  console.log(`[ProviderRegistry] Loaded ${providers.length} providers, vector index built`)
}

/**
 * 精确匹配：按域名查找供应商
 * 返回匹配的供应商，或 null
 */
export function matchByDomain(domainOrUrl: string): ProviderEntry | null {
  // 从 URL 提取域名
  let domain = domainOrUrl
  try {
    if (domainOrUrl.includes('://')) {
      domain = new URL(domainOrUrl).hostname
    }
  } catch {}

  domain = domain.toLowerCase().replace(/^www\./, '')

  // 精确匹配
  return providers.find(p =>
    p.domain.toLowerCase() === domain ||
    p.domain.toLowerCase().replace(/^www\./, '') === domain
  ) || null
}

/**
 * 向量搜索：按关键词语义匹配供应商
 * 返回按相似度排序的供应商列表
 */
export async function searchProviders(
  query: string,
  limit = 5,
  minScore = 0.05
): Promise<{ provider: ProviderEntry; score: number }[]> {
  if (!initialized) await initProviderRegistry()

  const results = await vectorStore.search(query, limit, minScore)

  return results
    .map(r => {
      // 从 id 中提取 provider id：'provider:openai' -> 'openai'
      const providerId = r.id.replace(/^provider:/, '')
      const provider = providers.find(p => p.id === providerId)
      if (!provider) return null
      return { provider, score: r.score }
    })
    .filter((r): r is { provider: ProviderEntry; score: number } => r !== null)
}

/**
 * 智能匹配：先精确，后向量
 * 用户输入可能是域名、供应商名、或关键词
 */
export async function smartMatch(
  input: string
): Promise<{ provider: ProviderEntry; score: number; method: 'exact' | 'vector' } | null> {
  if (!initialized) await initProviderRegistry()

  // 1. 先精确匹配域名
  const exact = matchByDomain(input)
  if (exact) {
    return { provider: exact, score: 1.0, method: 'exact' }
  }

  // 2. 再精确匹配 ID 或名称
  const byId = providers.find(
    p => p.id === input.toLowerCase() || p.name.toLowerCase() === input.toLowerCase()
  )
  if (byId) {
    return { provider: byId, score: 0.95, method: 'exact' }
  }

  // 3. 向量搜索
  const results = await searchProviders(input, 3, 0.1)
  if (results.length > 0) {
    return { provider: results[0].provider, score: results[0].score, method: 'vector' }
  }

  return null
}

/**
 * 获取供应商完整端点 URL
 * 先查供应商的 endpoints，拼接 api base
 */
export function getEndpointUrl(provider: ProviderEntry, endpointIndex = 0): string {
  const endpoint = provider.endpoints[endpointIndex] || '/v1/chat/completions'
  const base = provider.api.replace(/\/$/, '')
  // 如果 endpoint 已经是完整 URL，直接返回
  if (endpoint.startsWith('http')) return endpoint
  // 否则拼接：api base + endpoint path
  // 注意：api 已经包含了版本路径（如 /v1），endpoint 也包含，所以取 endpoint 即可
  return base
}

/**
 * 获取所有供应商
 */
export function getAllProviders(): ProviderEntry[] {
  return providers
}

/**
 * 按 ID 获取供应商
 */
export function getProviderById(id: string): ProviderEntry | null {
  return providers.find(p => p.id === id) || null
}
