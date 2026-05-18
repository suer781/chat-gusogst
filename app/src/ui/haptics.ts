/**
 * 转子马达触觉反馈工具
 * 检测设备是否支持 Vibration API，支持时提供多种震动模式
 * 不支持时静默降级，不影响功能
 */

let _enabled = true

export function setHapticEnabled(v: boolean) { _enabled = v }

/** 设备是否支持震动 */
export function canVibrate(): boolean {
  return typeof navigator !== 'undefined' && 'vibrate' in navigator
}

function vibrate(pattern: number | number[]) {
  if (!_enabled || !canVibrate()) return
  try { navigator.vibrate(pattern) } catch {}
}

/** 轻触 — 按钮点击、tab 切换 */
export function light() { vibrate(10) }

/** 中等 — toggle 开关、选项选择 */
export function medium() { vibrate(25) }

/** 重触 — 删除、重要确认 */
export function heavy() { vibrate(50) }

/** 成功 — 发送成功、保存完成 */
export function success() { vibrate([15, 60, 15]) }

/** 错误 — 操作失败、校验不通过 */
export function error() { vibrate([40, 50, 40, 50, 40]) }

/** 滑块拖动 — 轻微连续反馈 */
export function sliderTick() { vibrate(5)}
