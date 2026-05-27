package com.gusogst.chat.util

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 语义化触感反馈 — 对应 Web haptics.ts 的 15 个模式
 *
 * 性能优化：
 *   - 使用原生 Vibrator API 而非 Web Navigator.vibrate()
 *   - 精确控制振幅和时长，比 Web 的 pattern 数组更省电
 *   - 节流机制防止高频触发（glassSlide: 60ms, selectionChanged: 80ms）
 */
class HapticsHelper(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    var enabled = true

    private val handler = Handler(Looper.getMainLooper())

    // ── Impact 系列 ──
    /** 轻触 — 按钮点击、tab 切换、卡片选择 */
    fun light() { if (enabled) vibrate(10, 128) }

    /** 中等 — toggle 开关、选项确认、重新生成 */
    fun medium() { if (enabled) vibrate(25, 192) }

    /** 重触 — 删除、重要确认、停止生成 */
    fun heavy() { if (enabled) vibrate(50, 255) }

    // ── Notification 系列 ──
    /** 成功 — 发送完成、保存成功、连接成功 */
    fun success() {
        if (!enabled) return
        vibrate(20, 180)
        handler.postDelayed({ vibrate(20, 160) }, 80)
    }

    /** 警告 — 参数越限、接近上限 */
    fun warning() {
        if (!enabled) return
        vibrate(40, 200)
        handler.postDelayed({ vibrate(30, 180) }, 100)
    }

    /** 错误 — 操作失败、校验不通过、连接断开 */
    fun error() {
        if (!enabled) return
        vibrate(40, 220)
        handler.postDelayed({ vibrate(40, 220) }, 120)
        handler.postDelayed({ vibrate(40, 220) }, 240)
    }

    // ── Selection 系列（含节流） ──
    private var _lastSelection = 0L

    /** 选择开始 — 滑块按下、选择器打开 */
    fun selectionStart() { if (enabled) vibrate(5, 100) }

    /** 选择变化 — 滑块拖动经过刻度、列表滚动经过项（节流 80ms） */
    fun selectionChanged() {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - _lastSelection < 80) return
        _lastSelection = now
        vibrate(5, 120)
    }

    /** 选择结束 — 滑块松手、选择器关闭 */
    fun selectionEnd() { light() }

    /** 滑块刻度（同 selectionChanged） */
    fun sliderTick() = selectionChanged()

    // ── 语义复合模式 ──
    /** 玻璃轻敲 — 清脆双击，模拟\"叮\"的感觉 */
    fun glassTap() {
        if (!enabled) return
        vibrate(5, 140)
        handler.postDelayed({ vibrate(5, 120) }, 30)
    }

    /** 玻璃按压 — 按下时的清脆确认感 */
    fun glassPress() {
        if (!enabled) return
        vibrate(5, 130)
        handler.postDelayed({ vibrate(10, 180) }, 45)
    }

    /** 玻璃滑动 — 滑过玻璃表面的颗粒感（节流 60ms） */
    private var _lastGlassSlide = 0L

    fun glassSlide() {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - _lastGlassSlide < 60) return
        _lastGlassSlide = now
        vibrate(3, 80)
    }

    /** 发送脉冲 — 消息发送按钮 */
    fun sendPulse() {
        if (!enabled) return
        vibrate(15, 200)
        handler.postDelayed({ vibrate(8, 140) }, 60)
    }

    /** 展开折纸 — 折叠面板展开/收起 */
    fun unfold() {
        if (!enabled) return
        vibrate(5, 110)
        handler.postDelayed({ vibrate(5, 100) }, 50)
    }

    // ── 底层 ──
    private fun vibrate(ms: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(ms, amplitude)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }
}
