package com.gusogst.chat.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.animation.TimeInterpolator
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
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
                        .setDuration(100).setInterpolator(SPRING)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(350).setInterpolator(SPRING_BOUNCE)
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
                        .setDuration(100).setInterpolator(SPRING).start()
                    v.elevation = 8 * v.resources.displayMetrics.density
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f).scaleY(1f)
                        .translationY(0f)
                        .setDuration(350).setInterpolator(SPRING_BOUNCE).start()
                    v.elevation = 2 * v.resources.displayMetrics.density
                }
            }
            false
        }
    }

    // ── M12: Page enter transition ──
    fun viewEnter(view: View, duration: Long = 400) {
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
        view.animate()
            .alpha(0f).scaleX(0.98f).scaleY(0.98f)
            .setDuration(duration).setInterpolator(ACCELERATE)
            .withEndAction { onEnd?.invoke() }.start()
    }

    // ── 导航指示器 spring 动画 ──
    fun animateIndicator(indicator: View, targetX: Float, targetWidth: Float) {
        indicator.animate()
            .x(targetX)
            .setDuration(400)
            .setInterpolator(SPRING_BOUNCE)
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
    // 直接用反射设置 EdgeEffect 颜色，不依赖 AndroidX EdgeEffectCompat
    fun applyOverscrollGlow(scrollView: ScrollView, glowColor: Int = Color.argb(40, 180, 120, 200)) {
        if (Build.VERSION.SDK_INT >= 31) {
            scrollView.edgeEffectColor = glowColor
        } else {
            try {
                // API 21-30: 反射设置 EdgeEffect 颜色
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
    }

    // ── 环境光背景设置 ──
    // Web: body::after radial-gradient (独立合成层)
    // Android: 用 GradientDrawable 程序化创建径向渐变
    fun setAmbientBackground(rootView: View) {
        val density = rootView.resources.displayMetrics.density
        val w = rootView.resources.displayMetrics.widthPixels
        val h = rootView.resources.displayMetrics.heightPixels
        val radius = Math.sqrt((w * w + h * h).toDouble()).toFloat() * 1.2f

        val bg = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                0x99FFE0FF,  // 顶部紫光
                0x33E8D5FF,  // 中部
                0x000D0D2B   // 底部透明（露出 bg_primary）
            )
        )
        bg.gradientType = GradientDrawable.RADIAL_GRADIENT
        bg.gradientRadius = radius
        bg.setGradientCenter((w / 2).toFloat(), 0f)
        rootView.background = bg
    }
}
