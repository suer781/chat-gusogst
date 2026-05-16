import type { HermesConfig, PlatformInfo, HermesCapabilities } from './types'

/**
 * HermesClient — 与 Hermes 后端通信
 *
 * 聊天走 OpenAI provider（apiHost = hermes.baseUrl）
 * 本类只管平台管理相关的 API
 */
export class HermesClient {
  private config: HermesConfig
  private cachedCapabilities: HermesCapabilities | null = null
  private lastFetch = 0

  constructor(config: HermesConfig) {
    this.config = config
  }

  updateConfig(config: HermesConfig): void {
    this.config = config
    this.cachedCapabilities = null
  }

  async isOnline(): Promise<boolean> {
    try {
      const res = await this.get('/v1/models')
      return res.ok
    } catch {
      return false
    }
  }

  async getPlatforms(): Promise<PlatformInfo[]> {
    try {
      const res = await this.get('/v1/platforms')
      if (!res.ok) return []
      const data = await res.json()
      return (data.platforms ?? []).map((p: any) => ({
        name: p.name,
        displayName: p.display_name ?? p.name,
        enabled: p.enabled ?? true,
        connected: p.connected ?? false,
        status: p.status ?? 'inactive',
        error: p.error,
        type: p.type ?? 'im',
        icon: p.icon ?? '💬',
      }))
    } catch {
      return []
    }
  }

  async togglePlatform(name: string, enabled: boolean): Promise<boolean> {
    try {
      const res = await this.post(`/v1/platforms/${name}/${enabled ? 'enable' : 'disable'}`)
      return res.ok
    } catch {
      return false
    }
  }

  async getCapabilities(): Promise<HermesCapabilities> {
    if (this.cachedCapabilities && Date.now() - this.lastFetch < 30000) {
      return this.cachedCapabilities
    }

    try {
      const res = await this.get('/v1/capabilities')
      if (!res.ok) throw new Error('not ok')
      const data = await res.json()

      this.cachedCapabilities = {
        platforms: data.platforms ?? [],
        models: data.models ?? [],
        features: data.features ?? [],
      }
      this.lastFetch = Date.now()
      return this.cachedCapabilities
    } catch {
      return { platforms: [], models: [], features: [] }
    }
  }

  private getHeaders(): Record<string, string> {
    const h: Record<string, string> = { 'Content-Type': 'application/json' }
    if (this.config.apiKey) {
      h['Authorization'] = `Bearer ${this.config.apiKey}`
    }
    return h
  }

  private get(path: string): Promise<Response> {
    const url = `${this.config.baseUrl.replace(/\/+$/, '')}${path}`
    return fetch(url, {
      method: 'GET',
      headers: this.getHeaders(),
      signal: AbortSignal.timeout(this.config.timeout ?? 10000),
    })
  }

  private post(path: string, body?: any): Promise<Response> {
    const url = `${this.config.baseUrl.replace(/\/+$/, '')}${path}`
    return fetch(url, {
      method: 'POST',
      headers: this.getHeaders(),
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(this.config.timeout ?? 10000),
    })
  }
}
