package com.gusogst.chat.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import android.view.SurfaceView
import android.view.WindowManager
import androidx.annotation.RequiresApi

/**
 * 真正的Android HDR实现类
 * 
 * 实现真正的HDR功能：
 * 1. Window HDR颜色模式切换
 * 2. SurfaceView HDR支持
 * 3. HDR内容检测和适配
 * 4. 自动检测设备HDR能力
 */
object RealHdrHelper {

    // HDR颜色模式常量
    @RequiresApi(Build.VERSION_CODES.O)
    private val HDR_COLOR_MODES = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            android.view.Display.COLOR_MODE_BT2020_HDR to "BT.2020 HDR",
            android.view.Display.COLOR_MODE_HDR10 to "HDR10",
            android.view.Display.COLOR_MODE_HLG to "HLG",
            android.view.Display.COLOR_MODE_DOLBY_VISION to "Dolby Vision"
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        listOf(
            android.view.Display.COLOR_MODE_HDR10 to "HDR10"
        )
    } else {
        emptyList()
    }

    /**
     * 检查设备是否支持HDR
     */
    fun isHdrSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        return try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
            }

            display?.isHdr ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取支持的HDR模式
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getSupportedHdrModes(context: Context): List<Pair<Int, String>> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return emptyList()
        }

        return try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
            }

            display?.supportedColorModes?.filter { colorMode ->
                HDR_COLOR_MODES.any { it.first == colorMode }
            }?.mapNotNull { colorMode ->
                HDR_COLOR_MODES.find { it.first == colorMode }
            } ?: emptyList()
        } catch (e: Exception) {
            HDR_COLOR_MODES
        }
    }

    /**
     * 获取当前颜色模式是否HDR
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun isHdrModeActive(activity: Activity): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.isHdr ?: false
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay?.isHdr ?: false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 为Activity启用HDR模式
     * 这会触发系统在右上角显示HDR倍率（如果是真正的HDR内容）
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enableHdr(activity: Activity): Boolean {
        if (!isHdrSupported(activity)) {
            return false
        }

        return try {
            val window = activity.window
            
            // 获取支持的HDR颜色模式
            val supportedModes = getSupportedHdrModes(activity)
            if (supportedModes.isEmpty()) {
                return false
            }

            // 选择最高优先级的HDR模式
            val hdrMode = supportedModes.first().first
            
            // 设置Window颜色模式
            val result = window.setColorMode(hdrMode)
            
            // 确保Window使用HDR配置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    // 允许HDR表面
                    preferMinimalPostProcessing = false
                }
            }

            result
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 为Activity禁用HDR模式
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun disableHdr(activity: Activity): Boolean {
        return try {
            val window = activity.window
            
            // 设置回SDR颜色模式
            window.setColorMode(android.view.Display.COLOR_MODE_DEFAULT)
            
            // 恢复默认属性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    preferMinimalPostProcessing = true
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 为SurfaceView启用HDR支持
     * 用于显示HDR图像或视频
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enableHdrForSurface(surfaceView: SurfaceView, isHdr: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        return try {
            val holder = surfaceView.holder
            
            if (isHdr) {
                // 设置HDR颜色空间
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.setFormat(android.graphics.PixelFormat.RGBA_1010102)
                }
                
                // 设置Secure surface用于HDR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    holder.setSurfaceViewHdrHeadChain(false)
                }
            } else {
                // 恢复SDR格式
                holder.setFormat(android.graphics.PixelFormat.RGBA_8888)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 创建HDR兼容的Bitmap
     * 使用BT.2020颜色空间
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createHdrBitmap(width: Int, height: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null
        }

        return try {
            Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.RGBA_FP16
            ).apply {
                // 设置HDR颜色空间
                colorSpace = ColorSpace.get(ColorSpace.Named.BT2020)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查Bitmap是否是HDR格式
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun isHdrBitmap(bitmap: Bitmap): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }

        return try {
            bitmap.config == Bitmap.Config.RGBA_FP16 ||
            bitmap.colorSpace == ColorSpace.get(ColorSpace.Named.BT2020) ||
            bitmap.colorSpace == ColorSpace.get(ColorSpace.Named.DISPLAY_P3)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取HDR状态描述
     */
    fun getHdrStatusDescription(context: Context): String {
        if (!isHdrSupported(context)) {
            return "设备不支持HDR"
        }

        return try {
            val supportedModes = getSupportedHdrModes(context)
            if (supportedModes.isEmpty()) {
                return "HDR可用（基础模式）"
            }
            
            val modes = supportedModes.joinToString(", ") { it.second }
            "HDR可用: $modes"
        } catch (e: Exception) {
            "HDR可用"
        }
    }

    /**
     * 自动适配最佳HDR设置
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun autoOptimize(activity: Activity): Boolean {
        if (!isHdrSupported(activity)) {
            return false
        }

        return try {
            val supportedModes = getSupportedHdrModes(activity)
            
            // 根据设备选择最佳模式
            val bestMode = when {
                supportedModes.any { it.second.contains("Dolby") } -> 
                    supportedModes.first { it.second.contains("Dolby") }.first
                supportedModes.any { it.second.contains("BT.2020") } -> 
                    supportedModes.first { it.second.contains("BT.2020") }.first
                supportedModes.any { it.second.contains("HDR10") } -> 
                    supportedModes.first { it.second.contains("HDR10") }.first
                supportedModes.any { it.second.contains("HLG") } -> 
                    supportedModes.first { it.second.contains("HLG") }.first
                else -> return false
            }

            activity.window.setColorMode(bestMode)
        } catch (e: Exception) {
            false
        }
    }
}
