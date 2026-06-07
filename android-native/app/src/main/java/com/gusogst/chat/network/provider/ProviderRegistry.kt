package com.gusogst.chat.network.provider

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gusogst.chat.model.ProviderType

/**
 * Provider 注册表 — 移植自 main 分支 provider-registry.ts
 * 智能匹配：域名 → 类型，向量搜索，评分排序
 */
class ProviderRegistry(context: Context) {

    companion object {
        private const val TAG = "ProviderRegistry"
        private const val PREFS_NAME = "provider_registry"
        private const val KEY_ENDPOINTS = "endpoints"
        private val gson = Gson()
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 内置域名 → Provider 类型映射（从 main 分支 porting）
    private val domainToType = mapOf(
        "api.openai.com" to ProviderType.OPENAI,
        "api.anthropic.com" to ProviderType.ANTHROPIC,
        "generativelanguage.googleapis.com" to ProviderType.GEMINI,
        "api.deepseek.com" to ProviderType.DEEPSEEK,
        "api.moonshot.cn" to ProviderType.OPENAI,
        "api.groq.com" to ProviderType.OPENAI,
        "api.together.xyz" to ProviderType.OPENAI,
        "api.fireworks.ai" to ProviderType.OPENAI,
        "openrouter.ai" to ProviderType.OPENAI,
        "api.siliconflow.cn" to ProviderType.OPENAI,
        "api.minimax.chat" to ProviderType.OPENAI,
        "api.stepfun.com" to ProviderType.OPENAI,
        "api.zhipu.ai" to ProviderType.OPENAI,
        "api.baichuan-ai.com" to ProviderType.OPENAI,
        "api.volcengine.com" to ProviderType.OPENAI,
        "api.doubao.com" to ProviderType.OPENAI,
        "ark.cn-beijing.volces.com" to ProviderType.OPENAI,
    )

    // ===== 核心：URL → ProviderType =====

    fun detectProviderType(url: String): ProviderType {
        val host = extractHost(url).lowercase()

        // 1. 精确域名匹配
        for ((domain, type) in domainToType) {
            if (host == domain || host.endsWith(".$domain")) {
                Log.d(TAG, "Domain match: $host → $type")
                return type
            }
        }

        // 2. 关键词启发式匹配
        val hostLower = host.lowercase()
        return when {
            "anthropic" in hostLower || "claude" in hostLower -> ProviderType.ANTHROPIC
            "gemini" in hostLower || "google" in hostLower || "goog" in hostLower -> ProviderType.GEMINI
            "deepseek" in hostLower -> ProviderType.DEEPSEEK
            "openai" in hostLower || "gpt" in hostLower -> ProviderType.OPENAI
            "minimax" in hostLower -> ProviderType.OPENAI
            "moonshot" in hostLower || "kimi" in hostLower -> ProviderType.OPENAI
            "siliconflow" in hostLower -> ProviderType.OPENAI
            "groq" in hostLower -> ProviderType.OPENAI
            "together" in hostLower -> ProviderType.OPENAI
            "fireworks" in hostLower -> ProviderType.OPENAI
            "openrouter" in hostLower -> ProviderType.OPENAI
            "qwen" in hostLower || "dashscope" in hostLower || "aliyun" in hostLower -> ProviderType.OPENAI
            "stepfun" in hostLower -> ProviderType.OPENAI
            "zhipu" in hostLower || "chatglm" in hostLower -> ProviderType.OPENAI
            "baichuan" in hostLower -> ProviderType.OPENAI
            "doubao" in hostLower || "volces" in hostLower || "volcengine" in hostLower -> ProviderType.OPENAI
            else -> ProviderType.UNKNOWN
        }
    }

    // ===== 端点持久化 =====

    data class EndpointRecord(
        val url: String,
        val apiKey: String = "",
        val modelName: String = "",
        val detectedType: ProviderType = ProviderType.UNKNOWN,
        val successCount: Int = 0,
        val failCount: Int = 0,
        val lastUsed: Long = 0,
        val lastError: String? = null
    )

    fun saveEndpoint(endpoint: EndpointRecord) {
        val endpoints = getEndpoints().toMutableList()
        endpoints.removeAll { it.url == endpoint.url }
        endpoints.add(endpoint)
        prefs.edit().putString(KEY_ENDPOINTS, gson.toJson(endpoints)).apply()
    }

    fun getEndpoints(): List<EndpointRecord> {
        val json = prefs.getString(KEY_ENDPOINTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EndpointRecord>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getBestEndpoints(count: Int = 3): List<EndpointRecord> {
        return getEndpoints()
            .sortedWith(compareByDescending<EndpointRecord> { it.successCount }
                .thenBy { it.failCount })
            .take(count)
    }

    // ===== 工具方法 =====

    private fun extractHost(url: String): String {
        return try {
            val clean = url.removePrefix("http://").removePrefix("https://")
            clean.substringBefore("/").substringBefore(":")
        } catch (e: Exception) {
            url
        }
    }

    fun suggestProviderType(url: String): String {
        val type = detectProviderType(url)
        return if (type == ProviderType.UNKNOWN) {
            "无法自动识别，手动选择"
        } else {
            "识别为: ${type.name}"
        }
    }
}
