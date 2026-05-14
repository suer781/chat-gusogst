// ============================================
// 搜索调度器
// 内置抓取引擎 + Tavily + 用户自定义 API 引擎
// 人设选引擎 → 按优先级逐个尝试 → fallback → 缓存
// ============================================

import type { SearchEngine, SearchResult, SearchOptions, EngineConfig, EngineConfigMap } from './types'
import { SearchCache } from './cache'
import { createBuiltinEngines, createCustomApiEngine, ENGINE_MAP } from './engines'
import type { CustomApiConfig, CustomApiState } from './engines'
import { DEFAULT_ENGINE_CHAIN, PRESETS } from './engines/registry'

const CUSTOM_API_STORAGE_KEY = 'search_custom_apis'

export interface SearchDispatcherOptions {
  personaEngines?: string[]
  engineConfigs?: EngineConfigMap
  fallbackToDefault?: boolean
}

export class SearchDispatcher {
  private cache = new SearchCache()
  private engineConfigs: EngineConfigMap
  private personaEngines: string[]
  private fallbackToDefault: boolean
  private engineInstances = new Map<string, SearchEngine>()
  private customApiConfigs = new Map<string, CustomApiConfig>()

  constructor(options: SearchDispatcherOptions = {}) {
    this.personaEngines = options.personaEngines || []
    this.engineConfigs = options.engineConfigs || {}
    this.fallbackToDefault = options.fallbackToDefault !== false

    for (const engine of createBuiltinEngines()) {
      this.engineInstances.set(engine.id, engine)
    }
    this.loadCustomApis()
  }

  // ── 配置更新 ──────────────────────

  updateConfigs(configs: EngineConfigMap): void {
    this.engineConfigs = configs
    this.cache.clear()
  }

  updatePersonaEngines(engines: string[]): void {
    this.personaEngines = engines
  }

  applyPreset(presetId: string): void {
    const preset = PRESETS[presetId]
    if (preset) this.personaEngines = [...preset.engines]
  }

  // ── 自定义 API 管理 ──────────────────────

  addCustomApi(config: CustomApiConfig): void {
    this.customApiConfigs.set(config.id, config)
    this.engineInstances.set(config.id, createCustomApiEngine(config))
    this.persistCustomApis()
    this.cache.clear()
  }

  removeCustomApi(id: string): void {
    this.customApiConfigs.delete(id)
    this.engineInstances.delete(id)
    this.persistCustomApis()
    this.cache.clear()
  }

  updateCustomApi(config: CustomApiConfig): void {
    this.customApiConfigs.set(config.id, config)
    this.engineInstances.set(config.id, createCustomApiEngine(config))
    this.persistCustomApis()
    this.cache.clear()
  }

  getCustomApis(): CustomApiConfig[] {
    return Array.from(this.customApiConfigs.values())
  }

  // ── 核心搜索 ──────────────────────

  async search(query: string, options?: SearchOptions): Promise<SearchResult[]> {
    const cached = this.cache.get(query)
    if (cached) return cached

    const chain = this.buildEngineChain()
    let lastError: Error | null = null

    for (const engineId of chain) {
      const engine = this.engineInstances.get(engineId)
      if (!engine) continue

      const config = this.getEngineConfig(engineId)
      if (!engine.isAvailable(config)) continue

      try {
        const results = await engine.search(query, options, config.apiKey)
        if (results.length > 0) {
          this.cache.set(query, results)
          return results
        }
      } catch (err) {
        lastError = err instanceof Error ? err : new Error(String(err))
        console.warn(`[Search] ${engineId} 失败:`, lastError.message)
        continue
      }
    }

    if (lastError) throw lastError
    throw new Error('没有可用的搜索引擎，请在设置中配置')
  }

  // ── 状态查询 ──────────────────────

  getEngineStatusList(): {
    id: string
    name: string
    category: string
    capabilities: SearchEngine['capabilities']
    config: EngineConfig
    isAvailable: boolean
  }[] {
    return Array.from(this.engineInstances.entries()).map(([id, engine]) => {
      const config = this.getEngineConfig(id)
      const meta = ENGINE_MAP.get(id)
      const category = meta?.category || (this.customApiConfigs.has(id) ? 'custom_api' : 'unknown')
      return { id, name: engine.name, category, capabilities: engine.capabilities, config, isAvailable: engine.isAvailable(config) }
    })
  }

  async validateEngineKey(engineId: string, key: string): Promise<boolean> {
    const engine = this.engineInstances.get(engineId)
    if (!engine?.validateKey) return false
    try { return await engine.validateKey(key) } catch { return false }
  }

  getPresets() { return PRESETS }

  getAllEngineIds(): string[] {
    return Array.from(this.engineInstances.keys())
  }

  // ── 私有方法 ──────────────────────

  private buildEngineChain(): string[] {
    const chain: string[] = []
    const seen = new Set<string>()

    for (const id of this.personaEngines) {
      if (!seen.has(id) && this.engineInstances.has(id)) {
        chain.push(id)
        seen.add(id)
      }
    }
    if (this.fallbackToDefault) {
      for (const id of DEFAULT_ENGINE_CHAIN) {
        if (!seen.has(id) && this.engineInstances.has(id)) {
          chain.push(id)
          seen.add(id)
        }
      }
    }
    return chain
  }

  private getEngineConfig(engineId: string): EngineConfig {
    return this.engineConfigs[engineId] || { enabled: true, mode: 'scrape' }
  }

  // ── 自定义 API 持久化 ──────────────────────

  private loadCustomApis(): void {
    try {
      const raw = localStorage.getItem(CUSTOM_API_STORAGE_KEY)
      if (!raw) return
      const state: CustomApiState = JSON.parse(raw)
      for (const config of state.configs) {
        this.customApiConfigs.set(config.id, config)
        this.engineInstances.set(config.id, createCustomApiEngine(config))
      }
    } catch { /* ignore */ }
  }

  private persistCustomApis(): void {
    const configs = Array.from(this.customApiConfigs.values())
    localStorage.setItem(CUSTOM_API_STORAGE_KEY, JSON.stringify({ configs }))
  }
}
