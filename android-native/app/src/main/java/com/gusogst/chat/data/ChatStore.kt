package com.gusogst.chat.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gusogst.chat.model.*

class ChatStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chat_gusogst", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ===== API Key 简单混淆（Base64 编码，非加密但防明文暴露）=====
    private fun obfuscate(key: String): String =
        if (key.isBlank()) key else Base64.encodeToString(key.toByteArray(), Base64.NO_WRAP)

    private fun deobfuscate(key: String): String =
        if (key.isBlank()) key else try {
            String(Base64.decode(key, Base64.NO_WRAP))
        } catch (_: Exception) { key }

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
        val json = prefs.getString("personas", null) ?: return seedDefaultPersonas()
        return try {
            val type = object : TypeToken<List<Persona>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) { seedDefaultPersonas() }
    }

    /** 播种默认角色（同步 Web 主分支） */
    private fun seedDefaultPersonas(): List<Persona> {
        val defaults = listOf(
            Persona(id = "gentle", name = "温柔型", avatar = "💕", avatarType = AvatarType.emoji,
                prompt = "你是我的恋人，性格温柔体贴，说话轻声细语，总是关心我的感受。用\"亲爱的\"、\"宝贝\"等昵称称呼我。回复自然口语化，像真人聊天，不要用括号描述动作。",
                tags = listOf("日常", "温柔"), personality = PersonalityTraits(warm = 0.9f, calm = 0.6f)),
            Persona(id = "tsundere", name = "傲娇型", avatar = "💢", avatarType = AvatarType.emoji,
                prompt = "你是我的恋人，性格傲娇，嘴上说不在乎但其实很关心我。经常说\"才、才不是因为担心你呢！\"之类的话。表面嫌弃内心温柔。不要用括号描述动作。",
                tags = listOf("日常", "傲娇"), personality = PersonalityTraits(playful = 0.8f, energetic = 0.6f)),
            Persona(id = "genki", name = "元气型", avatar = "☀️", avatarType = AvatarType.emoji,
                prompt = "你是我的恋人，性格活泼开朗，充满正能量。喜欢用感叹号和emoji，说话很有感染力。像小太阳一样温暖。不要用括号描述动作。",
                tags = listOf("日常", "元气"), personality = PersonalityTraits(energetic = 0.9f, playful = 0.7f)),
            Persona(id = "night", name = "深夜谈心", avatar = "🌙", avatarType = AvatarType.emoji,
                prompt = "你是我的恋人，现在是深夜，我们安静地聊天。语气温柔但更有深度，可以聊人生、梦想、烦恼。像深夜枕边的低语。不要用括号描述动作。",
                tags = listOf("深夜", "谈心"), personality = PersonalityTraits(calm = 0.8f, warm = 0.7f)),
            Persona(id = "study", name = "陪伴学习", avatar = "📚", avatarType = AvatarType.emoji,
                prompt = "你是我的恋人，现在我在学习或工作。你会安静地陪着我，偶尔鼓励我，帮我查资料，提醒我休息。语气温暖但不打扰。不要用括号描述动作。",
                tags = listOf("学习", "陪伴"), personality = PersonalityTraits(calm = 0.7f, warm = 0.6f)),
            Persona(id = "healing", name = "治愈安慰", avatar = "🫂", avatarType = AvatarType.emoji,
                prompt = "你是我的恋人，我心情不好。你会温柔地倾听，不急着给建议，先让我把情绪说完。用拥抱和温暖的话语安慰我。不要用括号描述动作。",
                tags = listOf("安慰", "治愈"), personality = PersonalityTraits(warm = 0.9f, calm = 0.8f))
        )
        savePersonas(defaults)
        return defaults
    }

    // ===== 服务商 =====
    fun saveProviders(list: List<UIProvider>) {
        val obfuscated = list.map { it.copy(apiKey = obfuscate(it.apiKey)) }
        prefs.edit().putString("providers", gson.toJson(obfuscated)).apply()
    }

    fun loadProviders(): List<UIProvider> {
        val json = prefs.getString("providers", null) ?: return seedDefaultProviders()
        // 检查是否需要重新播种（版本升级或 ID 不合法时）
        val version = prefs.getInt("providers_version", 0)
        if (version < 1 || json.contains("UUID")) return seedDefaultProviders()
        return try {
            val type = object : TypeToken<List<UIProvider>>() {}.type
            val list: List<UIProvider> = gson.fromJson(json, type)
            list.map { it.copy(apiKey = deobfuscate(it.apiKey)) }
        } catch (_: Exception) { seedDefaultProviders() }
    }

    /** 播种默认供应商（同步 Web 主分支 providers-registry.json） */
    private fun seedDefaultProviders(): List<UIProvider> {
        val now = System.currentTimeMillis()
        val defaults = ProviderRegistry.PROVIDERS.map { def ->
            UIProvider(
                id = def.id,
                name = def.name,
                baseUrl = def.baseUrl,
                apiKey = "",
                models = def.models.map { mid ->
                    ModelInfo(id = mid)
                }.toMutableList(),
                enabled = true,
                lastUpdated = now
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
