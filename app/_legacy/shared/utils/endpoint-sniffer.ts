/**
 * API Endpoint Sniffer
 * 动态路径嗅探系统：自动探测供应商 API 端点，基于信心值排序，支持热更新。
 *
 * 核心公式：Cn = clamp(Cn-1 + ΔC - k × Cn-1, -10000, 10000)
 *   - Cn-1: 上次信心值
 *   - ΔC: 本次加减分
 *   - k: 衰减系数，随聊天消息数增长（消息越多 k 越小，加减越细腻）
 *   - clamp: 信心值范围 [-10000, +10000]
 *
 * 流程：
 *   1. 用户只填域名 → 首次使用触发嗅探
 *   2. 按信心值降序试路径，每个路径试两次独立调用
 *   3. 两次都成功 → 定性 → 热更新写入，信心大幅 +ΔC
 *   4. 失败 → 轻微降权，换路径
 *   5. 所有路径试完 → ping 域名
 *     - 能通：字典里没这个端点，提示用户
 *     - 不通：网络问题，回档信心值
 */

// ============================================================
// 类型定义
// ============================================================

/** 单个路径的信心记录 */
export interface PathConfidence {
  path: string
  confidence: number          // [-10000, +10000]
  successCount: number        // 累计成功次数
  failCount: number           // 累计失败次数
  lastSuccessAt: number       // 最后成功时间戳
  lastFailAt: number          // 最后失败时间戳
  confirmed: boolean          // 是否已定性（连续两次成功）
}

/** 域名级别的信心库 */
export interface HostConfidenceRecord {
  host: string
  paths: PathConfidence[]
  messageCount: number        // 该域名累计聊天消息数（用于算 k）
  snapshot?: PathConfidence[] // 回档快照（ping 失败时恢复）
}

/** 探测结果 */
export interface SniffResult {
  success: boolean
  host: string
  path: string | null
  confidence: number
  mode: 'hot' | 'sniffed' | 'failed'
  // hot: 直接走热路径
  // sniffed: 嗅探找到的新路径
  // failed: 所有路径都失败
  networkDown: boolean        // 是否判定为网络问题
}

/** 嗅探器配置 */
export interface SnifferConfig {
  /** 初始信心值（新路径首次成功时） */
  initialConfidence: number
  /** 成功加权 ΔC */
  successDelta: number
  /** 失败减权 ΔC */
  failDelta: number
  /** 定性成功加权（两次确认后的大奖励） */
  confirmedDelta: number
  /** 衰减系数初始值 */
  kInitial: number
  /** 衰减系数最小值（消息很多时 k 不会低于此） */
  kMin: number
  /** 衰减系数衰减速度（每条消息减少多少 k） */
  kDecayRate: number
  /** 请求超时 ms */
  timeoutMs: number
  /** Ping 超时 ms */
  pingTimeoutMs: number
  /** 候选路径字典 */
  candidatePaths: string[]
}

/** 默认候选路径字典 */
const DEFAULT_CANDIDATE_PATHS: string[] = [
  '/v1/chat/completions',
  '/v1/messages',
  '/chat/completions',
  '/v1/completions',
  '/v1beta/models',
  '/api/v1/chat/completions',
  '/api/v1/messages',
  '/openai/v1/chat/completions',
]

/** 默认配置 */
const DEFAULT_CONFIG: SnifferConfig = {
  initialConfidence: 100,
  successDelta: 200,
  failDelta: -50,
  confirmedDelta: 500,
  kInitial: 0.05,
  kMin: 0.001,
  kDecayRate: 0.001,
  timeoutMs: 8000,
  pingTimeoutMs: 3000,
  candidatePaths: DEFAULT_CANDIDATE_PATHS,
}

// ============================================================
// 信心值公式
// ============================================================

/**
 * 核心公式：Cn = clamp(Cn-1 + ΔC - k × Cn-1, -10000, 10000)
 */
function applyConfidence(
  current: number,
  delta: number,
  messageCount: number,
  config: SnifferConfig
): number {
  // k 随消息数衰减：前期激进，后期细腻
  const k = Math.max(
    config.kMin,
    config.kInitial - messageCount * config.kDecayRate
  )
  const raw = current + delta - k * current
  return Math.max(-10000, Math.min(10000, Math.round(raw)))
}

// ============================================================
// 信心库持久化
// ============================================================

const STORAGE_KEY = 'endpoint-sniffer-confidence'

function loadConfidenceStore(): Record<string, HostConfidenceRecord> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

function saveConfidenceStore(store: Record<string, HostConfidenceRecord>): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(store))
  } catch {
    // storage full or unavailable, silently fail
  }
}

// ============================================================
// 工具函数
// ============================================================

/**
 * 从 URL 或域名中提取 host（含协议，不含路径）
 */
