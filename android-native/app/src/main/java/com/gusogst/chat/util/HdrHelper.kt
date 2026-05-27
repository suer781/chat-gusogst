package com.gusogst.chat.util

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * HDR v4.0 glass glow helper — mirrors Web hdr_v3.css
 *
 * Applies HDR glow effects to views when enabled.
 * Performance: uses simple color overlays + elevation shadows,
 * NOT CSS box-shadow which triggers repaint on every scroll.
 * Native Android drawables are GPU-composited for free.
 */
object HdrHelper {

    data class HdrColors(
        val glowBase: Int, val glowAccent: Int, val glowWhite: Int,
        val borderHighlight: Int, val shadowGlow: Int, val bgTint: Int,
        val cardBorder: Int, val headerBg: Int, val navBg: Int, val bubbleTint: Int,
        val buttonGlow: Int, val indicatorGlow: Int, val inputFocusGlow: Int
    )

    // Dark theme HDR colors (from CSS vars)
    val DARK = HdrColors(
        glowBase = Color.argb(230, 220, 225, 245),
        glowAccent = Color.argb(230, 220, 100, 140),
        glowWhite = Color.argb(242, 255, 255, 255),
        borderHighlight = Color.argb(102, 180, 160, 220),
        shadowGlow = Color.argb(64, 200, 100, 150),
        bgTint = Color.argb(15, 180, 120, 200),
        cardBorder = Color.argb(77, 180, 160, 220),
        headerBg = Color.argb(20, 180, 120, 200),
        navBg = Color.argb(13, 180, 120, 200),
        bubbleTint = Color.argb(20, 180, 140, 220),
        buttonGlow = Color.argb(64, 200, 100, 150),
        indicatorGlow = Color.argb(230, 220, 100, 140),
        inputFocusGlow = Color.argb(64, 200, 100, 150)
    )

    // Light theme HDR colors
    val LIGHT = HdrColors(
        glowBase = Color.argb(230, 220, 225, 245),
        glowAccent = Color.argb(217, 180, 60, 100),
        glowWhite = Color.argb(242, 255, 255, 255),
        borderHighlight = Color.argb(77, 160, 100, 200),
        shadowGlow = Color.argb(38, 180, 80, 140),
        bgTint = Color.argb(10, 180, 100, 200),
        cardBorder = Color.argb(64, 160, 100, 200),
        headerBg = Color.argb(15, 180, 100, 200),
        navBg = Color.argb(10, 180, 100, 200),
        bubbleTint = Color.argb(15, 180, 100, 200),
        buttonGlow = Color.argb(38, 180, 80, 140),
        indicatorGlow = Color.argb(217, 180, 60, 100),
        inputFocusGlow = Color.argb(38, 180, 80, 140)
    )

    // ── 公开 API ──

    /** Apply HDR glow to header view */
    fun applyHeaderGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { view.setBackgroundColor(Color.TRANSPARENT); return }
        val c = if (isDark) DARK else LIGHT
        view.setBackgroundColor(c.headerBg)
        view.elevation = 2f * view.resources.displayMetrics.density
    }

    /** Apply HDR glow to nav bar */
    fun applyNavGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { view.setBackgroundColor(Color.TRANSPARENT); return }
        val c = if (isDark) DARK else LIGHT
        view.setBackgroundColor(c.navBg)
    }

    /** Apply HDR glow to card (glass-card equivalent) */
    fun applyCardGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        val bg = GradientDrawable().apply {
            setColor(c.bgTint)
            setStroke(1, c.cardBorder)
            cornerRadius = 16 * view.resources.displayMetrics.density
        }
        view.background = bg
        view.elevation = 4f * view.resources.displayMetrics.density
    }

    /** Apply HDR glow to any message bubble */
    fun applyBubbleGlow(view: View, enabled: Boolean, isUser: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        if (isUser) {
            val bg = view.background as? GradientDrawable ?: GradientDrawable()
            bg.setColor(c.bubbleTint)
            bg.setStroke(1, c.cardBorder)
            view.background = bg
        }
        // 非 user 气泡（assistant）只加边框辉光
        view.elevation = 2f * view.resources.displayMetrics.density
    }

    /** Apply HDR glow to accent button */
    fun applyButtonGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        view.elevation = 6f * view.resources.displayMetrics.density
    }

    /** Apply HDR glow to nav indicator */
    fun applyIndicatorGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        // indicator glow 通过半透明底色实现——比 CSS filter/backdrop-filter 省 GPU
        view.setBackgroundColor(c.indicatorGlow)
    }

    /** Apply HDR glow to input focus state */
    fun applyInputGlow(view: View, enabled: Boolean, hasFocus: Boolean, isDark: Boolean = true) {
        if (!enabled || !hasFocus) return
        val c = if (isDark) DARK else LIGHT
        view.elevation = 3f * view.resources.displayMetrics.density
    }
}
