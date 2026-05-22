package com.gusogst.chat.data

/**
 * Agent system type definitions - mirrors Web shared/agent-types.ts
 */

// --- Message (OpenAI format) ---
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: ToolFunction
)

data class ToolFunction(
    val name: String,
    val arguments: String
)

data class Message(
    val id: String? = null,
    val role: String, // system / user / assistant / tool
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null
)

// --- Agent Config ---
data class AgentConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "",
    val tools: List<ToolDef>? = null,
    val mcpServers: List<MCPServerConfig>? = null
)

data class ToolDef(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>? = null
)

data class MCPServerConfig(
    val name: String,
    val url: String,
    val headers: Map<String, String>? = null
)

// --- Stream Events ---
sealed class StreamEvent {
    data class TextDelta(val data: String) : StreamEvent()
    data class Thinking(val data: String) : StreamEvent()
    data class ToolUse(val tool: String, val input: String, val id: String? = null) : StreamEvent()
    data class ToolResult(val tool: String, val output: String, val id: String? = null, val isError: Boolean = false) : StreamEvent()
    data class Error(val data: String) : StreamEvent()
    data class Done(val message: Message? = null) : StreamEvent()
}
