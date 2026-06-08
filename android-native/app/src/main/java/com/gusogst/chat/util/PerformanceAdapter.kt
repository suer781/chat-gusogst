package com.gusogst.chat.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.sqrt

/**
 * 设备性能适配引擎
 * 
 * 功能：
 * 1. 自动检测设备性能等级
 * 2. 根据性能等级调整动画质量
 * 3. 为低配设备提供优化方案
 * 4. 平衡视觉效果和性能消耗
 */
object PerformanceAdapter {

    // 性能等级
    enum class PerformanceLevel {
        ULTRA_LOW,   // 极低配：老旧设备、512MB-1GB RAM
        LOW,          // 低配：1-2GB RAM、单核/双核CPU
        MEDIUM,      // 中配：2-3GB RAM、四核CPU
        HIGH,        // 高配：3-4GB RAM、六核CPU
        ULTRA_HIGH   // 顶配：4GB+ RAM、八核CPU + 高端GPU
    }
    
    // 动画质量配置
    data class AnimationQuality(
        val enable3DEffects: Boolean,       // 3D效果（旋转、翻转）
        val enableParallax: Boolean,         // 视差效果
        val enableGlowEffects: Boolean,      // 发光效果
        val enableComplexGradients: Boolean,  // 复杂渐变
        val enableHdrEnhancement: Boolean,   // HDR增强
        val enableParticleEffects: Boolean,   // 粒子效果
        val maxFps: Int,                     // 最大帧率
        val blurRadius: Int,                 // 模糊半径
        val animationDuration: Float,         // 动画时长系数
        val hardwareLayerEnabled: Boolean    // 硬件加速层
    )
    
    private var cachedLevel: PerformanceLevel? = null
    private var cachedQuality: AnimationQuality? = null
    
    /**
     * 检测设备性能等级
     */
    fun detectPerformanceLevel(context: Context): PerformanceLevel {
        cachedLevel?.let { return it }
        
        val level = calculatePerformanceLevel(context)
        cachedLevel = level
        return level
    }
    
    /**
     * 获取动画质量配置
     */
    fun getAnimationQuality(context: Context): AnimationQuality {
        cachedQuality?.let { return it }
        
        val level = detectPerformanceLevel(context)
        val quality = createQualityForLevel(level)
        cachedQuality = quality
        return quality
    }
    
    /**
     * 计算性能等级
     */
    private fun calculatePerformanceLevel(context: Context): PerformanceLevel {
        val metrics = getDeviceMetrics(context)
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val totalRam = getTotalRam(context)
        val gpuScore = estimateGpuPerformance(context)
        
        // 综合评分
        var score = 0
        
        // CPU评分（最高3分）
        score += when {
            cpuCores >= 8 -> 3
            cpuCores >= 6 -> 2
            cpuCores >= 4 -> 1
            else -> 0
        }
        
        // RAM评分（最高3分）
        score += when {
            totalRam >= 4 * 1024 -> 3      // 4GB+
            totalRam >= 3 * 1024 -> 2      // 3-4GB
            totalRam >= 2 * 1024 -> 1      // 2-3GB
            else -> 0
        }
        
        // GPU评分（最高2分）
        score += gpuScore
        
        // 屏幕分辨率评分（最高2分）
        score += when {
            metrics.widthPixels >= 2560 || metrics.heightPixels >= 2560 -> 2
            metrics.widthPixels >= 1920 || metrics.heightPixels >= 1920 -> 1
            else -> 0
        }
        
        // 根据总分判定等级
        return when {
            score >= 8 -> PerformanceLevel.ULTRA_HIGH
            score >= 6 -> PerformanceLevel.HIGH
            score >= 4 -> PerformanceLevel.MEDIUM
            score >= 2 -> PerformanceLevel.LOW
            else -> PerformanceLevel.ULTRA_LOW
        }
    }
    
