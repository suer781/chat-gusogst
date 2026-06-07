package com.gusogst.chat.data.memory

// ═══════════════════════════════════════════════
// 向量存储 — TF-IDF + 余弦相似度
// 移植自 main 分支 memory/vectorStore.ts
// IndexedDB → SharedPreferences + Gson
// ═══════════════════════════════════════════════

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.math.sqrt

class VectorStore(context: Context) {

    data class VectorEntry(
        val id: String,
        val text: String,
        val tokens: List<String>,
        val tf: Map<String, Double>,
        val magnitude: Double
    )

    data class SearchResult(
        val id: String,
        val score: Double
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vector_store", Context.MODE_PRIVATE)
    private val gson = Gson()

    val entries = mutableListOf<VectorEntry>()
    private var idf = mutableMapOf<String, Double>()
    var loaded = false
        private set

    init {
        loadFromDisk()
    }

    fun add(id: String, text: String) {
        remove(id)
        val tokens = tokenize(text)
        val tf = computeTF(tokens)
        val magnitude = computeMagnitude(tf)
        entries.add(VectorEntry(id, text, tokens, tf, magnitude))
        rebuildIDF()
        saveToDisk()
    }

    fun remove(id: String) {
        entries.removeAll { it.id == id }
        rebuildIDF()
        saveToDisk()
    }

    fun search(query: String, limit: Int = 5, minScore: Double = 0.01): List<SearchResult> {
        if (entries.isEmpty()) return emptyList()
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) return emptyList()
        val queryTF = computeTF(queryTokens)
        val queryMag = computeMagnitude(queryTF)
        if (queryMag == 0.0) return emptyList()
        return entries
            .map { entry -> SearchResult(entry.id, cosineSimilarity(queryTF, queryMag, entry)) }
            .filter { it.score >= minScore }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun rebuild(items: List<Pair<String, String>>) {
        entries.clear()
        for ((id, text) in items) {
            val tokens = tokenize(text)
            val tf = computeTF(tokens)
            val magnitude = computeMagnitude(tf)
            entries.add(VectorEntry(id, text, tokens, tf, magnitude))
        }
        rebuildIDF()
        saveToDisk()
    }

    fun clear() {
        entries.clear()
        idf.clear()
        saveToDisk()
    }

    fun getStats(): Map<String, Any> {
        val vocab = entries.flatMap { it.tokens }.toSet()
        return mapOf("documents" to entries.size, "vocabulary" to vocab.size)
    }

    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val chinese = StringBuilder()
        val english = StringBuilder()
        for (c in text.lowercase()) {
            when {
                c.code in 0x4e00..0x9fff -> {
                    if (english.isNotEmpty()) { tokens.add(english.toString()); english.clear() }
                    chinese.append(c)
                }
                c.isLetterOrDigit() -> {
                    if (chinese.isNotEmpty()) { extractChineseNgrams(chinese.toString(), tokens); chinese.clear() }
                    english.append(c)
                }
                else -> {
                    if (chinese.isNotEmpty()) { extractChineseNgrams(chinese.toString(), tokens); chinese.clear() }
                    if (english.isNotEmpty()) { tokens.add(english.toString()); english.clear() }
                }
            }
        }
        if (chinese.isNotEmpty()) extractChineseNgrams(chinese.toString(), tokens)
        if (english.isNotEmpty()) tokens.add(english.toString())
        return tokens
    }

    private fun extractChineseNgrams(text: String, tokens: MutableList<String>) {
        for (c in text) tokens.add(c.toString())
        for (i in 0 until text.length - 1) tokens.add(text.substring(i, i + 2))
    }

    private fun computeTF(tokens: List<String>): Map<String, Double> {
        if (tokens.isEmpty()) return emptyMap()
        val counts = tokens.groupingBy { it }.eachCount()
        val max = counts.values.maxOrNull() ?: 1
        return counts.mapValues { (_, v) -> 0.5 + 0.5 * v.toDouble() / max }
    }

    private fun rebuildIDF() {
        if (entries.isEmpty()) { idf.clear(); return }
        val n = entries.size.toDouble()
        val df = mutableMapOf<String, Int>()
        for (entry in entries) {
            for (token in entry.tf.keys) df[token] = (df[token] ?: 0) + 1
        }
        idf = df.mapValues { (_, v) -> kotlin.math.ln(n / v) + 1.0 }.toMutableMap()
    }

    private fun computeMagnitude(tf: Map<String, Double>): Double {
        var sum = 0.0
        for ((token, value) in tf) {
            val w = value * (idf[token] ?: 1.0)
            sum += w * w
        }
        return sqrt(sum)
    }

    private fun cosineSimilarity(queryTF: Map<String, Double>, queryMag: Double, entry: VectorEntry): Double {
        if (entry.magnitude == 0.0) return 0.0
        var dot = 0.0
        for ((token, qVal) in queryTF) {
            val eVal = entry.tf[token] ?: continue
            dot += qVal * (idf[token] ?: 1.0) * eVal * (idf[token] ?: 1.0)
        }
        return dot / (queryMag * entry.magnitude)
    }

    private data class PersistEntry(val id: String, val text: String)

    private fun saveToDisk() {
        prefs.edit().putString("entries", gson.toJson(entries.map { PersistEntry(it.id, it.text) })).apply()
    }

    private fun loadFromDisk() {
        try {
            val json = prefs.getString("entries", null) ?: run { loaded = true; return }
            val type = object : TypeToken<List<PersistEntry>>() {}.type
            val data: List<PersistEntry> = gson.fromJson(json, type)
            entries.clear()
            for (item in data) {
                val tokens = tokenize(item.text)
                val tf = computeTF(tokens)
                val magnitude = computeMagnitude(tf)
                entries.add(VectorEntry(item.id, item.text, tokens, tf, magnitude))
            }
            rebuildIDF()
        } catch (e: Exception) {
        } finally {
            loaded = true
        }
    }
}