export function extractHost(input: string): string {
  let trimmed = input.trim()
  if (!trimmed) return ''
  // 补协议
  if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://')) {
    trimmed = 'https://' + trimmed
  }
  try {
    const url = new URL(trimmed)
    return url.origin
  } catch {
    return trimmed.replace(/\/+$/, '')
  }
}

/**
 * 排序信心路径（降序）
 */
function sortPaths(paths: PathConfidence[]): PathConfidence[] {
  return [...paths].sort((a, b) => b.confidence - a.confidence)
}

/**
 * 确认域名是否有路径（用户只填了域名，没填路径）
 */
export function isHostOnly(apiHost: string, apiPath?: string): boolean {
  if (apiPath && apiPath.trim()) return false
  try {
    const url = new URL(apiHost.startsWith('http') ? apiHost : 'https://' + apiHost)
    // 如果路径只有 / 或空，认为只有域名
    return !url.pathname || url.pathname === '/'
  } catch {
    return true
  }
}

// ============================================================
// 探测器类
// ============================================================

export class EndpointSniffer {
  private config: SnifferConfig
  private store: Record<string, HostConfidenceRecord>
  private pendingConfirm: Map<string, { path: string; firstSuccessAt: number }> = new Map()

  constructor(config?: Partial<SnifferConfig>) {
    this.config = { ...DEFAULT_CONFIG, ...config }
    this.store = loadConfidenceStore()
  }

  /**
   * 获取或创建域名记录
   */
  private getOrCreateHost(host: string): HostConfidenceRecord {
    if (!this.store[host]) {
      this.store[host] = {
        host,
        paths: this.config.candidatePaths.map(p => ({
          path: p,
          confidence: 0,
          successCount: 0,
          failCount: 0,
          lastSuccessAt: 0,
          lastFailAt: 0,
          confirmed: false,
        })),
        messageCount: 0,
      }
    }
    return this.store[host]
  }

