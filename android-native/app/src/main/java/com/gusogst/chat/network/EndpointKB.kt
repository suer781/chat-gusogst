package com.gusogst.chat.network

/**
 * 本地端点知识库。
 *
 * 内置常见域名的 API 路径映射，供 RAG 检索使用。
 * 当用户只填域名没填路径时，从此知识库推断最可能的端点路径。
 *
 * 搜索策略：
 *   1. 精确域名匹配
 *   2. 域名后缀匹配（如 "deepseek.com" 匹配 "*.deepseek.com"）
 *   3. 标签关键词相似度（按共有标签数量排序）
 *   4. 格式类 fallback（如所有 openai 兼容格式）
 *
 * 纯本地运行，不依赖外部服务。
 */
object EndpointKB {

    data class KbEntry(
        /** 域名模式，如 "deepseek.com", "*.aliyuncs.com" */
        val domain: String,
        /** 推荐路径模板 */
        val path: String,
        /** 格式类型：openai / anthropic / google / custom */
        val format: String,
        /** 语义标签，用于 RAG 检索 */
        val tags: List<String>,
        /** 该端点是否已知确定，false 表示是推测 */
        val confirmed: Boolean = true
    )

    /**
     * 内置知识库。
     * domain 使用小写，匹配时忽略大小写。
     */
    private val ENTRIES = listOf(
        // === OpenAI 兼容格式（/v1/chat/completions）===
        KbEntry("openai.com", "/v1/chat/completions", "openai",
            listOf("openai", "gpt", "chatgpt", "completions", "chat")),
        KbEntry("api.openai.com", "/v1/chat/completions", "openai",
            listOf("openai", "gpt", "chatgpt", "completions", "chat")),
        KbEntry("deepseek.com", "/v1/chat/completions", "openai",
            listOf("deepseek", "openai-compatible", "completions", "chat")),
        KbEntry("api.deepseek.com", "/v1/chat/completions", "openai",
            listOf("deepseek", "openai-compatible", "completions", "chat")),
        KbEntry("groq.com", "/openai/v1/chat/completions", "openai",
            listOf("groq", "openai-compatible", "completions", "llama")),
        KbEntry("api.groq.com", "/openai/v1/chat/completions", "openai",
            listOf("groq", "openai-compatible", "completions", "llama")),
        KbEntry("moonshot.cn", "/v1/chat/completions", "openai",
            listOf("moonshot", "openai-compatible", "completions", "chat")),
        KbEntry("api.moonshot.cn", "/v1/chat/completions", "openai",
            listOf("moonshot", "openai-compatible", "completions", "chat")),
        KbEntry("aliyuncs.com", "/api/v1/chat/completions", "openai",
            listOf("qwen", "aliyun", "dashscope", "openai-compatible", "completions")),
        KbEntry("dashscope.aliyuncs.com", "/api/v1/chat/completions", "openai",
            listOf("qwen", "aliyun", "dashscope", "openai-compatible", "completions")),
        KbEntry("volcengine.com", "/v1/chat/completions", "openai",
            listOf("volcengine", "ark", "doubao", "openai-compatible", "completions")),
        KbEntry("api.volcengine.com", "/v1/chat/completions", "openai",
            listOf("volcengine", "ark", "doubao", "openai-compatible", "completions")),
        KbEntry("ark.volcengine.com", "/api/v3/chat/completions", "openai",
            listOf("volcengine", "ark", "doubao", "openai-compatible", "completions")),
        KbEntry("together.xyz", "/v1/chat/completions", "openai",
            listOf("together", "openai-compatible", "completions")),
        KbEntry("api.together.xyz", "/v1/chat/completions", "openai",
            listOf("together", "openai-compatible", "completions")),
        KbEntry("openrouter.ai", "/api/v1/chat/completions", "openai",
            listOf("openrouter", "openai-compatible", "completions", "aggregator")),
        KbEntry("api.openrouter.ai", "/api/v1/chat/completions", "openai",
            listOf("openrouter", "openai-compatible", "completions", "aggregator")),
        KbEntry("localhost", "/v1/chat/completions", "openai",
            listOf("local", "ollama", "lm-studio", "openai-compatible", "completions"),
            confirmed = false),
        KbEntry("127.0.0.1", "/v1/chat/completions", "openai",
            listOf("local", "ollama", "lm-studio", "openai-compatible", "completions"),
            confirmed = false),

        // === Anthropic 格式（/v1/messages）===
        KbEntry("anthropic.com", "/v1/messages", "anthropic",
            listOf("anthropic", "claude", "messages")),
        KbEntry("api.anthropic.com", "/v1/messages", "anthropic",
            listOf("anthropic", "claude", "messages")),

        // === Google 格式 ===
        KbEntry("googleapis.com", "/v1/models/gemini-pro:generateContent", "google",
            listOf("google", "gemini", "generative")),
        KbEntry("generativelanguage.googleapis.com", "/v1/models/gemini-pro:generateContent", "google",
            listOf("google", "gemini", "generative")),

        // === 智谱 ===
        KbEntry("bigmodel.cn", "/api/paas/v4/chat/completions", "openai",
            listOf("zhipu", "glm", "bigmodel", "openai-compatible", "completions")),
        KbEntry("open.bigmodel.cn", "/api/paas/v4/chat/completions", "openai",
            listOf("zhipu", "glm", "bigmodel", "openai-compatible", "completions")),

        // === 百度 ===
        KbEntry("baidu.com", "/rpc/2.0/ai_custom/v1/wenxinworkspace/chat/completions", "baidu",
            listOf("baidu", "wenxin", "ernie", "completions")),
        KbEntry("qianfan.baidubce.com", "/v2/chat/completions", "openai",
            listOf("baidu", "qianfan", "openai-compatible", "completions")),

        // === 硅基流动 ===
        KbEntry("siliconflow.cn", "/v1/chat/completions", "openai",
            listOf("siliconflow", "openai-compatible", "completions")),
        KbEntry("api.siliconflow.cn", "/v1/chat/completions", "openai",
            listOf("siliconflow", "openai-compatible", "completions")),
    )

