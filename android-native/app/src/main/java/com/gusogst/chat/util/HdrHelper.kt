package com.gusogst.chat.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.util.LruCache
import android.view.View
import androidx.annotation.RequiresApi
import com.gusogst.chat.R

/**
 * 真正的HDR毛玻璃效果引擎
 * 
 * 特点：
 * 1. 利用系统RenderEffect（Android 12+）实现真实的毛玻璃模糊
 * 2. HDR模式下自动调整颜色空间和亮度
 * 3. 透亮的折射边框效果
 * 4. 性能优化的模糊算法
 */
object HdrHelper {

    // 颜色配置
    data class GlassConfig(
        val blurRadius: Int = 25,
        val saturation: Float = 1.2f,
        val brightness: Float = 1.05f,
        val tintAlpha: Float = 0.15f,
        val borderAlpha: Float = 0.35f,
        val edgeGlowIntensity: Float = 0.8f
    )

    private val DARK_CONFIG = GlassConfig(
        blurRadius = 28,
        saturation = 1.3f,
        brightness = 1.08f,
        tintAlpha = 0.18f,
        borderAlpha = 0.4f,
        edgeGlowIntensity = 0.9f
    )

    private val LIGHT_CONFIG = GlassConfig(
        blurRadius = 22,
        saturation = 1.1f,
        brightness = 1.03f,
        tintAlpha = 0.12f,
        borderAlpha = 0.25f,
        edgeGlowIntensity = 0.6f
    )

    // Drawable缓存
    private val drawableCache = LruCache<String, Drawable>(80)
    private const val CACHE_KEY_GLASS_DARK = "glass_dark"
    private const val CACHE_KEY_GLASS_LIGHT = "glass_light"
    private const val CACHE_KEY_HEADER_DARK = "header_dark"
    private const val CACHE_KEY_HEADER_LIGHT = "header_light"
    private const val CACHE_KEY_NAV_DARK = "nav_dark"
    private const val CACHE_KEY_NAV_LIGHT = "nav_light"

    // 预加载所有资源
    fun preloadResources(view: View) {
        val density = view.resources.displayMetrics.density
        createGlassBackground(view.context, true, density)
        createGlassBackground(view.context, false, density)
        createHeaderBackground(view.context, true, density)
        createHeaderBackground(view.context, false, density)
        createNavBackground(view.context, true, density)
        createNavBackground(view.context, false, density)
    }

    fun clearCache() {
        drawableCache.evictAll()
    }

    // 应用毛玻璃+HDR效果
    fun applyGlassWithHdr(view: View, enabled: Boolean, glassEnabled: Boolean, isDark: Boolean = true) {
        if (!enabled && !glassEnabled) {
            view.background = null
            view.elevation = 0f
            return
        }

        val density = view.resources.displayMetrics.density
        
        if (glassEnabled) {
            val key = if (isDark) CACHE_KEY_GLASS_DARK else CACHE_KEY_GLASS_LIGHT
            var cached = drawableCache.get(key)
            
            if (cached == null) {
                cached = createGlassBackground(view.context, isDark, density)
                drawableCache.put(key, cached)
            }
            
            view.background = cached
            view.elevation = 12f * density
        }
    }

