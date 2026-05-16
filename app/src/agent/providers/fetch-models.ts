/**
 * 从各 LLM 供应商 API 动态拉取可用模型列表
 * 4 种 API 格式覆盖所有主流供应商:
 *   openai           — OpenAI Chat Completions 兼容 (含 /v1, /v3, /v4 变体)
 *   openai-responses — OpenAI Responses API 兼容 (新格式)
 *   anthropic        — Anthropic 原生 (x-api-key header)
 *   google           — Google Gemini (URL query key=)
 */

export interface FetchedModel {
  id: string
  owned_by?: string
  created?: number
  displayName?: string
}

export type ApiType = 'openai' | 'openai-responses' | 'anthropic' | 'google'

async function fetchWithTimeout(
  url: string, init: RequestInit, timeout: number
): Promise<Response> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeout)
  try {
    const res = await fetch(url, { ...init, signal: controller.signal })
    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`)
    return res
  } finally {
    clearTimeout(timer)
  }
}

/**
 * OpenAI Chat Completions 兼容端点 — 自动探测版本路径
 * 标准: GET {base}/v1/models
 * 变体: {base}/v3/models (火山引擎), {base}/v4/models (智谱GLM)
 */
async function fetchOpenAI(
  baseUrl: string, apiKey: string, timeout: number
): Promise<FetchedModel[]> {
  const base = baseUrl.replace(/\/+$/, '')
  const versionMatch = base.match(/\/v\d+$/)
  const paths = versionMatch ? [''] : ['/v1', '/v3', '/v4']

  for (const path of paths) {
    try {
      const url = `${base}${path}/models`
      const res = await fetchWithTimeout(url, {
        headers: {
          'Authorization': `Bearer ${apiKey}`,
          'Content-Type': 'application/json',
        },
      }, timeout)
      const data = await res.json()
      const models: any[] = data.data ?? data.models ?? []
      if (models.length > 0) {
        return models
          .map(m => typeof m === 'string'
            ? { id: m }
            : { id: m.id, owned_by: m.owned_by, created: m.created }
          )
          .filter(m => m.id)
      }
    } catch {
      continue
    }
  }
  return []
}

/**
 * OpenAI Responses API 兼容端点
 * 使用 POST /v1/responses 格式，但模型列表仍走 GET /v1/models
 */
async function fetchOpenAIResponses(
  baseUrl: string, apiKey: string, timeout: number
): Promise<FetchedModel[]> {
  // Responses API 的模型列表端点和 Chat Completions 相同
  const base = baseUrl.replace(/\/+$/, '')
  const versionMatch = base.match(/\/v\d+$/)
  const paths = versionMatch ? [''] : ['/v1', '/v3']

  for (const path of paths) {
    try {
      const url = `${base}${path}/models`
      const res = await fetchWithTimeout(url, {
        headers: {
          'Authorization': `Bearer ${apiKey}`,
          'Content-Type': 'application/json',
        },
      }, timeout)
      const data = await res.json()
      const models: any[] = data.data ?? data.models ?? []
      if (models.length > 0) {
        return models
          .map(m => typeof m === 'string'
            ? { id: m }
            : { id: m.id, owned_by: m.owned_by, created: m.created }
          )
          .filter(m => m.id)
      }
    } catch {
      continue
    }
  }
  return []
}

/**
 * Anthropic 原生端点
 * GET /v1/models?limit=100  header: x-api-key + anthropic-version
 */
async function fetchAnthropic(
  baseUrl: string, apiKey: string, timeout: number
): Promise<FetchedModel[]> {
  const url = `${baseUrl.replace(/\/+$/, '')}/v1/models?limit=100`
  const res = await fetchWithTimeout(url, {
    headers: {
      'x-api-key': apiKey,
      'anthropic-version': '2023-06-01',
      'Content-Type': 'application/json',
    },
  }, timeout)
  const data = await res.json()
  const models: any[] = data.data ?? []
  return models
    .map(m => ({ id: m.id, displayName: m.display_name, created: m.created_at }))
    .filter(m => m.id)
}

/**
 * Google Gemini 端点
 * GET /v1beta/models?key=xxx
 */
async function fetchGoogle(
  baseUrl: string, apiKey: string, timeout: number
): Promise<FetchedModel[]> {
  const url = `${baseUrl.replace(/\/+$/, '')}/v1beta/models?key=${apiKey}`
  const res = await fetchWithTimeout(url, {}, timeout)
  const data = await res.json()
  const models: any[] = data.models ?? []
  return models
    .map(m => {
      const id = (m.name ?? '').replace('models/', '')
      return { id, displayName: m.displayName }
    })
    .filter(m => m.id)
}

/**
 * 根据 API 类型自动选择拉取方式
 */
export async function fetchModels(
  baseUrl: string,
  apiKey: string,
  apiType: ApiType = 'openai',
  timeout = 10000
): Promise<FetchedModel[]> {
  try {
    switch (apiType) {
      case 'anthropic':        return await fetchAnthropic(baseUrl, apiKey, timeout)
      case 'google':           return await fetchGoogle(baseUrl, apiKey, timeout)
      case 'openai-responses': return await fetchOpenAIResponses(baseUrl, apiKey, timeout)
      default:                 return await fetchOpenAI(baseUrl, apiKey, timeout)
    }
  } catch (e) {
    console.warn(`[fetchModels] Failed (${apiType}):`, e)
    return []
  }
}

/**
 * 拉取并过滤出 chat/text 模型
 */
export async function fetchChatModels(
  baseUrl: string,
  apiKey: string,
  apiType: ApiType = 'openai',
  timeout?: number
): Promise<string[]> {
  const excludePatterns = [
    /embed/i, /tts/i, /whisper/i, /dall-e/i, /image.*gen/i, /audio/i,
    /moderation/i, /bge-/i, /e5-/i, /rerank/i,
  ]
  const models = await fetchModels(baseUrl, apiKey, apiType, timeout)
  return models
    .map(m => m.id)
    .filter(id => !excludePatterns.some(p => p.test(id)))
}
