package com.gusogst.chat.util

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ColorDrawable
import android.util.LruCache
import android.view.View
import com.gusogst.chat.R

/**
 * HDR + 毛玻璃 + 荣耀式折射边框效果 - 性能优化版本
 * 
 * 关键优化：
 * 1. Drawable 对象缓存（LruCache）
 * 2. 避免重复创建对象
 * 3. 线程安全
 */
object HdrHelper {

    // ==================== 颜色定义 ====================
    
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

    // ==================== Drawable 缓存 ====================
    
    // 缓存大小：100 个 drawable
    private val drawableCache = LruCache<String, android.graphics.drawable.Drawable>(100)
    
    private const val CACHE_KEY_GLASS_CARD_DARK = "glass_card_dark"
    private const val CACHE_KEY_GLASS_CARD_LIGHT = "glass_card_light"
    private const val CACHE_KEY_HDR_CARD_DARK = "hdr_card_dark"
    private const val CACHE_KEY_HDR_CARD_LIGHT = "hdr_card_light"
    private const val CACHE_KEY_BUBBLE_USER = "bubble_user"
    private const val CACHE_KEY_BUBBLE_AI_DARK = "bubble_ai_dark"
    private const val CACHE_KEY_BUBBLE_AI_LIGHT = "bubble_ai_light"
    private const val CACHE_KEY_HEADER_DARK = "header_dark"
    private const val CACHE_KEY_HEADER_LIGHT = "header_light"
    private const val CACHE_KEY_NAV_DARK = "nav_dark"
    private const val CACHE_KEY_NAV_LIGHT = "nav_light"

    /**
     * 预加载所有主题资源（在主题切换前调用）
     */
    fun preloadResources(view: View) {
        val density = view.resources.displayMetrics.density
        
        // 预创建毛玻璃卡片
        getGlassCardDrawable(view.context, true, density)
        getGlassCardDrawable(view.context, false, density)
        
        // 预创建 HDR 卡片
        getHdrCardDrawable(view.context, true, density)
        getHdrCardDrawable(view.context, false, density)
        
        // 预创建气泡
        getBubbleUserDrawable(view.context, density)
        getBubbleAiDrawable(view.context, true, density)
        getBubbleAiDrawable(view.context, false, density)
        
        // 预创建头部和导航
        getHeaderDrawable(view.context, true, density)
        getHeaderDrawable(view.context, false, density)
        getNavDrawable(view.context, true, density)
        getNavDrawable(view.context, false, density)
    }

    /**
     * 清理缓存（主题变更时调用）
     */
    fun clearCache() {
        drawableCache.evictAll()
    }

    // ==================== 公共 API ====================

    fun applyGlassWithHdr(view: View, enabled: Boolean, glassEnabled: Boolean, isDark: Boolean = true) {
        val density = view.resources.displayMetrics.density
        
        if (!enabled && !glassEnabled) {
            // 移除缓存的 drawable
            view.background = null
            view.elevation = 0f
            return
        }
        
        if (glassEnabled) {
            // 使用缓存的毛玻璃卡片
            val cached = drawableCache.get(
                if (isDark) CACHE_KEY_GLASS_CARD_DARK else CACHE_KEY_GLASS_CARD_LIGHT
            )
            if (cached != null) {
                view.background = cached
                view.elevation = 8f * density
                return
            }
            // 缓存未命中，创建并缓存
            getGlassCardDrawable(view.context, isDark, density)?.let {
                view.background = it
                view.elevation = 8f * density
            }
        } else if (enabled) {
            // 使用缓存的 HDR 卡片
            val cached = drawableCache.get(
                if (isDark) CACHE_KEY_HDR_CARD_DARK else CACHE_KEY_HDR_CARD_LIGHT
            )
            if (cached != null) {
                view.background = cached
                view.elevation = 2f * density
                return
            }
            getHdrCardDrawable(view.context, isDark, density)?.let {
                view.background = it
                view.elevation = 2f * density
            }
        }
    }

