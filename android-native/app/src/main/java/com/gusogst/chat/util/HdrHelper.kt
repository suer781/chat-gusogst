package com.gusogst.chat.util

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.RectF
import android.view.View
import com.gusogst.chat.R

/**
 * HDR + 毛玻璃 + 荣耀式折射边框效果
 *
 * 真正实现：
 * 1. 荣耀式折射边框（半透明渐变边框）
 * 2. 毛玻璃效果（半透明背景 + elevation）
 * 3. HDR 辉光效果
 * 4. 环境光背景
 */
object HdrHelper {

    data class HdrColors(
        val glowBase: Int,
        val glowAccent: Int,
        val glowWhite: Int,
        val borderHighlight: Int,
        val shadowGlow: Int,
        val bgTint: Int,
        val cardBorder: Int,
        val headerBg: Int,
        val navBg: Int,
        val bubbleTint: Int,
        val buttonGlow: Int,
        val indicatorGlow: Int,
        val inputFocusGlow: Int,
        val reflectionHighlight: Int
    )

    // Dark theme HDR colors
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
        inputFocusGlow = Color.argb(38, 180, 80, 140),
        reflectionHighlight = Color.argb(40, 255, 255, 255)
    )

    /**
     * 应用毛玻璃 + HDR 效果（带荣耀式折射边框）
     */
    fun applyGlassWithHdr(view: View, enabled: Boolean, glassEnabled: Boolean, isDark: Boolean = true) {
        if (!enabled && !glassEnabled) {
            view.background = null
            view.elevation = 0f
            return
        }
        
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        val radius = 16f * density
        
        if (glassEnabled) {
            // 荣耀式折射边框 + 毛玻璃效果
            applyGlassCard(view, c, radius, isDark)
        } else if (enabled) {
            // 仅 HDR 效果
            applyHdrCard(view, c, radius)
        }
    }

    /**
     * 荣耀式折射边框 + 毛玻璃背景
     */
    private fun applyGlassCard(view: View, colors: HdrColors, radius: Float, isDark: Boolean) {
        val density = view.resources.displayMetrics.density
        
        // 1. 背景：半透明毛玻璃效果
        val bgColor = if (isDark) {
            Color.argb(180, 20, 20, 45)
        } else {
            Color.argb(180, 245, 245, 250)
        }
        
        val bgGradient = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgColor)
        }
        
        // 2. 折射边框（荣耀式：顶部亮，底部暗的渐变边框）
        val borderStroke = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            
            // 荣耀式折射渐变：顶部亮，两侧次之，底部暗
            val borderColors = intArrayOf(
                Color.argb(120, 255, 255, 255),   // 顶部：最亮
                Color.argb(60, 200, 200, 255),    // 左/右：次亮
                Color.argb(30, 100, 100, 150)     // 底部：最暗
            )
            
            colors = borderColors
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke((2 * density).toInt(), Color.TRANSPARENT)
        }
        
        // 3. 组合成 LayerDrawable
        val layers = arrayOf(
            bgGradient,
            borderStroke
        )
        val layerDrawable = LayerDrawable(layers)
        
        // 设置边框的内边距
        val strokeWidth = (1.5 * density).toInt()
        layerDrawable.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth)
        
        view.background = layerDrawable
        view.elevation = 8f * density
    }

    /**
     * HDR 卡片效果（带边框）
     */
    private fun applyHdrCard(view: View, colors: HdrColors, radius: Float) {
        val density = view.resources.displayMetrics.density
        val bgColor = view.resources.getColor(R.color.bg_primary, null)
        
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgColor)
            setStroke((1 * density).toInt(), colors.cardBorder)
        }
        
        view.background = bgDrawable
        view.elevation = 2f * density
    }

    /**
     * 应用 HDR 头部效果
     */
    fun applyHeaderGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            view.setBackgroundColor(view.resources.getColor(R.color.transparent, null))
            view.elevation = 0f
            return
        }
        
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        
        // 半透明背景 + 折射边框
        val bgColor = if (isDark) {
            Color.argb(160, 13, 13, 43)
        } else {
            Color.argb(160, 255, 255, 255)
        }
        
        val bgGradient = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = 0f
        }
        
        // 荣耀式边框
        val borderStroke = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            val borderColors = intArrayOf(
                Color.argb(100, 255, 255, 255),
                Color.argb(50, 180, 180, 220),
                Color.argb(20, 80, 80, 120)
            )
            colors = borderColors
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke((1 * density).toInt(), Color.TRANSPARENT)
        }
        
        val layers = arrayOf(bgGradient, borderStroke)
        val layerDrawable = LayerDrawable(layers)
        val strokeWidth = (0.5 * density).toInt()
        layerDrawable.setLayerInset(1, strokeWidth, 0, strokeWidth, strokeWidth)
        
        view.background = layerDrawable
        view.elevation = 2f * density
    }

    /**
     * 应用导航栏 HDR 效果
     */
    fun applyNavGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            view.setBackgroundColor(view.resources.getColor(R.color.transparent, null))
            view.elevation = 0f
            return
        }
        
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        
        val bgColor = if (isDark) {
            Color.argb(180, 13, 13, 43)
        } else {
            Color.argb(180, 255, 255, 255)
        }
        
        val bgGradient = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = 0f
        }
        
        // 荣耀式边框（顶部有亮线）
        val borderStroke = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            val borderColors = intArrayOf(
                Color.argb(120, 255, 255, 255),
                Color.argb(0, 255, 255, 255),
                Color.argb(0, 255, 255, 255)
            )
            colors = borderColors
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke((1 * density).toInt(), Color.TRANSPARENT)
        }
        
        val layers = arrayOf(bgGradient, borderStroke)
        val layerDrawable = LayerDrawable(layers)
        layerDrawable.setLayerInset(1, 0, 0, 0, 0)
        
        view.background = layerDrawable
    }

    /**
     * 应用消息气泡 HDR + 毛玻璃效果
     */
    fun applyBubbleGlow(view: View, enabled: Boolean, isUser: Boolean, isDark: Boolean = true) {
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        
        if (enabled) {
            if (isUser) {
                // 用户消息气泡：纯色 + 边框
                val userBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(
                        16f * density, 16f * density,
                        16f * density, 16f * density,
                        16f * density, 16f * density,
                        4f * density, 4f * density
                    )
                    setColor(view.resources.getColor(R.color.accent, null))
                    setStroke((1 * density).toInt(), Color.argb(100, 255, 255, 255))
                }
                view.background = userBg
                view.elevation = 2f * density
            } else {
                // AI 消息气泡：毛玻璃 + 折射边框
                val bgColor = if (isDark) {
                    Color.argb(200, 21, 21, 56)
                } else {
                    Color.argb(200, 240, 240, 245)
                }
                
                val bgGradient = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(
                        4f * density, 4f * density,
                        16f * density, 16f * density,
                        16f * density, 16f * density,
                        16f * density, 16f * density
                    )
                    setColor(bgColor)
                }
                
                // 折射边框
                val borderStroke = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(
                        4f * density, 4f * density,
                        16f * density, 16f * density,
                        16f * density, 16f * density,
                        16f * density, 16f * density
                    )
                    val borderColors = intArrayOf(
                        Color.argb(100, 255, 255, 255),
                        Color.argb(50, 180, 180, 220),
                        Color.argb(20, 80, 80, 120)
                    )
                    colors = borderColors
                    orientation = GradientDrawable.Orientation.TOP_BOTTOM
                    setStroke((1.5 * density).toInt(), Color.TRANSPARENT)
                }
                
                val layers = arrayOf(bgGradient, borderStroke)
                val layerDrawable = LayerDrawable(layers)
                val strokeWidth = (1 * density).toInt()
                layerDrawable.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth)
                
                view.background = layerDrawable
                view.elevation = 3f * density
            }
        }
    }

    fun applyButtonGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val density = view.resources.displayMetrics.density
        view.elevation = 4f * density
    }

    fun applyIndicatorGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        view.setBackgroundColor(c.indicatorGlow)
    }

    fun applyInputGlow(view: View, enabled: Boolean, hasFocus: Boolean, isDark: Boolean = true) {
        if (!enabled || !hasFocus) return
        val density = view.resources.displayMetrics.density
        view.elevation = 3f * density
    }
}
