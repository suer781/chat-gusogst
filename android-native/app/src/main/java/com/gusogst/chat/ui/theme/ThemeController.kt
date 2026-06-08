package com.gusogst.chat.ui.theme

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils
import com.gusogst.chat.data.settings.ChatSettingsManager

/**
 * 主题控制器 - 本地函数方式管理主题切换
 * 不依赖 HTTP，使用内部函数直接操作
 */
class ThemeController private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "theme_controller_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT_SIZE = "font_size"
        
        private const val DEFAULT_THEME_COLOR = "#6200EE"
        private const val DEFAULT_ACCENT_COLOR = "#03DAC6"

        @Volatile
        private var instance: ThemeController? = null

        fun getInstance(context: Context): ThemeController {
            return instance ?: synchronized(this) {
                instance ?: ThemeController(context.applicationContext).also { instance = it }
            }
        }

        // 主题模式常量
        const val MODE_LIGHT = "light"
        const val MODE_DARK = "dark"
        const val MODE_PURE_WHITE = "pureWhite"
        const val MODE_PURE_BLACK = "pureBlack"
        const val MODE_SYSTEM = "system"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 获取当前主题模式
    fun getThemeMode(): String {
        return prefs.getString(KEY_THEME_MODE, MODE_DARK) ?: MODE_DARK
    }

    // 获取主题色
    fun getThemeColor(): String {
        return prefs.getString(KEY_THEME_COLOR, DEFAULT_THEME_COLOR) ?: DEFAULT_THEME_COLOR
    }

    // 获取强调色
    fun getAccentColor(): String {
        return prefs.getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR) ?: DEFAULT_ACCENT_COLOR
    }

    // 获取字体缩放
    fun getFontSize(): Float {
        return prefs.getFloat(KEY_FONT_SIZE, 1.0f)
    }

    // 设置主题模式
    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        applyTheme(mode)
    }

    // 设置主题色
    fun setThemeColor(color: String) {
        prefs.edit().putString(KEY_THEME_COLOR, color).apply()
        applyColorTheme(color)
    }

    // 设置强调色
    fun setAccentColor(color: String) {
        prefs.edit().putString(KEY_ACCENT_COLOR, color).apply()
        applyAccentColor(color)
    }

    // 设置字体缩放
    fun setFontSize(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SIZE, scale).apply()
        applyFontSize(scale)
    }

    /**
     * 应用主题模式 - 本地函数，不依赖网络
     */
    fun applyTheme(mode: String = getThemeMode()) {
        val nightMode = when (mode) {
            MODE_LIGHT, MODE_PURE_WHITE -> AppCompatDelegate.MODE_NIGHT_NO
            MODE_DARK, MODE_PURE_BLACK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode)
        
        // 如果在 Activity 中应用
        if (context is Application) {
            // 全局应用
        } else if (context is Activity) {
            val activity = context as Activity
            applyActivityTheme(activity, mode)
        }
    }

    /**
     * 应用 Activity 主题 - 内部函数
     */
    fun applyActivityTheme(activity: Activity, mode: String = getThemeMode()) {
        val window = activity.window
        
        // 状态栏和导航栏颜色
        val (statusBarColor, navBarColor, isLightStatusBar) = when (mode) {
            MODE_LIGHT -> Triple(Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF"), true)
            MODE_PURE_WHITE -> Triple(Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF"), true)
            MODE_DARK -> Triple(Color.parseColor("#0D0D2B"), Color.parseColor("#0D0D2B"), false)
            MODE_PURE_BLACK -> Triple(Color.BLACK, Color.BLACK, false)
            else -> {
                val isNight = (activity.resources.configuration.uiMode and 
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                if (isNight) {
                    Triple(Color.parseColor("#0D0D2B"), Color.parseColor("#0D0D2B"), false)
                } else {
                    Triple(Color.parseColor("#FFFFFF"), Color.parseColor("#FFFFFF"), true)
                }
            }
        }
        
        // 应用颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = statusBarColor
            window.navigationBarColor = navBarColor
        }
        
        // 设置状态栏图标颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (isLightStatusBar) {
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
        
        // 设置导航栏图标颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = if (isLightStatusBar) {
                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            } else {
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
            }
        }
    }

    /**
     * 应用主题色 - 内部函数
     */
    fun applyColorTheme(color: String = getThemeColor()) {
        try {
            val parsedColor = Color.parseColor(color)
            // 可以在此处应用动态颜色到 UI 元素
            // 例如：按钮颜色、强调色等
        } catch (e: Exception) {
            // 颜色解析失败，使用默认颜色
        }
    }

    /**
     * 应用强调色 - 内部函数
     */
    fun applyAccentColor(color: String = getAccentColor()) {
        try {
            val parsedColor = Color.parseColor(color)
            // 应用强调色到 UI 元素
        } catch (e: Exception) {
            // 颜色解析失败，使用默认颜色
        }
    }

    /**
     * 应用字体缩放 - 内部函数
     */
    fun applyFontSize(scale: Float = getFontSize()) {
        // 字体缩放可以通过配置或反射应用到整个应用
        // 这里可以根据 scale 调整全局字体配置
    }

    /**
     * 获取主题颜色值
     */
    fun getThemeColorInt(): Int {
        return try {
            Color.parseColor(getThemeColor())
        } catch (e: Exception) {
            Color.parseColor(DEFAULT_THEME_COLOR)
        }
    }

    /**
     * 获取强调色值
     */
    fun getAccentColorInt(): Int {
        return try {
            Color.parseColor(getAccentColor())
        } catch (e: Exception) {
            Color.parseColor(DEFAULT_ACCENT_COLOR)
        }
    }

    /**
     * 计算主题颜色亮度
     */
    fun isDarkTheme(): Boolean {
        val mode = getThemeMode()
        return mode == MODE_DARK || mode == MODE_PURE_BLACK
    }

    /**
     * 获取主题背景色
     */
    fun getBackgroundColor(): Int {
        return try {
            when (getThemeMode()) {
                MODE_LIGHT, MODE_PURE_WHITE -> Color.parseColor("#FFFFFF")
                MODE_DARK -> Color.parseColor("#0D0D2B")
                MODE_PURE_BLACK -> Color.BLACK
                else -> Color.parseColor("#0D0D2B")
            }
        } catch (e: Exception) {
            Color.parseColor("#0D0D2B")
        }
    }

    /**
     * 获取主题文字颜色
     */
    fun getTextColor(): Int {
        return try {
            when (getThemeMode()) {
                MODE_LIGHT, MODE_PURE_WHITE -> Color.parseColor("#1A1A2E")
                MODE_DARK, MODE_PURE_BLACK -> Color.WHITE
                else -> Color.WHITE
            }
        } catch (e: Exception) {
            Color.WHITE
        }
    }

    /**
     * 获取次要文字颜色
     */
    fun getSecondaryTextColor(): Int {
        return try {
            when (getThemeMode()) {
                MODE_LIGHT, MODE_PURE_WHITE -> Color.parseColor("#555570")
                MODE_DARK, MODE_PURE_BLACK -> Color.parseColor("#A0A0B8")
                else -> Color.parseColor("#A0A0B8")
            }
        } catch (e: Exception) {
            Color.parseColor("#A0A0B8")
        }
    }
}
