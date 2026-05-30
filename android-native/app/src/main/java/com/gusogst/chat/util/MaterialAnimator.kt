package com.gusogst.chat.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.animation.TimeInterpolator
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ScrollView
import android.widget.EdgeEffect

/**
 * Material You vitality animations — mirrors Web material_you.css
 * Performance-optimized for native Android (no CSS paint thrashing).
 *
 * Key perf improvements over Web:
 *   - Hardware-accelerated ViewPropertyAnimator (GPU compositing)
 *   - No box-shadow repaints (use elevation + layer drawables instead)
 *   - No backdrop-filter per element (use single gradient overlay)
 *   - Built-in RippleDrawable (native, zero-Java overhead on touch)
 *   - Spring via OvershootInterpolator (no JS physics engine) */
object MaterialAnimator {

    // 动画时长常量 (ms)
    private const val PRESS_DURATION = 100L
    private const val RELEASE_DURATION = 350L

    // ── M6: Spring easing curves (hardware-friendly) ──
    val SPRING: TimeInterpolator = OvershootInterpolator(1.0f)
    val SPRING_BOUNCE: TimeInterpolator = OvershootInterpolator(1.56f)
    val DECELERATE: TimeInterpolator = DecelerateInterpolator()
    val ACCELERATE: TimeInterpolator = android.view.animation.AccelerateInterpolator()
    val EASE_DEFAULT: TimeInterpolator = AccelerateDecelerateInterpolator()

    // Material You decelerate: cubic-bezier(0, 0, 0, 1)
    val EASE_DECELERATE: TimeInterpolator = if (Build.VERSION.SDK_INT >= 21)
        PathInterpolator(0f, 0f, 0f, 1f) else DECELERATE
    // Material You accelerate: cubic-bezier(0.3, 0, 1, 1)
    val EASE_ACCELERATE: TimeInterpolator = if (Build.VERSION.SDK_INT >= 21)
        PathInterpolator(0.3f, 0f, 1f, 1f) else ACCELERATE