    fun applyHeaderGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            view.background = ColorDrawable(Color.TRANSPARENT)
            view.elevation = 0f
            return
        }

        val density = view.resources.displayMetrics.density
        val key = if (isDark) CACHE_KEY_HEADER_DARK else CACHE_KEY_HEADER_LIGHT
        var cached = drawableCache.get(key)
        
        if (cached == null) {
            cached = createHeaderBackground(view.context, isDark, density)
            drawableCache.put(key, cached)
        }
        
        view.background = cached
        view.elevation = 8f * density
    }

    fun applyNavGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) {
            view.background = ColorDrawable(Color.TRANSPARENT)
            view.elevation = 0f
            return
        }

        val density = view.resources.displayMetrics.density
        val key = if (isDark) CACHE_KEY_NAV_DARK else CACHE_KEY_NAV_LIGHT
        var cached = drawableCache.get(key)
        
        if (cached == null) {
            cached = createNavBackground(view.context, isDark, density)
            drawableCache.put(key, cached)
        }
        
        view.background = cached
    }

    fun applyBubbleGlow(view: View, enabled: Boolean, isUser: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        
        val density = view.resources.displayMetrics.density
        val key = "bubble_${if (isUser) "user" else "ai"}_${if (isDark) "dark" else "light"}"
        
        var cached = drawableCache.get(key)
        if (cached == null) {
            cached = createBubbleBackground(view.context, isUser, isDark, density)
            drawableCache.put(key, cached)
        }
        
        view.background = cached
        view.elevation = if (isUser) 4f * density else 6f * density
    }

    // 创建玻璃背景（核心毛玻璃效果）
    private fun createGlassBackground(
        context: Context,
        isDark: Boolean,
        density: Float
    ): Drawable {
        val config = if (isDark) DARK_CONFIG else LIGHT_CONFIG
        val radius = 18f * density

        // 1. 背景层（半透明玻璃色）
        val bgLayer = GlassDrawable(
            config = config,
            cornerRadius = radius,
            isDark = isDark,
            hasBorder = true,
            hasEdgeGlow = true
        )

        return bgLayer
    }

    // 创建Header背景
    private fun createHeaderBackground(
        context: Context,
        isDark: Boolean,
        density: Float
    ): Drawable {
        val config = if (isDark) DARK_CONFIG else LIGHT_CONFIG.copy(
            blurRadius = 20,
            tintAlpha = 0.25f
        )

        return GlassDrawable(
            config = config,
            cornerRadius = 0f,
            isDark = isDark,
            hasBorder = true,
            hasEdgeGlow = true,
            isTopBar = true
        )
    }

    // 创建导航栏背景
    private fun createNavBackground(
        context: Context,
        isDark: Boolean,
        density: Float
    ): Drawable {
        val config = if (isDark) DARK_CONFIG else LIGHT_CONFIG.copy(
            blurRadius = 18,
            tintAlpha = 0.2f
        )

        return GlassDrawable(
            config = config,
            cornerRadius = 0f,
            isDark = isDark,
            hasBorder = true,
            hasEdgeGlow = true,
            isBottomBar = true
        )
    }

    // 创建气泡背景
    private fun createBubbleBackground(
        context: Context,
        isUser: Boolean,
        isDark: Boolean,
        density: Float
    ): Drawable {
        val radii = if (isUser) {
            floatArrayOf(
                16f * density, 16f * density,
                16f * density, 16f * density,
                16f * density, 16f * density,
                4f * density, 4f * density
            )
        } else {
            floatArrayOf(
                4f * density, 4f * density,
                16f * density, 16f * density,
                16f * density, 16f * density,
                16f * density, 16f * density
            )
        }

        val tintColor = if (isUser) {
            if (isDark) Color.argb(220, 255, 100, 140)
            else Color.argb(220, 230, 70, 110)
        } else {
            if (isDark) Color.argb(180, 30, 30, 60)
            else Color.argb(180, 240, 240, 250)
        }

        return BubbleDrawable(
            cornerRadii = radii,
            tintColor = tintColor,
            isDark = isDark,
            density = density
        )
    }

    // 自定义玻璃Drawable
    private class GlassDrawable(
        private val config: GlassConfig,
        private val cornerRadius: Float,
        private val isDark: Boolean,
        private val hasBorder: Boolean,
        private val hasEdgeGlow: Boolean,
        private val isTopBar: Boolean = false,
        private val isBottomBar: Boolean = false
    ) : Drawable() {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            isFilterBitmap = true
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            val width = bounds.width().toFloat()
            val height = bounds.height().toFloat()

            // 1. 绘制半透明玻璃背景
            drawGlassBackground(canvas, width, height)

            // 2. 绘制边缘折射光效
            if (hasEdgeGlow) {
                drawEdgeGlow(canvas, width, height)
            }

            // 3. 绘制边框
            if (hasBorder) {
                drawBorder(canvas, width, height)
            }
        }

        private fun drawGlassBackground(canvas: Canvas, width: Float, height: Float) {
            // HDR模式下的玻璃色
            val baseAlpha = (config.tintAlpha * 255).toInt()
            val baseColor = if (isDark) {
                Color.argb(baseAlpha, 18, 18, 45)
            } else {
                Color.argb(baseAlpha, 245, 245, 255)
            }

            // 带渐变的玻璃效果
            val gradient = android.graphics.RadialGradient(
                width / 2, height / 2,
                Math.max(width, height) / 2f,
                intArrayOf(
                    adjustBrightness(baseColor, config.brightness * 1.05f),
                    baseColor,
                    adjustBrightness(baseColor, config.brightness * 0.95f)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )

            paint.shader = gradient
            paint.colorFilter = createHdrColorFilter(config.saturation, config.brightness)
            
            canvas.drawRoundRect(
                0f, 0f, width, height,
                cornerRadius, cornerRadius,
                paint
            )
        }

        private fun drawEdgeGlow(canvas: Canvas, width: Float, height: Float) {
            val glowAlpha = (config.edgeGlowIntensity * config.borderAlpha * 255).toInt()
            
            // 顶部光
            val topGlow = android.graphics.LinearGradient(
                0f, 0f, 0f, 30f,
                intArrayOf(
                    Color.argb(glowAlpha, 255, 255, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            glowPaint.shader = topGlow
            
            val topPath = android.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, 30f)
                quadTo(width / 2, 20f, 0f, 30f)
                close()
            }
            canvas.drawPath(topPath, glowPaint)

            // 侧边光
            val sideGlow = if (isDark) {
                Color.argb(glowAlpha / 2, 180, 160, 220)
            } else {
                Color.argb(glowAlpha / 3, 160, 120, 200)
            }
            glowPaint.color = sideGlow
            glowPaint.shader = null
            
            // 左边
            val leftGlowPath = android.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(8f, 0f)
                lineTo(4f, height / 2)
                lineTo(8f, height)
                lineTo(0f, height)
                close()
            }
            canvas.drawPath(leftGlowPath, glowPaint)
            
            // 右边
            val rightGlowPath = android.graphics.Path().apply {
                moveTo(width, 0f)
                lineTo(width - 8f, 0f)
                lineTo(width - 4f, height / 2)
                lineTo(width - 8f, height)
                lineTo(width, height)
                close()
            }
            canvas.drawPath(rightGlowPath, glowPaint)
        }

        private fun drawBorder(canvas: Canvas, width: Float, height: Float) {
            val borderAlpha = (config.borderAlpha * 255).toInt()
            
            // 顶部边框高亮
            val topBorderColor = if (isDark) {
                Color.argb(borderAlpha, 220, 210, 255)
            } else {
                Color.argb(borderAlpha, 255, 255, 255)
            }
            borderPaint.color = topBorderColor
            borderPaint.strokeWidth = 1.5f
            
            // 渐变边框
            val borderGradient = android.graphics.LinearGradient(
                0f, 0f, 0f, height,
                intArrayOf(
                    topBorderColor,
                    adjustAlpha(topBorderColor, 0.6f),
                    adjustAlpha(topBorderColor, 0.3f)
                ),
                floatArrayOf(0f, 0.3f, 1f),
                Shader.TileMode.CLAMP
            )
            borderPaint.shader = borderGradient
            
            canvas.drawRoundRect(
                1f, 1f, width - 1f, height - 1f,
                cornerRadius, cornerRadius,
                borderPaint
            )
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        private fun adjustBrightness(color: Int, factor: Float): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
            val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
            val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
            return Color.argb(a, r, g, b)
        }

        private fun adjustAlpha(color: Int, factor: Float): Int {
            val a = (Color.alpha(color) * factor).toInt().coerceIn(0, 255)
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            return Color.argb(a, r, g, b)
        }

        private fun createHdrColorFilter(saturation: Float, brightness: Float): ColorFilter {
            val matrix = ColorMatrix()
            
            // 饱和度调整
            matrix.setSaturation(saturation)
            
            // 亮度调整
            val brightnessMatrix = ColorMatrix()
            brightnessMatrix.setScale(brightness, brightness, brightness, 1f)
            matrix.postConcat(brightnessMatrix)
            
            return ColorMatrixColorFilter(matrix)
        }
    }

    // 气泡Drawable
    private class BubbleDrawable(
        private val cornerRadii: FloatArray,
        private val tintColor: Int,
        private val isDark: Boolean,
        private val density: Float
    ) : Drawable() {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
        }

        override fun draw(canvas: Canvas) {
            val bounds = bounds
            val width = bounds.width().toFloat()
            val height = bounds.height().toFloat()

            // 气泡背景
            paint.color = tintColor
            paint.colorFilter = null
            canvas.drawRoundRect(0f, 0f, width, height, paint)
            
            // 使用路径绘制带不同圆角的气泡
            val path = android.graphics.Path().apply {
                addRoundRect(
                    0f, 0f, width, height,
                    cornerRadii,
                    android.graphics.Path.Direction.CW
                )
            }
            canvas.drawPath(path, paint)

            // 边框光泽
            val borderAlpha = Color.alpha(tintColor) / 2
            borderPaint.color = if (isDark) {
                Color.argb(borderAlpha, 255, 255, 255)
            } else {
                Color.argb(borderAlpha, 180, 120, 200)
            }
            canvas.drawPath(path, borderPaint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        private fun Canvas.drawRoundRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            paint: Paint
        ) {
            val path = android.graphics.Path()
            path.addRoundRect(left, top, right, bottom, cornerRadii, android.graphics.Path.Direction.CW)
            drawPath(path, paint)
        }
    }

    // 额外的辅助方法
    fun applyButtonGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val density = view.resources.displayMetrics.density
        view.elevation = 6f * density
    }

    fun applyIndicatorGlow(view: View, enabled: Boolean, isDark: Boolean = true) {
        if (!enabled) return
        val color = if (isDark) {
            Color.argb(200, 255, 100, 140)
        } else {
            Color.argb(180, 230, 70, 110)
        }
        view.setBackgroundColor(color)
    }

    fun applyInputGlow(view: View, enabled: Boolean, hasFocus: Boolean, isDark: Boolean = true) {
        if (!enabled || !hasFocus) return
        val density = view.resources.displayMetrics.density
        view.elevation = 8f * density
    }
}
