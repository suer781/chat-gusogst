package com.gusogst.chat.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Material You vitality animations - mirrors Web material_you.css
 * M6-M12: Spring easing, ripple, card lift, page transitions
 */
object MaterialAnimator {

    // M6: Spring easing curves
    private val SPRING = OvershootInterpolator(1.0f)
    private val SPRING_BOUNCE = OvershootInterpolator(1.56f)
    private val DECELERATE = DecelerateInterpolator()
    private val ACCELERATE = android.view.animation.AccelerateInterpolator()

    // M7: Button elastic press
    fun applyButtonPress(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(100).setInterpolator(SPRING).start()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(SPRING_BOUNCE).start()
                }
            }
            false
        }
    }

    // M8: Dynamic ripple
    fun applyRipple(view: View, color: Int = Color.argb(64, 255, 255, 255)) {
        val rippleColor = ColorStateList.valueOf(color)
        val mask = ShapeDrawable(OvalShape())
        view.background = RippleDrawable(rippleColor, view.background, mask)
    }

    // M11: Card touch lift
    fun applyCardLift(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.98f).scaleY(0.98f)
                        .translationY(-1 * v.resources.displayMetrics.density)
                        .setDuration(100).setInterpolator(SPRING).start()
                    v.elevation = 8 * v.resources.displayMetrics.density
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
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

    // M12: Page enter transition
    fun viewEnter(view: View, duration: Long = 400) {
        view.alpha = 0f
        view.translationY = 8 * view.resources.displayMetrics.density
        view.scaleX = 0.98f
        view.scaleY = 0.98f
        view.animate()
            .alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(duration).setInterpolator(DECELERATE).start()
    }

    // M12: Page exit transition
    fun viewExit(view: View, duration: Long = 200, onEnd: (() -> Unit)? = null) {
        view.animate()
            .alpha(0f).scaleX(0.98f).scaleY(0.98f)
            .setDuration(duration).setInterpolator(ACCELERATE)
            .withEndAction { onEnd?.invoke() }.start()
    }

    // M9: Overscroll glow (attach to ScrollView)
    fun applyOverscrollGlow(view: View) {
        // Android has built-in edge effect; just set color
        if (view is android.widget.ScrollView) {
            try {
                val f = android.widget.ScrollView::class.java.getDeclaredField("mEdgeGlowTop")
                f.isAccessible = true
                val edge = f.get(view) as? android.widget.EdgeEffect
                edge?.color = Color.argb(40, 180, 120, 200)
            } catch (_: Exception) {}
        }
    }
}
