package com.gusogst.chat

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.gusogst.chat.agent.HermesBridge
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

        // ── Theme / night-mode setup (early, before any Activity) ─────
        // 与 ChatStore 使用同一个 SharedPreferences 文件，确保设置一致性
        val prefs = getSharedPreferences("chat_gusogst", MODE_PRIVATE)
        val settingsJson = prefs.getString("settings", null)
        cachedTheme = if (settingsJson != null) {
            try {
                JSONObject(settingsJson).optString("theme", "dark")
            } catch (_: Exception) { "dark" }
        } else "dark"

        // pureBlack 需要额外的 Amoled 主题覆盖（在 Activity 创建前设置）
        if (cachedTheme == "pureBlack") {
            setTheme(R.style.Theme_ChatGusogst_Amoled)
        }

        val mode = when (cachedTheme) {
            "dark", "pureBlack" -> AppCompatDelegate.MODE_NIGHT_YES
            "light", "pureWhite" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)

        // ── Hermes Agent initialization (Chaquopy Python runtime) ─────
        // Deferred to a background thread so Application.onCreate() returns
        // quickly.  The Python runtime boot + agent module load takes
        // ~200–500 ms on first launch; the UI renders immediately with the
        // fallback direct-API path, and HermesBridge takes over once ready.
        //
        // The init call is idempotent — ChatViewModel determines the actual
        // provider/model when it calls send_message.
        Thread({
            try {
                HermesBridge.init(
                    context = this,
                    apiKey = "",
                    baseUrl = "",
                    provider = "",
                    model = "gpt-4o",
                )
                Log.i("ChatApp", "Hermes Agent initialized successfully")

                // Quick diagnostic — logs Python path and module status
                val envInfo = HermesBridge.verifyEnvironment()
                Log.d("ChatApp", "Hermes env: ${envInfo.take(500)}")
            } catch (e: Exception) {
                Log.e("ChatApp", "Hermes Agent init failed — falling back to direct API", e)
                // Non-fatal: ChatViewModel falls back to direct Retrofit API
                // when HermesBridge.isStarted() returns false.
            }
        }, "hermes-init").start()
    }
}
