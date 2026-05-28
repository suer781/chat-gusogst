package com.gusogst.chat.data.memory

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * 纯本地 TF-IDF 向量存储
 * 功能与 main 分支 vectorStore.ts 完全对齐
 */
class VectorStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "vector_store"
        private const val KEY_ENTRIES = "entries"
    }

    private data class VectorEntry(
        val id: String,
        val tokens: List<String>,
        val tf: Map<String, Double>,
        var magnitude: Double
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private var entries: MutableMap<String, VectorEntry> = mutableMapOf()
    private var idf: MutableMap<String, Double> = mutableMapOf()
    private var loaded = false

    // ── 持久化 ──

    @Synchronized
    private fun ensureLoaded() {
        if (loaded) return
        val json = prefs.getString(KEY_ENTRIES, null)
        if (json != null) {
            val type = object : TypeToken<Map<String, VectorEntry>>() {}.type
            val raw: Map<String, VectorEntry> = gson.fromJson(json, type)
            entries = raw.toMutableMap()
        }
        rebuildIDF()
        loaded = true
    }

    @Synchronized
    private fun save() {
        prefs.edit().putString(KEY_ENTRIES, gson.toJson(entries)).apply()
    }

    // ── 分词 ──

    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        // 英文单词 + 数字
        val wordRegex = Regex("[a-zA-Z0-9]+")
        wordRegex.findAll(text.lowercase()).forEach {
            tokens.add(it.value)
        }
        // 中文：unigram + bigram
        val chinese = text.filter { it.code in 0x4e00..0x9fff }
        for (i in chinese.indices) {
            tokens.add(chinese[i].toString())
            if (i + 1 < chinese.length) {
                tokens.add("${chinese[i]}${chinese[i + 1]}")
            }
        }
        return tokens
    }

    // ── IDF / TF / 余弦 ──

    private fun rebuildIDF() {
        val n = entries.size.toDouble()
        val df = mutableMapOf<String, Int>()
        for (entry in entries.values) {
            for (token in entry.tf.keys) {
                df[token] = (df[token] ?: 0) + 1
            }
        }
        idf.clear()
        for ((token, count) in df) {
            idf[token] = ln((n + 1) / (count + 1)) + 1
        }
    }

    private fun computeTF(tokens: List<String>): Map<String, Double> {
        if (tokens.isEmpty()) return emptyMap()
        val freq = mutableMapOf<String, Int>()
        for (t in tokens) freq[t] = (freq[t] ?: 0) + 1
        val maxFreq = freq.values.max().toDouble()
        return freq.mapValues { (_, v) -> v / maxFreq }
    }

    private fun computeMagnitude(tf: Map<String, Double>): Double {
        var sum = 0.0
        for ((token, tfVal) in tf) {
            val w = tfVal * (idf[token] ?: 1.0)
            sum += w * w
        }
        return sqrt(sum)
    }

    private fun cosineSimilarity(
        queryTF: Map<String, Double>,
        queryMag: Double,
        entry: VectorEntry
    ): Double {
        if (queryMag == 0.0 || entry.magnitude == 0.0) return 0.0
        var dot = 0.0
        for ((token, qVal) in queryTF) {
            val eVal = entry.tf[token] ?: continue
            dot += qVal * (idf[token] ?: 1.0) * eVal * (idf[token] ?: 1.0)
        }
        return dot / (queryMag * entry.magnitude)
    }

    // ── 公开 API ──

    @Synchronized
    fun add(id: String, text: String) {
        ensureLoaded()
        val tokens = tokenize(text)
        val tf = computeTF(tokens)
        entries[id] = VectorEntry(id, tokens, tf, 0.0)
        rebuildIDF()
        // 重算所有模长
        for (e in entries.values) {
            e.magnitude = computeMagnitude(e.tf)
        }
        save()
    }

    @Synchronized
    fun remove(id: String) {
        ensureLoaded()
        if (entries.remove(id) != null) {
            rebuildIDF()
            for (e in entries.values) {
                e.magnitude = computeMagnitude(e.tf)
            }
            save()
        }
    }

    @Synchronized
    fun search(query: String, limit: Int = 5, minScore: Double = 0.05): List<SearchResult> {
        ensureLoaded()
        val tokens = tokenize(query)
        if (tokens.isEmpty()) return emptyList()
        val queryTF = computeTF(tokens)
        val queryMag = computeMagnitude(queryTF)
        if (queryMag == 0.0) return emptyList()

        return entries.values
            .map { SearchResult(it.id, cosineSimilarity(queryTF, queryMag, it)) }
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(limit)
    }

    @Synchronized
    fun getStats(): VectorStats {
        ensureLoaded()
        return VectorStats(
            totalEntries = entries.size,
            vocabularySize = idf.size
        )
    }

    @Synchronized
    fun clear() {
        entries.clear()
        idf.clear()
        loaded = true
        save()
    }

    @Synchronized
    fun rebuild(items: List<Pair<String, String>>) {
        entries.clear()
        for ((id, text) in items) {
            val tokens = tokenize(text)
            val tf = computeTF(tokens)
            entries[id] = VectorEntry(id, tokens, tf, 0.0)
        }
        rebuildIDF()
        for (e in entries.values) {
            e.magnitude = computeMagnitude(e.tf)
        }
        loaded = true
        save()
    }

    // ── 数据类 ──

    data class SearchResult(val id: String, val score: Double)
    data class VectorStats(val totalEntries: Int, val vocabularySize: Int)
}
