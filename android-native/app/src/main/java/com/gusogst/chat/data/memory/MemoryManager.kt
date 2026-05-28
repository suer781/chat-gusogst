package com.gusogst.chat.data.memory

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 记忆管理器（混合检索：向量 + 关键词）
 * 与 main 分支 manager.ts 完全对齐
 */
class MemoryManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "memory_store"
        private const val KEY_ENTRIES = "entries"
        private const val MAX_MEMORIES = 200
        private const val CONTEXT_LIMIT = 15
        private const val VECTOR_WEIGHT = 0.5
        private const val KEYWORD_WEIGHT = 0.5
        private const val AUTO_EXTRACT_THRESHOLD = 50
        private const val MEMORY_TYPES_JSON = """[
            "preference", "fact", "habit", "plan", "emotion",
            "relationship", "opinion", "goal", "experience"
        ]"""
        private const val EXTRACT_PROMPT = """从对话中提取值得记住的信息，JSON格式返回：
\{\"type\": "xxx", "content": "xxx", "confidence": 0.0-1.0\}
type: preference/fact/habit/plan/emotion/relationship/opinion/goal/experience
无需记忆返回空数组[]。每次最多提取3条。"""
    }

    data class MemoryEntry(
        val id: String,
        val type: String,
        val content: String,
        val timestamp: Long,
        var importance: Double = 0.5,
        val context: String? = null,
        val source: String = "conversation",
        var accessCount: Int = 0
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    val vectorStore = VectorStore(context)

    private var entries: MutableMap<String, MemoryEntry> = mutableMapOf()
    private var loaded = false

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        val json = prefs.getString(KEY_ENTRIES, null)
        if (json != null) {
            val type = object : TypeToken<Map<String, MemoryEntry>>() {}.type
            entries = gson.fromJson<Map<String, MemoryEntry>>(json, type).toMutableMap()
        }
        rebuildVectorIndex()
        loaded = true
    }

    @Synchronized
    private fun save() {
        prefs.edit().putString(KEY_ENTRIES, gson.toJson(entries)).apply()
    }

    private fun rebuildVectorIndex() {
        val items = entries.map { (id, e) -> Pair(id, e.content) }
        vectorStore.rebuild(items)
    }

    private fun generateId(): String {
        return "mem_" + System.currentTimeMillis() + "_" + (Math.random() * 1000).toInt()
    }

    // ── 添加记忆 ──

    @Synchronized
    fun addMemory(
        type: String,
        content: String,
        importance: Double = 0.5,
        context: String? = null,
        source: String = "conversation"
    ): MemoryEntry {
        ensureLoaded()
        val id = generateId()
        val entry = MemoryEntry(
            id = id, type = type, content = content,
            timestamp = System.currentTimeMillis(),
            importance = importance, context = context,
            source = source
        )
        entries[id] = entry
        vectorStore.add(id, content)
        autoEvict()
        save()
        return entry
    }

    // ── 删除 ──

    @Synchronized
    fun removeMemory(id: String) {
        ensureLoaded()
        if (entries.remove(id) != null) {
            vectorStore.remove(id)
            save()
        }
    }

    @Synchronized
    fun clear() {
        ensureLoaded()
        entries.clear()
        vectorStore.clear()
        save()
    }

    // ── 混合检索（向量 + 关键词）──

    @Synchronized
    fun search(query: String, limit: Int = 5): List<MemorySearchResult> {
        ensureLoaded()
        if (query.isBlank()) return emptyList()

        // 向量搜索
        val vectorResults = vectorStore.search(query, limit * 2)
        val vectorScores = mutableMapOf<String, Double>()
        for (r in vectorResults) {
            vectorScores[r.id] = r.score
        }

        // 关键词搜索
        val queryLower = query.lowercase()
        val keywords = queryLower.split(Regex("\\s+"))
        val keywordScores = mutableMapOf<String, Double>()
        for ((id, entry) in entries) {
            val contentLower = entry.content.lowercase()
            var matches = 0
            for (kw in keywords) {
                if (kw.length >= 2 && contentLower.contains(kw)) matches++
            }
            if (matches > 0) {
                keywordScores[id] = matches.toDouble() / keywords.size
            }
        }

        // 合并打分
        val allIds = (vectorScores.keys + keywordScores.keys).distinct()
        val results = allIds.mapNotNull { id ->
            val entry = entries[id] ?: return@mapNotNull null
            val vs = vectorScores[id] ?: 0.0
            val ks = keywordScores[id] ?: 0.0
            val combined = vs * VECTOR_WEIGHT + ks * KEYWORD_WEIGHT
            if (combined < 0.05) return@mapNotNull null
            entry.accessCount++
            MemorySearchResult(
                id = id, content = entry.content,
                type = entry.type, score = combined,
                vectorScore = vs, keywordScore = ks
            )
        }

        // 按 score 降序
        return results.sortedByDescending { it.score }.take(limit)
    }

    // ── 获取上下文字符串（注入到 API 请求）──

    @Synchronized
    fun getContextStrings(query: String? = null, limit: Int = CONTEXT_LIMIT): List<String> {
        ensureLoaded()
        val memories: List<MemoryEntry>
        if (query != null) {
            val results = search(query, limit)
            val idSet = results.map { it.id }.toSet()
            memories = results.mapNotNull { entries[it.id] }
        } else {
            // 无查询时按重要度+时间排序
            memories = entries.values
                .sortedWith(compareByDescending<MemoryEntry> { it.importance }
                    .thenByDescending { it.timestamp })
                .take(limit)
        }
        return memories.map { e ->
            val tag = if (e.context != null) " (${e.context})" else ""
            "[${e.type}]${tag}: ${e.content}"
        }
    }

    // ── 统计 ──

    @Synchronized
    fun getStats(): MemoryStats {
        ensureLoaded()
        val typeBreakdown = entries.values.groupBy { it.type }
            .mapValues { it.value.size }
        return MemoryStats(
            totalEntries = entries.size,
            typeBreakdown = typeBreakdown,
            vectorStats = vectorStore.getStats()
        )
    }

    // ── 导出 / 导入 ──

    @Synchronized
    fun exportMemories(): String {
        ensureLoaded()
        return gson.toJson(entries.values.toList())
    }

    @Synchronized
    fun importMemories(json: String): Int {
        ensureLoaded()
        val type = object : TypeToken<List<MemoryEntry>>() {}.type
        val imported: List<MemoryEntry> = gson.fromJson(json, type)
        var count = 0
        for (entry in imported) {
            if (!entries.containsKey(entry.id)) {
                entries[entry.id] = entry
                vectorStore.add(entry.id, entry.content)
                count++
            }
        }
        if (count > 0) save()
        return count
    }

    // ── 自动淘汰 ──

    private fun autoEvict() {
        if (entries.size <= MAX_MEMORIES) return
        val sorted = entries.values
            .sortedWith(compareBy<MemoryEntry> { it.importance }
                .thenBy { it.accessCount }
                .thenBy { it.timestamp })
        val toRemove = entries.size - MAX_MEMORIES
        for (i in 0 until minOf(toRemove, sorted.size)) {
            entries.remove(sorted[i].id)
            vectorStore.remove(sorted[i].id)
        }
    }

    // ── 数据类 ──

    data class MemorySearchResult(
        val id: String,
        val content: String,
        val type: String,
        val score: Double,
        val vectorScore: Double,
        val keywordScore: Double
    )

    data class MemoryStats(
        val totalEntries: Int,
        val typeBreakdown: Map<String, Int>,
        val vectorStats: VectorStore.VectorStats
    )
}
