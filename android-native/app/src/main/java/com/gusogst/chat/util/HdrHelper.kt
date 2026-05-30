package com.gusogst.chat.util

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.view.animation.PathInterpolator
import java.util.Random

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
 *   8. 微纹理噪点 (glass noise) — 玻璃微观瑕疵的模拟
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

    // ── 内部：追踪 view 原始背景，HDR/Glass 关闭时还原 ──
    // Opt 4: Use View.setTag (faster than WeakHashMap, no weak-ref overhead)
    // Opt 5: API level check constant to avoid repeated SDK_INT comparisons
    private val HAS_OUTLINE_SHADOW = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    private val TAG_KEY_ORIGINAL_BG = View.generateViewId()

    // Dark theme HDR colors (精确匹配 Web hdr_v3.css [data-hdr="on"])
    val DARK = HdrColors(
        glowBase = Color.argb(230, 220, 225, 245),
        glowAccent = Color.argb(230, 220, 100, 140),      // = rgba(220,100,140,0.9)
        glowWhite = Color.argb(242, 255, 255, 255),      // = rgba(255,255,255,0.95)
        borderHighlight = Color.argb(102, 180, 160, 220), // = rgba(180,160,220,0.4)
        shadowGlow = Color.argb(64, 200, 100, 150),       // = rgba(200,100,150,0.25)
        bgTint = Color.argb(15, 180, 120, 200),           // = rgba(180,120,200,0.06)
        cardBorder = Color.argb(77, 180, 160, 220),       // = rgba(180,160,220,0.3)
        headerBg = Color.argb(20, 180, 120, 200),         // = rgba(180,120,200,0.08)
        navBg = Color.argb(13, 180, 120, 200),            // = rgba(180,120,200,0.05)
        bubbleTint = Color.argb(20, 180, 140, 220),       // = rgba(180,140,220,0.08)
        buttonGlow = Color.argb(64, 200, 100, 150),
        indicatorGlow = Color.argb(230, 220, 100, 140),   // = glowAccent
        inputFocusGlow = Color.argb(64, 200, 100, 150),
        reflectionHighlight = Color.argb(60, 255, 255, 255)
    )

    // Light theme HDR colors (精确匹配 Web hdr_v3.css [data-hdr="on"][data-theme="light"])
    val LIGHT = HdrColors(
        glowBase = Color.argb(230, 220, 225, 245),
        glowAccent = Color.argb(217, 180, 60, 100),       // = rgba(180,60,100,0.85)
        glowWhite = Color.argb(242, 255, 255, 255),      // = rgba(255,255,255,0.95)
        borderHighlight = Color.argb(77, 160, 100, 200),  // = rgba(160,100,200,0.3)
        shadowGlow = Color.argb(38, 180, 80, 140),        // = rgba(180,80,140,0.15)
        bgTint = Color.argb(10, 180, 100, 200),           // = rgba(180,100,200,0.04)
        cardBorder = Color.argb(64, 160, 100, 200),       // = rgba(160,100,200,0.25)
        headerBg = Color.argb(15, 180, 100, 200),         // = rgba(180,100,200,0.06)
        navBg = Color.argb(10, 180, 100, 200),            // = rgba(180,100,200,0.04)
        bubbleTint = Color.argb(15, 180, 100, 200),       // = rgba(180,100,200,0.06)
        buttonGlow = Color.argb(38, 180, 80, 140),        // = rgba(180,80,140,0.15)
        indicatorGlow = Color.argb(217, 180, 60, 100),    // = glowAccent
        inputFocusGlow = Color.argb(38, 180, 80, 140),    // = rgba(180,80,140,0.15)
        reflectionHighlight = Color.argb(40, 255, 255, 255) // Less intense than dark
    )

    // Light-only card shadow color (hdr_v3.css line 136: rgba(0,0,0,0.06) for outer shadow)
    private val LIGHT_CARD_SHADOW = Color.argb(15, 0, 0, 0)     // rgba(0,0,0,0.06)
    private val LIGHT_HEADER_SHADOW = Color.argb(20, 0, 0, 0)   // rgba(0,0,0,0.08)
    private val DARK_CARD_SHADOW = Color.argb(51, 0, 0, 0)      // rgba(0,0,0,0.2)
    private val DARK_HEADER_SHADOW = Color.argb(77, 0, 0, 0)    // rgba(0,0,0,0.3)

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
     * 底部高光线 — 匹配 Web `inset 0 -1px 0 / inset 0 -1px 20px`
     */
    private class BottomEdgeHighlight(color: Int, density: Float, cornerRadius: Float = 0f) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        private val cr = cornerRadius
        private val thickness = 1f * density
        override fun draw(canvas: Canvas) {
            val r = if (cr > 0) cr else 0f
            canvas.drawRoundRect(
                bounds.left.toFloat(), bounds.bottom - thickness,
                bounds.right.toFloat(), bounds.bottom.toFloat(),
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
     * 玻璃卡片边缘组合 — 合并三个独立的高光层为一次 GPU draw call (Opt 1)
     *
     * 原先 3 个独立的 LayerDrawable 层 (TopEdgeHighlight×2 + LeftEdgeHighlight):
     *   Layer 2: 顶部 1px 白色高光线 — inset 0 1px 0 glow-white
     *   Layer 3: 顶部 1px 彩色边 — border-top
     *   Layer 4: 左边缘 1px 高光线 — border-left
     *
     * 合并为单次 drawRoundRect×3，减少 LayerDrawable 合成开销
     */
    private class CombinedGlassEdges(
        topGlowColor: Int,
        borderColor: Int,
        leftGlowColor: Int,
        density: Float,
        cornerRadius: Float = 0f
    ) : Drawable() {
        private val thickness = 1f * density
        private val cr = cornerRadius
        // 三层 paint 各自独立着色（绘制顺序：白线 → 彩边 → 左边缘）
        private val topGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = topGlowColor }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = borderColor }
        private val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = leftGlowColor }

        override fun draw(canvas: Canvas) {
            val l = bounds.left.toFloat()
            val t = bounds.top.toFloat()
            val r = bounds.right.toFloat()
            val b = bounds.bottom.toFloat()
            val rr = if (cr > 0) cr else 0f
            // 1) Top white glow (inset 0 1px 0 — drawn first, bottom layer)
            canvas.drawRoundRect(l, t, r, t + thickness, rr, rr, topGlowPaint)
            // 2) Top border color (overlaps white line — matches CSS border-top over box-shadow)
            canvas.drawRoundRect(l, t, r, t + thickness, rr, rr, borderPaint)
            // 3) Left edge highlight
            canvas.drawRoundRect(l, t, l + thickness, b, rr, rr, leftPaint)
        }

        override fun setAlpha(alpha: Int) {
            topGlowPaint.alpha = alpha
            borderPaint.alpha = alpha
            leftPaint.alpha = alpha
        }

        override fun setColorFilter(cf: ColorFilter?) {
            topGlowPaint.colorFilter = cf
            borderPaint.colorFilter = cf
            leftPaint.colorFilter = cf
        }

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
        private var cachedShader: Shader? = null
        private var lastW = 0
        private var lastH = 0
        override fun draw(canvas: Canvas) {
            val w = bounds.width()
            val h = bounds.height()
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
     * 底部内散射辉光 — 匹配 Web header HDR+glass `inset 0 -1px 20px`
     * 从底部向上扩散的径向渐变
     */
    private class BottomInsetGlow(private val glowColor: Int, cornerRadius: Float = 0f) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val cr = cornerRadius
        private var cachedShader: Shader? = null
        private var lastW = 0
        private var lastH = 0
        override fun draw(canvas: Canvas) {
            val w = bounds.width()
            val h = bounds.height()
            if (cachedShader == null || w != lastW || h != lastH) {
                val cx = bounds.exactCenterX()
                val cy = bounds.bottom.toFloat()
                val r = Math.max(w, h) * 0.5f
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

    // ── Glass micro-texture noise (Fix 12) ──
    // 对齐 Web tailwind.css --glass-noise SVG feTurbulence
    // Android 用预生成的 16x16 随机噪点 Bitmap 平铺模拟，4% 不透明度

    private var cachedNoiseBitmap: Bitmap? = null
    private var cachedNoiseBitmapLight: Bitmap? = null

    // Opt 7: 预创建 NoiseDrawable 单例 (dark/light)，所有 view 共用（tile shader 自动平铺）
    private var cachedNoiseDrawableDark: NoiseDrawable? = null
    private var cachedNoiseDrawableLight: NoiseDrawable? = null

    private fun getNoiseDrawable(dark: Boolean): NoiseDrawable {
        if (dark) {
            if (cachedNoiseDrawableDark == null) {
                cachedNoiseDrawableDark = NoiseDrawable(getNoiseBitmap(true), 0.04f)
            }
            return cachedNoiseDrawableDark!!
        } else {
            if (cachedNoiseDrawableLight == null) {
                cachedNoiseDrawableLight = NoiseDrawable(getNoiseBitmap(false), 0.025f)
            }
            return cachedNoiseDrawableLight!!
        }
    }

    private fun getNoiseBitmap(dark: Boolean): Bitmap {
        if (dark && cachedNoiseBitmap != null) return cachedNoiseBitmap!!
        if (!dark && cachedNoiseBitmapLight != null) return cachedNoiseBitmapLight!!
        val size = 16
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rng = Random(42) // 固定种子，确保每次生成一致
        val pixels = IntArray(size * size)
        for (i in pixels.indices) {
            val gray = 128 + (rng.nextGaussian() * 32).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(255, gray, gray, gray)
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        if (dark) cachedNoiseBitmap = bmp else cachedNoiseBitmapLight = bmp
        return bmp
    }

    /**
     * 噪点纹理 Drawable — 平铺 16x16 随机噪点
     * 匹配 Web glass-noise + mix-blend-mode: overlay
     *
     * Opt 8: 移除 setAlpha 实现 — noise 层 alpha 固定，减少 LayerDrawable 遍历开销
     * Opt 9: Android 10+ (API 29) 使用 BlendMode.OVERLAY 模拟 Web mix-blend-mode: overlay
     *        低版本回退为当前 DITHER + alpha 方案
     */
    private class NoiseDrawable(private val noiseBitmap: Bitmap, private val opacity: Float) : Drawable() {
        private val tileShader: Shader = android.graphics.BitmapShader(
            noiseBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT
        )
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            alpha = (255 * opacity).toInt()
            shader = tileShader
            // Opt 9: OVERLAY blend mode on API 29+ (matches Web mix-blend-mode: overlay)
            if (Build.VERSION.SDK_INT >= 29) {
                blendMode = android.graphics.BlendMode.OVERLAY
            }
        }

        override fun draw(canvas: Canvas) {
            canvas.drawRect(bounds, paint)
        }

        // Opt 8: 噪声 alpha 固定不变，无需响应 setAlpha 调用（省 CPU）
        override fun setAlpha(alpha: Int) { /* fixed alpha, no-op */ }
        override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
        @Suppress("Deprecation")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    /**
     * 构建玻璃卡片的 LayerDrawable — 精确匹配 Web (Opt 1: 7→5 layers)
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
     * Opt 1: 原 Layer 2-4 (TopEdge×2 + LeftEdge) 合并为 CombinedGlassEdges 单层
     *        减少 LayerDrawable 从 7 层到 5 层（~29% 合成更快）
     *        bgTint 移除 setStroke（边缘由 CombinedGlassEdges 处理，匹配 Web border-top + border-left）
     * Opt 7: 噪声层用 getNoiseDrawable() 单例，避免每次 new
     *
     * Layer 0: 背景色染 (bgTint, no stroke)
     * Layer 1: 内散射辉光 (insetGlow) — inset 0 0 30px
     * Layer 2: 组合边缘高光 (CombinedGlassEdges: topWhite + topBorder + leftEdge)
     * Layer 3: 对角线反光 (reflection)
     * Layer 4: 噪点纹理 (glass noise, 单例复用) — 仅 glassEnabled 时
     */
    private fun buildGlassCardDrawable(
        c: HdrColors, glassEnabled: Boolean, hdrEnabled: Boolean, density: Float,
        isDark: Boolean = true
    ): Drawable {
        val layers = mutableListOf<Drawable>()
        val cr = 16f * density  // 卡片圆角

        // Layer 0: 玻璃底色染（边缘由 CombinedGlassEdges 处理，匹配 Web border-top + border-left）
        layers.add(GradientDrawable().apply {
            setColor(c.bgTint)
            cornerRadius = cr
        })

        // Layer 1: 内散射辉光 — HDR+glass 组合时使用 30px (Fix 3)
        if (hdrEnabled || glassEnabled) {
            layers.add(InsetScatterGlow(c.shadowGlow, cr))
        }

        // Layer 2: 组合边缘高光 (Opt 1: 合并 topWhite + topBorder + leftEdge 为单层)
        layers.add(CombinedGlassEdges(
            topGlowColor = c.glowWhite,
            borderColor = c.cardBorder,
            leftGlowColor = Color.argb(15, 255, 255, 255), // rgba(255,255,255,0.06)
            density = density,
            cornerRadius = cr
        ))

        // Layer 3: 对角线表面反光 (HDR enabled 时更显眼)
        if (hdrEnabled) {
            layers.add(GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(c.reflectionHighlight, Color.TRANSPARENT)
            ).apply { cornerRadius = cr })
        }

        // Layer 4: 噪点纹理 (Fix 12 + Opt 7: 单例复用，避免每次 new)
        if (glassEnabled) {
            layers.add(getNoiseDrawable(isDark))
        }

        return LayerDrawable(layers.toTypedArray())
    }

    // ── 公开 API ──

    /**
     * 保存 view 原始背景（第一次调用时）
     * 如果 HDR/Glass 关闭，可从 tag 还原
     * Opt 4: View.setTag avoids WeakHashMap weak-ref overhead, O(1) no-lock
     */
    private fun saveOriginalBg(view: View) {
        if (view.getTag(TAG_KEY_ORIGINAL_BG) == null) {
            view.setTag(TAG_KEY_ORIGINAL_BG, view.background)
        }
    }

    /**
     * 还原 view 原始背景，清除所有 HDR/Glass 叠加层 (Fix 1, 10)
     * Opt 4: View.getTag is direct SparseArray lookup, faster than WeakHashMap
     */
    private fun restoreOriginalBg(view: View) {
        val original = view.getTag(TAG_KEY_ORIGINAL_BG) as? Drawable
        view.setTag(TAG_KEY_ORIGINAL_BG, null)
        if (original != null) {
            view.background = original
        } else {
            view.background = null
        }
        view.elevation = 0f
        resetGlowShadow(view)
    }

    /**
     * Apply 玻璃效果到根视图（header/content），HDR 锦上添花 (Fix 1, 2, 3, 10, 12)
     *
     * 玻璃层（glassEnabled=true→始终生效，不依赖 HDR）：
     *   - 底色染 + 顶部 1px 高光线 + 内散射辉光 + 噪点纹理
     *
     * HDR 额外层（enabled=true→锦上添花）：
     *   - 对角线反光 + 彩色辉光阴影 + 更强 elevation
     *
     * HDR+Glass 组合 (Fix 3)：额外内散射辉光 + 底部辉光 + 叠加阴影
     *
     * 关闭时 (Fix 1, 10)：完全还原原始背景，移除所有效果
     */
    fun applyGlassWithHdr(view: View, enabled: Boolean, glassEnabled: Boolean, isDark: Boolean = true) {
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        // Fix 1 & 10: 关闭时完全清理
        if (!enabled && !glassEnabled) {
            restoreOriginalBg(view)
            return
        }

        // 首次启用前保存原始背景
        saveOriginalBg(view)

        val layers = mutableListOf<Drawable>()

        // 保持原有背景作为最底层（header 的 bg_header 等）
        val originalBg = view.getTag(TAG_KEY_ORIGINAL_BG) as? Drawable
        if (originalBg != null) {
            layers.add(originalBg)
        }

        // ── 玻璃层（glassEnabled=true 时始终生效）──
        if (glassEnabled) {
            // 内散射辉光
            layers.add(InsetScatterGlow(c.shadowGlow))
            // Fix 12 + Opt 7: 噪点纹理（单例复用）
            layers.add(getNoiseDrawable(isDark))
        }

        // ── HDR 层（锦上添花）──
        if (enabled) {
            // 对角线表面反光
            layers.add(GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(c.reflectionHighlight, Color.TRANSPARENT)
            ))
        }

        // Fix 3: HDR+Glass 组合 — 底部额外内散射辉光
        if (enabled && glassEnabled) {
            layers.add(BottomInsetGlow(c.shadowGlow))
        }

        view.background = LayerDrawable(layers.toTypedArray())

        // elevation + glow shadow (Opt 12: 400ms 动画匹配 Web transition, Opt 6: GPU 硬件层)
        enableHardwareLayer(view)
        val targetElevDp = when {
            enabled -> 3f
            glassEnabled -> 2f
            else -> 0f
        }
        animateElevation(view, targetElevDp * density)
        if (enabled) setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用 Header HDR 辉光 (Fix 2, 3, 10)
     *
     * Web: [data-hdr="on"][data-theme="dark"] .app-header
     *   background-color: var(--hdr-header-bg) !important;
     *   border-bottom: 1px solid var(--hdr-border-highlight) !important;
     *   box-shadow: inset 0 1px 0 var(--hdr-glow-white), 0 1px 3px rgba(0,0,0,0.3), 0 0 20px var(--hdr-shadow-glow) !important;
     *
     * Web combined: [data-hdr="on"][data-glass="on"][data-theme="dark"] .app-header
     *   box-shadow: inset 0 1px 0 var(--hdr-glow-white),
     *               inset 0 -1px 20px var(--hdr-shadow-glow),
     *               0 1px 3px rgba(0,0,0,0.3),
     *               0 0 24px var(--hdr-shadow-glow) !important;
     */
    fun applyHeaderGlow(view: View, enabled: Boolean, isDark: Boolean = true, glassEnabled: Boolean = false) {
        if (!enabled) {
            restoreOriginalBg(view)
            return
        }
        saveOriginalBg(view)
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        val layers = mutableListOf<Drawable>()

        // Web: background-color: var(--hdr-header-bg)
        layers.add(ColorDrawable(c.headerBg))
        // Web: inset 0 1px 0 var(--hdr-glow-white)
        layers.add(TopEdgeHighlight(c.glowWhite, density))

        // Fix 3: HDR+Glass 组合 — 底部内散射辉光 (inset 0 -1px 20px)
        if (glassEnabled) {
            layers.add(BottomInsetGlow(c.shadowGlow))
        }

        // Web: border-bottom: 1px solid var(--hdr-border-highlight)
        layers.add(GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            intArrayOf(c.borderHighlight, Color.TRANSPARENT)
        ))
        // 对角线反光
        layers.add(GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(c.reflectionHighlight, Color.TRANSPARENT)
        ))

        // Fix 12 + Opt 7: 噪点纹理（glass enabled 时，单例复用）
        if (glassEnabled) {
            layers.add(getNoiseDrawable(isDark))
        }

        view.background = LayerDrawable(layers.toTypedArray())
        enableHardwareLayer(view)
        animateElevation(view, 2f * density)
        setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用导航栏 HDR 辉光 (Fix 2, 10)
     *
     * Web (dark): [data-hdr="on"][data-theme="dark"] .app-nav
     *   background-color: var(--hdr-nav-bg) !important;
     *   box-shadow: inset 0 -1px 0 var(--hdr-border-highlight), 0 -2px 12px var(--hdr-shadow-glow) !important;
     *
     * Web (light): [data-hdr="on"][data-theme="light"] .app-nav
     *   background-color: var(--hdr-nav-bg) !important;
     *   box-shadow: inset 0 1px 0 var(--hdr-border-highlight), 0 2px 12px var(--hdr-shadow-glow) !important;
     */
    fun applyNavGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            restoreOriginalBg(view)
            return
        }
        saveOriginalBg(view)
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density
        val layers = mutableListOf<Drawable>()

        // Web: background-color: var(--hdr-nav-bg)
        layers.add(ColorDrawable(c.navBg))

        // Web dark: inset 0 -1px 0 var(--hdr-border-highlight)
        // Web light: inset 0 1px 0 var(--hdr-border-highlight)
        if (isDark) {
            layers.add(BottomEdgeHighlight(c.borderHighlight, density))
        } else {
            layers.add(TopEdgeHighlight(c.borderHighlight, density))
        }
        // 边缘高光渐变
        layers.add(GradientDrawable(
            if (isDark) GradientDrawable.Orientation.BOTTOM_TOP else GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(c.borderHighlight, Color.TRANSPARENT)
        ))

        view.background = LayerDrawable(layers.toTypedArray())
        enableHardwareLayer(view)
        animateElevation(view, 2f * density)
        setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用玻璃卡片效果 — 精确匹配 Web `.glass-card` (Fix 3, 5, 12)
     *
     * HDR only: border-color + box-shadow glow
     * Glass only: backdrop blur simulation + noise
     * HDR+Glass combined: inset 0 0 30px extra glow + stronger shadows
     */
    fun applyCardGlow(view: View, enabled: Boolean, isDark: Boolean = true, glassEnabled: Boolean = false) {
        if (!enabled && !glassEnabled) {
            restoreOriginalBg(view)
            return
        }
        saveOriginalBg(view)
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        if (glassEnabled || enabled) {
            view.background = buildGlassCardDrawable(c, glassEnabled, enabled, density, isDark)
        }

        // Fix 3: HDR+Glass combined — stronger elevation (30px glow vs 24px)
        view.elevation = if (enabled && glassEnabled) 6f * density
            else if (glassEnabled) 4f * density
            else if (enabled) 3f * density
            else 0f

        if (enabled) setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用消息气泡 HDR 效果 (Fix 5)
     *
     * Web: [data-hdr="on"][data-theme="dark"] .msg-bubble
     *   border-color: var(--hdr-card-border) !important;
     *   box-shadow: 0 0 8px var(--hdr-shadow-glow) !important;
     *
     * Web: .msg-bubble.user
     *   background-color: var(--hdr-bubble-tint) !important;
     */
    fun applyBubbleGlow(view: View, enabled: Boolean, isUser: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            restoreOriginalBg(view)
            return
        }
        saveOriginalBg(view)
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        val layers = mutableListOf<Drawable>()

        // Fix 5: All bubbles get border color; user bubbles get tint
        val bubbleBg = GradientDrawable().apply {
            setColor(if (isUser) c.bubbleTint else Color.TRANSPARENT)
            setStroke(1, c.cardBorder)
            cornerRadius = 16f * density
        }
        layers.add(bubbleBg)

        // 顶部 1px 高光 — 仅用户气泡
        if (isUser) {
            layers.add(TopEdgeHighlight(c.glowWhite, density, 16f * density))
        }

        view.background = LayerDrawable(layers.toTypedArray())

        // Fix 5: shadow glow on all bubbles
        view.elevation = 2f * density
        setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用按钮 HDR 按下效果 (Fix 6)
     *
     * Web: [data-hdr="on"][data-theme="dark"] .btn-accent,
     *       [data-hdr="on"][data-theme="dark"] button:active
     *   box-shadow: inset 0 1px 2px var(--hdr-glow-white), 0 0 16px var(--hdr-shadow-glow) !important;
     */
    fun applyButtonGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            restoreOriginalBg(view)
            return
        }
        saveOriginalBg(view)
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        // Fix 6: 添加内阴影高光
        val origBg = view.getTag(TAG_KEY_ORIGINAL_BG) as? Drawable
        val layers = mutableListOf<Drawable>()
        if (origBg != null) layers.add(origBg)
        // inset 0 1px 2px white glow
        layers.add(TopEdgeHighlight(c.glowWhite, density))
        view.background = LayerDrawable(layers.toTypedArray())

        view.elevation = 6f * density
        setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用导航指示器 HDR 效果 (Fix 4)
     *
     * Web: [data-hdr="on"][data-theme="dark"] .nav-indicator>div
     *   background: var(--hdr-glow-accent) !important;
     *   backdrop-filter: blur(16px) saturate(1.8) !important;
     *   box-shadow: inset 0 1px 0 var(--hdr-glow-white),
     *               0 2px 8px var(--hdr-shadow-glow),
     *               0 0 20px var(--hdr-shadow-glow) !important;
     *
     * Light: 无 backdrop-filter
     */
    fun applyIndicatorGlow(
        view: View, enabled: Boolean, isDark: Boolean = true,
        glassEnabled: Boolean = false
    ) {
        if (!enabled) {
            restoreOriginalBg(view)
            return
        }
        saveOriginalBg(view)
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        val layers = mutableListOf<Drawable>()

        // Web: background: var(--hdr-glow-accent)
        layers.add(ColorDrawable(c.indicatorGlow))

        // Web: inset 0 1px 0 var(--hdr-glow-white)
        layers.add(TopEdgeHighlight(c.glowWhite, density))

        // Fix 4: 模拟 backdrop-filter blur + saturate (半透明白色叠加)
        if (glassEnabled) {
            layers.add(GradientDrawable().apply {
                setColor(Color.argb(30, 255, 255, 255))
                cornerRadius = 24f * density
            })
        }

        view.background = LayerDrawable(layers.toTypedArray())

        // Fix 4: Stronger shadow (0 2px 8px + 0 0 20px)
        enableHardwareLayer(view)
        animateElevation(view, 4f * density)
        setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用输入框 HDR 聚焦效果 (Fix 7)
     *
     * Web: [data-hdr="on"][data-theme="dark"] input:focus, textarea:focus
     *   border-color: var(--hdr-glow-accent) !important;
     *   box-shadow: 0 0 0 3px var(--hdr-shadow-glow), 0 0 16px var(--hdr-shadow-glow) !important;
     */
    fun applyInputGlow(view: View, enabled: Boolean, hasFocus: Boolean, isDark: Boolean = true) {
        val c = if (isDark) DARK else LIGHT
        val density = view.resources.displayMetrics.density

        if (!enabled || !hasFocus) {
            // Fix 10: 失去焦点/HDR关闭时清理
            if (!enabled) {
                // 仅当 HDR 关闭时清理（失焦但 HDR 开启时不清理，保留基础状态）
                resetGlowShadow(view)
                view.elevation = 0f
            } else {
                // HDR 开启但失焦：保留低 elevation
                view.elevation = 1f * density
            }
            return
        }

        // Fix 7: 聚焦 + HDR — border glow + ring + shadow
        // 对 GradientDrawable 背景设置 border color
        val bg = view.background
        if (bg is GradientDrawable) {
            bg.setStroke(2, c.glowAccent)
        } else if (bg is LayerDrawable) {
            // 找到第一层 GradientDrawable 设置边框
            for (i in 0 until bg.numberOfLayers) {
                val layer = bg.getDrawable(i)
                if (layer is GradientDrawable) {
                    layer.setStroke(2, c.glowAccent)
                    break
                }
            }
        }

        // box-shadow ring: 0 0 0 3px + 0 0 16px
        view.elevation = 3f * density
        setGlowShadow(view, c.shadowGlow)
    }

    /**
     * 应用 Toggle 开关 HDR 效果 (Fix 8)
     *
     * Web: [data-hdr="on"][data-theme="dark"] .toggle-active
     *   box-shadow: 0 0 12px var(--hdr-shadow-glow) !important;
     */
    fun applyToggleGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            resetGlowShadow(view)
            view.elevation = 0f
            return
        }
        val c = if (isDark) DARK else LIGHT
        view.elevation = 3f * view.resources.displayMetrics.density
        setGlowShadow(view, c.shadowGlow)
    }

    // ── 工具方法 ──

    /** 设置 API 28+ 彩色辉光阴影 (Opt 5: cached constant) */
    private fun setGlowShadow(view: View, glowColor: Int) {
        if (HAS_OUTLINE_SHADOW) {
            view.outlineSpotShadowColor = glowColor
            view.outlineAmbientShadowColor = glowColor
        }
    }

    /** 重置阴影颜色为默认 (Fix 10) */
    private fun resetGlowShadow(view: View) {
        if (HAS_OUTLINE_SHADOW) {
            view.outlineSpotShadowColor = Color.TRANSPARENT
            view.outlineAmbientShadowColor = Color.TRANSPARENT
        }
    }

    // ── Opt 11 & 12: HDR/Glass 开关动画过渡 ──
    // Web hdr_v3.css 第57-59行：transition: box-shadow 0.4s cubic-bezier(0.2,0,0,1), ...
    // 对齐 cubic-bezier(0.2, 0, 0, 1) = PathInterpolator(0.2f, 0f, 0f, 1f)

    /** 对 view 背景变化做 400ms 交叉淡入淡出（模拟 CSS transition） */
    private fun crossFadeBackground(view: View) {
        view.animate().cancel()
        view.alpha = 0.85f
        view.animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f))
            .start()
    }

    /** 对 view elevation 做 400ms 平滑动画（匹配 Web transition: box-shadow 0.4s） */
    private fun animateElevation(view: View, targetPx: Float) {
        val current = view.elevation
        if (java.lang.Float.compare(current, targetPx) == 0) {
            view.elevation = targetPx
            return
        }
        ValueAnimator.ofFloat(current, targetPx).apply {
            duration = 400
            interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener { view.elevation = it.animatedValue as Float }
            start()
        }
    }

    /** Opt 6: 对需要频繁绘制 HDR 效果的 View 启用 GPU 硬件层 */
    private fun enableHardwareLayer(view: View) {
        if (view.layerType != View.LAYER_TYPE_HARDWARE) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }
}
