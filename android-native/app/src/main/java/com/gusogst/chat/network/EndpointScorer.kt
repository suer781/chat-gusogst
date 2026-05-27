package com.gusogst.chat.network

/**
 * 端点独立评分引擎。
 *
 * 每个端点（域名 + 路径）有一个独立的 EndpointScorer 实例，
 * 互不干扰，不存在「端点歧视」。
 *
 * 公式：
 *   k_m = 12 / (m + 15)
 *   C_m = clamp( C_{m-1} * (1 - k_m) + ΔC,  -10000, 10000 )
 *
 * 失败处理：
 *   - 前 3 次连续失败不计入错误计数（波动容忍）
 *   - 从第 4 次开始错误计数 +1，降权灵敏度递增
 *   - 成功一次后错误计数清零
 */
class EndpointScorer(
    val baseUrl: String,
    val apiKey: String
) {
    /** 调用次数 */
    private var callCount: Int = 0

    /** 当前评分 */
    private var score: Float = 0f

    /** 连续失败次数 */
    private var consecutiveFailures: Int = 0

    /** 超出容忍阈值后的错误计数（第 4 次起） */
    private var errorCount: Int = 0

    /** 容忍阈值：前 N 次失败不计入错误计数 */
    companion object {
        private const val TOLERANCE = 3
        private const val SCORE_MIN = -10000f
        private const val SCORE_MAX = 10000f
        private const val SUCCESS_DELTA = 500f
        private const val FAILURE_BASE_DELTA = -300f
    }

    // ================================================================
    //  公开 API
    // ================================================================

    /** 记录一次成功，返回新评分 */
    fun recordSuccess(): Float {
        callCount++
        consecutiveFailures = 0
        errorCount = 0
        score = compute(score, callCount, SUCCESS_DELTA)
        return score
    }

    /** 记录一次失败，返回新评分 */
    fun recordFailure(): Float {
        callCount++
        consecutiveFailures++

        if (consecutiveFailures > TOLERANCE) {
            errorCount++
        }

        // 失败 ΔC：基础值 + 错误计数放大
        val delta = FAILURE_BASE_DELTA - (errorCount * 150f)
        score = compute(score, callCount, delta)
        return score
    }

    /** 获取当前评分（只读） */
    fun getScore(): Float = score

    /** 获取连续失败次数 */
    fun getConsecutiveFailures(): Int = consecutiveFailures

    /** 获取错误计数 */
    fun getErrorCount(): Int = errorCount

    /** 获取调用次数 */
    fun getCallCount(): Int = callCount

    /** 判断端点是否健康（评分 > 0 且未超过容忍阈值，或误差计数 <= 0） */
    fun isHealthy(): Boolean {
        return score > 0f && (consecutiveFailures <= TOLERANCE || errorCount == 0)
    }

    /** 判断是否需要尝试切换到备用端点 */
    fun shouldTryFallback(): Boolean {
        return consecutiveFailures > TOLERANCE && score < 0f
    }

    /** 重置状态 */
    fun reset() {
        callCount = 0
        score = 0f
        consecutiveFailures = 0
        errorCount = 0
    }

    override fun toString(): String {
        return "EndpointScorer(url=$baseUrl, score=$score, calls=$callCount, " +
            "failures=$consecutiveFailures, errors=$errorCount)"
    }

    // ================================================================
    //  内部计算
    // ================================================================

    /**
     * 核心公式：
     *   k_m = 12 / (m + 15)
     *   C_m = clamp( C_{m-1} * (1 - k_m) + ΔC,  MIN, MAX )
     */
    private fun compute(prevScore: Float, m: Int, delta: Float): Float {
        val k = 12f / (m + 15f)
        val raw = prevScore * (1f - k) + delta
        return raw.coerceIn(SCORE_MIN, SCORE_MAX)
    }
}
