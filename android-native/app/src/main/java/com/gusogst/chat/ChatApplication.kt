package com.gusogst.chat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONObject

class ChatApplication : Application() {

    companion object {
        /** 缓存已读取的主题名，Activity 直接读不用再读 Preferences */
        @JvmStatic
        var cachedTheme: String = "dark"
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // 在 Activity 创建前设置默认深色，避免 Activity 重建闪烁
        val prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        val settingsJson = prefs.getString("settings", null)
        cachedTheme = if (settingsJson != null) {
            try {
                JSONObject(settingsJson).optString("theme", "dark")
            } catch (_: Exception) { "dark" }
        } else "dark"

        val mode = when (cachedTheme) {
            "dark", "pureBlack" -> AppCompatDelegate.MODE_NIGHT_YES
            "light", "pureWhite" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
