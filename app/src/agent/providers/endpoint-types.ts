// ============================================================
// endpoint-types.ts — 端点选择系统类型定义
// ============================================================

/** 供应商端点信息 */
export interface EndpointEntry {
  url: string              // 完整端点 URL，如 "https://api.openai.com/v1/chat/completions"
  providerId: string       // 供应商 ID，如 "openai"
}

/** 端点评分状态（持久化） */
export interface EndpointRating {
  endpoint: string         // 完整 URL
  providerId: string       // 供应商 ID
  C_m: number              // 累积评分 [-10000, 10000]
  m: number                // 聊天次数
  errorCount: number       // 连续错误计数
  lastSuccess: number      // 最后成功时间戳
  lastFailure: number      // 最后失败时间戳
}

/** 供应商条目（四合一：字典+向量+评分+模型） */
export interface ProviderEntry {
  id: string               // 如 "nano-gpt"
  name: string             // 如 "NanoGPT"
  domain: string           // 从 api URL 提取，如 "nano-gpt.com"
  api: string              // API base URL，如 "https://nano-gpt.com/api/v1"
  endpoints: string[]      // 端点路径列表，如 ["/v1/chat/completions"]
  models: ModelInfo[]      // 模型列表
  envKey: string[]         // 环境变量名
  doc?: string             // 文档链接
  searchText: string       // 拼接的搜索文本（name + domain + model names）
}

export interface ModelInfo {
  id: string
  name: string
  context_length: number
  max_output: number
  cost_input: number
  cost_output: number
}

/** 端点选择结果 */
export interface EndpointSelection {
  endpoint: string         // 选中的端点 URL
  providerId: string       // 供应商 ID
  providerName: string     // 供应商名称
  confidence: number       // 匹配置信度 (0-1)
}

/** 端点测试结果 */
export interface EndpointTestResult {
  success: boolean
  endpoint: string
  providerId: string
  latencyMs: number
  error?: string
  isNetworkIssue: boolean  // true = 网络问题，false = 端点问题
}

/** 评分常量 */
export const RATING_CONSTANTS = {
  MIN: -10000,
  MAX: 10000,
  K_NUMERATOR: 12,          // k_m = K_NUMERATOR / (m + K_OFFSET)
  K_OFFSET: 15,
  DELTA_SUCCESS: 100,       // 成功时的 ΔC
  DELTA_FAILURE: -200,      // 失败时的 ΔC
  DELTA_SUCCESS_FIRST: 500, // 首次成功的 ΔC（快速建立信任）
  RETRY_COUNT: 3,           // 请求重试次数
  PING_RETRY_COUNT: 3,      // Ping 重试次数
  ERROR_THRESHOLD: 3,       // 开始计数错误的阈值
  ERROR_AMPLIFIER: 2,       // 错误放大系数
  NETWORK_TIMEOUT: 10000,   // 网络判定超时 (ms)
} as const
