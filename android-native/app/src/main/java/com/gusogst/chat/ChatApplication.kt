package com.gusogst.chat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONObject

class ChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 在 Activity 创建前设置默认深色，避免 Activity 重建闪烁
        val prefs = getSharedPreferences("chat_prefs", MODE_PRIVATE)
        val settingsJson = prefs.getString("settings", null)
        val themeName = if (settingsJson != null) {
            try {
                JSONObject(settingsJson).optString("theme", "dark")
            } catch (_: Exception) { "dark" }
        } else "dark"

        val mode = when (themeName) {
            "dark", "pureBlack" -> AppCompatDelegate.MODE_NIGHT_YES
            "light", "pureWhite" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