    fun applyHeaderGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            view.setBackgroundColor(view.resources.getColor(R.color.transparent, null))
            view.elevation = 0f
            return
        }
        
        val cached = drawableCache.get(
            if (isDark) CACHE_KEY_HEADER_DARK else CACHE_KEY_HEADER_LIGHT
        )
        if (cached != null) {
            view.background = cached
            view.elevation = 2f * view.resources.displayMetrics.density
            return
        }
        
        getHeaderDrawable(view.context, isDark, view.resources.displayMetrics.density)?.let {
            view.background = it
            view.elevation = 2f * view.resources.displayMetrics.density
        }
    }

    fun applyNavGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            view.setBackgroundColor(view.resources.getColor(R.color.transparent, null))
            view.elevation = 0f
            return
        }
        
        val cached = drawableCache.get(
            if (isDark) CACHE_KEY_NAV_DARK else CACHE_KEY_NAV_LIGHT
        )
        if (cached != null) {
            view.background = cached
            return
        }
        
        getNavDrawable(view.context, isDark, view.resources.displayMetrics.density)?.let {
            view.background = it
        }
    }

    fun applyBubbleGlow(view: View, enabled: Boolean, isUser: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        
        val density = view.resources.displayMetrics.density
        
        if (isUser) {
            // 用户气泡
            val cached = drawableCache.get(CACHE_KEY_BUBBLE_USER)
            if (cached != null) {
                view.background = cached
                view.elevation = 2f * density
                return
            }
            getBubbleUserDrawable(view.context, density)?.let {
                view.background = it
                view.elevation = 2f * density
            }
        } else {
            // AI 气泡
            val cached = drawableCache.get(
                if (isDark) CACHE_KEY_BUBBLE_AI_DARK else CACHE_KEY_BUBBLE_AI_LIGHT
            )
            if (cached != null) {
                view.background = cached
                view.elevation = 3f * density
                return
            }
            getBubbleAiDrawable(view.context, isDark, density)?.let {
                view.background = it
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

    // ==================== 私有方法 ====================

    private fun getGlassCardDrawable(context: android.content.Context, isDark: Boolean, density: Float): android.graphics.drawable.Drawable? {
        val key = if (isDark) CACHE_KEY_GLASS_CARD_DARK else CACHE_KEY_GLASS_CARD_LIGHT
        val cached = drawableCache.get(key)
        if (cached != null) return cached

        val radius = 16f * density
        val bgColor = if (isDark) Color.argb(180, 20, 20, 45) else Color.argb(180, 245, 245, 250)
        
        // 毛玻璃背景
        val bgGradient = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgColor)
        }
        
        // 折射边框
        val borderStroke = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            val borderColors = intArrayOf(
                Color.argb(120, 255, 255, 255),
                Color.argb(60, 200, 200, 255),
                Color.argb(30, 100, 100, 150)
            )
            colors = borderColors
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
            setStroke((2 * density).toInt(), Color.TRANSPARENT)
        }
        
        // 组合成 LayerDrawable
        val layerDrawable = LayerDrawable(arrayOf(bgGradient, borderStroke))
        val strokeWidth = (1.5 * density).toInt()
        layerDrawable.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth)
        
        // 添加阴影辉光层
        layerDrawable.setId(0, android.R.id.background)
        layerDrawable.setId(1, android.R.id SecondaryBackground)
        
        // 缓存
        drawableCache.put(key, layerDrawable)
        
        return layerDrawable
    }

    private fun getHdrCardDrawable(context: android.content.Context, isDark: Boolean, density: Float): android.graphics.drawable.Drawable? {
        val key = if (isDark) CACHE_KEY_HDR_CARD_DARK else CACHE_KEY_HDR_CARD_LIGHT
        val cached = drawableCache.get(key)
        if (cached != null) return cached

        val radius = 16f * density
        val c = if (isDark) DARK else LIGHT
        
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(context.getColor(R.color.bg_primary))
            setStroke((1 * density).toInt(), c.cardBorder)
        }
        
        drawableCache.put(key, bgDrawable)
        return bgDrawable
    }

    private fun getBubbleUserDrawable(context: android.content.Context, density: Float): android.graphics.drawable.Drawable? {
        val cached = drawableCache.get(CACHE_KEY_BUBBLE_USER)
        if (cached != null) return cached

        val userBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(
                16f * density, 16f * density,
                16f * density, 16f * density,
                16f * density, 16f * density,
                4f * density, 4f * density
            )
            setColor(context.getColor(R.color.accent))
            setStroke((1 * density).toInt(), Color.argb(100, 255, 255, 255))
        }
        
        drawableCache.put(CACHE_KEY_BUBBLE_USER, userBg)
        return userBg
    }

    private fun getBubbleAiDrawable(context: android.content.Context, isDark: Boolean, density: Float): android.graphics.drawable.Drawable? {
        val key = if (isDark) CACHE_KEY_BUBBLE_AI_DARK else CACHE_KEY_BUBBLE_AI_LIGHT
        val cached = drawableCache.get(key)
        if (cached != null) return cached

        val bgColor = if (isDark) Color.argb(200, 21, 21, 56) else Color.argb(200, 240, 240, 245)
        
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
        
        val layerDrawable = LayerDrawable(arrayOf(bgGradient, borderStroke))
        val strokeWidth = (1 * density).toInt()
        layerDrawable.setLayerInset(1, strokeWidth, strokeWidth, strokeWidth, strokeWidth)
        
        drawableCache.put(key, layerDrawable)
        return layerDrawable
    }

    private fun getHeaderDrawable(context: android.content.Context, isDark: Boolean, density: Float): android.graphics.drawable.Drawable? {
        val key = if (isDark) CACHE_KEY_HEADER_DARK else CACHE_KEY_HEADER_LIGHT
        val cached = drawableCache.get(key)
        if (cached != null) return cached

        val bgColor = if (isDark) Color.argb(160, 13, 13, 43) else Color.argb(160, 255, 255, 255)
        
        val bgGradient = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = 0f
        }
        
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
        
        val layerDrawable = LayerDrawable(arrayOf(bgGradient, borderStroke))
        val strokeWidth = (0.5 * density).toInt()
        layerDrawable.setLayerInset(1, strokeWidth, 0, strokeWidth, strokeWidth)
        
        drawableCache.put(key, layerDrawable)
        return layerDrawable
    }

    private fun getNavDrawable(context: android.content.Context, isDark: Boolean, density: Float): android.graphics.drawable.Drawable? {
        val key = if (isDark) CACHE_KEY_NAV_DARK else CACHE_KEY_NAV_LIGHT
        val cached = drawableCache.get(key)
        if (cached != null) return cached

        val bgColor = if (isDark) Color.argb(180, 13, 13, 43) else Color.argb(180, 255, 255, 255)
        
        val bgGradient = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = 0f
        }
        
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
        
        val layerDrawable = LayerDrawable(arrayOf(bgGradient, borderStroke))
        layerDrawable.setLayerInset(1, 0, 0, 0, 0)
        
        drawableCache.put(key, layerDrawable)
        return layerDrawable
    }
}
