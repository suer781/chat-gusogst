package com.gusogst.chat.data

/**
 * Agent 系统类型定义 —— 对应 Web 端的 shared/agent-types.ts
 *
 * 命名约定：
 *   ApiXxx     — API / 网络传输层的类型（与 OpenAI 协议对齐）
 *   com.gusogst.chat.model.Xxx — UI / 业务逻辑层的类型
 *   没有前缀的  — 此文件内通用的辅助类型
 *
 * 为什么不用 Message / ToolCall：
 *   Models.kt 中也有 Message 和 ToolCall（UI 层），同名会导致
 *   import 冲突。AgentBridge 原先靠 typealias UIMessage 绕路，
 *   现在显式用 ApiMessage / ApiToolCall 区分，清晰无歧义。
 */

// --- API 消息 (OpenAI 格式) ---

/** OpenAI API 响应中的 tool_call 结构 */
data class ApiToolCall(
    val id: String,
    val type: String = "function",
    val function: ApiToolFunction
)

data class ApiToolFunction(
    val name: String,
    val arguments: String
)

/** 发送给 API 的消息（与 Models.kt 的 Message 区分，后者是 UI 层类型） */
data class ApiMessage(
    val id: String? = null,
    val role: String, // system / user / assistant / tool
    val content: String,
    val toolCalls: List<ApiToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null
)

// --- Agent 配置 ---

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

// --- 流式事件 (SSE 解析结果) ---

sealed class StreamEvent {
    data class TextDelta(val data: String) : StreamEvent()
    data class Thinking(val data: String) : StreamEvent()
    data class ToolUse(val tool: String, val input: String, val id: String? = null) : StreamEvent()
    data class ToolResult(val tool: String, val output: String, val id: String? = null, val isError: Boolean = false) : StreamEvent()
    data class Error(val data: String) : StreamEvent()
    data class Done(val message: ApiMessage? = null) : StreamEvent()
}
