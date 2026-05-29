package com.gusogst.chat.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonParser

/**
 * Provider definitions - loaded from Web main branch's providers-registry.json
 * Contains 129 providers with 4774+ models total.
 */
data class ProviderDef(
    val id: String,
    val name: String,
    val env_key: List<String> = emptyList(),
    val base_url: String = "",
    val doc: String = "",
    val api: String = "",
    val models: List<ProviderModel> = emptyList(),
    val isLocal: Boolean = false
)

data class ProviderModel(
    val id: String,
    val name: String? = null,
    val context_length: Long = 0,
    val max_output: Long = 0,
    val cost_input: Double = 0.0,
    val cost_output: Double = 0.0
)

object ProviderRegistry {
    private var cached: List<ProviderDef>? = null

    fun load(context: Context): List<ProviderDef> {
        cached?.let { return it }

        return try {
            val stream = context.assets.open("providers-registry.json")
            val text = stream.bufferedReader().use { it.readText() }

            // Normalize env_key: JSON has both string and array formats
            // Parse as tree, fix env_key, then convert to typed objects
            val root = JsonParser.parseString(text).asJsonArray
            for (i in 0 until root.size()) {
                val obj = root[i].asJsonObject
                val ek = obj.get("env_key")
                if (ek != null && ek.isJsonPrimitive) {
                    val arr = com.google.gson.JsonArray()
                    arr.add(ek.asString)
                    obj.add("env_key", arr)
                }
            }
            val gson = Gson()
            val type = object : TypeToken<List<ProviderDef>>() {}.type
            val providers: List<ProviderDef> = gson.fromJson(root, type)
            if (providers.isEmpty()) throw RuntimeException("Empty provider list from JSON")
            cached = providers
            providers
        } catch (e: Exception) {
            android.util.Log.e("ProviderRegistry", "Failed to load providers-registry.json", e)
            val fallback = getFallbackProviders()
            cached = fallback
            fallback
        }
    }

    private fun getFallbackProviders(): List<ProviderDef> {
        return listOf(
            ProviderDef("openai", "OpenAI", base_url = "https://api.openai.com/v1",
                models = listOf(ProviderModel("gpt-4o"), ProviderModel("gpt-4o-mini"), ProviderModel("gpt-4-turbo"))),
            ProviderDef("anthropic", "Anthropic", base_url = "https://api.anthropic.com/v1",
                models = listOf(ProviderModel("claude-3-opus"), ProviderModel("claude-3-sonnet"), ProviderModel("claude-3-haiku"))),
            ProviderDef("deepseek", "DeepSeek", base_url = "https://api.deepseek.com/v1",
                models = listOf(ProviderModel("deepseek-chat"), ProviderModel("deepseek-coder"))),
            ProviderDef("zhipu", "ZhiPu", base_url = "https://open.bigmodel.cn/api/paas/v4",
                models = listOf(ProviderModel("glm-4"), ProviderModel("glm-3-turbo"))),
            ProviderDef("qwen", "Qwen", base_url = "https://dashscope.aliyuncs.com/api/v1",
                models = listOf(ProviderModel("qwen-max"), ProviderModel("qwen-plus"))),
            ProviderDef("nano-gpt", "NanoGPT", base_url = "https://api.nano-gpt.com/v1",
                models = listOf(ProviderModel("gpt-4o"), ProviderModel("claude-3-opus"))),
            ProviderDef("openrouter", "OpenRouter", base_url = "https://openrouter.ai/api/v1",
                models = listOf(ProviderModel("gpt-4o"), ProviderModel("claude-3-sonnet"))),
            ProviderDef("together", "Together", base_url = "https://api.together.xyz/v1",
                models = listOf(ProviderModel("llama-3-70b"), ProviderModel("mixtral-8x7b"))),
            ProviderDef("moonshot", "Moonshot", base_url = "https://api.moonshot.cn/v1",
                models = listOf(ProviderModel("moonshot-v1-128k"), ProviderModel("moonshot-v1-32k"))),
            ProviderDef("groq", "Groq", base_url = "https://api.groq.com/openai/v1",
                models = listOf(ProviderModel("llama3-70b"), ProviderModel("mixtral-8x7b"))),
            ProviderDef("ollama", "Ollama", base_url = "http://localhost:11434/v1", isLocal = true,
                models = listOf(ProviderModel("llama3"), ProviderModel("mistral"))),
            ProviderDef("google", "Google", base_url = "https://generativelanguage.googleapis.com/v1",
                models = listOf(ProviderModel("gemini-pro"))),
            ProviderDef("lmstudio", "LM Studio", base_url = "http://localhost:1234/v1", isLocal = true,
                models = listOf(ProviderModel("local-model"))),
            ProviderDef("custom", "\u81EA\u5B9A\u4E49", base_url = "")
        )
    }

    val PROVIDERS: List<ProviderDef>
        get() = cached ?: emptyList()

    fun getById(id: String): ProviderDef? = cached?.find { it.id == id }

    val RECOMMENDED_IDS = setOf("nano-gpt", "openai", "anthropic", "zhipu", "deepseek")

    val DOMESTIC_KEYWORDS = listOf(
        "zhipu","glm","qwen","wenxin","ernie","tongyi","doubao",
        "deepseek","tencent","kuae","step","hunyuan","minimax","moonshot","kimi"
    )

    val AGGREGATOR_KEYWORDS = listOf("nano","wafer","router","proxy","relay","openrouter")

    val EXACT_CATEGORY_MAP = mapOf(
        "nano-gpt" to "aggregator",
        "wafer" to "aggregator",
        "openrouter" to "aggregator",
        "kuae-cloud-coding-plan" to "domestic",
        "tencent-tokenhub" to "domestic",
        "xpersona" to "overseas",
        "abliteration-ai" to "overseas",
        "claudinio" to "overseas",
        "firepass" to "overseas"
    )

    fun classify(id: String): String {
        EXACT_CATEGORY_MAP[id]?.let { return it }
        val lower = id.lowercase()
        if (DOMESTIC_KEYWORDS.any { lower.contains(it) }) return "domestic"
        if (AGGREGATOR_KEYWORDS.any { lower.contains(it) }) return "aggregator"
        return "overseas"
    }
}
