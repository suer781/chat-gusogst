package com.gusogst.chat.agent

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

object HermesBridge {
    private const val TAG = "HermesBridge"
    private val gson = Gson()
    private val started = AtomicBoolean(false)
    private var pyModule: com.chaquo.python.PyObject? = null

    // ── Types ──────────────────────────────────────────────────────────

    data class StreamDelta(val text: String)
    data class MessageResult(
        val ok: Boolean,
        val content: String,
        val toolCalls: List<ToolCallResult> = emptyList(),
        val error: String? = null,
    )
    data class ToolCallResult(
        val id: String = "",
        val name: String = "",
        val arguments: String = "",
    )
    data class PlatformInfo(
        val id: String,
        val name: String,
        val emoji: String,
        val available: Boolean,
    )
    data class PlatformConnectResult(
        val ok: Boolean,
        val status: String,
        val error: String? = null,
    )
    data class MemoryResult(
        val id: String = "",
        val content: String = "",
        val type: String = "general",
        val score: Double = 0.0,
        val trustScore: Double = 0.5,
        val timestamp: String = "",
        val retrievalCount: Int = 0,
        val helpfulCount: Int = 0,
    )
    data class MemoryStats(
        val totalEntries: Int = 0,
        val typeBreakdown: Map<String, Int> = emptyMap(),
        val totalSizeBytes: Long = 0,
    )

    // ── Initialization ─────────────────────────────────────────────────

    @Synchronized
    fun init(
        context: Context,
        apiKey: String = "",
        baseUrl: String = "",
        provider: String = "",
        model: String = "gpt-4o",
    ) {
        if (started.get()) {
            Log.w(TAG, "Already initialized — no-op")
            return
        }
        try {
            if (!Python.isStarted()) {
                com.chaquo.python.android.AndroidPlatform(context)
            }
            val py = Python.getInstance()
            pyModule = py.getModule("hermes_bridge")
            pyModule?.callAttr("init", apiKey, baseUrl, provider, model)
            started.set(true)
            Log.i(TAG, "HermesBridge initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize HermesBridge", e)
            started.set(false)
        }
    }

