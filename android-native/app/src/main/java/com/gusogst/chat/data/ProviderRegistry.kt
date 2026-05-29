package com.gusogst.chat.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

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
    val models: List<ProviderModel> = emptyList()
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
            val reader = InputStreamReader(stream)
            val type = object : TypeToken<List<ProviderDef>>() {}.type
            val providers: List<ProviderDef> = Gson().fromJson(reader, type)
            reader.close()
            cached = providers
            providers
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    val PROVIDERS: List<ProviderDef>
        get() = cached ?: emptyList()

    fun getById(id: String): ProviderDef? = cached?.find { it.id == id }

    /**
     * 获取推荐供应商 ID 列表（对齐 Web 主分支）
     */
    val RECOMMENDED_IDS = setOf("nano-gpt", "openai", "anthropic", "zhipu", "deepseek")

    /**
     * 国产关键词（用于模糊匹配分类）
     */
    val DOMESTIC_KEYWORDS = listOf(
        "zhipu","glm","qwen","wenxin","ernie","tongyi","doubao",
        "deepseek","tencent","kuae","step","hunyuan","minimax","moonshot","kimi"
    )

    /**
     * 聚合关键词（用于模糊匹配分类）
     */
    val AGGREGATOR_KEYWORDS = listOf("nano","wafer","router","proxy","relay","openrouter")

    /**
     * 精确分类映射（覆盖关键词匹配）
     */
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

    /**
     * 分类函数（对齐 Web 主分支）
     */
    fun classify(id: String): String {
        EXACT_CATEGORY_MAP[id]?.let { return it }
        val lower = id.lowercase()
        if (DOMESTIC_KEYWORDS.any { lower.contains(it) }) return "domestic"
        if (AGGREGATOR_KEYWORDS.any { lower.contains(it) }) return "aggregator"
        return "overseas"
    }
}
