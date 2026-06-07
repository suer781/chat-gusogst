package com.gusogst.chat.network

// ═══════════════════════════════════════════════
// Endpoint 管理器 (完整版)
// 移植自 main 分支 providers/endpoint-manager.ts
// 合并原 EndpointKB + EndpointScorer
// ═══════════════════════════════════════════════

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.gusogst.chat.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class EndpointManager(private val context: Context) {
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("endpoint_ratings", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // 评分缓存
    private val ratings = mutableMapOf<String, EndpointRating>()
    private val chatCounts = mutableMapOf<String, Int>()

    init {
        loadPersistedData()
    }

    // ═══════════════════════════════════════════
    // 核心：选择最佳端点
    // ═══════════════════════════════════════════

    suspend fun selectEndpoint(
        userInput: String,
        apiKey: String = "",
        customEndpoint: String? = null,
        customProviderId: String? = null
    ): EndpointSelection? = withContext(Dispatchers.IO) {

        // 1. 如果用户直接指定了端点
        if (customEndpoint != null) {
            return@withContext EndpointSelection(
                endpoint = customEndpoint,
                apiKey = apiKey,
                providerId = customProviderId ?: "custom"
            )
        }

        // 2. 匹配供应商
        val providerId = matchProvider(userInput) ?: return@withContext null
        val provider = PROVIDERS[providerId] ?: return@withContext null

        // 3. 构建候选端点列表，按评分降序
        val candidates = buildCandidateList(providerId, provider)

        // 4. 逐个尝试，找到可用的
        for (endpoint in candidates) {
            val testResult = testEndpoint(endpoint, apiKey)
            if (testResult.success) {
                recordSuccess(endpoint, providerId)
                return@withContext EndpointSelection(
                    endpoint = endpoint,
                    apiKey = apiKey,
                    providerId = providerId
                )
            } else if (testResult.isNetworkError) {
                // 网络不通，直接返回 null
                return@withContext null
            }
            recordFailure(endpoint, providerId)
        }

        null
    }

    // ═══════════════════════════════════════════
    // 供应商匹配 (简化版 smartMatch)
    // ═══════════════════════════════════════════

    private fun matchProvider(input: String): String? {
        val lower = input.lowercase().trim()

        // 1. 精确 ID 匹配
        if (PROVIDERS.containsKey(lower)) return lower

        // 2. 精确名称匹配
        for ((id, provider) in PROVIDERS) {
            if (provider.name.lowercase() == lower) return id
            if (provider.aliases.any { it.lowercase() == lower }) return id
        }

        // 3. 包含匹配
        for ((id, provider) in PROVIDERS) {
            if (lower.contains(id) || lower.contains(provider.name.lowercase())) return id
            if (provider.aliases.any { lower.contains(it.lowercase()) }) return id
        }

        // 4. URL 匹配
        if (lower.startsWith("http")) {
            for ((id, provider) in PROVIDERS) {
                if (provider.endpoints.any { lower.contains(it.url) }) return id
            }
        }

        return null
    }

    // ═══════════════════════════════════════════
    // 端点评分系统
    // ═══════════════════════════════════════════

    // 评分公式：C_m+1 = C_m + k_m * ΔR
    // k_m = 12 / (m + 15) — 学习率递减
    // 成功: ΔR = +100 (首次 +500)
    // 失败: ΔR = -200 (连续错误放大)

    private fun recordSuccess(endpoint: String, providerId: String) {
        val key = "${providerId}:${endpoint}"
        val rating = ratings.getOrPut(key) { EndpointRating() }
        val chatCount = chatCounts.getOrPut(key) { 0 }
        val learningRate = 12.0 / (rating.sampleCount + 15)
        val delta = if (rating.sampleCount == 0) 500.0 else 100.0

        rating.cumulativeScore += learningRate * delta
        rating.sampleCount++
        rating.chatCount = chatCount + 1
        rating.lastChatAt = System.currentTimeMillis()
        chatCounts[key] = rating.chatCount

        persistData()
    }

    private fun recordFailure(endpoint: String, providerId: String) {
        val key = "${providerId}:${endpoint}"
        val rating = ratings.getOrPut(key) { EndpointRating() }
        val learningRate = 12.0 / (rating.sampleCount + 15)
        val consecutiveErrors = getConsecutiveErrors(key)
        val delta = -200.0 * (1 + consecutiveErrors * 0.5)

        rating.cumulativeScore += learningRate * delta
        rating.cumulativeScore = max(rating.cumulativeScore, -1000.0)
        rating.sampleCount++

        incrementConsecutiveErrors(key)
        persistData()
    }

    private val consecutiveErrors = mutableMapOf<String, Int>()

    private fun getConsecutiveErrors(key: String): Int = consecutiveErrors.getOrDefault(key, 0)
    private fun incrementConsecutiveErrors(key: String) {
        consecutiveErrors[key] = getConsecutiveErrors(key) + 1
    }
    private fun resetConsecutiveErrors(key: String) {
        consecutiveErrors[key] = 0
    }

    // ═══════════════════════════════════════════
    // 端点测试 (搭便车测试)
    // ═══════════════════════════════════════════

    data class TestResult(val success: Boolean, val isNetworkError: Boolean = false)

    private suspend fun testEndpoint(endpoint: String, apiKey: String): TestResult {
        return try {
            val body = JsonObject().apply {
                addProperty("model", "gpt-3.5-turbo")
                addProperty("messages", "[]")
                addProperty("max_tokens", 1)
            }

            val request = Request.Builder()
                .url("${endpoint}/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(body.toString().toRequestBody(JSON))
                .build()

            val response = client.newCall(request).execute()
            response.close()
            TestResult(success = response.isSuccessful)
        } catch (e: java.net.UnknownHostException) {
            TestResult(success = false, isNetworkError = true)
        } catch (e: java.net.ConnectException) {
            TestResult(success = false, isNetworkError = true)
        } catch (e: Exception) {
            TestResult(success = false, isNetworkError = false)
        }
    }

    // ═══════════════════════════════════════════
    // 候选列表构建 (按评分降序)
    // ═══════════════════════════════════════════

    private fun buildCandidateList(providerId: String, provider: ProviderInfo): List<String> {
        return provider.endpoints
            .sortedByDescending { ep ->
                val key = "${providerId}:${ep.url}"
                ratings[key]?.quality() ?: 0.5
            }
            .map { it.url }
    }

    // ═══════════════════════════════════════════
    // 持久化
    // ═══════════════════════════════════════════

    private fun persistData() {
        prefs.edit()
            .putString("ratings", gson.toJson(ratings))
            .putString("chat_counts", gson.toJson(chatCounts))
            .apply()
    }

    private fun loadPersistedData() {
        try {
            prefs.getString("ratings", null)?.let {
                val type = object : TypeToken<Map<String, EndpointRating>>() {}.type
                ratings.putAll(gson.fromJson(it, type))
            }
            prefs.getString("chat_counts", null)?.let {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                chatCounts.putAll(gson.fromJson(it, type))
            }
        } catch (e: Exception) {
            // 解析失败，使用空数据
        }
    }

    // ═══════════════════════════════════════════
    // 供应商数据 (静态注册表)
    // ═══════════════════════════════════════════

    data class ProviderInfo(
        val id: String,
        val name: String,
        val endpoints: List<EndpointInfo>,
        val aliases: List<String> = emptyList(),
        val models: List<String> = emptyList()
    )

    data class EndpointInfo(
        val url: String,
        val name: String = "",
        val needsApiKey: Boolean = true
    )

    companion object {
        val PROVIDERS = mapOf(
            "openai" to ProviderInfo(
                id = "openai",
                name = "OpenAI",
                endpoints = listOf(
                    EndpointInfo("https://api.openai.com", "OpenAI Official")
                ),
                aliases = listOf("chatgpt"),
                models = listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "o1-preview", "o1-mini")
            ),
            "anthropic" to ProviderInfo(
                id = "anthropic",
                name = "Anthropic",
                endpoints = listOf(
                    EndpointInfo("https://api.anthropic.com", "Anthropic Official")
                ),
                aliases = listOf("claude"),
                models = listOf("claude-sonnet-4-20250514", "claude-3-5-haiku-20241022", "claude-3-opus-20240229")
            ),
            "deepseek" to ProviderInfo(
                id = "deepseek",
                name = "DeepSeek",
                endpoints = listOf(
                    EndpointInfo("https://api.deepseek.com", "DeepSeek Official"),
                    EndpointInfo("https://api.deepseek.com/v1", "DeepSeek v1")
                ),
                aliases = listOf("ds"),
                models = listOf("deepseek-chat", "deepseek-coder", "deepseek-reasoner")
            ),
            "openrouter" to ProviderInfo(
                id = "openrouter",
                name = "OpenRouter",
                endpoints = listOf(
                    EndpointInfo("https://openrouter.ai/api", "OpenRouter Official")
                ),
                aliases = listOf("or"),
                models = listOf("anthropic/claude-sonnet-4", "google/gemini-2.5-pro")
            ),
            "moonshot" to ProviderInfo(
                id = "moonshot",
                name = "Moonshot",
                endpoints = listOf(
                    EndpointInfo("https://api.moonshot.cn", "Moonshot Official")
                ),
                aliases = listOf("kimi", "月之暗面"),
                models = listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
            ),
            "zhipu" to ProviderInfo(
                id = "zhipu",
                name = "Zhipu AI",
                endpoints = listOf(
                    EndpointInfo("https://open.bigmodel.cn/api/paas/v4", "Zhipu Official")
                ),
                aliases = listOf("glm", "智谱", "chatglm"),
                models = listOf("glm-4", "glm-4-flash", "glm-3-turbo")
            ),
            "qwen" to ProviderInfo(
                id = "qwen",
                name = "Alibaba Qwen",
                endpoints = listOf(
                    EndpointInfo("https://dashscope.aliyuncs.com/compatible-mode/v1", "DashScope")
                ),
                aliases = listOf("通义", "千问", "ali", "alibaba"),
                models = listOf("qwen-max", "qwen-plus", "qwen-turbo")
            ),
            "siliconflow" to ProviderInfo(
                id = "siliconflow",
                name = "SiliconFlow",
                endpoints = listOf(
                    EndpointInfo("https://api.siliconflow.cn/v1", "SiliconFlow Official")
                ),
                aliases = listOf("硅基", "sf"),
                models = listOf("deepseek-ai/DeepSeek-V3", "Qwen/Qwen2.5-72B-Instruct")
            ),
            "groq" to ProviderInfo(
                id = "groq",
                name = "Groq",
                endpoints = listOf(
                    EndpointInfo("https://api.groq.com/openai/v1", "Groq Official")
                ),
                aliases = listOf(),
                models = listOf("llama-3.1-70b-versatile", "mixtral-8x7b-32768")
            ),
            "together" to ProviderInfo(
                id = "together",
                name = "Together AI",
                endpoints = listOf(
                    EndpointInfo("https://api.together.xyz/v1", "Together Official")
                ),
                aliases = listOf("ta"),
                models = listOf("meta-llama/Meta-Llama-3.1-405B-Instruct-Turbo")
            ),
            "mistral" to ProviderInfo(
                id = "mistral",
                name = "Mistral AI",
                endpoints = listOf(
                    EndpointInfo("https://api.mistral.ai/v1", "Mistral Official")
                ),
                aliases = listOf(),
                models = listOf("mistral-large-latest", "mistral-small-latest")
            ),
            "volcengine" to ProviderInfo(
                id = "volcengine",
                name = "Volcengine",
                endpoints = listOf(
                    EndpointInfo("https://ark.cn-beijing.volces.com/api/v3", "Volcengine Official")
                ),
                aliases = listOf("火山", "doubao", "豆包"),
                models = listOf("doubao-pro-4k", "doubao-pro-32k", "doubao-lite-4k")
            ),
            "baidu" to ProviderInfo(
                id = "baidu",
                name = "Baidu AI",
                endpoints = listOf(
                    EndpointInfo("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop", "Baidu Official")
                ),
                aliases = listOf("文心", "ernie", "百度"),
                models = listOf("ernie-4.0-8k", "ernie-3.5-8k")
            )
        )
    }
}