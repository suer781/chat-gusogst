package com.gusogst.chat.util

import android.os.Build
import android.os.SystemClock
import android.view.Choreographer
import android.view.View
import android.view.Window

/**
 * 性能监控工具 - 追踪 FPS、帧时间、卡顿等指标
 * 
 * 用于优化 UI 性能和调试性能问题
 */
object PerformanceMonitor {

    private var isMonitoring = false
    private var frameCount = 0
    private var lastFrameTime = 0L
    private var totalFrameTime = 0L
    private var droppedFrames = 0
    
    private var fpsCallback: ((fps: Float) -> Unit)? = null
    private var jankCallback: ((frameTime: Long) -> Unit)? = null
    
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isMonitoring) return
            
            val currentTime = SystemClock.uptimeMillis()
            
            if (lastFrameTime > 0) {
                val frameTime = currentTime - lastFrameTime
                
                // 累计帧时间
                totalFrameTime += frameTime
                frameCount++
                
                // 检测卡顿（帧时间超过 16.67ms 即 60fps 阈值）
                if (frameTime > 16) {
                    droppedFrames++
                    
                    // 回调卡顿信息
                    jankCallback?.invoke(frameTime)
                }
                
                // 每秒计算一次 FPS
                if (frameCount >= 60) {
                    val fps = 1000f * frameCount / totalFrameTime
                    fpsCallback?.invoke(fps)
                    
                    // 重置计数
                    frameCount = 0
                    totalFrameTime = 0
                }
            }
            
            lastFrameTime = currentTime
            
            // 继续监听下一帧
            if (isMonitoring) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }
    
    /**
     * 开始性能监控
     */
    fun startMonitoring(
        onFpsUpdate: ((fps: Float) -> Unit)? = null,
        onJank: ((frameTime: Long) -> Unit)? = null
    ) {
        if (isMonitoring) return
        
        fpsCallback = onFpsUpdate
        jankCallback = onJank
        
        isMonitoring = true
        frameCount = 0
        totalFrameTime = 0
        droppedFrames = 0
        lastFrameTime = 0
        
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }
    
    /**
     * 停止性能监控
     */
    fun stopMonitoring(): PerformanceStats {
        isMonitoring = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        
        val stats = PerformanceStats(
            droppedFrames = droppedFrames,
            totalFrames = frameCount
        )
        
        // 清理
        fpsCallback = null
        jankCallback = null
        droppedFrames = 0
        frameCount = 0
        
        return stats
    }
    
    /**
     * 测量视图绘制时间
     */
    fun measureViewDraw(view: View, block: () -> Unit): Long {
        val startTime = SystemClock.uptimeMillis()
        block()
        val endTime = SystemClock.uptimeMillis()
        return endTime - startTime
    }
    
    /**
     * 测量代码块执行时间
     */
    fun measureTime(block: () -> Unit): Long {
        val startTime = SystemClock.uptimeMillis()
        block()
        return SystemClock.uptimeMillis() - startTime
    }

    /**
     * 带返回值的测量
     */
    inline fun <T> measureWithResult(
        crossinline block: () -> T,
        crossinline onResult: (Long) -> Unit
    ): T {
        val startTime = SystemClock.uptimeMillis()
        val result = block()
        val endTime = SystemClock.uptimeMillis()
        onResult(endTime - startTime)
        return result
    }
    
    /**
     * 检查是否支持高刷新率
     */
    fun isHighRefreshRateSupported(window: Window): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        
        return try {
            val display = window.context.display ?: return false
            val modes = display.supportedModes
            modes.any { it.refreshRate > 60f }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取最佳显示模式
     */
    fun getOptimalDisplayMode(window: Window): DisplayModeInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        
        return try {
            val display = window.context.display ?: return null
            val modes = display.getSupportedModes() ?: return null
            
            if (modes.isEmpty()) return null
            
            // 找到最高刷新率 + 最高分辨率的模式
            var best: android.view.Display.Mode? = null
            var bestScore = -1.0
            for (mode in modes) {
                val score = mode.refreshRate * 1000000.0 + mode.physicalWidth * mode.physicalHeight
                if (score > bestScore) {
                    bestScore = score
                    best = mode
                }
            }
            
            best?.let { mode ->
                DisplayModeInfo(
                    width = mode.physicalWidth,
                    height = mode.physicalHeight,
                    refreshRate = mode.refreshRate,
                    modeId = mode.modeId
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 性能统计结果
     */
    data class PerformanceStats(
        val droppedFrames: Int,
        val totalFrames: Int
    ) {
        val droppedFrameRate: Float
            get() = if (totalFrames > 0) droppedFrames.toFloat() / totalFrames else 0f
        
        val isHealthy: Boolean
            get() = droppedFrameRate < 0.1f  // 丢帧率低于 10% 视为健康
    }
    
    /**
     * 显示模式信息
     */
    data class DisplayModeInfo(
        val width: Int,
        val height: Int,
        val refreshRate: Float,
        val modeId: Int
    )
}
