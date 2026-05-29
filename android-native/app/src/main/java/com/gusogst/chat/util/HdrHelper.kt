package com.gusogst.chat.util

import android.graphics.Color
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.ColorFilter
import android.os.Build
import android.view.View

/**
 * HDR v4.0 glass glow helper — mirrors Web hdr_v3.css pixel-perfect.
 *
 * 模拟真实玻璃的光学特性：
 *   1. 边缘辉光 (border highlight) — 玻璃切面折射
 *   2. 表面反光 (diagonal reflection) — 环境光在玻璃表面的反射
 *   3. 内散射辉光 (inset scattering glow) — 光线进入玻璃内部后的散射
 *   4. 透射光晕 (shadow glow) — 玻璃透出的彩色辉光
 *   5. 环境色染 (ambient tint) — 玻璃本身的轻微着色
 *   6. 顶部高光 (top edge highlight) — 1px 光源在玻璃上缘的反射
 *   7. 上/左边缘高光线 (card edge line) — 玻璃切面折射的高光边缘
 *
 * Performance: 使用 LayerDrawable 组合多层效果，GPU 单次合成
 */
object HdrHelper {

    data class HdrColors(
        val glowBase: Int, val glowAccent: Int, val glowWhite: Int,
        val borderHighlight: Int, val shadowGlow: Int, val bgTint: Int,
        val cardBorder: Int, val headerBg: Int, val navBg: Int, val bubbleTint: Int,
        val buttonGlow: Int, val indicatorGlow: Int, val inputFocusGlow: Int,
        val reflectionHighlight: Int
    )

    // Dark theme HDR colors (精确匹配 Web)
    val DARK = HdrColors(
        glowBase = Color.argb(230, 220, 225, 245),
        glowAccent = Color.argb(230, 220, 100, 140),
        glowWhite = Color.argb(242, 255, 255, 255),      // = rgba(255,255,255,0.95)
        borderHighlight = Color.argb(102, 180, 160, 220), // = rgba(180,160,220,0.4)
        shadowGlow = Color.argb(64, 200, 100, 150),       // = rgba(200,100,150,0.25)
        bgTint = Color.argb(15, 180, 120, 200),           // = rgba(180,120,200,0.06)
        cardBorder = Color.argb(77, 180, 160, 220),       // = rgba(180,160,220,0.3)
        headerBg = Color.argb(20, 180, 120, 200),         // = rgba(180,120,200,0.08)
        navBg = Color.argb(13, 180, 120, 200),            // = rgba(180,120,200,0.05)
        bubbleTint = Color.argb(20, 180, 140, 220),       // = rgba(180,140,220,0.08)
        buttonGlow = Color.argb(64, 200, 100, 150),
        indicatorGlow = Color.argb(230, 220, 100, 140),
        inputFocusGlow = Color.argb(64, 200, 100, 150),
        reflectionHighlight = Color.argb(60, 255, 255, 255)
    )

    // Light theme HDR colors
    val LIGHT = HdrColors(
        glowBase = Color.argb(230, 220, 225, 245),
        glowAccent = Color.argb(217, 180, 60, 100),       // = rgba(180,60,100,0.85)
        glowWhite = Color.argb(242, 255, 255, 255),
        borderHighlight = Color.argb(77, 160, 100, 200),  // = rgba(160,100,200,0.3)
        shadowGlow = Color.argb(38, 180, 80, 140),        // = rgba(180,80,140,0.15)
        bgTint = Color.argb(10, 180, 100, 200),           // = rgba(180,100,200,0.04)
        cardBorder = Color.argb(64, 160, 100, 200),       // = rgba(160,100,200,0.25)
        headerBg = Color.argb(15, 180, 100, 200),         // = rgba(180,100,200,0.06)
        navBg = Color.argb(10, 180, 100, 200),            // = rgba(180,100,200,0.04)
        bubbleTint = Color.argb(15, 180, 100, 200),       // = rgba(180,100,200,0.06)
        buttonGlow = Color.argb(38, 180, 80, 140),
        indicatorGlow = Color.argb(217, 180, 60, 100),
        inputFocusGlow = Color.argb(38, 180, 80, 140),
        reflectionHighlight = Color.argb(40, 255, 255, 255)
    )

    // ── 内部：玻璃效果 Drawable ──