    // ── M7: Button elastic press ──
    // NOTE: onTouch returns false so the view's onClickListener still fires.
    fun applyButtonPress(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.94f).scaleY(0.94f)
                        .setDuration(PRESS_DURATION).setInterpolator(SPRING)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(RELEASE_DURATION).setInterpolator(SPRING_BOUNCE)
                        .start()
                }
            }
            false  // 关键：返回 false 让 onClickListener 也能收到事件
        }
    }

    // ── M8: Ripple ──
    fun applyRipple(view: View, color: Int = Color.argb(64, 255, 255, 255)) {
        val ripple = RippleDrawable(
            ColorStateList.valueOf(color),
            view.background,
            null  // null = content bounds, cheaper than mask
        )
        view.background = ripple
        view.isClickable = true
    }

    /** 对按钮同时应用按压+涟漪（组合快捷方式） */
    fun applyButtonEffects(view: View, rippleColor: Int = Color.argb(64, 255, 255, 255)) {
        applyRipple(view, rippleColor)
        applyButtonPress(view)
    }

    // ── M11: Card touch lift ──
    fun applyCardLift(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.98f).scaleY(0.98f)
                        .translationY(-1 * v.resources.displayMetrics.density)
                        .setDuration(PRESS_DURATION).setInterpolator(SPRING).start()
                    v.elevation = 8 * v.resources.displayMetrics.density
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f).scaleY(1f)
                        .translationY(0f)
                        .setDuration(RELEASE_DURATION).setInterpolator(SPRING_BOUNCE).start()
                    v.elevation = 2 * v.resources.displayMetrics.density
                }
            }
            false
        }
    }

    // ── M12: Page enter transition ──
    fun viewEnter(view: View, duration: Long = 400) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        view.alpha = 0f
        view.translationY = 8 * view.resources.displayMetrics.density
        view.scaleX = 0.98f
        view.scaleY = 0.98f
        view.animate()
            .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(duration).setInterpolator(DECELERATE).start()
    }

    // ── M12: Page exit transition ──
    fun viewExit(view: View, duration: Long = 200, onEnd: (() -> Unit)? = null) {
        view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        view.animate()
            .alpha(0f).scaleX(0.98f).scaleY(0.98f)
            .setDuration(duration).setInterpolator(ACCELERATE)
            .withEndAction { onEnd?.invoke() }.start()
    }

    // ── 导航指示器动画（匹配主分支：cubic-bezier(0.4, 0, 0.2, 1) 350ms）──
    fun animateIndicator(indicator: View, targetX: Float, targetWidth: Float) {
        indicator.animate()
            .x(targetX)
            .setDuration(350)
            .setInterpolator(PathInterpolator(0.4f, 0f, 0.2f, 1f))
            .start()
    }

    // ── 主题切换交叉渐变 ──
    fun applyThemeTransition(rootView: View, durationMs: Long = 600) {
        rootView.animate()
            .alpha(0.7f)
            .setDuration(durationMs / 3)
            .setInterpolator(EASE_DEFAULT)
            .withEndAction {
                rootView.animate()
                    .alpha(1f)
                    .setDuration(durationMs * 2 / 3)
                    .setInterpolator(EASE_DEFAULT)
                    .start()
            }.start()
    }

    // ── M9: Overscroll glow ──
    fun applyOverscrollGlow(scrollView: ScrollView, glowColor: Int = Color.argb(40, 180, 120, 200)) {
        // API 31+ EdgeEffect color via reflection (Kotlin property accessor unresolved)
        try {
            val edgeField = ScrollView::class.java.getDeclaredField("mEdgeGlowTop")
            edgeField.isAccessible = true
            val edge = edgeField.get(scrollView) as? EdgeEffect
            if (edge != null) {
                val colorField = EdgeEffect::class.java.getDeclaredField("mColor")
                colorField.isAccessible = true
                colorField.setInt(edge, glowColor)
            }
        } catch (_: Exception) {}
    }

    // ── 环境光背景设置 ──
    // 匹配 Web 主分支 tailwind.css body::after 的三椭圆径向渐变
    // Web: radial-gradient(ellipse 80% 50% at 50% 0%, ...) +
    //      radial-gradient(ellipse 60% 40% at 20% 100%, ...) +
    //      radial-gradient(ellipse 50% 35% at 80% 90%, ...)

    data class AmbientConfig(
        val topColor: Int,    // 顶部居中渐变颜色
        val leftColor: Int,   // 左下渐变颜色
        val rightColor: Int   // 右下渐变颜色
    )

    /**
     * 根据主题和环境获取环境光颜色（预乘 CSS opacity: 0.6）
     *
     * HDR 开启时 (Fix 11)：提升饱和度/色度，使环境光更鲜艳
     * HDR 关闭时：降低透明度，减弱环境光
     */
    fun getAmbientConfig(themeName: String, hdrEnabled: Boolean = false): AmbientConfig {
        // HDR 乘数：开启时提升颜色强度 ~30%
        val hdrMul = if (hdrEnabled) 1.3f else 1.0f
        return when (themeName) {
            "dark", "pureBlack" -> {
                val topAlpha = (92 * hdrMul).toInt().coerceIn(0, 255)
                val leftAlpha = (61 * hdrMul).toInt().coerceIn(0, 255)
                val rightAlpha = (46 * hdrMul).toInt().coerceIn(0, 255)
                AmbientConfig(
                    topColor = Color.argb(topAlpha, 22, 22, 60),
                    leftColor = Color.argb(leftAlpha, 40, 10, 60),
                    rightColor = Color.argb(rightAlpha, 10, 25, 60)
                )
            }
            "light" -> {
                val topAlpha = (77 * hdrMul).toInt().coerceIn(0, 255)
                val leftAlpha = (46 * hdrMul).toInt().coerceIn(0, 255)
                val rightAlpha = (46 * hdrMul).toInt().coerceIn(0, 255)
                AmbientConfig(
                    topColor = Color.argb(topAlpha, 230, 230, 245),
                    leftColor = Color.argb(leftAlpha, 245, 225, 230),
                    rightColor = Color.argb(rightAlpha, 225, 235, 250)
                )
            }
            "pureWhite" -> {
                val topAlpha = (61 * hdrMul).toInt().coerceIn(0, 255)
                val leftAlpha = (31 * hdrMul).toInt().coerceIn(0, 255)
                val rightAlpha = (31 * hdrMul).toInt().coerceIn(0, 255)
                AmbientConfig(
                    topColor = Color.argb(topAlpha, 250, 248, 255),
                    leftColor = Color.argb(leftAlpha, 255, 245, 248),
                    rightColor = Color.argb(rightAlpha, 245, 250, 255)
                )
            }
            else -> {
                val topAlpha = (77 * hdrMul).toInt().coerceIn(0, 255)
                val leftAlpha = (46 * hdrMul).toInt().coerceIn(0, 255)
                val rightAlpha = (46 * hdrMul).toInt().coerceIn(0, 255)
                AmbientConfig(
                    topColor = Color.argb(topAlpha, 230, 230, 245),
                    leftColor = Color.argb(leftAlpha, 245, 225, 230),
                    rightColor = Color.argb(rightAlpha, 225, 235, 250)
                )
            }
        }
    }

    /**
     * 自定义 Drawable：绘制三椭圆径向渐变，匹配 Web body::after
     * 透明底色让 bg_primary 透出来
     */
    class AmbientDrawable(
        private val w: Float,
        private val h: Float,
        private val config: AmbientConfig
    ) : android.graphics.drawable.Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 缓存 Shader，避免每帧重新创建（GPU 内存分配开销大）
        private val topShader: Shader = RadialGradient(
            w * 0.5f, 0f,
            Math.max(w * 0.4f, h * 0.25f),
            intArrayOf(config.topColor, Color.TRANSPARENT),
            floatArrayOf(0f, 0.6f),
            Shader.TileMode.CLAMP
        )
        private val leftShader: Shader = RadialGradient(
            w * 0.2f, h,
            Math.max(w * 0.3f, h * 0.2f),
            intArrayOf(config.leftColor, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f),
            Shader.TileMode.CLAMP
        )
        private val rightShader: Shader = RadialGradient(
            w * 0.8f, h * 0.9f,
            Math.max(w * 0.25f, h * 0.175f),
            intArrayOf(config.rightColor, Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f),
            Shader.TileMode.CLAMP
        )

        override fun draw(canvas: Canvas) {
            // 背景完全透明，让 bg_primary 透出
            paint.shader = topShader
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = leftShader
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.shader = rightShader
            canvas.drawRect(0f, 0f, w, h, paint)
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
        @Suppress("Deprecation")
        override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
    }

    /**
     * 设置/更新环境光叠加层 (Fix 11: HDR 联动)
     * @param animate 是否启用 600ms 交叉淡入淡出（主题切换时用）
     * @param hdrEnabled HDR 开启时提升环境光饱和度
     */
    fun setAmbientBackground(rootView: View, themeName: String, animate: Boolean = false, hdrEnabled: Boolean = false) {
        val w = rootView.width.coerceAtLeast(1).toFloat()
        val h = rootView.height.coerceAtLeast(1).toFloat()
        val config = getAmbientConfig(themeName, hdrEnabled)

        // 纯黑模式不需要环境光
        if (themeName == "pureBlack") {
            val existing = rootView.findViewWithTag<View>("ambient_overlay")
            if (existing != null) {
                existing.animate().alpha(0f).setDuration(300).withEndAction {
                    (existing.parent as? ViewGroup)?.removeView(existing)
                }.start()
            }
            return
        }

        val drawable = AmbientDrawable(w, h, config)
        val existing = rootView.findViewWithTag<View>("ambient_overlay")

        if (existing != null && !animate) {
            // 无动画直接替换
            existing.background = drawable
            return
        }

        if (existing != null && animate) {
            // 交叉淡入淡出：新层 alpha=0 渐入，旧层渐出后移除
            val newOverlay = View(rootView.context).apply {
                tag = "ambient_overlay_new"
                background = drawable
                alpha = 0f
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            (rootView as? ViewGroup)?.addView(newOverlay, 0)

            newOverlay.animate()
                .alpha(1f)
                .setDuration(600)
                .setInterpolator(PathInterpolator(0.4f, 0f, 0.2f, 1f))
                .withEndAction {
                    newOverlay.tag = "ambient_overlay"
                }.start()

            existing.animate()
                .alpha(0f)
                .setDuration(600)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .withEndAction {
                    (existing.parent as? ViewGroup)?.removeView(existing)
                }.start()
            return
        }

        // 首次创建
        val cover = View(rootView.context).apply {
            tag = "ambient_overlay"
            background = drawable
            // GPU 合成层，避免每帧回调到 draw()
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        (rootView as? ViewGroup)?.addView(cover, 0)
    }
}
