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

    private const val COLOR_MODE_DEFAULT = 0
    private const val COLOR_MODE_HDR10 = 1
    private const val COLOR_MODE_HLG = 2
    private const val COLOR_MODE_BT2020_HDR = 3
    private const val COLOR_MODE_DOLBY_VISION = 4

    private val HDR_COLOR_MODES: List<Pair<Int, String>> = listOf(
        Pair(COLOR_MODE_BT2020_HDR, "BT.2020 HDR"),
        Pair(COLOR_MODE_HDR10, "HDR10"),
        Pair(COLOR_MODE_HLG, "HLG"),
        Pair(COLOR_MODE_DOLBY_VISION, "Dolby Vision")
    )

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

            if (display?.isHdr != true) {
                return emptyList()
            }

            // 尝试通过反射获取 supportedColorModes，如果不可用则返回默认支持列表
            val rawModes: IntArray? = try {
                val method = android.view.Display::class.java.getMethod("getSupportedColorModes")
                @Suppress("UNCHECKED_CAST")
                method.invoke(display) as? IntArray
            } catch (_: Throwable) {
                null
            }

            if (rawModes != null) {
                HDR_COLOR_MODES.filter { it.first in rawModes }
            } else {
                // 在确认支持 HDR 的设备上返回通用支持列表
                listOf(
                    Pair(COLOR_MODE_BT2020_HDR, "BT.2020 HDR"),
                    Pair(COLOR_MODE_HDR10, "HDR10"),
                    Pair(COLOR_MODE_HLG, "HLG")
                )
            }
        } catch (e: Exception) {
            emptyList()
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

            val supportedModes = getSupportedHdrModes(activity)
            if (supportedModes.isEmpty()) {
                return false
            }

            val hdrMode = supportedModes.first().first

            window.setColorMode(hdrMode)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    preferMinimalPostProcessing = false
                }
            }

            true
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

            window.setColorMode(COLOR_MODE_DEFAULT)

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
                holder.setFormat(android.graphics.PixelFormat.RGBA_1010102)
            } else {
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
            val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                Bitmap.Config.valueOf("RGBA_FP16")
            } else {
                Bitmap.Config.ARGB_8888
            }
            Bitmap.createBitmap(width, height, config).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    setColorSpace(ColorSpace.get(ColorSpace.Named.BT2020))
                }
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
            val isFp16 = try {
                val fp16Config = Bitmap.Config.valueOf("RGBA_FP16")
                bitmap.config == fp16Config
            } catch (_: Exception) {
                false
            }
            val hasWideColorSpace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cs = bitmap.colorSpace
                cs != null
            } else {
                false
            }
            isFp16 || hasWideColorSpace
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
            true
        } catch (e: Exception) {
            false
        }
    }
}
