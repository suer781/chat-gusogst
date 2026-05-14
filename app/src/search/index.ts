// ============================================
// 搜索模块统一入口
// ============================================

export { SearchDispatcher } from './dispatcher'
export type { SearchDispatcherOptions } from './dispatcher'

export type {
  SearchOptions,
  SearchResult,
  EngineCapabilities,
  EngineConfig,
  EngineConfigMap,
  SearchEngine,
} from './types'

export { SearchCache } from './cache'

export {
  ALL_ENGINES,
  ENGINE_MAP,
  createBuiltinEngines,
  createCustomApiEngine,
  getEngineMeta,
  CustomApiEngine,
  API_TEMPLATES,
} from './engines'
export type { EngineMeta, EngineCategory, CustomApiConfig, CustomApiState } from './engines'

export { PRESETS, DEFAULT_ENGINE_CHAIN } from './engines/registry'
