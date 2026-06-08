package com.gusogst.chat.data.settings

import android.content.Context
import android.content.SharedPreferences
import com.gusogst.chat.data.ChatStore
import com.gusogst.chat.model.UISettings

/**
 * 设置便捷访问层 — 封装 ChatStore 的 UISettings
 * 提供类型安全的 getter/setter，UI 层直接调用
 */
class ChatSettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("chat_settings", Context.MODE_PRIVATE)

    // ===== 主题 =====
    fun getLaunchCount(): Int = prefs.getInt("launchCount", 0)
    fun setLaunchCount(v: Int) = prefs.edit().putInt("launchCount", v).apply()
    
    fun getThemeMode(): String = prefs.getString("themeMode", "dark") ?: "dark"
    fun setThemeMode(v: String) = prefs.edit().putString("themeMode", v).apply()

    fun getThemeColor(): String = prefs.getString("themeColor", "#6200EE") ?: "#6200EE"
    fun setThemeColor(v: String) = prefs.edit().putString("themeColor", v).apply()

    fun getAccentColor(): String = prefs.getString("accentColor", "#03DAC6") ?: "#03DAC6"
    fun setAccentColor(v: String) = prefs.edit().putString("accentColor", v).apply()

    fun getFontSize(): Float = prefs.getFloat("fontSize", 1.0f)
    fun setFontSize(v: Float) = prefs.edit().putFloat("fontSize", v).apply()

    // ===== 消息显示 =====
    fun getUseBubbles(): Boolean = prefs.getBoolean("useBubbles", true)
    fun setUseBubbles(v: Boolean) = prefs.edit().putBoolean("useBubbles", v).apply()

    fun getQuoteAsBubble(): Boolean = prefs.getBoolean("quoteAsBubble", false)
    fun setQuoteAsBubble(v: Boolean) = prefs.edit().putBoolean("quoteAsBubble", v).apply()

    fun getMarkdownRendering(): Boolean = prefs.getBoolean("markdownRendering", true)
    fun setMarkdownRendering(v: Boolean) = prefs.edit().putBoolean("markdownRendering", v).apply()

    fun getMarkdownRenderingMode(): String = prefs.getString("markdownRenderingMode", "full") ?: "full"
    fun setMarkdownRenderingMode(v: String) = prefs.edit().putString("markdownRenderingMode", v).apply()

    fun getLongMessageThreshold(): Int = prefs.getInt("longMessageThreshold", 2000)
    fun setLongMessageThreshold(v: Int) = prefs.edit().putInt("longMessageThreshold", v).apply()

    fun getMessageCounterEnabled(): Boolean = prefs.getBoolean("messageCounterEnabled", true)
    fun setMessageCounterEnabled(v: Boolean) = prefs.edit().putBoolean("messageCounterEnabled", v).apply()

    fun getResourcePreviewEnabled(): Boolean = prefs.getBoolean("resourcePreviewEnabled", true)
    fun setResourcePreviewEnabled(v: Boolean) = prefs.edit().putBoolean("resourcePreviewEnabled", v).apply()

    // ===== HDR =====
    fun isHdrEnabled(): Boolean = prefs.getBoolean("hdrEnabled", false)
    fun setHdrEnabled(v: Boolean) = prefs.edit().putBoolean("hdrEnabled", v).apply()

    fun isAutoHdrEnabled(): Boolean = prefs.getBoolean("autoHdrEnabled", true)
    fun setAutoHdrEnabled(v: Boolean) = prefs.edit().putBoolean("autoHdrEnabled", v).apply()

    // ===== 轮询 =====
    fun getPollingIntervalSeconds(): Int = prefs.getInt("pollingIntervalSeconds", 10)
    fun setPollingIntervalSeconds(v: Int) = prefs.edit().putInt("pollingIntervalSeconds", v).apply()

    // ===== Agent =====
    fun getAgentName(): String = prefs.getString("agentName", "助手") ?: "助手"
    fun setAgentName(v: String) = prefs.edit().putString("agentName", v).apply()

    fun getSystemPrompt(): String = prefs.getString("systemPrompt", "") ?: ""
    fun setSystemPrompt(v: String) = prefs.edit().putString("systemPrompt", v).apply()

    fun getAgentTone(): String = prefs.getString("agentTone", "default") ?: "default"
    fun setAgentTone(v: String) = prefs.edit().putString("agentTone", v).apply()

    fun getAgentResponseLength(): String = prefs.getString("agentResponseLength", "auto") ?: "auto"
    fun setAgentResponseLength(v: String) = prefs.edit().putString("agentResponseLength", v).apply()

    fun getTemperature(): Float = prefs.getFloat("temperature", 0.7f)
    fun setTemperature(v: Float) = prefs.edit().putFloat("temperature", v).apply()

    fun getTopP(): Float = prefs.getFloat("topP", 0.9f)
    fun setTopP(v: Float) = prefs.edit().putFloat("topP", v).apply()

    // ===== 导出/导入 =====
    fun toUISettings(): UISettings = UISettings(
        themeMode = getThemeMode(),
        themeColor = getThemeColor(),
        accentColor = getAccentColor(),
        fontSize = getFontSize(),
        useBubbles = getUseBubbles(),
        quoteAsBubble = getQuoteAsBubble(),
        markdownRendering = getMarkdownRendering(),
        markdownRenderingMode = getMarkdownRenderingMode(),
        longMessageThreshold = getLongMessageThreshold(),
        messageCounterEnabled = getMessageCounterEnabled(),
        resourcePreviewEnabled = getResourcePreviewEnabled(),
        pollingIntervalSeconds = getPollingIntervalSeconds(),
        agentName = getAgentName(),
        systemPrompt = getSystemPrompt(),
        agentTone = getAgentTone(),
        agentResponseLength = getAgentResponseLength(),
        temperature = getTemperature(),
        topP = getTopP()
    )
}
