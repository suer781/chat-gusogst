package com.gusogst.chat.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 语义化触感反馈 — 对应 Web haptics.ts 的 15 个模式 + Grok 风格微震动
 *
 * Grok 风格：AI 回复逐字输出时触发超短微震动，
 * 利用线性马达的精准控制，营造"每个字都在震动"的高级质感。
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

    // ── Grok 风格微震动（超短、低振幅）──
    private var _lastMicroTick = 0L

    /**
     * 微震动 — AI 逐字输出时触发
     * 时长 2ms + 振幅 40（极轻），节流 60ms
     * 效果：线性马达的细腻质感，不是"嗡嗡"而是"沙沙"
     */
    fun microTick() {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - _lastMicroTick < 60) return  // 节流 60ms
        _lastMicroTick = now
        vibrate(2, 40)
    }

    /** 强微震动 — 用于段落结束 / 标点符号 */
    fun microTickStrong() {
        if (!enabled) return
        vibrate(4, 80)
    }

    // ── Impact 系列 ──
    fun light() { if (enabled) vibrate(10, 128) }
    fun medium() { if (enabled) vibrate(25, 192) }
    fun heavy() { if (enabled) vibrate(50, 255) }

    // ── Notification 系列 ──
    fun success() {
        if (!enabled) return
        vibrate(20, 180)
        handler.postDelayed({ vibrate(20, 160) }, 80)
    }

    fun warning() {
        if (!enabled) return
        vibrate(40, 200)
        handler.postDelayed({ vibrate(30, 180) }, 100)
    }

    fun error() {
        if (!enabled) return
        vibrate(40, 220)
        handler.postDelayed({ vibrate(40, 220) }, 120)
        handler.postDelayed({ vibrate(40, 220) }, 240)
    }

    // ── Selection 系列（含节流） ──
    private var _lastSelection = 0L

    fun selectionStart() { if (enabled) vibrate(5, 100) }

    fun selectionChanged() {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - _lastSelection < 80) return
        _lastSelection = now
        vibrate(5, 120)
    }

    fun selectionEnd() { light() }
    fun sliderTick() = selectionChanged()

    // ── 语义复合模式 ──
    fun glassTap() {
        if (!enabled) return
        vibrate(5, 140)
        handler.postDelayed({ vibrate(5, 120) }, 30)
    }

    fun glassPress() {
        if (!enabled) return
        vibrate(5, 130)
        handler.postDelayed({ vibrate(10, 180) }, 45)
    }

    private var _lastGlassSlide = 0L

    fun glassSlide() {
        if (!enabled) return
        val now = System.currentTimeMillis()
        if (now - _lastGlassSlide < 60) return
        _lastGlassSlide = now
        vibrate(3, 80)
    }

    fun sendPulse() {
        if (!enabled) return
        vibrate(15, 200)
        handler.postDelayed({ vibrate(8, 140) }, 60)
    }

    fun unfold() {
        if (!enabled) return
        vibrate(5, 110)
        handler.postDelayed({ vibrate(5, 100) }, 50)
    }

    /**
     * 清理所有 pending 的 Handler postDelayed 回调。
     * 在 Activity/Fragment onDestroy 中调用，防止内存泄漏。
     */
    fun destroy() {
        handler.removeCallbacksAndMessages(null)
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
