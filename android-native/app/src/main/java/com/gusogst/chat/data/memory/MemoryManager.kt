package com.gusogst.chat.data.memory

// ═══════════════════════════════════════════════
// 记忆管理器 — 混合搜索（向量+关键词+时间衰减）
// 移植自 main 分支 memory/manager.ts
// IndexedDB → SharedPreferences + Gson
// ═══════════════════════════════════════════════

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.ln
import kotlin.math.pow

class MemoryManager(context: Context) {

    data class MemoryEntry(
        val id: String,
        val content: String,
        val type: String = "context",
        val importance: Double = 1.0,
        val tags: List<String> = emptyList(),
        val timestamp: Long = System.currentTimeMillis(),
        var accessCount: Int = 0,
        var lastAccessed: Long = System.currentTimeMillis()
    )

    companion object {
        private const val MAX_MEMORIES = 500
        private const val HALF_LIFE_MS = 7 * 24 * 60 * 60 * 1000.0
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("memory_store", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val vectorStore = VectorStore(context)

    private val cache = mutableListOf<MemoryEntry>()
    private var loaded = false

    init {
        loadFromDisk()
    }

    fun searchMemory(query: String, limit: Int = 5): List<MemoryEntry> {
        ensureLoaded()
        if (cache.isEmpty()) return emptyList()

        val now = System.currentTimeMillis()
        val vectorResults = vectorStore.search(query, limit = limit * 3)
        val vectorScores = vectorResults.associate { it.id to it.score }
        val queryLower = query.lowercase()
        val queryWords = queryLower.split(Regex("\\s+"))

        return cache
            .map { entry ->
                var score = 0.0
                score += (vectorScores[entry.id] ?: 0.0) * 50
                val contentLower = entry.content.lowercase()
                for (word in queryWords) {
                    if (contentLower.contains(word)) score += 10
                }
                for (tag in entry.tags) {
                    for (word in queryWords) {
                        if (tag.lowercase().contains(word)) score += 5
                    }
                }
                score += entry.importance
                val age = (now - entry.timestamp).toDouble()
                val decay = 0.5.pow(age / HALF_LIFE_MS)
                score *= decay
                score += ln((entry.accessCount + 1).toDouble())
                entry to score
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { (entry, _) ->
                entry.accessCount++
                entry.lastAccessed = now
                saveToDisk()
                entry
            }
    }

    fun add(
        content: String,
        type: String = "context",
        importance: Double = 1.0,
        tags: List<String> = emptyList()
    ): MemoryEntry {
        ensureLoaded()
        val entry = MemoryEntry(
            id = "mem_${System.currentTimeMillis()}_${(1000..9999).random()}",
            content = content,
            type = type,
            importance = importance,
            tags = tags
        )
        cache.add(entry)
        vectorStore.add(entry.id, content)
        if (cache.size > MAX_MEMORIES) {
            val oldest = cache.minByOrNull { it.timestamp } ?: cache.first()
            remove(oldest.id)
        }
        saveToDisk()
        return entry
    }

    fun extractAndSave(messages: List<String>) {
        val recent = messages.takeLast(10)
        for (content in recent) {
            val lower = content.lowercase()
            val prefPatterns = listOf(
                "我喜欢", "我不喜欢", "我偏好", "我习惯",
                "i like", "i prefer", "i hate", "i love",
                "我的名字", "叫我", "my name", "call me",
                "我是", "i am", "i'm"
            )
            if (prefPatterns.any { lower.contains(it) }) {
                add(content, type = "preference", importance = 3.0, tags = listOf("preference"))
                continue
            }
            val factPatterns = listOf(
                "我是", "我在", "我住", "我工作",
                "i am", "i live", "i work", "my "
            )
            if (factPatterns.any { lower.contains(it) }) {
                add(content, type = "fact", importance = 2.0, tags = listOf("fact"))
                continue
            }
            val emotionPatterns = listOf(
                "我很开心", "我很难过", "我很累", "我生气",
                "i am happy", "i am sad", "i am tired", "i feel"
            )
            if (emotionPatterns.any { lower.contains(it) }) {
                add(content, type = "emotion", importance = 1.5, tags = listOf("emotion"))
            }
        }
    }

    fun getContextStrings(query: String, limit: Int = 5): List<String> {
        return searchMemory(query, limit).map { it.content }
    }

    fun getAll(): List<MemoryEntry> {
        ensureLoaded()
        return cache.toList()
    }

    fun clear() {
        cache.clear()
        vectorStore.clear()
        saveToDisk()
    }

    fun getMemoryCount(): Int {
        ensureLoaded()
        return cache.size
    }

    fun getStats(): Map<String, Any> {
        ensureLoaded()
        val vs = vectorStore.getStats()
        val json = gson.toJson(cache)
        return mapOf(
            "totalMemories" to cache.size,
            "vocabulary" to (vs["vocabulary"] ?: 0),
            "estimatedSizeKB" to (json.toByteArray().size / 1024.0),
            "byType" to cache.groupingBy { it.type }.eachCount()
        )
    }

    private fun ensureLoaded() {
        if (!loaded) loaded = true
    }

    private fun remove(id: String) {
        cache.removeAll { it.id == id }
        vectorStore.remove(id)
    }

    private fun saveToDisk() {
        prefs.edit().putString("memories", gson.toJson(cache)).apply()
    }

    private fun loadFromDisk() {
        try {
            val json = prefs.getString("memories", null) ?: run {
                loaded = true
                return
            }
            val type = object : TypeToken<List<MemoryEntry>>() {}.type
            val data: List<MemoryEntry> = gson.fromJson(json, type)
            cache.clear()
            cache.addAll(data)
            vectorStore.rebuild(cache.map { it.id to it.content })
        } catch (e: Exception) {
        } finally {
            loaded = true
        }
    }
}