package com.gusogst.chat.util

import android.app.Application
import android.graphics.Color
import com.gusogst.chat.R

/**
 * Eye care color mapper - mirrors Web EyeCareColorMapper.tsx
 * Preset warm colors + custom color + intensity slider
 */
object EyeCareColorMapper {

    private lateinit var appContext: android.content.Context

    fun init(context: android.content.Context) {
        appContext = context.applicationContext
    }

    private fun c(id: Int): Int = appContext.resources.getColor(id, null)

    private fun parseColorSafe(hex: String): Int {
        return try {
            if (::appContext.isInitialized) {
                when (hex) {
                    "#FFF8E1" -> c(R.color.eyecare_warm_white)
                    "#FFE0B2" -> c(R.color.eyecare_amber)
                    "#FFCC80" -> c(R.color.eyecare_honey)
                    "#FFF9C4" -> c(R.color.eyecare_pale_yellow)
                    "#E0F2F1" -> c(R.color.eyecare_mint)
                    else -> Color.parseColor(hex)
                }
            } else Color.parseColor(hex)
        } catch (_: Exception) { Color.parseColor(hex) }
    }

    data class ColorPreset(val name: String, val color: Int, val description: String)

    val PRESETS = listOf(
        ColorPreset("\u6696\u767D", parseColorSafe("#FFF8E1"), "\u67D4\u548C\u6696\u767D"),
        ColorPreset("\u7425\u73C0", parseColorSafe("#FFE0B2"), "\u6E29\u6696\u7425\u73C0"),
        ColorPreset("\u871C\u6843", parseColorSafe("#FFCC80"), "\u7518\u7F8E\u871C\u6843"),
        ColorPreset("\u6DE1\u9EC4", parseColorSafe("#FFF9C4"), "\u6E05\u65B0\u6DE1\u9EC4"),
        ColorPreset("\u8584\u8377", parseColorSafe("#E0F2F1"), "\u6E05\u1C9\u8584\u8377")
    )

    fun applyEyeCare(color: Int, warmth: Int, intensity: Float): Int {
        if (intensity <= 0f) return color
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val wr = Color.red(warmth)
        val wg = Color.green(warmth)
        val wb = Color.blue(warmth)
        val factor = intensity.coerceIn(0f, 1f)
        val nr = (r + (wr - r) * factor).toInt().coerceIn(0, 255)
        val ng = (g + (wg - g) * factor).toInt().coerceIn(0, 255)
        val nb = (b + (wb - b) * factor).toInt().coerceIn(0, 255)
        return Color.argb(Color.alpha(color), nr, ng, nb)
    }

    fun applyToBackground(bgColor: Int, warmth: Int, intensity: Float): Int {
        return applyEyeCare(bgColor, warmth, intensity * 0.4f)
    }

    fun applyToText(textColor: Int, warmth: Int, intensity: Float): Int {
        return applyEyeCare(textColor, warmth, intensity * 0.15f)
    }
}