  /**
   * 对某个路径发送探测请求
   * 用轻量级 GET 或 HEAD 请求测试端点是否可达
   */
  private async probePath(host: string, path: string): Promise<boolean> {
    const url = host + path
    try {
      const controller = new AbortController()
      const timer = setTimeout(() => controller.abort(), this.config.timeoutMs)

      // 用 OPTIONS/GET 探测，不发完整聊天请求
      const resp = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          model: 'test',
          messages: [{ role: 'user', content: 'hi' }],
          max_tokens: 1,
        }),
        signal: controller.signal,
      })

      clearTimeout(timer)

      // 2xx 或 4xx（认证错误等）都算路径存在
      // 只有 404/405 才算路径不存在
      if (resp.status === 404 || resp.status === 405) {
        return false
      }
      return true
    } catch {
      return false
    }
  }

  /**
   * Ping 域名（简单网络连通性检测）
   */
  private async pingHost(host: string): Promise<boolean> {
    try {
      const controller = new AbortController()
      const timer = setTimeout(() => controller.abort(), this.config.pingTimeoutMs)
      const resp = await fetch(host, {
        method: 'HEAD',
        signal: controller.signal,
      })
      clearTimeout(timer)
      return resp.status > 0 // 只要能收到响应就算通
    } catch {
      return false
    }
  }

  /**
   * 持久化
   */
  private persist(): void {
    saveConfidenceStore(this.store)
  }

  /**
   * 主入口：嗅探端点
   * 返回最终可用的完整路径
   */
  async sniff(apiHost: string): Promise<SniffResult> {
    const host = extractHost(apiHost)
    if (!host) {
      return { success: false, host: '', path: null, confidence: 0, mode: 'failed', networkDown: false }
    }

    const record = this.getOrCreateHost(host)

    // 创建快照（用于 ping 失败时回档）
    record.snapshot = record.paths.map(p => ({ ...p }))

    // 按信心值降序排列
    const sorted = sortPaths(record.paths)

    for (const pathConf of sorted) {
      if (pathConf.confidence < -5000) continue // 信心太低的跳过

      // 第一次探测
      const ok1 = await this.probePath(host, pathConf.path)
      if (ok1) {
        // 第一次成功 → 加权
        record.messageCount++
        pathConf.confidence = applyConfidence(
          pathConf.confidence,
          this.config.successDelta,
          record.messageCount,
          this.config
        )
        pathConf.successCount++
        pathConf.lastSuccessAt = Date.now()

        // 第二次探测
        const ok2 = await this.probePath(host, pathConf.path)
        if (ok2) {
          // 两次都成功 → 定性 → 大幅加权 → 热更新
          record.messageCount++
          pathConf.confidence = applyConfidence(
            pathConf.confidence,
            this.config.confirmedDelta,
            record.messageCount,
            this.config
          )
          pathConf.successCount++
          pathConf.lastSuccessAt = Date.now()
          pathConf.confirmed = true
          record.snapshot = undefined // 清除快照
          this.persist()

          return {
            success: true,
            host,
            path: pathConf.path,
            confidence: pathConf.confidence,
            mode: 'sniffed',
            networkDown: false,
          }
        } else {
          // 第一次成功第二次失败 → 轻微降权
          record.messageCount++
          pathConf.confidence = applyConfidence(
            pathConf.confidence,
            this.config.failDelta,
            record.messageCount,
            this.config
          )
          pathConf.failCount++
          pathConf.lastFailAt = Date.now()
        }
      } else {
        // 第一次失败 → 降权，换下一个路径
        record.messageCount++
        pathConf.confidence = applyConfidence(
          pathConf.confidence,
          this.config.failDelta,
          record.messageCount,
          this.config
        )
        pathConf.failCount++
        pathConf.lastFailAt = Date.now()
      }
    }

    // 所有路径都试过了 → ping
    const pingOk = await this.pingHost(host)
    if (!pingOk) {
      // 网络不通 → 回档信心值
      if (record.snapshot) {
        record.paths = record.snapshot
        record.snapshot = undefined
      }
      this.persist()
      return {
        success: false,
        host,
        path: null,
        confidence: 0,
        mode: 'failed',
        networkDown: true,
      }
    }

    // Ping 通了但所有路径都不行 → 字典里没这个端点
    record.snapshot = undefined
    this.persist()
    return {
      success: false,
      host,
      path: null,
      confidence: 0,
      mode: 'failed',
      networkDown: false,
    }
  }

  /**
   * 获取热路径（已定性的最高信心路径）
   */
  getHotPath(apiHost: string): string | null {
    const host = extractHost(apiHost)
    const record = this.store[host]
    if (!record) return null

    const confirmed = record.paths.filter(p => p.confirmed)
    if (confirmed.length === 0) return null

    const sorted = sortPaths(confirmed)
    return sorted[0].path
  }

  /**
   * 记录一次成功调用（用户正常聊天成功后调用）
   */
  recordSuccess(apiHost: string, path: string): void {
    const host = extractHost(apiHost)
    const record = this.getOrCreateHost(host)
    record.messageCount++

    let pathConf = record.paths.find(p => p.path === path)
    if (!pathConf) {
      pathConf = {
        path,
        confidence: 0,
        successCount: 0,
        failCount: 0,
        lastSuccessAt: 0,
        lastFailAt: 0,
        confirmed: false,
      }
      record.paths.push(pathConf)
    }

    pathConf.confidence = applyConfidence(
      pathConf.confidence,
      this.config.successDelta,
      record.messageCount,
      this.config
    )
    pathConf.successCount++
    pathConf.lastSuccessAt = Date.now()

    // 检查是否可以定性（连续两次成功）
    if (!pathConf.confirmed && pathConf.successCount >= 2) {
      // 检查最近两次是否连续成功（简化：successCount >= 2 即可定性）
      pathConf.confirmed = true
      pathConf.confidence = applyConfidence(
        pathConf.confidence,
        this.config.confirmedDelta,
        record.messageCount,
        this.config
      )
    }

    this.persist()
  }

  /**
   * 记录一次失败调用（用户聊天失败后调用）
   * 失败超过 2 次触发重新嗅探
   */
  recordFailure(apiHost: string, path: string): boolean {
    const host = extractHost(apiHost)
    const record = this.getOrCreateHost(host)
    record.messageCount++

    let pathConf = record.paths.find(p => p.path === path)
    if (!pathConf) return true // 未知路径，触发嗅探

    pathConf.confidence = applyConfidence(
      pathConf.confidence,
      this.config.failDelta,
      record.messageCount,
      this.config
    )
    pathConf.failCount++
    pathConf.lastFailAt = Date.now()
    this.persist()

    // 连续失败 >= 2 次 → 触发重新嗅探
    return pathConf.failCount >= 2
  }

  /**
   * 同时更新加减权（供外部直接调用公式）
   */
  updateConfidence(
    apiHost: string,
    path: string,
    success: boolean
  ): void {
    if (success) {
      this.recordSuccess(apiHost, path)
    } else {
      this.recordFailure(apiHost, path)
    }
  }

  /**
   * 获取指定域名的完整信心库（调试用）
   */
  getDebugInfo(apiHost: string): HostConfidenceRecord | null {
    const host = extractHost(apiHost)
    return this.store[host] || null
  }

  /**
   * 清除指定域名的记录
   */
  clearHost(apiHost: string): void {
    const host = extractHost(apiHost)
    delete this.store[host]
    this.persist()
  }

  /**
   * 获取所有记录
   */
  getAllRecords(): Record<string, HostConfidenceRecord> {
    return { ...this.store }
  }
}

// ============================================================
// 单例导出
// ============================================================

let _instance: EndpointSniffer | null = null

export function getEndpointSniffer(config?: Partial<SnifferConfig>): EndpointSniffer {
  if (!_instance) {
    _instance = new EndpointSniffer(config)
  }
  return _instance
}
