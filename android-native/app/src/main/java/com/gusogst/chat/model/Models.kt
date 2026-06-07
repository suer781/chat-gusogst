package com.gusogst.chat.model

// ═══════════════════════════════════════════════
// gusogst Android - 完整类型系统
// 移植自 main 分支 agent-types.ts + shared/types.ts
// ═══════════════════════════════════════════════

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// ───────── 消息相关 ─────────

enum class MessageRole {
    @SerializedName("system") SYSTEM,
    @SerializedName("user") USER,
    @SerializedName("assistant") ASSISTANT,
    @SerializedName("tool") TOOL
}

data class Message(
    val role: MessageRole,
    val content: String,
    @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val thinking: String? = null  // 思考内容 (Claude/OpenRouter)
)

data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

data class ToolCallFunction(
    val name: String,
    val arguments: String
)

data class ToolResult(
    @SerializedName("tool_call_id") val toolCallId: String,
    val name: String,
    val content: String,
    @SerializedName("is_error") val isError: Boolean = false
)

// ───────── 配置 ─────────

data class ModelConfig(
    val provider: String,
    val model: String,
    @SerializedName("api_key") val apiKey: String,
    @SerializedName("api_host") val apiHost: String = "",
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    @SerializedName("top_p") val topP: Float? = null
)

data class AgentConfig(
    val model: ModelConfig,
    val persona: PersonaConfig? = null,
    val provider: String? = null,
    val memory: MemoryConfig = MemoryConfig(),
    @SerializedName("mcp_servers") val mcpServers: Map<String, MCPServerConfig>? = null,
    val search: SearchConfig = SearchConfig(),
    @SerializedName("max_history_tokens") val maxHistoryTokens: Int = 32000
)

data class SearchConfig(
    val engine: String = "builtin",
    @SerializedName("tavily_api_key") val tavilyApiKey: String? = null
)

data class MemoryConfig(
    val enabled: Boolean = true
)

data class MCPServerConfig(
    val name: String = "",
    val url: String,
    val headers: Map<String, String>? = null,
    val enabled: Boolean = true,
    val timeout: Int = 30
)

data class PersonaConfig(
    val id: String = "",
    val name: String = "",
    @SerializedName("system_prompt") val systemPrompt: String = "",
    val avatar: String? = null,
    val emoji: String? = null,
    val tags: List<String>? = null
)

// ───────── Agent 事件流 ─────────

sealed class AgentEvent {
    data class Token(val content: String) : AgentEvent()
    data class Thinking(val content: String) : AgentEvent()
    data class ToolCall(val id: String, val name: String, val arguments: String) : AgentEvent()
    data class ToolResult(val toolCallId: String, val name: String, val content: String) : AgentEvent()
    data class Error(val message: String, val provider: String? = null) : AgentEvent()
    data class Done(val message: Message) : AgentEvent()
}

// ───────── Provider 适配器 ─────────

data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunctionDef
)

