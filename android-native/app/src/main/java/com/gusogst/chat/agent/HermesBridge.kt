package com.gusogst.chat.agent

import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hermes Agent Android Bridge
 *
 * Bridges Kotlin ↔ Python through Chaquopy, giving the Android app access
 * to the real Hermes Agent (from /opt/hermes) for LLM message processing,
 * tool execution, and platform connectivity.
 *
 * Architecture:
 *   ChatViewModel → HermesBridge.sendMessage() → Chaquopy → hermes_bridge.py
 *                                                                   ↓
 *                                                            run_agent.AIAgent
 *
 * All heavy lifting (prompt building, tool orchestration, streaming) happens
 * inside the Python process.  Kotlin only marshals inputs/outputs and pushes
 * stream deltas to the UI via Kotlin Flow.
 */
object HermesBridge {
    private const val TAG = "HermesBridge"
    private val gson = Gson()
    private val started = AtomicBoolean(false)
    private var pyModule: PyObject? = null

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

    /**
     * Initialize the Chaquopy Python runtime and load Hermes Bridge.
     *
     * Must be called once during application startup (e.g. from
     * ChatApplication.onCreate).  Safe to call multiple times — subsequent
     * calls are no-ops.
     *
     * @param context  Application context (for Chaquopy platform init).
     * @param apiKey   Optional API key for the default LLM provider.
     * @param baseUrl  Optional base URL for the default LLM provider.
     * @param provider Optional provider identifier.
     * @param model    Optional default model ID.
     */
    @Synchronized
    fun init(
        context: Context,
        apiKey: String = "",
        baseUrl: String = "",
        provider: String = "",
        model: String = "gpt-4o",
    ) {
        if (started.get()) {
            // Already initialized — just update config
            try {
                pyModule?.callAttr("init", apiKey, baseUrl, provider, model)
            } catch (e: Exception) {
                Log.w(TAG, "Config update failed (non-fatal): $e")
            }
            return
        }

        try {
            // Start Chaquopy Python runtime
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            // Load the bridge module
            val py = Python.getInstance()
            val module = py.getModule("hermes_bridge")
            pyModule = module

            // Initialize with config
            module.callAttr("init", apiKey, baseUrl, provider, model)

            // Register a streaming callback that Chaquopy will invoke
            // from Python for each text delta
            registerStreamCallback(context)

            started.set(true)
            Log.i(TAG, "Hermes Bridge initialized: provider=$provider model=$model")

            // Initialize memory provider (holographic) — lazy, non-blocking
            try {
                module.callAttr("init_memory", "holographic")
            } catch (e: Exception) {
                Log.w(TAG, "Memory init deferred (will retry on first use): $e")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Hermes Bridge init failed", e)
            started.set(false)
            throw RuntimeException("Failed to initialize Hermes Agent Python runtime", e)
        }
    }

    /**
     * Verify the Python environment is healthy.  Returns a diagnostic
     * JSON string suitable for logging or debug UI.  Must call init() first.
     */
    fun verifyEnvironment(): String {
        val module = pyModule ?: return """{"error": "Bridge not initialized"}"""
        return try {
            module.callAttr("_verify_environment").toString()
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }

    // ── Streaming callback registration ────────────────────────────────
    // We store a list of Kotlin lambdas that get called from Python via
    // Chaquopy's Java-to-Python proxy.  Each lambda is wrapped in a
    // StreamCallback Java interface instance; Python calls onDelta(text)
    // through Chaquopy's proxy, which routes to the Kotlin code.

    private val streamListeners = mutableListOf<(String) -> Unit>()

    /**
     * Add a listener that receives each text delta during streaming.
     * The listener is called from a background thread (not main).
     */
    fun addStreamListener(listener: (String) -> Unit) {
        synchronized(streamListeners) { streamListeners.add(listener) }
    }

    fun removeStreamListener(listener: (String) -> Unit) {
        synchronized(streamListeners) { streamListeners.remove(listener) }
    }

    /**
     * Java StreamCallback implementation that bridges Chaquopy → Kotlin.
     *
     * Chaquopy passes this object to Python.  Python calls
     * {@code cb.onDelta(text)} and Chaquopy transparently routes the
     * call back to this JVM method, which dispatches to registered
     * Kotlin listeners.
     */
    private class BridgeStreamCallback(
        private val listeners: MutableList<(String) -> Unit>,
    ) : StreamCallback {
        override fun onDelta(text: String?) {
            val delta = text ?: return
            synchronized(listeners) {
                for (l in listeners) {
                    try { l(delta) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun registerStreamCallback(context: Context) {
        // Create a Java callback instance implementing StreamCallback.
        // Chaquopy passes this to Python's _register_stream_callback().
        // Python inspects it, finds onDelta(), and calls it for each
        // text delta.
        val callback = BridgeStreamCallback(streamListeners)
        try {
            pyModule?.callAttr("_register_stream_callback", callback)
            Log.d(TAG, "Stream callback registered via Chaquopy Java proxy")
        } catch (e: Exception) {
            Log.w(TAG, "Stream callback registration failed (streaming may be unavailable): $e")
        }
    }

    // ── Core API: send_message ─────────────────────────────────────────

    /**
     * Send a message through the Hermes Agent and stream the response.
     *
     * This is the main entry point for AI chat.  The Agent handles:
     *   1. Tool calling loop (if tools are provided)
     *   2. Tool execution
     *   3. Streaming text output
     *
     * Text deltas are emitted through the returned [Flow].  The final
     * value in the flow contains the complete [MessageResult].
     *
     * @param providerId  Provider identifier (e.g. "openai").
     * @param model       Model ID to use.
     * @param messages    List of messages as Map (role, content).
     * @param tools       Optional tool definitions in OpenAI format.
     * @return Flow of [StreamEvent] — text deltas followed by final result.
     */
    fun sendMessage(
        providerId: String,
        model: String,
        messages: List<Map<String, Any>>,
        tools: List<Map<String, Any>>? = null,
    ): Flow<StreamEvent> = flow {
        val module = pyModule
        if (module == null) {
            emit(StreamEvent.Error("Hermes Bridge not initialized. Call HermesBridge.init() first."))
            return@flow
        }

        val messagesJson = gson.toJson(messages)
        val toolsJson = if (tools != null) gson.toJson(tools) else "[]"

        Log.d(TAG, "sendMessage: provider=$providerId model=$model messages=${messages.size} tools=${tools?.size ?: 0}")

        // ── Set up streaming relay ─────────────────────────────────
        val deltas = mutableListOf<String>()
        val listener: (String) -> Unit = { delta ->
            synchronized(deltas) { deltas.add(delta) }
        }
        addStreamListener(listener)

        try {
            // ── Call Python ────────────────────────────────────────
            // This call blocks until the Agent completes the full
            // tool-calling loop.  Streaming deltas arrive on our
            // listener concurrently (Chaquopy uses a background thread
            // for callbacks).
            // We perform the blocking call on Dispatchers.IO.
            val resultJson = withContext(Dispatchers.IO) {
                module.callAttr(
                    "send_message",
                    providerId,
                    model,
                    messagesJson,
                    toolsJson,
                ).toString()
            }

            // ── Emit collected deltas ───────────────────────────────
            synchronized(deltas) {
                for (delta in deltas) {
                    emit(StreamEvent.Delta(delta))
                }
                deltas.clear()
            }

            // ── Parse and emit result ──────────────────────────────
            val result = parseResult(resultJson)
            emit(StreamEvent.Complete(result))

        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed", e)
            // Emit any collected deltas before the error
            synchronized(deltas) {
                for (delta in deltas) {
                    emit(StreamEvent.Delta(delta))
                }
                deltas.clear()
            }
            emit(StreamEvent.Error(e.message ?: "Unknown error"))

        } finally {
            removeStreamListener(listener)
        }
    }.flowOn(Dispatchers.IO)

    // ── Platform connectivity ─────────────────────────────────────────

    /**
     * Connect to a messaging platform via the Hermes Gateway.
     *
     * @param platformId  Platform identifier ("telegram", "discord", etc.).
     * @param config      Platform-specific configuration map.
     * @return            Result with connectivity status.
     */
    suspend fun connectPlatform(
        platformId: String,
        config: Map<String, String>,
    ): PlatformConnectResult = withContext(Dispatchers.IO) {
        val module = pyModule
        if (module == null) {
            return@withContext PlatformConnectResult(ok = false, status = "error",
                error = "Bridge not initialized")
        }
        try {
            val configJson = gson.toJson(config)
            val resultJson = module.callAttr(
                "connect_platform", platformId, configJson
            ).toString()
            return@withContext gson.fromJson(resultJson, PlatformConnectResult::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "connectPlatform failed: $platformId", e)
            return@withContext PlatformConnectResult(ok = false, status = "error",
                error = e.message ?: "Unknown error")
        }
    }

    /**
     * Get the list of available messaging platforms from the Hermes
     * Gateway platform registry.
     */
    suspend fun getPlatforms(): List<PlatformInfo> = withContext(Dispatchers.IO) {
        val module = pyModule
        if (module == null) return@withContext emptyList()
        try {
            val json = module.callAttr("get_platforms").toString()
            val type = object : TypeToken<List<PlatformInfo>>() {}.type
            return@withContext gson.fromJson(json, type)
        } catch (e: Exception) {
            Log.e(TAG, "getPlatforms failed", e)
            return@withContext emptyList()
        }
    }

    // ── Tool execution ────────────────────────────────────────────────

    /**
     * Execute a single named Hermes tool and return the result.
     * Useful for standalone tool operations (e.g. testing, platform setup).
     */
    suspend fun executeTool(
        toolName: String,
        arguments: Map<String, Any> = emptyMap(),
    ): MessageResult = withContext(Dispatchers.IO) {
        val module = pyModule
        if (module == null) {
            return@withContext MessageResult(ok = false, content = "",
                error = "Bridge not initialized")
        }
        try {
            val argsJson = gson.toJson(arguments)
            val resultJson = module.callAttr(
                "execute_tool", toolName, argsJson
            ).toString()
            return@withContext parseResult(resultJson)
        } catch (e: Exception) {
            return@withContext MessageResult(ok = false, content = "",
                error = e.message ?: "Unknown error")
        }
    }

    // ── Memory system ─────────────────────────────────────────────────

    /**
     * Initialize the Hermes Agent memory system (holographic provider).
     *
     * Must be called once after [init]. Uses the holographic memory provider
     * which stores facts in a local SQLite database with FTS5 search,
     * entity resolution, trust scoring, and HRR compositional retrieval.
     *
     * @param providerName Memory provider name (default: "holographic").
     * @return True on success.
     */
    fun initMemory(providerName: String = "holographic"): Boolean {
        val module = pyModule ?: return false
        return try {
            val result = module.callAttr("init_memory", providerName)
            result.toBoolean()
        } catch (e: Exception) {
            Log.e(TAG, "initMemory failed", e)
            false
        }
    }

    /**
     * Add a memory entry to the store.
     *
     * @param content    Memory content text.
     * @param type       Memory category (fact, preference, plan, etc.).
     * @param importance Trust score [0.0, 1.0].
     * @param context    Optional context label.
     * @return The fact ID if successfully added, empty string otherwise.
     */
    fun addMemory(
        content: String,
        type: String = "fact",
        importance: Double = 0.5,
        context: String? = null,
    ): String {
        val module = pyModule ?: return ""
        return try {
            val json = module.callAttr(
                "add_memory",
                content, type, importance, context ?: ""
            ).toString()
            val map = gson.fromJson(json, Map::class.java) as? Map<*, *> ?: return ""
            if (map["status"] == "added") (map["fact_id"] as? String) ?: "" else ""
        } catch (e: Exception) {
            Log.e(TAG, "addMemory failed", e)
            ""
        }
    }

    /**
     * Search memories with semantic + keyword hybrid retrieval.
     *
     * @param query Search query.
     * @param limit Max results.
     * @return List of matching memory results, scored by relevance.
     */
    fun searchMemory(query: String, limit: Int = 5): List<MemoryResult> {
        val module = pyModule ?: return emptyList()
        return try {
            val json = module.callAttr("search_memory", query, limit).toString()
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val rawList: List<Map<String, Any>> = gson.fromJson(json, type)
            rawList.map { map ->
                MemoryResult(
                    id = (map["id"] as? String) ?: "",
                    content = (map["content"] as? String) ?: "",
                    type = (map["type"] as? String) ?: "general",
                    score = (map["score"] as? Double) ?: 0.0,
                    trustScore = (map["trust_score"] as? Double) ?: 0.5,
                    timestamp = (map["timestamp"] as? String) ?: "",
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchMemory failed", e)
            emptyList()
        }
    }

    /**
     * Extract memories from a conversation turn using LLM or regex fallback.
     *
     * Extracted memories are automatically persisted to the store.
     *
     * @param userMsg    The user's message.
     * @param aiResponse The AI's response.
     * @return List of extracted MemoryResult with types and confidence.
     */
    fun extractMemories(userMsg: String, aiResponse: String): List<MemoryResult> {
        val module = pyModule ?: return emptyList()
        return try {
            val json = module.callAttr("extract_memories", userMsg, aiResponse).toString()
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val rawList: List<Map<String, Any>> = gson.fromJson(json, type)
            rawList.map { map ->
                MemoryResult(
                    id = (map["id"] as? String) ?: "",
                    content = (map["content"] as? String) ?: "",
                    type = (map["type"] as? String) ?: "fact",
                    score = (map["confidence"] as? Double) ?: 0.5,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "extractMemories failed", e)
            emptyList()
        }
    }

    /**
     * Get memory context strings for prompt injection.
     *
     * Returns formatted context lines relevant to the query, suitable for
     * inclusion in the system prompt to personalize AI responses.
     *
     * @param query The current user message for relevance matching.
     * @param limit Max context lines.
     * @return List of formatted context strings.
     */
    fun getMemoryContext(query: String, limit: Int = 5): List<String> {
        val module = pyModule ?: return emptyList()
        return try {
            val json = module.callAttr("get_memory_context", query, limit).toString()
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getMemoryContext failed", e)
            emptyList()
        }
    }

    /**
     * Get all stored memories.
     *
     * @return Complete list of all memory entries.
     */
    fun getAllMemories(): List<MemoryResult> {
        val module = pyModule ?: return emptyList()
        return try {
            val json = module.callAttr("get_all_memories").toString()
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val rawList: List<Map<String, Any>> = gson.fromJson(json, type)
            rawList.map { map ->
                MemoryResult(
                    id = (map["id"] as? String) ?: "",
                    content = (map["content"] as? String) ?: "",
                    type = (map["type"] as? String) ?: "general",
                    score = (map["score"] as? Double) ?: 0.5,
                    timestamp = (map["timestamp"] as? String) ?: "",
                    retrievalCount = ((map["retrieval_count"] as? Double)?.toInt()) ?: 0,
                    helpfulCount = ((map["helpful_count"] as? Double)?.toInt()) ?: 0,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAllMemories failed", e)
            emptyList()
        }
    }

    /**
     * Delete a single memory by ID.
     *
     * @param id The memory fact ID (integer as string).
     * @return True if deleted.
     */
    fun deleteMemory(id: String): Boolean {
        val module = pyModule ?: return false
        return try {
            module.callAttr("delete_memory", id).toBoolean()
        } catch (e: Exception) {
            Log.e(TAG, "deleteMemory failed", e)
            false
        }
    }

    /**
     * Delete all stored memories.
     *
     * @return True on success.
     */
    fun clearMemories(): Boolean {
        val module = pyModule ?: return false
        return try {
            module.callAttr("clear_memories").toBoolean()
        } catch (e: Exception) {
            Log.e(TAG, "clearMemories failed", e)
            false
        }
    }

    /**
     * Get memory system statistics.
     *
     * @return MemoryStats with total entries, type breakdown, and DB file size.
     */
    fun getMemoryStats(): MemoryStats {
        val module = pyModule ?: return MemoryStats()
        return try {
            val json = module.callAttr("get_memory_stats").toString()
            gson.fromJson(json, MemoryStats::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "getMemoryStats failed", e)
            MemoryStats()
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun parseResult(json: String): MessageResult {
        return try {
            gson.fromJson(json, MessageResult::class.java)
        } catch (e: Exception) {
            MessageResult(ok = false, content = "", error = "Failed to parse result: $json")
        }
    }

    /**
     * Check if the Hermes Bridge Python runtime is active.
     */
    fun isStarted(): Boolean = started.get()
}

// ── Stream event sealed class ──────────────────────────────────────────

/**
 * Events emitted by [HermesBridge.sendMessage] during streaming.
 *
 * Consumers should handle each variant:
 *   - [Delta]: a text chunk received during streaming.
 *   - [Complete]: the final assembled result.
 *   - [Error]: a fatal error that terminated the stream.
 */
sealed class StreamEvent {
    data class Delta(val text: String) : StreamEvent()
    data class Complete(val result: HermesBridge.MessageResult) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
