// ============================================================
// endpoint-manager.ts — 端点管理器
// 评分系统 + 搭便车测试 + 网络判定 + Failover
// ============================================================

import type {
  EndpointRating,
  EndpointSelection,
  EndpointTestResult,
} from './endpoint-types'
import { RATING_CONSTANTS } from './endpoint-types'
import {
  initProviderRegistry,
  smartMatch,
  getEndpointUrl,
  getProviderById,
  type ProviderEntry,
} from './provider-registry'

// ---- 持久化键 ----
const RATINGS_KEY = 'chat-gusogst-endpoint-ratings'
const CHAT_COUNTS_KEY = 'chat-gusogst-chat-counts'

// ---- 内存状态 ----
let ratings: Map<string, EndpointRating> = new Map()
let chatCounts: Map<string, number> = new Map()  // providerId -> count
let loaded = false

// ============================================================
// 持久化
// ============================================================

function loadState(): void {
  if (loaded) return
  try {
    const ratingsRaw = localStorage.getItem(RATINGS_KEY)
    if (ratingsRaw) {
      const arr: EndpointRating[] = JSON.parse(ratingsRaw)
      for (const r of arr) ratings.set(r.endpoint, r)
    }
    const countsRaw = localStorage.getItem(CHAT_COUNTS_KEY)
    if (countsRaw) {
      const obj: Record<string, number> = JSON.parse(countsRaw)
      for (const [k, v] of Object.entries(obj)) chatCounts.set(k, v)
    }
  } catch (e) {
    console.warn('[EndpointManager] Failed to load state:', e)
  }
  loaded = true
}

function saveRatings(): void {
  try {
    localStorage.setItem(RATINGS_KEY, JSON.stringify([...ratings.values()]))
  } catch {}
}

function saveChatCounts(): void {
  try {
    localStorage.setItem(CHAT_COUNTS_KEY, JSON.stringify(Object.fromEntries(chatCounts)))
  } catch {}
}

// ============================================================
// 评分公式
// ============================================================

/**
 * 计算学习率 k_m = 12 / (m + 15)
 * m 越大 -> k 越小 -> 评分变动越慢
 */
function calcK(m: number): number {
  return RATING_CONSTANTS.K_NUMERATOR / (m + RATING_CONSTANTS.K_OFFSET)
}

/**
 * 更新累积评分
 * C_m = clamp(C_{m-1} * (1 - k_m) + ΔC, -10000, 10000)
 */
function updateRating(
  current: number,
  m: number,
  deltaC: number
): number {
  const k = calcK(m)
  const newVal = current * (1 - k) + deltaC
  return Math.max(RATING_CONSTANTS.MIN, Math.min(RATING_CONSTANTS.MAX, newVal))
}

// ============================================================
// 端点评分操作
// ============================================================

function getRating(endpoint: string, providerId: string): EndpointRating {
  let r = ratings.get(endpoint)
  if (!r) {
    r = {
      endpoint,
      providerId,
      C_m: 0,
      m: 0,
      errorCount: 0,
      lastSuccess: 0,
      lastFailure: 0,
    }
    ratings.set(endpoint, r)
  }
  return r
}

/**
 * 记录成功
 * - 首次成功：ΔC = DELTA_SUCCESS_FIRST（快速建立信任）
 * - 后续成功：ΔC = DELTA_SUCCESS
 * - errorCount 清零
 */
function recordSuccess(endpoint: string, providerId: string): void {
  loadState()
  const r = getRating(endpoint, providerId)
  const isFirst = r.m === 0 && r.C_m === 0
  const deltaC = isFirst
    ? RATING_CONSTANTS.DELTA_SUCCESS_FIRST
    : RATING_CONSTANTS.DELTA_SUCCESS

  r.C_m = updateRating(r.C_m, r.m, deltaC)
  r.m++
  r.errorCount = 0
  r.lastSuccess = Date.now()
  saveRatings()
}

/**
 * 记录失败
 * - errorCount >= ERROR_THRESHOLD 后，失败 ΔC 放大
 * - errorCount 越大，ΔC 越负
 */
