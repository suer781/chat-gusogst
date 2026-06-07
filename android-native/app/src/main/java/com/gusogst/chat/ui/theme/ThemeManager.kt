package com.gusogst.chat.ui.theme

import android.content.Context
import android.content.SharedPreferences

/**
 * 主题管理器 — 深色模式、主题色、强调色、字体大小
 */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

class ThemeManager private constructor(context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_prefs"

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ===== 深色模式 =====
    fun getThemeMode(): ThemeMode {
        return when (prefs.getString("themeMode", "dark")) {
            "light" -> ThemeMode.LIGHT
            "system" -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("themeMode", when (mode) {
            ThemeMode.LIGHT -> "light"
            ThemeMode.DARK -> "dark"
            ThemeMode.SYSTEM -> "system"
        }).apply()
    }

    fun isDarkMode(): Boolean = getThemeMode() == ThemeMode.DARK

    // ===== 主题色 =====
    fun getThemeColor(): String = prefs.getString("themeColor", "#6200EE") ?: "#6200EE"
    fun setThemeColor(color: String) = prefs.edit().putString("themeColor", color).apply()

    // ===== 强调色 =====
    fun getAccentColor(): String = prefs.getString("accentColor", "#03DAC6") ?: "#03DAC6"
    fun setAccent(color: String) = prefs.edit().putString("accentColor", color).apply()

    fun setAccentColor(color: String) = setAccent(color)

    // ===== 字体大小 =====
    fun getFontSize(): Float = prefs.getFloat("fontSize", 1.0f)
    fun setFontSize(scale: Float) = prefs.edit().putFloat("fontSize", scale).apply()

    // ===== 颜色解析 =====
    fun getThemeColorInt(): Int = android.graphics.Color.parseColor(getThemeColor())
    fun getAccentColorInt(): Int = android.graphics.Color.parseColor(getAccentColor())
}
