/**
 * perf.ts — 设备性能检测模块
 *
 * 检测维度：
 *   1. CPU 核心数（navigator.hardwareConcurrency）
 *   2. 可用内存（navigator.deviceMemory）
 *   3. WebView / Chrome 版本（navigator.userAgent）
 *   4. 像素密度（window.devicePixelRatio）
 *   5. 是否可能在滑动/动画时掉帧（综合判断）
 *
 * 输出：
 *   - score: 0-10 的性能分
 *   - tier:  'high' | 'medium' | 'low'（硬件等级 ⭐⭐⭐ ⭐⭐ ⭐）
 *   - hint:  人类可读的性能描述
 *   - bumpy: 是否为"低端性能波动"设备（用于额外降级 HDR/glass）
 */

// ── 单次探测 ──
let _cache: PerfResult | null = null

export interface PerfResult {
  score: number // 0-10
  tier: 'high' | 'medium' | 'low'
  hint: string
  /** 当 bumpy=true 时应该对效果做更激进的降级 */
  bumpy: boolean
  /** 推荐的毛玻璃等级 */
  recommendedGlassTier: 'full' | 'light' | 'off'
}

/**
 * 从 UA 中提取 Chrome/WebView 主版本号。
 * Android WebView UA 格式：
 *   Mozilla/5.0 ... Chrome/123.0.6312.80 Mobile Safari/537.36
 */
function extractChromeVersion(): number {
  if (typeof navigator === 'undefined') return 0
  try {
    // Chrome/XXX. 或 CriOS/XXX.
    const m = navigator.userAgent.match(/(?:Chrome|CriOS)\/(\d+)/)
    if (m && m[1]) return parseInt(m[1], 10)
  } catch { /* 忽略 */ }
  return 0
}

/** 获取可用内存（GB），不支持返回 0 */
function getDeviceMemory(): number {
  // @ts-ignore — navigator.deviceMemory 不是标准属性
  if (typeof navigator !== 'undefined' && navigator.deviceMemory) {
    // @ts-ignore
    return navigator.deviceMemory
  }
  return 0
}

/** 获取 CPU 逻辑核心数 */
function getHardwareConcurrency(): number {
  if (typeof navigator !== 'undefined' && navigator.hardwareConcurrency) {
    return navigator.hardwareConcurrency
  }
  return 0
}

/** 获取设备像素比 */
function getDpr(): number {
  if (typeof window !== 'undefined' && window.devicePixelRatio) {
    return window.devicePixelRatio
  }
  return 1
}

/** 是否为低端浏览器（Chrome/WebView < 90 性能很差） */
function isOldBrowser(chromeVersion: number): boolean {
  return chromeVersion > 0 && chromeVersion < 90
}

/** 是否为中端浏览器 */
function isMidBrowser(chromeVersion: number): boolean {
  return chromeVersion >= 90 && chromeVersion < 110
}

/**
 * 综合评估 0-10 性能分，转换为推荐 tier
 *
 * 策略（UI 舒适度优先）：
 *  - 任何不可获取的参数（deviceMemory/hardwareConcurrency=0）→ 偏保守，不冒风险
 *  - chrome < 90: 直接视为 low（不支持新 CSS，backdrop-filter 会崩）
 *  - 可用内存 < 2G: 重度降级（WebView 可能被系统回收）
 *  - core < 4: 重度降级（多核才能 offload compositor）
 */
export function detectPerformance(): PerfResult {
  if (_cache) return _cache

  const chromeVersion = extractChromeVersion()
  const mem = getDeviceMemory()
  const cores = getHardwareConcurrency()
  const dpr = getDpr()

  let score = 5 // 基线

  // 减分项（越低越扣）
  if (mem > 0 && mem < 2)  score -= 3  // 可用内存 < 2G
  if (mem >= 2 && mem < 4) score -= 1  // 2-4G
  if (cores > 0 && cores < 4) score -= 2
  if (isOldBrowser(chromeVersion)) score -= 3
  if (isMidBrowser(chromeVersion)) score -= 1
  if (dpr >= 3) score -= 1  // 高分屏 (3x) 需要更多 VRAM 做模糊

  // 加分项
  if (mem >= 6) score += 1
  if (cores >= 8) score += 1
  if (chromeVersion >= 120) score += 1

  // 钳位
  if (score > 10) score = 10
  if (score < 0) score = 0

  // 确定等级和推荐
  let tier: 'high' | 'medium' | 'low'
  let recommendedGlassTier: PerfResult['recommendedGlassTier']
  let hint: string

  if (score >= 7) {
    tier = 'high'
    recommendedGlassTier = 'full'
    hint = '高性能：毛玻璃全开'
  } else if (score >= 4) {
    tier = 'medium'
    recommendedGlassTier = 'light'
    hint = '中性能：毛玻璃减弱'
  } else {
    tier = 'low'
    recommendedGlassTier = 'off'
    hint = '低性能：毛玻璃已自动关闭'
  }

  // bumpy：低端 + 中低端 Chrome 版本 → 画面可能抖动
  const bumpy = tier === 'low' || (tier === 'medium' && chromeVersion < 100)

  _cache = { score, tier, hint, bumpy, recommendedGlassTier }
  return _cache
}

/** 清缓存（用户手动切等级后调用） */
export function clearPerfCache() {
  _cache = null
}