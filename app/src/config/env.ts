// ============================================
// 环境变量 & API Key 管理
// 所有 key 由用户在设置页手动填写，代码不预制
// ============================================

import type { EngineConfigMap } from '../search/types'

const STORAGE_KEY = 'chat-gusogst:search-config'

/**
 * 默认配置 — 全部 scrape 模式，无 key
 * 用户填了 key 后自动切换到 api 模式
 */
const DEFAULT_CONFIGS: EngineConfigMap = {
  tavily:    { enabled: true, mode: 'scrape' },
  duckduckgo: { enabled: true, mode: 'scrape' },
  baidu:     { enabled: true, mode: 'scrape' },
  quark:     { enabled: true, mode: 'scrape' },
  brave:     { enabled: true, mode: 'scrape' },
}

/**
 * 从 localStorage 加载搜索引擎配置
 */
export function loadSearchConfigs(): EngineConfigMap {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const saved = JSON.parse(raw) as EngineConfigMap
      // 合并：新引擎用默认值，已有的覆盖
      return { ...DEFAULT_CONFIGS, ...saved }
    }
  } catch {
    console.warn('[Config] 搜索配置解析失败，使用默认值')
  }
  return { ...DEFAULT_CONFIGS }
}

/**
 * 保存搜索引擎配置到 localStorage
 */
export function saveSearchConfigs(configs: EngineConfigMap): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(configs))
}

/**
 * 单个引擎的配置更新
 */
export function updateEngineConfig(
  engineId: string,
  patch: Partial<EngineConfigMap[string]>
): EngineConfigMap {
  const configs = loadSearchConfigs()
  const current = configs[engineId] || { enabled: true, mode: 'scrape' as const }
  configs[engineId] = { ...current, ...patch }
  saveSearchConfigs(configs)
  return configs
}

/**
 * 清除所有搜索引擎配置
 */
export function clearSearchConfigs(): void {
  localStorage.removeItem(STORAGE_KEY)
}
