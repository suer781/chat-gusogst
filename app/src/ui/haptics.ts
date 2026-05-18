import { Haptics, ImpactStyle, NotificationType } from '@capacitor/haptics'

let _enabled = true
let _capacitorAvailable = false

// 检测 Capacitor Haptics 是否可用
try {
  // @ts-ignore — Capacitor 在运行时注入
  if (typeof window !== 'undefined' && (window as any).Capacitor?.isNativePlatform?.()) {
    _capacitorAvailable = true
  }
} catch {}

export function setHapticEnabled(v: boolean) { _enabled = v }

/** 设备是否支持震动（任意后端） */
export function canVibrate(): boolean {
  if (_capacitorAvailable) return true
  return typeof navigator !== 'undefined' && 'vibrate' in navigator
}

/* ── 底层引擎 ── */

function webVibrate(pattern: number | number[]) {
  try { navigator.vibrate(pattern) } catch {}
}

async function impact(style: ImpactStyle) {
  if (!_enabled) return
  if (_capacitorAvailable) {
    try { await Haptics.impact({ style }) } catch { webVibrate(15) }
  } else {
    webVibrate(style === ImpactStyle.Heavy ? 50 : style === ImpactStyle.Medium ? 25 : 10)
  }
}

async function notification(type: NotificationType) {
  if (!_enabled) return
  if (_capacitorAvailable) {
    try { await Haptics.notification({ type }) } catch { webVibrate([15, 60, 15]) }
  } else {
    if (type === NotificationType.Success) webVibrate([15, 60, 15])
    else if (type === NotificationType.Warning) webVibrate([30, 40, 30])
    else webVibrate([40, 50, 40, 50, 40])
  }
}

async function selectionTick() {
  if (!_enabled) return
  if (_capacitorAvailable) {
    try { await Haptics.selectionStart() } catch { webVibrate(5) }
  } else {
    webVibrate(5)
  }
}

/* ═══════════════════════════════════════
   15 个语义化触感函数
   ═══════════════════════════════════════ */

// ── Impact 系列 ──
/** 轻触 — 按钮点击、tab 切换、卡片选择 */
export function light() { impact(ImpactStyle.Light) }
/** 中等 — toggle 开关、选项确认、重新生成 */
export function medium() { impact(ImpactStyle.Medium) }
/** 重触 — 删除、重要确认、停止生成 */
export function heavy() { impact(ImpactStyle.Heavy) }

// ── Notification 系列 ──
/** 成功 — 发送完成、保存成功、连接成功 */
export function success() { notification(NotificationType.Success) }
/** 警告 — 参数越限、接近上限 */
export function warning() { notification(NotificationType.Warning) }
/** 错误 — 操作失败、校验不通过、连接断开 */
export function error() { notification(NotificationType.Error) }

// ── Selection 系列 ──
/** 选择开始 — 滑块按下、选择器打开 */
export function selectionStart() { selectionTick() }
/** 选择变化 — 滑块拖动经过刻度、列表滚动经过项 */
let _lastSelection = 0
export function selectionChanged() {
  const now = Date.now()
  if (now - _lastSelection < 80) return // 节流 80ms
  _lastSelection = now
  selectionTick()
}
/** 选择结束 — 滑块松手、选择器关闭 */
export function selectionEnd() { impact(ImpactStyle.Light) }
/** 带节流的选择变化（滑块拖动专用） */
export const selectionChangedThrottled = selectionChanged

// ── 语义复合 ──
/** 玻璃轻敲 — 玻璃卡片点击 */
export function glassTap() { impact(ImpactStyle.Light) }
/** 发送脉冲 — 消息发送按钮 */
export function sendPulse() { impact(ImpactStyle.Medium) }
/** 展开折纸 — 折叠面板展开/收起 */
export function unfold() { impact(ImpactStyle.Light) }
/** 滑块刻度 — 兼容旧 API，等同 selectionChanged */
export function sliderTick() { selectionChanged() }
