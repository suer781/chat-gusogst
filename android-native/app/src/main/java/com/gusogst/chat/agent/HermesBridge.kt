package com.gusogst.chat.agent

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
            Log.w(TAG, "Already initialized — no-op (Python runtime not available in this build)")
            return
        }
        Log.w(TAG, "HermesBridge.init() — Chaquopy Python runtime not available in this build. Operating in stub mode.")
        started.set(false)
    }

    fun verifyEnvironment(): String {
        return "Hermes Bridge: Python runtime not available"
    }

    // ── Stream listener plumbing (kept for API compatibility) ─────────

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
        emit(StreamEvent.Error("Hermes Agent not available in this build"))
    }.flowOn(Dispatchers.IO)

    // ── Platform connectivity ─────────────────────────────────────────

    suspend fun connectPlatform(
        platformId: String,
        config: Map<String, String>,
    ): PlatformConnectResult = withContext(Dispatchers.IO) {
        Log.w(TAG, "connectPlatform($platformId) — Hermes Agent not available in this build")
        PlatformConnectResult(
            ok = false,
            status = "error",
            error = "Hermes Agent not available in this build"
        )
    }

    suspend fun getPlatforms(): List<PlatformInfo> = withContext(Dispatchers.IO) {
        Log.w(TAG, "getPlatforms() — Hermes Agent not available in this build")
        emptyList()
    }

    // ── Tool execution ────────────────────────────────────────────────

    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, Any> = emptyMap(),
    ): MessageResult = withContext(Dispatchers.IO) {
        Log.w(TAG, "executeTool($toolName) — Hermes Agent not available in this build")
        MessageResult(
            ok = false,
            content = "",
            error = "Hermes Agent not available in this build"
        )
    }

    // ── Memory system ─────────────────────────────────────────────────

    fun initMemory(providerName: String = "holographic"): Boolean {
        Log.w(TAG, "initMemory($providerName) — Hermes Agent not available in this build")
        return false
    }

    fun addMemory(
        content: String,
        type: String = "fact",
        importance: Double = 0.5,
        context: String? = null,
    ): String {
        Log.w(TAG, "addMemory() — Hermes Agent not available in this build")
        return ""
    }

    fun searchMemory(query: String, limit: Int = 5): List<MemoryResult> {
        Log.w(TAG, "searchMemory() — Hermes Agent not available in this build")
        return emptyList()
    }

    fun extractMemories(userMsg: String, aiResponse: String): List<MemoryResult> {
        Log.w(TAG, "extractMemories() — Hermes Agent not available in this build")
        return emptyList()
    }

    fun getMemoryContext(query: String, limit: Int = 5): List<String> {
        Log.w(TAG, "getMemoryContext() — Hermes Agent not available in this build")
        return emptyList()
    }

    fun getAllMemories(): List<MemoryResult> {
        Log.w(TAG, "getAllMemories() — Hermes Agent not available in this build")
        return emptyList()
    }

    fun deleteMemory(id: String): Boolean {
        Log.w(TAG, "deleteMemory($id) — Hermes Agent not available in this build")
        return false
    }

    fun clearMemories(): Boolean {
        Log.w(TAG, "clearMemories() — Hermes Agent not available in this build")
        return false
    }

    fun getMemoryStats(): MemoryStats {
        Log.w(TAG, "getMemoryStats() — Hermes Agent not available in this build")
        return MemoryStats()
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun parseResult(json: String): MessageResult {
        return try {
            gson.fromJson(json, MessageResult::class.java)
        } catch (e: Exception) {
            MessageResult(ok = false, content = "", error = "Failed to parse result: $json")
        }
    }

    fun isStarted(): Boolean = started.get()
}

// ── Stream event sealed class ──────────────────────────────────────────

sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data class Complete(val result: HermesBridge.MessageResult) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