    fun verifyEnvironment(): String {
        return if (pyModule != null) {
            try {
                pyModule!!.callAttr("_verify_environment").toString()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        } else {
            "HermesBridge: not initialized"
        }
    }

    // ── Stream listener plumbing ─────────────────────────────────────

    private val streamListeners = mutableListOf<(String) -> Unit>()

    fun addStreamListener(listener: (String) -> Unit) {
        synchronized(streamListeners) { streamListeners.add(listener) }
    }

    fun removeStreamListener(listener: (String) -> Unit) {
        synchronized(streamListeners) { streamListeners.remove(listener) }
    }

    // ── Core API: send_message ─────────────────────────────────────────

    fun sendMessage(
        providerId: String,
        model: String,
        messages: List<Map<String, Any>>,
        tools: List<Map<String, Any>>? = null,
    ): Flow<StreamEvent> = flow {
        if (!started.get() || pyModule == null) {
            emit(StreamEvent.Error("Hermes Agent not available"))
            return@flow
        }
        try {
            val messagesJson = gson.toJson(messages)
            val toolsJson = if (tools != null) gson.toJson(tools) else "[]"
            val result = pyModule!!.callAttr("send_message", providerId, model, messagesJson, toolsJson).toString()
            val parsed = parseResult(result)
            if (parsed.ok) {
                if (parsed.content.isNotBlank()) {
                    emit(StreamEvent.Delta(parsed.content))
                }
                emit(StreamEvent.Complete(parsed))
            } else {
                emit(StreamEvent.Error(parsed.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage error", e)
            emit(StreamEvent.Error(e.message ?: "sendMessage failed"))
        }
    }.flowOn(Dispatchers.IO)

    // ── Platform connectivity ─────────────────────────────────────────

    suspend fun connectPlatform(
        platformId: String,
        config: Map<String, String>,
    ): PlatformConnectResult = withContext(Dispatchers.IO) {
        if (!started.get() || pyModule == null) {
            return@withContext PlatformConnectResult(ok = false, status = "error", error = "Hermes Agent not available")
        }
        try {
            val configJson = gson.toJson(config)
            val result = pyModule!!.callAttr("connect_platform", platformId, configJson).toString()
            val map = gson.fromJson(result, Map::class.java)
            PlatformConnectResult(
                ok = map["ok"] as? Boolean ?: false,
                status = map["status"] as? String ?: "unknown",
                error = map["error"] as? String,
            )
        } catch (e: Exception) {
            Log.e(TAG, "connectPlatform error", e)
            PlatformConnectResult(ok = false, status = "error", error = e.message)
        }
    }

    suspend fun getPlatforms(): List<PlatformInfo> = withContext(Dispatchers.IO) {
        if (!started.get() || pyModule == null) {
            return@withContext emptyList()
        }
        try {
            val result = pyModule!!.callAttr("get_platforms").toString()
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, Map::class.java).type
            val list: List<Map<String, Any>> = gson.fromJson(result, type)
            list.map {
                PlatformInfo(
                    id = it["id"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    emoji = it["emoji"] as? String ?: "",
                    available = it["available"] as? Boolean ?: false,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPlatforms error", e)
            emptyList()
        }
    }

    // ── Tool execution ────────────────────────────────────────────────

    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, Any> = emptyMap(),
    ): MessageResult = withContext(Dispatchers.IO) {
        if (!started.get() || pyModule == null) {
            return@withContext MessageResult(ok = false, content = "", error = "Hermes Agent not available")
        }
        try {
            val argsJson = gson.toJson(arguments)
            val result = pyModule!!.callAttr("execute_tool", toolName, argsJson).toString()
            parseResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "executeTool error", e)
            MessageResult(ok = false, content = "", error = e.message)
        }
    }

    // ── Memory system ─────────────────────────────────────────────────

    fun initMemory(providerName: String = "holographic"): Boolean {
        if (!started.get() || pyModule == null) return false
        return try {
            pyModule!!.callAttr("init_memory", providerName).toJava(Boolean::class.java) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "initMemory error", e)
            false
        }
    }

    fun addMemory(
        content: String,
        type: String = "fact",
        importance: Double = 0.5,
        context: String? = null,
    ): String {
        if (!started.get() || pyModule == null) return ""
        return try {
            pyModule!!.callAttr("add_memory", content, type, importance, context ?: "").toString()
        } catch (e: Exception) {
            Log.e(TAG, "addMemory error", e)
            ""
        }
    }

    fun searchMemory(query: String, limit: Int = 5): List<MemoryResult> {
        if (!started.get() || pyModule == null) return emptyList()
        return try {
            val result = pyModule!!.callAttr("search_memory", query, limit).toString()
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, Map::class.java).type
            val list: List<Map<String, Any>> = gson.fromJson(result, type)
            list.map { parseMemoryResult(it) }
        } catch (e: Exception) {
            Log.e(TAG, "searchMemory error", e)
            emptyList()
        }
    }

    fun extractMemories(userMsg: String, aiResponse: String): List<MemoryResult> {
        if (!started.get() || pyModule == null) return emptyList()
        return try {
            val result = pyModule!!.callAttr("extract_memories", userMsg, aiResponse).toString()
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, Map::class.java).type
            val list: List<Map<String, Any>> = gson.fromJson(result, type)
            list.map { parseMemoryResult(it) }
        } catch (e: Exception) {
            Log.e(TAG, "extractMemories error", e)
            emptyList()
        }
    }

    fun getMemoryContext(query: String, limit: Int = 5): List<String> {
        if (!started.get() || pyModule == null) return emptyList()
        return try {
            val result = pyModule!!.callAttr("get_memory_context", query, limit).toString()
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, String::class.java).type
            gson.fromJson(result, type)
        } catch (e: Exception) {
            Log.e(TAG, "getMemoryContext error", e)
            emptyList()
        }
    }

