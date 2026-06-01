package com.gusogst.chat.util

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ColorDrawable
import android.view.View

/**
 * HDR v4.0 glass glow helper
 */
object HdrHelper {

    data class HdrColors(
        val glowBase: Int, val glowAccent: Int, val glowWhite: Int,
        val borderHighlight: Int, val shadowGlow: Int, val bgTint: Int,
        val cardBorder: Int, val headerBg: Int, val navBg: Int, val bubbleTint: Int,
        val buttonGlow: Int, val indicatorGlow: Int, val inputFocusGlow: Int,
        val reflectionHighlight: Int
    )

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
        inputFocusGlow = Color.argb(64, 200, 100, 150),
        reflectionHighlight = Color.argb(60, 255, 255, 255)
    )

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
        inputFocusGlow = Color.argb(38, 180, 80, 140),
        reflectionHighlight = Color.argb(40, 255, 255, 255)
    )

    fun applyGlassWithHdr(view: View, enabled: Boolean, glassEnabled: Boolean, isDark: Boolean = true) {
        if (!enabled && !glassEnabled) { view.background = null; return }
        val c = if (isDark) DARK else LIGHT
        val res = view.resources
        val bgGradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                if (glassEnabled) 0x23FFFFFF else Color.TRANSPARENT,
                if (enabled) c.headerBg else Color.TRANSPARENT
            )
        )
        bgGradient.cornerRadius = 0f
        if (enabled) {
            val reflection = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(c.reflectionHighlight, Color.TRANSPARENT)
            )
            val layers = arrayOf<android.graphics.drawable.Drawable>(bgGradient, reflection)
            view.background = LayerDrawable(layers)
        } else {
            view.background = bgGradient
        }
        view.elevation = if (enabled) 3f * view.resources.displayMetrics.density
                         else if (glassEnabled) 2f * view.resources.displayMetrics.density
                         else 0f
    }

    fun applyHeaderGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { view.setBackgroundColor(Color.TRANSPARENT); return }
        val c = if (isDark) DARK else LIGHT
        val bottom = ColorDrawable(c.headerBg)
        val reflection = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(c.reflectionHighlight, Color.TRANSPARENT)
        )
        view.background = LayerDrawable(arrayOf(bottom, reflection))
        view.elevation = 2f * view.resources.displayMetrics.density
    }

    fun applyNavGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { view.setBackgroundColor(Color.TRANSPARENT); return }
        val c = if (isDark) DARK else LIGHT
        val bottom = ColorDrawable(c.navBg)
        val reflection = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.TRANSPARENT, c.reflectionHighlight)
        )
        view.background = LayerDrawable(arrayOf(bottom, reflection))
    }

    fun applyCardGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            setColor(c.bgTint)
            setStroke(1, c.cardBorder)
            cornerRadius = 16f * density
        }
        view.background = bg
        view.elevation = 4f * density
    }

    fun applyBubbleGlow(view: View, enabled: Boolean, isUser: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        if (isUser) {
            val bg = GradientDrawable().apply {
                setColor(c.bubbleTint)
                setStroke(1, c.cardBorder)
                cornerRadius = 16f * density
            }
            view.background = bg
        }
        view.elevation = 2f * density
    }

    fun applyButtonGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        view.elevation = 6f * view.resources.displayMetrics.density
    }

    fun applyIndicatorGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        view.setBackgroundColor(c.indicatorGlow)
    }

    fun applyInputGlow(view: View, enabled: Boolean, hasFocus: Boolean, isDark: Boolean = true) {
        if (!enabled || !hasFocus) return
        view.elevation = 3f * view.resources.displayMetrics.density
    }
}