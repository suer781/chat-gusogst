package com.gusogst.chat.util

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt

/**
 * 动态颜色引擎 - 实现主题色的平滑过渡和动态变化
 * 
 * 特点：
 * 1. 平滑的颜色过渡动画
 * 2. 根据时间自动变化的主题色
 * 3. 响应触摸事件的颜色反馈
 * 4. 性能优化的颜色计算
 */
object DynamicColorEngine {

    // 颜色过渡动画时长
    private const val COLOR_TRANSITION_DURATION = 500L
    
    // 当前主题色
    private var currentPrimaryColor = 0xFF6200EE.toInt()
    private var currentAccentColor = 0xFF03DAC6.toInt()
    
    // 过渡动画
    private var colorAnimator: ValueAnimator? = null
    
    /**
     * 获取平滑过渡的主题色
     */
    @ColorInt
    fun getSmoothPrimaryColor(): Int = currentPrimaryColor
    
    @ColorInt
    fun getSmoothAccentColor(): Int = currentAccentColor
    
    /**
     * 平滑切换主题色
     */
    fun animateToColor(
        targetPrimary: Int,
        targetAccent: Int,
        onUpdate: (primary: Int, accent: Int) -> Unit
    ) {
        // 取消之前的动画
        colorAnimator?.cancel()
        
        val startPrimary = currentPrimaryColor
        val startAccent = currentAccentColor
        
        colorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = COLOR_TRANSITION_DURATION
            interpolator = DecelerateInterpolator()
            
            val argbEvaluator = ArgbEvaluator()
            
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                
                // 计算过渡颜色
                val primary = argbEvaluator.evaluate(fraction, startPrimary, targetPrimary) as Int
                val accent = argbEvaluator.evaluate(fraction, startAccent, targetAccent) as Int
                
                // 更新当前颜色
                currentPrimaryColor = primary
                currentAccentColor = accent
                
                // 回调
                onUpdate(primary, accent)
            }
            
            start()
        }
    }
    
    /**
     * 获取基于时间的动态颜色（轻微变化）
     * 模拟环境光变化效果
     */
    @ColorInt
    fun getTimeBasedColor(baseColor: Int, timeMillis: Long = System.currentTimeMillis()): Int {
        // 24小时周期，从0点到24点颜色有轻微变化
        val hourOfDay = ((timeMillis / (1000 * 60 * 60)) % 24).toInt()
        
        // 根据时间调整亮度：白天亮一些，晚上暗一些
        val brightnessFactor = when (hourOfDay) {
            in 6..8 -> 1.1f    // 早晨稍亮
            in 9..17 -> 1.05f  // 白天正常
            in 18..20 -> 0.95f  // 傍晚稍暗
            else -> 0.9f        // 夜晚较暗
        }
        
        return adjustBrightness(baseColor, brightnessFactor)
    }
    
    /**
     * 调整颜色亮度
     */
    @ColorInt
    private fun adjustBrightness(@ColorInt color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
    
    /**
     * 获取触摸反馈颜色
     */
    @ColorInt
    fun getTouchFeedbackColor(baseColor: Int, touchIntensity: Float = 1f): Int {
        // 触摸时颜色变亮并偏白
        val factor = 1f + (0.2f * touchIntensity)
        return adjustBrightness(baseColor, factor)
    }
    
    /**
     * 混合两个颜色
     */
    @ColorInt
    fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio
        val g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio
        val b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio
        val a = Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio
        return Color.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }
    
    /**
     * 获取渐变色数组
     */
    fun getGradientColors(baseColor: Int, count: Int = 5): List<Int> {
        val colors = mutableListOf<Int>()
        
        for (i in 0 until count) {
            val factor = 0.6f + (0.8f * i / count)
            colors.add(adjustBrightness(baseColor, factor))
        }
        
        return colors
    }
    
    /**
     * 获取强调色（在基础色上增加饱和度）
     */
    @ColorInt
    fun getSaturatedColor(baseColor: Int, saturationBoost: Float = 1.2f): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        
        // 提升饱和度
        hsv[1] = (hsv[1] * saturationBoost).coerceIn(0f, 1f)
        
        // 轻微增加亮度
        hsv[2] = (hsv[2] * 1.1f).coerceIn(0f, 1f)
        
        return Color.HSVToColor(Color.alpha(baseColor), hsv)
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        colorAnimator?.cancel()
        colorAnimator = null
    }
}