    fun getAllMemories(): List<MemoryResult> {
        if (!started.get() || pyModule == null) return emptyList()
        return try {
            val result = pyModule!!.callAttr("get_all_memories").toString()
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, Map::class.java).type
            val list: List<Map<String, Any>> = gson.fromJson(result, type)
            list.map { parseMemoryResult(it) }
        } catch (e: Exception) {
            Log.e(TAG, "getAllMemories error", e)
            emptyList()
        }
    }

    fun deleteMemory(id: String): Boolean {
        if (!started.get() || pyModule == null) return false
        return try {
            pyModule!!.callAttr("delete_memory", id).toJava(Boolean::class.java) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "deleteMemory error", e)
            false
        }
    }

    fun clearMemories(): Boolean {
        if (!started.get() || pyModule == null) return false
        return try {
            pyModule!!.callAttr("clear_memories").toJava(Boolean::class.java) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "clearMemories error", e)
            false
        }
    }

    fun getMemoryStats(): MemoryStats {
        if (!started.get() || pyModule == null) return MemoryStats()
        return try {
            val result = pyModule!!.callAttr("get_memory_stats").toString()
            val type = com.google.gson.reflect.TypeToken.getParameterized(Map::class.java, String::class.java, Any::class.java).type
            val map: Map<String, Any> = gson.fromJson(result, type)
            val totalEntries = when (val v = map["totalEntries"] ?: map["total_entries"] ?: 0) {
                is Number -> v.toInt()
                else -> 0
            }
            val typeBreakdownRaw = (map["typeBreakdown"] ?: map["type_breakdown"] ?: emptyMap<String, Int>()) as? Map<*, *> ?: emptyMap<String, Int>()
            val safeBreakdown = typeBreakdownRaw.entries.associate { (k, v) ->
                k.toString() to when (v) {
                    is Number -> v.toInt()
                    else -> 0
                }
            }
            val totalSizeBytes = when (val v = map["totalSizeBytes"] ?: map["total_size_bytes"] ?: 0L) {
                is Number -> v.toLong()
                else -> 0L
            }
            MemoryStats(
                totalEntries = totalEntries,
                typeBreakdown = safeBreakdown,
                totalSizeBytes = totalSizeBytes,
            )
        } catch (e: Exception) {
            Log.e(TAG, "getMemoryStats error", e)
            MemoryStats()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun parseResult(json: String): MessageResult {
        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(Map::class.java, String::class.java, Any::class.java).type
            val map: Map<String, Any> = gson.fromJson(json, type)
            MessageResult(
                ok = map["ok"] as? Boolean ?: false,
                content = map["content"] as? String ?: "",
                toolCalls = parseToolCallResults(map["tool_calls"] ?: map["toolCalls"]),
                error = map["error"] as? String,
            )
        } catch (e: Exception) {
            MessageResult(ok = false, content = "", error = "Failed to parse result: $json")
        }
    }

    private fun parseToolCallResults(raw: Any?): List<ToolCallResult> {
        if (raw == null) return emptyList()
        return try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, Map::class.java).type
            val list: List<Map<String, Any>> = gson.fromJson(gson.toJson(raw), type)
            list.map {
                ToolCallResult(
                    id = it["id"] as? String ?: "",
                    name = it["name"] as? String ?: "",
                    arguments = it["arguments"]?.toString() ?: "",
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseMemoryResult(map: Map<String, Any>): MemoryResult {
        return MemoryResult(
            id = map["id"] as? String ?: "",
            content = map["content"] as? String ?: "",
            type = map["type"] as? String ?: "general",
            score = when (val v = map["score"]) { is Number -> v.toDouble() else -> 0.0 },
            trustScore = when (val v = map["trustScore"] ?: map["trust_score"]) { is Number -> v.toDouble() else -> 0.5 },
            timestamp = map["timestamp"] as? String ?: "",
            retrievalCount = when (val v = map["retrievalCount"] ?: map["retrieval_count"]) { is Number -> v.toInt() else -> 0 },
            helpfulCount = when (val v = map["helpfulCount"] ?: map["helpful_count"]) { is Number -> v.toInt() else -> 0 },
        )
    }

    fun isStarted(): Boolean = started.get()
}

// ── Stream event sealed class ──────────────────────────────────────────

sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data class Complete(val result: HermesBridge.MessageResult) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