function recordFailure(endpoint: string, providerId: string): void {
  loadState()
  const r = getRating(endpoint, providerId)
  let deltaC = RATING_CONSTANTS.DELTA_FAILURE

  if (r.errorCount >= RATING_CONSTANTS.ERROR_THRESHOLD) {
    // 放大惩罚：errorCount 越大，惩罚越重
    deltaC *= RATING_CONSTANTS.ERROR_AMPLIFIER * (r.errorCount - RATING_CONSTANTS.ERROR_THRESHOLD + 1)
  }

  r.C_m = updateRating(r.C_m, r.m, deltaC)
  r.m++
  r.errorCount++
  r.lastFailure = Date.now()
  saveRatings()
}

// ============================================================
// 聊天次数
// ============================================================

/**
 * 记录一次聊天（仅计数，不记内容）
 */
export function recordChat(providerId: string): void {
  loadState()
  const count = chatCounts.get(providerId) || 0
  chatCounts.set(providerId, count + 1)
  saveChatCounts()
}

export function getChatCount(providerId: string): number {
  loadState()
  return chatCounts.get(providerId) || 0
}

// ============================================================
// 网络判定
// ============================================================

/**
 * 通过 img 标签探测域名连通性（浏览器环境无 ICMP）
 * 返回 true = 域名可达，false = 网络问题
 */
async function pingDomain(domain: string): Promise<boolean> {
  const retryCount = RATING_CONSTANTS.PING_RETRY_COUNT
  for (let i = 0; i < retryCount; i++) {
    try {
      const result = await new Promise<boolean>((resolve) => {
        const img = new Image()
        const timer = setTimeout(() => {
          img.src = ''
          resolve(false)
        }, RATING_CONSTANTS.NETWORK_TIMEOUT)
        img.onload = () => { clearTimeout(timer); resolve(true) }
        img.onerror = () => { clearTimeout(timer); resolve(true) } // 域名可达但非图片也算成功
        img.src = `https://${domain}/favicon.ico?_t=${Date.now()}`
      })
      if (result) return true
    } catch {}
  }
  return false
}

/**
 * 网络判定：区分端点问题 vs 网络问题
 * - Ping 成功 -> 端点问题 (isNetworkIssue = false)
 * - Ping 失败 -> 网络问题 (isNetworkIssue = true)
 */
async function judgeNetwork(domain: string): Promise<boolean> {
  return !(await pingDomain(domain))
}

// ============================================================
// 端点探测（搭便车测试）
// ============================================================

/**
 * 测试单个端点是否可用
 * 使用一个最小化的请求（搭便车：首次对话即测试）
 */
