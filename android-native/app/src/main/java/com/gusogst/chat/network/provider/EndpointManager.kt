package com.gusogst.chat.network.provider

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Endpoint 端点管理器 — 移植自 main 分支 endpoint-manager.ts
 * 功能：端点评分、故障转移、自动降级
 */
class EndpointManager(context: Context) {

    companion object {
        private const val TAG = "EndpointManager"
        private const val PREFS_NAME = "endpoint_manager"
        private const val KEY_ENDPOINTS = "scored_endpoints"
        private const val MAX_FAIL_PENALTY = 10
        private val gson = Gson()
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class ScoredEndpoint(
        val url: String,
        val apiKey: String = "",
        val modelName: String = "",
        val successCount: Int = 0,
        val failCount: Int = 0,
        val totalLatencyMs: Long = 0,
        val lastUsed: Long = 0,
        val lastError: String? = null,
        val disabled: Boolean = false,
        val disabledUntil: Long = 0
    ) {
        val avgLatencyMs: Long
            get() = if (successCount > 0) totalLatencyMs / successCount else 99999

        val score: Double
            get() {
                if (disabled && System.currentTimeMillis() < disabledUntil) return -1.0
                val base = successCount.toDouble() * 10.0
                val penalty = failCount.toDouble() * MAX_FAIL_PENALTY
                val latencyPenalty = avgLatencyMs.toDouble() / 1000.0
                return base - penalty - latencyPenalty
            }

        val successRate: Double
            get() {
                val total = successCount + failCount
                return if (total > 0) successCount.toDouble() / total else 0.0
            }
    }

    // ===== 端点管理 =====

    fun getEndpoints(): List<ScoredEndpoint> {
        val json = prefs.getString(KEY_ENDPOINTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ScoredEndpoint>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveEndpoints(endpoints: List<ScoredEndpoint>) {
        prefs.edit().putString(KEY_ENDPOINTS, gson.toJson(endpoints)).apply()
    }

    fun addEndpoint(url: String, apiKey: String = "", modelName: String = "") {
        val endpoints = getEndpoints().toMutableList()
        if (endpoints.none { it.url == url }) {
            endpoints.add(ScoredEndpoint(url = url, apiKey = apiKey, modelName = modelName))
            saveEndpoints(endpoints)
            Log.i(TAG, "Added endpoint: $url")
        }
    }

    fun removeEndpoint(url: String) {
        val endpoints = getEndpoints().toMutableList()
        endpoints.removeAll { it.url == url }
        saveEndpoints(endpoints)
    }

    // ===== 评分与选择 =====

    /**
     * 获取最佳端点（按评分排序）
     * @param count 返回数量
     * @param providerType 可选：筛选特定 Provider 类型
     */
    fun getBestEndpoints(count: Int = 3): List<ScoredEndpoint> {
        return getEndpoints()
            .filter { !it.disabled || System.currentTimeMillis() >= it.disabledUntil }
            .sortedByDescending { it.score }
            .take(count)
    }

    /**
     * 获取下一个可用端点（故障转移）
     * 排除最近失败的端点
     */
    fun getNextEndpoint(excludeUrls: Set<String> = emptySet()): ScoredEndpoint? {
        return getEndpoints()
            .filter { !it.disabled || System.currentTimeMillis() >= it.disabledUntil }
            .filter { it.url !in excludeUrls }
            .maxByOrNull { it.score }
    }

    // ===== 成功/失败记录 =====

    fun recordSuccess(url: String, latencyMs: Long = 0) {
        val endpoints = getEndpoints().toMutableList()
        val idx = endpoints.indexOfFirst { it.url == url }
        if (idx >= 0) {
            val ep = endpoints[idx]
            endpoints[idx] = ep.copy(
                successCount = ep.successCount + 1,
                totalLatencyMs = ep.totalLatencyMs + latencyMs,
                lastUsed = System.currentTimeMillis(),
                lastError = null,
                disabled = false
            )
            saveEndpoints(endpoints)
        }
    }

    fun recordFailure(url: String, error: String) {
        val endpoints = getEndpoints().toMutableList()
        val idx = endpoints.indexOfFirst { it.url == url }
        if (idx >= 0) {
            val ep = endpoints[idx]
            val newFailCount = ep.failCount + 1
            // 连续失败 3 次，临时禁用 5 分钟
            val shouldDisable = newFailCount >= 3
            endpoints[idx] = ep.copy(
                failCount = newFailCount,
                lastUsed = System.currentTimeMillis(),
                lastError = error,
                disabled = shouldDisable,
                disabledUntil = if (shouldDisable) System.currentTimeMillis() + 5 * 60 * 1000 else 0
            )
            saveEndpoints(endpoints)
            if (shouldDisable) {
                Log.w(TAG, "Endpoint disabled for 5min: $url (failures: $newFailCount)")
            }
        }
    }

    // ===== 重置 =====

    fun resetScores() {
        val endpoints = getEndpoints().map {
            it.copy(successCount = 0, failCount = 0, totalLatencyMs = 0, disabled = false)
        }
        saveEndpoints(endpoints)
    }

    fun enableEndpoint(url: String) {
        val endpoints = getEndpoints().toMutableList()
        val idx = endpoints.indexOfFirst { it.url == url }
        if (idx >= 0) {
            endpoints[idx] = endpoints[idx].copy(disabled = false, disabledUntil = 0)
            saveEndpoints(endpoints)
        }
    }

    // ===== 统计 =====

    fun getStats(): Map<String, Any> {
        val endpoints = getEndpoints()
        return mapOf(
            "total" to endpoints.size,
            "active" to endpoints.count { !it.disabled || System.currentTimeMillis() >= it.disabledUntil },
            "disabled" to endpoints.count { it.disabled && System.currentTimeMillis() < it.disabledUntil },
            "bestScore" to (endpoints.maxOfOrNull { it.score } ?: 0.0),
            "avgSuccessRate" to (if (endpoints.isNotEmpty()) endpoints.map { it.successRate }.average() else 0.0)
        )
    }
}
