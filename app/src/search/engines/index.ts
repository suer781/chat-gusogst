// ============================================
// 引擎实例工厂
// 从 registry 生成所有引擎实例 + 支持自定义 API
// ============================================

import type { SearchEngine } from '../types'
import { ALL_ENGINES, ENGINE_MAP } from './registry'
import { ScrapeEngine } from './scrape-engine'
import { TavilyEngine } from './tavily'
import { CustomApiEngine } from './custom-api'
import type { CustomApiConfig } from './custom-api'
export { ALL_ENGINES, ENGINE_MAP } from './registry'
export type { EngineMeta, EngineCategory } from './registry'
export { CustomApiEngine, API_TEMPLATES } from './custom-api'
export type { CustomApiConfig, CustomApiState } from './custom-api'

/** 所有内置引擎实例（不含自定义 API） */
export function createBuiltinEngines(): SearchEngine[] {
  const engines: SearchEngine[] = []
  for (const meta of ALL_ENGINES) {
    engines.push(new ScrapeEngine(meta))
  }
  return engines
}

/** 创建自定义 API 引擎实例 */
export function createCustomApiEngine(config: CustomApiConfig): SearchEngine {
  return new CustomApiEngine(config)
}

/** 获取内置引擎元数据（纯查询，不需要实例） */
export function getEngineMeta(id: string) {
  return ENGINE_MAP.get(id) || null
}
