package com.gusogst.chat.network

import com.gusogst.chat.model.UIProvider

/**
 * 故障转移引擎。
 *
 * 当主端点请求失败时：
 *   1. 记录失败并更新该端点的独立评分
 *   2. 如果连续失败超过容忍阈值，在用户供应商列表中寻找格式最接近的替代
 *   3. 自动用替代端点的 Key + 路径重试
 *   4. 成功者加分，失败者继续降权
 */
class AutoRetryEngine {

    /** 每个端点的独立评分器 */
    private val scorers = mutableMapOf<String, EndpointScorer>()

    /**
     * 获取或创建端点评分器。
     * key = baseUrl + "|" + apiKey 的前 8 位（区分不同 Key 的同一端点）
     */
    fun getOrCreateScorer(baseUrl: String, apiKey: String): EndpointScorer {
        val key = makeKey(baseUrl, apiKey)
        return scorers.getOrPut(key) { EndpointScorer(baseUrl, apiKey) }
    }

    /**
     * 记录请求成功。
     * @return 更新后的评分
     */
    fun recordSuccess(baseUrl: String, apiKey: String): Float {
        val scorer = getOrCreateScorer(baseUrl, apiKey)
        return scorer.recordSuccess()
    }

    /**
     * 记录请求失败。
     * @return 更新后的评分
     */
    fun recordFailure(baseUrl: String, apiKey: String): Float {
        val scorer = getOrCreateScorer(baseUrl, apiKey)
        return scorer.recordFailure()
    }

    /**
     * 判断是否需要尝试切换。
     */
    fun shouldTryFallback(baseUrl: String, apiKey: String): Boolean {
        val scorer = getOrCreateScorer(baseUrl, apiKey)
        return scorer.shouldTryFallback()
    }

    /**
     * 在用户的供应商列表中查找替代端点。
     *
     * @param failedUrl 失败的完整 baseUrl
     * @param providers 用户配置的供应商列表
     * @return 候选供应商列表，按评分排序
     */
    fun findFallbacks(failedUrl: String, providers: List<UIProvider>): List<FallbackCandidate> {
        // 1. 从失败 URL 推测格式类型
        val domain = extractDomain(failedUrl)
        val kbResult = EndpointKB.inferPath(domain)
        if (kbResult == null) return emptyList()

        val (_, _, matchedEntry) = kbResult
        val format = matchedEntry.format

        // 2. 找同格式的其他端点
        val alternatives = EndpointKB.findAlternatives(format, excludeDomain = matchedEntry.domain)

        // 3. 在用户配置的供应商中匹配
        val candidates = mutableListOf<FallbackCandidate>()

        for (alt in alternatives) {
            for (provider in providers) {
                if (!provider.enabled) continue
                val providerDomain = extractDomain(provider.baseUrl)
                if (providerDomain.contains(alt.domain.removePrefix("api."), ignoreCase = true) ||
                    alt.domain.contains(providerDomain, ignoreCase = true)
                ) {
                    val scorer = getOrCreateScorer(provider.baseUrl, provider.apiKey)
                    if (scorer.isHealthy()) {
                        candidates.add(
                            FallbackCandidate(
                                provider = provider,
                                kbEntry = alt,
                                score = scorer.getScore(),
                                confidence = kbResult.second
                            )
                        )
                    }
                }
            }
        }

        // 4. 按评分降序排列（健康的优先）
        return candidates.sortedByDescending { it.score }
    }

    /**
     * 尝试一次重试。
     * 返回 (是否成功, 使用的供应商, 错误消息)
     */
    suspend fun tryFallback(
        failedUrl: String,
        providers: List<UIProvider>,
        requester: suspend (UIProvider, EndpointKB.KbEntry) -> Result<Unit>
    ): FallbackResult {
        val candidates = findFallbacks(failedUrl, providers)
        if (candidates.isEmpty()) {
            return FallbackResult.NoCandidates
        }

        for (candidate in candidates) {
            try {
                val result = requester(candidate.provider, candidate.kbEntry)
                if (result.isSuccess) {
                    recordSuccess(candidate.provider.baseUrl, candidate.provider.apiKey)
                    return FallbackResult.Success(candidate.provider, candidate.kbEntry)
                } else {
                    recordFailure(candidate.provider.baseUrl, candidate.provider.apiKey)
                }
            } catch (e: Exception) {
                recordFailure(candidate.provider.baseUrl, candidate.provider.apiKey)
            }
        }

        return FallbackResult.AllFailed
    }

    /** 获取所有端点的评分快照 */
    fun getScoreSnapshot(): Map<String, Float> {
        return scorers.mapValues { it.value.getScore() }
    }

    // ================================================================
    //  辅助
    // ================================================================

    private fun makeKey(baseUrl: String, apiKey: String): String {
        val keyPrefix = if (apiKey.length > 8) apiKey.take(8) else apiKey
        return "$baseUrl|$keyPrefix"
    }

    /**
     * 从 baseUrl 中提取域名。
     * 如 "https://api.deepseek.com/v1" → "api.deepseek.com"
     */
    private fun extractDomain(baseUrl: String): String {
        return try {
            val cleaned = baseUrl.trimEnd('/')
            val start = if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
                cleaned.indexOf("://") + 3
            } else 0
            val end = cleaned.indexOf('/', start).let { if (it == -1) cleaned.length else it }
            cleaned.substring(start, end)
        } catch (_: Exception) {
            baseUrl
        }
    }
}

// ================================================================
//  数据类
// ================================================================

/** 故障转移候选 */
data class FallbackCandidate(
    val provider: UIProvider,
    val kbEntry: EndpointKB.KbEntry,
    val score: Float,
    val confidence: Float
)

/** 故障转移结果 */
sealed class FallbackResult {
    data class Success(val provider: UIProvider, val kbEntry: EndpointKB.KbEntry) : FallbackResult()
    data object NoCandidates : FallbackResult()
    data object AllFailed : FallbackResult()
}