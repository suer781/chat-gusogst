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
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
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
import androidx.core.widget.EdgeEffectCompat

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
    // Uses Android built-in RippleDrawable (native, zero overhead).
    // Much faster than Web CSS ripple which triggers paint on every frame.
    fun applyRipple(view: View, color: Int = Color.argb(64, 255, 255, 255)) {
        val ripple = RippleDrawable(
            ColorStateList.valueOf(color),
            view.background,
            null  // null = content bounds, cheaper than mask
        )
        view.background = ripple
        // 保持原有点击高亮
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
    // 代替 Web 的 CSS spring transition on left/width
    fun animateIndicator(indicator: View, targetX: Float, targetWidth: Float) {
        indicator.animate()
            .x(targetX)
            .setDuration(400)
            .setInterpolator(SPRING_BOUNCE)
            .start()
    }

    // ── 主题切换交叉渐变 ──
    // Web: 0.6s cross-fade via #root transition
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
    // Android 内置 EdgeEffect，比 Web 的 CSS sticky gradient 更高效
    fun applyOverscrollGlow(scrollView: ScrollView, glowColor: Int = Color.argb(40, 180, 120, 200)) {
        if (Build.VERSION.SDK_INT >= 31) {
            scrollView.edgeEffectColor = glowColor
        } else {
            try {
                val f = ScrollView::class.java.getDeclaredField("mEdgeGlowTop")
                f.isAccessible = true
                val edge = f.get(scrollView) as? EdgeEffect
                if (edge != null) {
                    EdgeEffectCompat(edge).let {
                        // set color via reflection on older APIs
                        val cf = EdgeEffect::class.java.getDeclaredField("mColor")
                        cf.isAccessible = true
                        cf.setInt(edge, glowColor)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // ── 环境光背景设置 ──
    // Web: body::after radial-gradient (独立合成层)
    // Android: 直接设置 root 背景 drawable（GPU 单次合成）
    fun setAmbientBackground(rootView: View, drawableRes: Int) {
        if (Build.VERSION.SDK_INT >= 23) {
            rootView.background = rootView.context.getDrawable(drawableRes)
        } else {
            @Suppress("DEPRECATION")
            rootView.setBackgroundDrawable(
                rootView.context.resources.getDrawable(drawableRes)
            )
        }
    }
}