    // ================================================================
    //  检索 API
    // ================================================================

    /**
     * 根据域名推断最佳路径。
     * 返回 (路径, 置信度, 匹配条目)，找不到返回 null。
     */
    fun inferPath(domain: String): Triple<String, Float, KbEntry>? {
        val lower = domain.lowercase().trim()

        // 策略 1：精确匹配
        ENTRIES.firstOrNull { it.domain == lower }?.let {
            return Triple(it.path, 1.0f, it)
        }

        // 策略 2：域名后缀匹配（如 deepseek.com 匹配 api.deepseek.com）
        ENTRIES.firstOrNull { lower.endsWith("." + it.domain) }?.let {
            return Triple(it.path, 0.95f, it)
        }

        // 策略 3：域名主体匹配（去掉 www. / api. 前缀后匹配）
        val stripped = lower.removePrefix("www.").removePrefix("api.")
        ENTRIES.firstOrNull { it.domain == stripped || stripped.endsWith("." + it.domain) }?.let {
            return Triple(it.path, 0.9f, it)
        }

        // 策略 4：标签匹配——找共有标签最多的
        val domainKeywords = extractKeywords(lower)
        if (domainKeywords.isNotEmpty()) {
            val scored = ENTRIES.map { entry ->
                val overlap = entry.tags.count { it in domainKeywords }
                entry to overlap
            }.filter { it.second > 0 }
                .sortedByDescending { it.second }

            if (scored.isNotEmpty()) {
                val (best, score) = scored.first()
                val confidence = (score.toFloat() / maxOf(domainKeywords.size, 1)).coerceAtMost(0.85f)
                return Triple(best.path, confidence, best)
            }
        }

        // 策略 5：完全没匹配——返回最通用的 openai 格式
        val fallback = ENTRIES.first { it.domain == "api.openai.com" }
        return Triple(fallback.path, 0.3f, fallback)
    }

    /**
     * 根据格式类型查找可替代端点路径。
     * 用于故障转移：主端点挂了，找同格式的其他候选。
     */
    fun findAlternatives(format: String, excludeDomain: String? = null): List<KbEntry> {
        return ENTRIES
            .filter { it.format == format && (excludeDomain == null || it.domain != excludeDomain) }
            .sortedByDescending { if (it.confirmed) 1 else 0 }
    }

    /**
     * 语义搜索：按标签查找最接近的条目。
     * 用于 RAG：找「提供 DeepSeek 模型的端点」之类的场景。
     */
    fun searchByTags(queryTags: List<String>, topK: Int = 3): List<Pair<KbEntry, Float>> {
        val scored = ENTRIES.map { entry ->
            val overlap = queryTags.count { t -> entry.tags.any { it.contains(t, ignoreCase = true) } }
            entry to (if (queryTags.isEmpty()) 0f else overlap.toFloat() / queryTags.size)
        }.filter { it.second > 0 }
            .sortedByDescending { it.second }

        return scored.take(topK)
    }

    /** 列出所有已知的格式类型 */
    fun listFormats(): Set<String> = ENTRIES.map { it.format }.toSet()

    /** 列出所有已知域名 */
    fun listDomains(): List<String> = ENTRIES.map { it.domain }

    // ================================================================
    //  辅助
    // ================================================================

    /** 从域名中提取关键词用于标签匹配 */
    private fun extractKeywords(domain: String): Set<String> {
        val parts = domain
            .replace("www.", "")
            .replace("api.", "")
            .split(".")
        return parts.toSet()
    }
}
