package com.gusogst.chat.data

import android.content.Context
import com.google.gson.JsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * Provider definitions loaded from Web main branch's providers-registry.json.
 * Uses manual JsonObject parsing (not Gson type binding) to avoid field format issues.
 */
data class ProviderDef(
    val id: String,
    val name: String,
    val baseUrl: String = "",
    val doc: String = "",
    val models: List<String> = emptyList() // model IDs
)

object ProviderRegistry {
    private var cached: List<ProviderDef>? = null

    fun load(context: Context): List<ProviderDef> {
        cached?.let { return it }

        val providers = mutableListOf<ProviderDef>()
        try {
            val stream = context.assets.open("providers-registry.json")
            val text = stream.bufferedReader().use { it.readText() }
            val arr = JsonParser.parseString(text).asJsonArray

            for (i in 0 until arr.size()) {
                try {
                    val obj = arr[i].asJsonObject
                    val id = obj.get("id")?.asString ?: continue
                    val name = obj.get("name")?.asString ?: id
                    val baseUrl = obj.get("base_url")?.asString ?: ""
                    val doc = obj.get("doc")?.asString ?: ""
                    val models = mutableListOf<String>()

                    val modelsArr = obj.getAsJsonArray("models")
                    if (modelsArr != null) {
                        for (j in 0 until modelsArr.size()) {
                            try {
                                val m = modelsArr[j].asJsonObject
                                val mid = m.get("id")?.asString
                                if (mid != null) models.add(mid)
                            } catch (_: Exception) {}
                        }
                    }

                    providers.add(ProviderDef(id, name, baseUrl, doc, models))
                } catch (_: Exception) {}
            }

            if (providers.isEmpty()) throw RuntimeException("No providers parsed")

            cached = providers
            return providers
        } catch (e: Exception) {
            android.util.Log.e("ProviderRegistry", "Failed to load providers", e)
            val fallback = getFallback()
            cached = fallback
            return fallback
        }
    }

    private fun getFallback(): List<ProviderDef> = listOf(
        ProviderDef("openai", "OpenAI", "https://api.openai.com/v1", models = listOf("gpt-4o","gpt-4o-mini")),
        ProviderDef("anthropic", "Anthropic", "https://api.anthropic.com/v1", models = listOf("claude-3-opus","claude-3-sonnet")),
        ProviderDef("deepseek", "DeepSeek", "https://api.deepseek.com/v1", models = listOf("deepseek-chat","deepseek-coder")),
        ProviderDef("zhipu", "ZhiPu", "https://open.bigmodel.cn/api/paas/v4", models = listOf("glm-4","glm-3-turbo")),
        ProviderDef("qwen", "Qwen", "https://dashscope.aliyuncs.com/api/v1", models = listOf("qwen-max","qwen-plus")),
        ProviderDef("nano-gpt", "NanoGPT", models = listOf("gpt-4o","claude-3-opus")),
        ProviderDef("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", models = listOf("gpt-4o","claude-3-sonnet")),
        ProviderDef("together", "Together", "https://api.together.xyz/v1", models = listOf("llama-3-70b","mixtral-8x7b")),
        ProviderDef("moonshot", "Moonshot", "https://api.moonshot.cn/v1", models = listOf("moonshot-v1-128k")),
        ProviderDef("groq", "Groq", "https://api.groq.com/openai/v1", models = listOf("llama3-70b")),
        ProviderDef("ollama", "Ollama", "http://localhost:11434/v1", models = listOf("llama3")),  // local-only, no HTTPS
        ProviderDef("google", "Google", "https://generativelanguage.googleapis.com/v1", models = listOf("gemini-pro")),
        ProviderDef("lmstudio", "LM Studio", "http://localhost:1234/v1"),  // local-only, no HTTPS
        ProviderDef("custom", "自定义", "https://your-api-endpoint.com/v1")  // 占位 URL，用户需自行替换
    )

    val PROVIDERS: List<ProviderDef> get() = cached ?: emptyList()
    fun getById(id: String) = cached?.find { it.id == id }
}

// Classification logic (aligned with Web main branch)
object ProviderClassifier {
    val RECOMMENDED = setOf("nano-gpt", "openai", "anthropic", "zhipu", "deepseek")

    private val DOMESTIC_KW = listOf("zhipu","glm","qwen","wenxin","ernie","tongyi","doubao","deepseek","tencent","kuae","step","hunyuan","minimax","moonshot","kimi")
    private val AGGREGATOR_KW = listOf("nano","wafer","router","proxy","relay","openrouter")

    private val EXACT = mapOf(
        "nano-gpt" to "aggregator", "openrouter" to "aggregator",
        "kuae-cloud-coding-plan" to "domestic", "tencent-tokenhub" to "domestic",
        "xpersona" to "overseas", "abliteration-ai" to "overseas",
        "claudinio" to "overseas", "firepass" to "overseas"
    )

    fun classify(id: String): String {
        EXACT[id]?.let { return it }
        val lower = id.lowercase()
        if (DOMESTIC_KW.any { lower.contains(it) }) return "domestic"
        if (AGGREGATOR_KW.any { lower.contains(it) }) return "aggregator"
        return "overseas"
    }
}