data class ToolFunctionDef(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

// ───────── Persona ─────────

data class Persona(
    val id: String,
    val name: String,
    @SerializedName("system_prompt") val systemPrompt: String,
    val avatar: String? = null,
    val emoji: String? = null,
    val tags: List<String>? = null,
    @SerializedName("is_default") val isDefault: Boolean = false,
    @SerializedName("built_in") val builtIn: Boolean = false,
    val personality: String? = null,
    @SerializedName("model_params_config") val modelParamsConfig: ModelParamsConfig? = null
)

data class ModelParamsConfig(
    val temperature: Float? = null,
    @SerializedName("max_tokens") val maxTokens: Int? = null,
    @SerializedName("top_p") val topP: Float? = null
)

// ───────── 记忆 ─────────

data class MemoryEntry(
    val id: String,
    val content: String,
    val type: String,  // fact, preference, context, instruction
    val importance: Float = 0.5f,
    @SerializedName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("last_accessed") val lastAccessed: Long = System.currentTimeMillis(),
    @SerializedName("access_count") val accessCount: Int = 0,
    val tags: List<String> = emptyList(),
    val embedding: List<Float>? = null
)

// ───────── Provider 定义 (UI用) ─────────

data class ProviderDef(
    val id: String,
    val name: String,
    val transport: String = "https",  // https, http, ws
    @SerializedName("api_key_env_vars") val apiKeyEnvVars: List<String>? = null,
    @SerializedName("base_url") val baseUrl: String? = null,
    @SerializedName("auth_type") val authType: String? = null,  // bearer, api-key, custom
    @SerializedName("is_aggregator") val isAggregator: Boolean = false,
    val aliases: List<String>? = null,
    val doc: String? = null,
    val source: String? = null
)

// ───────── 端点 ─────────

data class EndpointEntry(
    val url: String,
    val providerId: String,
    val name: String,
    val needsApiKey: Boolean = true
)

data class EndpointRating(
    var cumulativeScore: Double = 0.0,
    var sampleCount: Int = 0,
    var chatCount: Int = 0,
    var lastChatAt: Long = System.currentTimeMillis()
) {
    fun quality(): Double {
        return if (sampleCount == 0) 0.5
        else cumulativeScore / (sampleCount * 100.0)
    }
}

data class EndpointSelection(
    val endpoint: String,
    val apiKey: String,
    val providerId: String
)

// ───────── 聊天会话 ─────────

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "New Chat",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val personaId: String? = null,
    val messages: MutableList<Message> = mutableListOf()
)

// ───────── AI 生成选项 ─────────

data class AIGenerationOptions(
    val model: String = "gpt-4o-mini",
    val temperature: Float = 0.3f,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val signal: Boolean = true
)

// ───────── MCP 相关 ─────────

data class MCPToolResult(
    val content: String,
    @SerializedName("is_error") val isError: Boolean = false
)

data class MCPToolDef(
    val name: String,
    val description: String = "",
    val inputSchema: JsonObject = JsonObject()
)

// ───────── Agent 状态 ─────────

enum class AgentState {
    IDLE,
    THINKING,
    STREAMING,
    TOOL_CALLING,
    ERROR
}

/** 人设性格特质（滑块值 0-100） */

// ───────── 会话管理 ─────────

enum class AvatarType {
    emoji, url, builtin
}

data class Conversation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    @SerializedName("persona_id") val personaId: String? = null,
    @SerializedName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updated_at") val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val archived: Boolean = false
)

data class PersonalityTraits(
    val calm: Int = 50,
    val warm: Int = 50,
    val analytical: Int = 50,
    val creative: Int = 50,
    val curious: Int = 50,
    val precise: Int = 50,
    val playful: Int = 50,
    val energetic: Int = 50
)

/** UI 设置 */
data class UISettings(
    // 基础
    val enabled: Boolean = true,
    val themeMode: String = "dark",
    val themeColor: String = "#6200EE",
    val accentColor: String = "#03DAC6",
    val fontSize: Float = 1.0f,
    // 消息显示
    val useBubbles: Boolean = true,
    val quoteAsBubble: Boolean = false,
    val markdownRendering: Boolean = true,
    val markdownRenderingMode: String = "full",
    val longMessageThreshold: Int = 2000,
    val messageCounterEnabled: Boolean = true,
    val resourcePreviewEnabled: Boolean = true,
    // 轮询
    val pollingIntervalSeconds: Int = 10,
    // Agent
    val agentName: String = "助手",
    val systemPrompt: String = "",
    val agentTone: String = "default",
    val agentResponseLength: String = "auto",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f
)

// ===== Provider UI 模型 =====

data class ModelInfo(
    val id: String,
    val name: String = ""
)

data class UIProvider(
    val id: String = "",
    val name: String,
    val type: String = "custom",     // "anthropic" | "openai" | "gemini" | "deepseek" | "custom"
    val baseUrl: String = "",
    val apiKey: String = "",
    val models: List<ModelInfo> = emptyList(),
    val selectedModelId: String? = null,
    val enabled: Boolean = true,
    val lastUpdated: Long = 0L
)
