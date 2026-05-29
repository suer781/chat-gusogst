package com.gusogst.chat.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gusogst.chat.model.*

class ChatStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chat_gusogst", Context.MODE_PRIVATE)
    private val gson = Gson()

    init {
        ProviderRegistry.load(context)
    }

    companion object {
        @Volatile
        private var INSTANCE: ChatStore? = null
        fun getInstance(context: Context): ChatStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ChatStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ===== 对话 =====
    fun saveConversations(list: List<Conversation>) {
        prefs.edit().putString("conversations", gson.toJson(list)).apply()
    }

    fun loadConversations(): List<Conversation> {
        val json = prefs.getString("conversations", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Conversation>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) { emptyList() }
    }

    // ===== 角色 =====
    fun savePersonas(list: List<Persona>) {
        prefs.edit().putString("personas", gson.toJson(list)).apply()
    }

    fun loadPersonas(): List<Persona> {
        val json = prefs.getString("personas", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Persona>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) { emptyList() }
    }

    // ===== 服务商 =====
    // ===== 服务商 =====
    fun saveProviders(list: List<UIProvider>) {
        prefs.edit().putString("providers", gson.toJson(list)).apply()
    }

    fun loadProviders(): List<UIProvider> {
        val json = prefs.getString("providers", null) ?: return seedDefaultProviders()
        // 检查是否需要重新播种（版本升级或 ID 不合法时）
        val version = prefs.getInt("providers_version", 0)
        if (version < 1 || json.contains("UUID")) return seedDefaultProviders()
        return try {
            val type = object : TypeToken<List<UIProvider>>() {}.type
            val list: List<UIProvider> = gson.fromJson(json, type)
            list
        } catch (_: Exception) { seedDefaultProviders() }
    }

    /** 播种默认供应商（同步 Web 主分支 providers-registry.json） */
    private fun seedDefaultProviders(): List<UIProvider> {
        val defaults = ProviderRegistry.PROVIDERS.map { def ->
            UIProvider(
                id = def.id,
                name = def.name,
                baseUrl = def.baseUrl,
                apiKey = "",
                models = def.models.map { mid ->
                    ModelInfo(id = mid)
                }.toMutableList(),
                enabled = true
            )
        }
        saveProviders(defaults)
        prefs.edit().putInt("providers_version", 1).apply()
        return defaults
    }

    // ===== 设置 =====
    fun saveSettings(settings: UISettings) {
        prefs.edit().putString("settings", gson.toJson(settings)).apply()
    }

    fun loadSettings(): UISettings {
        val json = prefs.getString("settings", null) ?: return UISettings()
        return try {
            gson.fromJson(json, UISettings::class.java)
        } catch (_: Exception) { UISettings() }
    }

    // ===== 活跃对话ID =====
    fun saveActiveConversationId(id: String) {
        prefs.edit().putString("active_conversation_id", id).apply()
    }

    fun loadActiveConversationId(): String? {
        return prefs.getString("active_conversation_id", null)
    }

    // ===== 活跃角色ID =====
    fun saveActivePersonaId(id: String) {
        prefs.edit().putString("active_persona_id", id).apply()
    }

    fun loadActivePersonaId(): String? {
        return prefs.getString("active_persona_id", null)
    }
}
