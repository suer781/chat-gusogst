package com.gusogst.chat.util

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * HDR v4.0 glass glow helper - mirrors Web hdr_v3.css
 * Applies HDR glow effects to views when enabled
 */
object HdrHelper {

    data class HdrColors(
        val glowBase: Int, val glowAccent: Int, val glowWhite: Int,
        val borderHighlight: Int, val shadowGlow: Int, val bgTint: Int,
        val cardBorder: Int, val headerBg: Int, val navBg: Int, val bubbleTint: Int
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
        bubbleTint = Color.argb(20, 180, 140, 220)
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
        bubbleTint = Color.argb(15, 180, 100, 200)
    )

    /** Apply HDR glow to header view */
    fun applyHeaderGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { view.setBackgroundColor(Color.TRANSPARENT); return }
        val c = if (isDark) DARK else LIGHT
        view.setBackgroundColor(c.headerBg)
    }

    /** Apply HDR glow to nav bar */
    fun applyNavGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { view.setBackgroundColor(Color.TRANSPARENT); return }
        val c = if (isDark) DARK else LIGHT
        view.setBackgroundColor(c.navBg)
    }

    /** Apply HDR glow to card (glass-card equivalent) */
    fun applyCardGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { return }
        val c = if (isDark) DARK else LIGHT
        val bg = GradientDrawable().apply {
            setColor(c.bgTint)
            setStroke(1, c.cardBorder)
            cornerRadius = 16 * view.resources.displayMetrics.density
        }
        view.background = bg
        view.elevation = 4 * view.resources.displayMetrics.density
    }

    /** Apply HDR glow to message bubble */
    fun applyBubbleGlow(view: View, enabled: Boolean, isUser: Boolean, isDark: Boolean = true) {
        if (!enabled) { return }
        val c = if (isDark) DARK else LIGHT
        if (isUser) {
            val bg = view.background as? GradientDrawable ?: GradientDrawable()
            bg.setColor(c.bubbleTint)
            bg.setStroke(1, c.cardBorder)
            view.background = bg
        }
    }

    /** Apply HDR glow to accent button */
    fun applyButtonGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { return }
        val c = if (isDark) DARK else LIGHT
        view.elevation = 6 * view.resources.displayMetrics.density
    }
}