export async function testEndpoint(
  endpoint: string,
  apiKey: string
): Promise<EndpointTestResult> {
  const startTime = Date.now()

  // 提取域名用于网络判定
  let domain = ''
  try {
    domain = new URL(endpoint).hostname
  } catch {}

  // 重试 3 次
  for (let attempt = 0; attempt < RATING_CONSTANTS.RETRY_COUNT; attempt++) {
    try {
      const controller = new AbortController()
      const timeoutId = setTimeout(() => controller.abort(), 15000)

      // 最小化请求：只发 1 条 system 消息
      const resp = await fetch(endpoint, {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKey}`,
        },
        body: JSON.stringify({
          model: 'gpt-3.5-turbo',  // 用最小模型测试
          messages: [{ role: 'user', content: 'hi' }],
          max_tokens: 1,
          stream: false,
        }),
      })
      clearTimeout(timeoutId)

      if (resp.ok || resp.status === 400 || resp.status === 401 || resp.status === 429) {
        // 400/401/429 说明端点可达，只是请求有问题
        // 400 = bad request (模型名不对等), 401 = auth error, 429 = rate limit
        // 这些都说明端点本身是活的
        return {
          success: resp.ok || resp.status === 429,
          endpoint,
          providerId: '',
          latencyMs: Date.now() - startTime,
          isNetworkIssue: false,
          error: resp.ok ? undefined : `HTTP ${resp.status}`,
        }
      }
    } catch (e: any) {
      if (e.name === 'AbortError') {
        // 超时，可能是网络问题
        if (attempt < RATING_CONSTANTS.RETRY_COUNT - 1) continue
      }
      // 网络错误，重试
      if (attempt < RATING_CONSTANTS.RETRY_COUNT - 1) continue
    }
  }

  // 重试 3 次都失败，做网络判定
  const isNetwork = await judgeNetwork(domain)
  return {
    success: false,
    endpoint,
    providerId: '',
    latencyMs: Date.now() - startTime,
    isNetworkIssue: isNetwork,
    error: isNetwork ? 'Network unreachable' : 'Endpoint down',
  }
}

// ============================================================
// 端点选择（核心入口）
// ============================================================

/**
 * 选择最佳端点
 * 1. 匹配供应商（精确 + 向量）
 * 2. 按 C_m 降序排列候选端点
 * 3. 逐个尝试（搭便车）
 * 4. 成功则使用，失败则网络判定后尝试下一个
 *
 * @param userInput - 用户输入（域名、供应商名、关键词）
 * @param apiKey - API Key
 * @param customEndpoint - 用户自定义端点 URL（可选）
 * @returns 选中的端点，或 null
 */
export async function selectEndpoint(
  userInput: string,
  apiKey: string,
  customEndpoint?: string
): Promise<EndpointSelection | null> {
  await initProviderRegistry()
  loadState()

  let candidates: { provider: ProviderEntry; endpoint: string; score: number }[] = []

  if (customEndpoint) {
    // 用户自定义端点：直接用
    const provider = (await smartMatch(userInput))?.provider
    candidates = [{
      provider: provider || createFallbackProvider(userInput, customEndpoint),
      endpoint: customEndpoint,
      score: 0.5,
    }]
  } else {
    // 智能匹配供应商
    const match = await smartMatch(userInput)
    if (!match) {
      console.warn('[EndpointManager] No provider match for:', userInput)
      return null
    }

    // 构建候选端点列表
    for (let i = 0; i < match.provider.endpoints.length; i++) {
      candidates.push({
        provider: match.provider,
        endpoint: getEndpointUrl(match.provider, i),
        score: match.score,
      })
    }
  }

  // 按 C_m 降序排列
  candidates.sort((a, b) => {
    const rA = ratings.get(a.endpoint)?.C_m || 0
    const rB = ratings.get(b.endpoint)?.C_m || 0
    return rB - rA
  })

  // 逐个尝试（搭便车测试）
  for (const candidate of candidates) {
    const result = await testEndpoint(candidate.endpoint, apiKey)

    if (result.success) {
      recordSuccess(candidate.endpoint, candidate.provider.id)
      return {
        endpoint: candidate.endpoint,
        providerId: candidate.provider.id,
        providerName: candidate.provider.name,
        confidence: candidate.score,
      }
    }

    // 记录失败
    recordFailure(candidate.endpoint, candidate.provider.id)

    // 网络问题 -> 权重回档，停止测试
    if (result.isNetworkIssue) {
      console.warn('[EndpointManager] Network issue, stopping failover')
      return null
    }

    // 端点问题 -> 继续尝试下一个
    console.warn(`[EndpointManager] Endpoint ${candidate.endpoint} down, trying next...`)
  }

  return null
}

/**
 * 获取端点的评分信息（用于 UI 展示）
 */
export function getEndpointRatingInfo(
  endpoint: string
): { C_m: number; m: number; errorCount: number } | null {
  loadState()
  const r = ratings.get(endpoint)
  if (!r) return null
  return { C_m: r.C_m, m: r.m, errorCount: r.errorCount }
}

/**
 * 获取所有评分数据（用于调试/UI）
 */
export function getAllRatings(): EndpointRating[] {
  loadState()
  return [...ratings.values()]
}

/**
 * 重置所有评分数据
 */
export function resetAllRatings(): void {
  ratings.clear()
  chatCounts.clear()
  saveRatings()
  saveChatCounts()
}

/**
 * 获取所有聊天次数
 */
export function getAllChatCounts(): Record<string, number> {
  loadState()
  return Object.fromEntries(chatCounts)
}

// ============================================================
// 辅助
// ============================================================

/**
 * 创建 fallback 供应商条目（用户自定义供应商不在字典中时）
 */
function createFallbackProvider(domainOrUrl: string, endpoint: string): ProviderEntry {
  let domain = domainOrUrl
  try {
    if (domainOrUrl.includes('://')) {
      domain = new URL(domainOrUrl).hostname
    }
  } catch {}

  return {
    id: 'custom-' + domain.replace(/\./g, '-'),
    name: domain,
    domain,
    api: endpoint.replace(/\/v1\/.*$/, ''),
    endpoints: [endpoint],
    models: [],
    envKey: [],
    searchText: domain,
  }
}