    /**
     * 根据性能等级创建动画质量配置
     */
    private fun createQualityForLevel(level: PerformanceLevel): AnimationQuality {
        return when (level) {
            PerformanceLevel.ULTRA_HIGH -> AnimationQuality(
                enable3DEffects = true,
                enableParallax = true,
                enableGlowEffects = true,
                enableComplexGradients = true,
                enableHdrEnhancement = true,
                enableParticleEffects = true,
                maxFps = 120,
                blurRadius = 30,
                animationDuration = 1.0f,
                hardwareLayerEnabled = true
            )
            
            PerformanceLevel.HIGH -> AnimationQuality(
                enable3DEffects = true,
                enableParallax = true,
                enableGlowEffects = true,
                enableComplexGradients = true,
                enableHdrEnhancement = true,
                enableParticleEffects = false,
                maxFps = 90,
                blurRadius = 25,
                animationDuration = 0.9f,
                hardwareLayerEnabled = true
            )
            
            PerformanceLevel.MEDIUM -> AnimationQuality(
                enable3DEffects = true,
                enableParallax = true,
                enableGlowEffects = true,
                enableComplexGradients = false,
                enableHdrEnhancement = false,
                enableParticleEffects = false,
                maxFps = 60,
                blurRadius = 20,
                animationDuration = 0.8f,
                hardwareLayerEnabled = true
            )
            
            PerformanceLevel.LOW -> AnimationQuality(
                enable3DEffects = false,
                enableParallax = false,
                enableGlowEffects = true,
                enableComplexGradients = false,
                enableHdrEnhancement = false,
                enableParticleEffects = false,
                maxFps = 60,
                blurRadius = 15,
                animationDuration = 0.6f,
                hardwareLayerEnabled = true
            )
            
            PerformanceLevel.ULTRA_LOW -> AnimationQuality(
                enable3DEffects = false,
                enableParallax = false,
                enableGlowEffects = false,
                enableComplexGradients = false,
                enableHdrEnhancement = false,
                enableParticleEffects = false,
                maxFps = 30,
                blurRadius = 10,
                animationDuration = 0.5f,
                hardwareLayerEnabled = false
            )
        }
    }
    
    /**
     * 获取设备指标
     */
    private fun getDeviceMetrics(context: Context): DisplayMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        }
        
        return displayMetrics
    }
    
    /**
     * 获取总RAM（MB）
     */
    private fun getTotalRam(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024)
    }
    
    /**
     * 估算GPU性能
     */
    private fun estimateGpuPerformance(context: Context): Int {
        // 通过设备型号和API级别估算GPU性能
        var score = 0
        
        // API级别（越高通常GPU越好）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            score += 1
        }
        
        // 设备品牌（高端品牌通常GPU更好）
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (manufacturer.contains("samsung") || 
            manufacturer.contains("google") || 
            manufacturer.contains("oneplus") ||
            manufacturer.contains("xiaomi")) {
            score += 1
        }
        
        return score
    }
    
    /**
     * 获取性能等级描述
     */
    fun getPerformanceDescription(context: Context): String {
        val level = detectPerformanceLevel(context)
        return when (level) {
            PerformanceLevel.ULTRA_HIGH -> "顶配设备 - 全部动画效果"
            PerformanceLevel.HIGH -> "高配设备 - 高级动画效果"
            PerformanceLevel.MEDIUM -> "中配设备 - 基础动画效果"
            PerformanceLevel.LOW -> "低配设备 - 简化动画效果"
            PerformanceLevel.ULTRA_LOW -> "极低配设备 - 性能优先模式"
        }
    }
    
    /**
     * 动态调整性能等级（运行时）
     */
    fun adjustPerformanceLevel(context: Context, fps: Float): PerformanceLevel {
        // 如果FPS持续低于阈值，降低性能等级
        return when {
            fps < 20 && cachedLevel != PerformanceLevel.ULTRA_LOW -> {
                // 降级
                val newLevel = when (cachedLevel) {
                    PerformanceLevel.LOW -> PerformanceLevel.ULTRA_LOW
                    PerformanceLevel.MEDIUM -> PerformanceLevel.LOW
                    PerformanceLevel.HIGH -> PerformanceLevel.MEDIUM
                    PerformanceLevel.ULTRA_HIGH -> PerformanceLevel.HIGH
                    else -> PerformanceLevel.ULTRA_LOW
                }
                cachedLevel = newLevel
                cachedQuality = createQualityForLevel(newLevel)
                newLevel
            }
            fps > 50 && cachedLevel != PerformanceLevel.ULTRA_HIGH -> {
                // 升级（恢复）
                val newLevel = when (cachedLevel) {
                    PerformanceLevel.ULTRA_LOW -> PerformanceLevel.LOW
                    PerformanceLevel.LOW -> PerformanceLevel.MEDIUM
                    PerformanceLevel.MEDIUM -> PerformanceLevel.HIGH
                    PerformanceLevel.HIGH -> PerformanceLevel.ULTRA_HIGH
                    else -> PerformanceLevel.ULTRA_HIGH
                }
                cachedLevel = newLevel
                cachedQuality = createQualityForLevel(newLevel)
                newLevel
            }
            else -> cachedLevel ?: detectPerformanceLevel(context)
        }
    }
    
    /**
     * 清除缓存（重新检测）
     */
    fun clearCache() {
        cachedLevel = null
        cachedQuality = null
    }
}