    /**
     * 精确 1px 顶部高光线 — 匹配 Web `inset 0 1px 0 var(--hdr-glow-white)`
     */
    private class TopEdgeHighlight(color: Int, density: Float, cornerRadius: Float = 0f) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        private val cr = cornerRadius
        private val thickness = 1f * density
        override fun draw(canvas: Canvas) {
            val r = if (cr > 0) cr else 0f
            canvas.drawRoundRect(
                bounds.left.toFloat(), bounds.top.toFloat(),
                bounds.right.toFloat(), bounds.top + thickness,
                r, r, paint
            )
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
        @Suppress("Deprecation")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    /**
     * 左边缘高光线 — 匹配 Web `border-left: 1px solid rgba(255,255,255,0.06)`
     */
    private class LeftEdgeHighlight(color: Int, density: Float, cornerRadius: Float = 0f) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        private val cr = cornerRadius
        private val thickness = 1f * density
        override fun draw(canvas: Canvas) {
            canvas.drawRoundRect(
                bounds.left.toFloat(), bounds.top.toFloat(),
                bounds.left + thickness, bounds.bottom.toFloat(),
                cr, cr, paint
            )
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
        @Suppress("Deprecation")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    /**
     * 内散射辉光 — 匹配 Web `inset 0 0 30px var(--hdr-shadow-glow)`
     * 从中心向外扩散的径向渐变，模拟光线在玻璃内部的散射
     */
    private class InsetScatterGlow(private val glowColor: Int, cornerRadius: Float = 0f) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cr = cornerRadius
        // 缓存 Shader（大小在 draw 时计算）— 用惰性初始化
        private var cachedShader: Shader? = null
        private var lastW = 0
        private var lastH = 0
        override fun draw(canvas: Canvas) {
            val w = bounds.width()
            val h = bounds.height()
            // 尺寸变了才重建 Shader（窗口横竖屏切换）
            if (cachedShader == null || w != lastW || h != lastH) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = Math.max(w, h) * 0.7f
                cachedShader = RadialGradient(
                    cx, cy, r,
                    intArrayOf(glowColor, Color.TRANSPARENT),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
                lastW = w; lastH = h
            }
            paint.shader = cachedShader
            canvas.drawRoundRect(
                bounds.left.toFloat(), bounds.top.toFloat(),
                bounds.right.toFloat(), bounds.bottom.toFloat(),
                if (cr > 0) cr else 0f,
                if (cr > 0) cr else 0f,
                paint
            )
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
        @Suppress("Deprecation")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    /**
     * 构建玻璃卡片的 LayerDrawable — 精确匹配 Web
     *
     * Web (dark, HDR+glass): .glass-card
     *   background-color: var(--hdr-bg-tint)
     *   border-top: 1px solid var(--hdr-card-border)
     *   border-left: 1px solid rgba(255,255,255,0.06)
     *   box-shadow: inset 0 1px 0 var(--hdr-glow-white),
     *               inset 0 0 30px var(--hdr-shadow-glow),
     *               0 2px 8px rgba(0,0,0,0.2),
     *               0 0 30px var(--hdr-shadow-glow)
     *
     * Layer 0: 背景色染 (bgTint)
     * Layer 1: 内散射辉光 (insetGlow) — inset 0 0 30px
     * Layer 2: 顶部 1px 高光线 — inset 0 1px 0
     * Layer 3: 左边缘 1px 高光线 — border-left
     * Layer 4: 上边缘 1px 彩色边 — border-top
     * Layer 5: 对角线反光 (reflection)
     */
    private fun buildGlassCardDrawable(c: HdrColors, glassEnabled: Boolean, density: Float): Drawable {
        val layers = mutableListOf<Drawable>()

        val cr = 16f * density  // 卡片圆角

        // Layer 0: 玻璃底色染（含边框）
        layers.add(GradientDrawable().apply {
            setColor(c.bgTint)
            setStroke(1, c.cardBorder)
            cornerRadius = cr
        })

        // Layer 1: 内散射辉光 (HDR+glass: inset 0 0 30px glow)
        if (glassEnabled) {
            layers.add(InsetScatterGlow(c.shadowGlow, cr))
        }

        // Layer 2: 顶部 1px 白色高光线 (inset 0 1px 0 glow-white)
        layers.add(TopEdgeHighlight(c.glowWhite, density, cr))

        // Layer 3+4: 边缘高光线 (border-top + border-left)
        // 上边缘彩色，左边缘白色微光
        layers.add(TopEdgeHighlight(c.cardBorder, density, cr))
        layers.add(LeftEdgeHighlight(Color.argb(15, 255, 255, 255), density, cr))

        // Layer 5: 对角线表面反光
        layers.add(GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(c.reflectionHighlight, Color.TRANSPARENT)
        ).apply { cornerRadius = cr })

        return LayerDrawable(layers.toTypedArray())
    }

    // ── 公开 API ──

    /**
     * Apply 玻璃效果到根视图（header/content），HDR 锦上添花
     *
     * 玻璃层（glassEnabled=true→始终生效，不依赖 HDR）：
     *   - 底色染 + 顶部 1px 高光线 + 内散射辉光
     *
     * HDR 额外层（enabled=true→锦上添花）：
     *   - 对角线反光 + 彩色辉光阴影 + 更强 elevation
     */
    fun applyGlassWithHdr(view: View, enabled: Boolean, glassEnabled: Boolean, isDark: Boolean = true) {
        if (!enabled && !glassEnabled) {
            // 都不开时不动原有背景，避免 header 底色丢失
            view.elevation = 0f
            return
        }
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        val layers = mutableListOf<Drawable>()

        // 保持原有背景作为最底层（header 的 bg_header 等）
        val originalBg = view.background
        if (originalBg != null) {
            layers.add(originalBg)
        }

        // ── 玻璃层（glassEnabled=true 时始终生效）──
        if (glassEnabled) {
            // 内散射辉光
            layers.add(InsetScatterGlow(c.shadowGlow))
        }

        // ── HDR 层（锦上添花）──
        if (enabled) {
            // 对角线表面反光
            layers.add(GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(c.reflectionHighlight, Color.TRANSPARENT)
            ))
        }

        view.background = LayerDrawable(layers.toTypedArray())

        // elevation
        if (enabled) {
            view.elevation = 3f * density
            setGlowShadow(view, c.shadowGlow)
        } else if (glassEnabled) {
            view.elevation = 2f * density
        } else {
            view.elevation = 0f
        }
    }

    fun applyHeaderGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { view.setBackgroundColor(Color.TRANSPARENT); return }
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        val layers = mutableListOf<Drawable>()

        // Web: background-color: var(--hdr-header-bg)
        layers.add(ColorDrawable(c.headerBg))
        // Web: inset 0 1px 0 var(--hdr-glow-white)
        layers.add(TopEdgeHighlight(c.glowWhite, density))
        // Web: border-bottom: 1px solid var(--hdr-border-highlight)
        // (模拟为底部边缘高光)
        layers.add(GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(c.borderHighlight, Color.TRANSPARENT)
        ))
        // 对角线反光
        layers.add(GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(c.reflectionHighlight, Color.TRANSPARENT)
        ))

        view.background = LayerDrawable(layers.toTypedArray())
        view.elevation = 2f * density
        setGlowShadow(view, c.shadowGlow)
    }

    fun applyNavGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) { view.setBackgroundColor(Color.TRANSPARENT); return }
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        val layers = mutableListOf<Drawable>()

        // Web: background-color: var(--hdr-nav-bg)
        layers.add(ColorDrawable(c.navBg))
        // Web: inset 0 -1px 0 var(--hdr-border-highlight)
        layers.add(GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(c.borderHighlight, Color.TRANSPARENT)
        ))

        view.background = LayerDrawable(layers.toTypedArray())
        view.elevation = 2f * density
        setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用玻璃卡片效果 — 精确匹配 Web `.glass-card`
     * 玻璃层独立于 HDR，HDR 增加反光和彩色阴影
     */
    fun applyCardGlow(view: View, enabled: Boolean, isDark: Boolean = true, glassEnabled: Boolean = true) {
        if (!enabled && !glassEnabled) return
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        if (glassEnabled) {
            view.background = buildGlassCardDrawable(c, true, density)
        }
        view.elevation = 4f * density
        if (enabled) setGlowShadow(view, c.shadowGlow)
    }

    fun applyBubbleGlow(view: View, enabled: Boolean, isUser: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        if (isUser) {
            val layers = mutableListOf<Drawable>()
            // Web: background-color: var(--hdr-bubble-tint)
            val bg = GradientDrawable().apply {
                setColor(c.bubbleTint)
                cornerRadius = 16f * density
            }
            layers.add(bg)
            // 顶部 1px 高光
            layers.add(TopEdgeHighlight(c.glowWhite, density, 16f * density))

            view.background = LayerDrawable(layers.toTypedArray())
        }

        view.elevation = 2f * density
        setGlowShadow(view, c.shadowGlow)
    }

    fun applyButtonGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        view.elevation = 6f * density
        setGlowShadow(view, c.shadowGlow)
    }

    fun applyIndicatorGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val c = if (isDark) DARK else LIGHT
        view.setBackgroundColor(c.indicatorGlow)
        setGlowShadow(view, c.shadowGlow)
    }

    fun applyInputGlow(view: View, enabled: Boolean, hasFocus: Boolean, isDark: Boolean = true) {
        if (!enabled || !hasFocus) return
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        view.elevation = 3f * density
        setGlowShadow(view, c.shadowGlow)
    }

    // ── 工具方法 ──

    /** 设置 API 28+ 彩色辉光阴影 */
    private fun setGlowShadow(view: View, glowColor: Int) {
        if (Build.VERSION.SDK_INT >= 28) {
            view.outlineSpotShadowColor = glowColor
            view.outlineAmbientShadowColor = glowColor
        }
    }
}
