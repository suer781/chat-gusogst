package com.gusogst.chat.network

// ═══════════════════════════════════════════════
// Agent 核心引擎
// 移植自 main 分支 core/agent.ts
// 封装完整的 chat 流程：上下文构建 → 工具调用 → 记忆
// ═══════════════════════════════════════════════

import com.gusogst.chat.data.PersonaManager
import com.gusogst.chat.data.ToolRegistry
import com.gusogst.chat.data.mcp.MCPManager
import com.gusogst.chat.data.memory.MemoryManager
import com.gusogst.chat.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.util.UUID

// ─── Token 估算 ───
private fun estimateTokens(text: String): Int {
    val chinese = text.count { it.code in 0x4e00..0x9fff }
    val other = text.length - chinese
    return chinese + (other / 4)
}

class AgentEngine(
    private val memoryManager: MemoryManager? = null,
    private val toolRegistry: ToolRegistry,
    private val mcpManager: MCPManager = MCPManager()
) {
    companion object {
        const val MAX_MESSAGES = 100           // 硬上限：消息数
        const val CHAR_LIMIT_RATIO = 1.5       // 字符上限 = maxTokens * 1.5
        const val MEMORY_EXTRACT_INTERVAL = 10 // 每10轮提取记忆
        const val MAX_TOOL_DEPTH = 10          // 工具调用最大递归深度
        const val DEFAULT_MAX_HISTORY_TOKENS = 32000
    }

    private var chatRoundCount = 0

    // ═══════════════════════════════════════════
    // 主入口：发送消息，返回事件流
    // ═══════════════════════════════════════════

    fun sendMessage(
        content: String,
        history: MutableList<Message>,
        config: ModelConfig,
        personaId: String? = null,
        maxHistoryTokens: Int = DEFAULT_MAX_HISTORY_TOKENS
    ): Flow<AgentEvent> = flow {
        // 1. 添加用户消息
        if (content.isNotEmpty()) {
            history.add(Message(role = MessageRole.USER, content = content))
        }

        // 2. 裁剪历史
        truncateHistory(history, maxHistoryTokens)

        // 3. 构建上下文
        val context = buildContext(history, personaId)

        // 4. 获取工具定义
        val tools = getToolDefinitions()

        // 5. 调用 LLM
        try {
            val provider = createProvider(config)
            val response = provider.chat(context, config, if (tools.isEmpty()) null else tools)

            // 6. 处理工具调用
            if (response.toolCalls != null && response.toolCalls.isNotEmpty()) {
                handleToolCalls(response, history, config, personaId, maxHistoryTokens, 0)
                    .collect { emit(it) }
            } else {
                // 普通文本回复
                history.add(response)
                emit(AgentEvent.Done(response))

                // 7. 定期提取记忆
                chatRoundCount++
                if (chatRoundCount % MEMORY_EXTRACT_INTERVAL == 0) {
                    extractMemory(history)
                }
            }
        } catch (e: Exception) {
            emit(AgentEvent.Error(e.message ?: "Unknown error"))
        }
    }

    // ═══════════════════════════════════════════
    // 流式入口
    // ═══════════════════════════════════════════

    fun sendMessageStream(
        content: String,
        history: MutableList<Message>,
        config: ModelConfig,
        personaId: String? = null,
        maxHistoryTokens: Int = DEFAULT_MAX_HISTORY_TOKENS
    ): Flow<AgentEvent> = flow {
        // 1. 添加用户消息
        if (content.isNotEmpty()) {
            history.add(Message(role = MessageRole.USER, content = content))
        }

        // 2. 裁剪历史
        truncateHistory(history, maxHistoryTokens)

        // 3. 构建上下文
        val context = buildContext(history, personaId)

        // 4. 获取工具定义
        val tools = getToolDefinitions()

        // 5. 流式调用 LLM
        try {
            val provider = createProvider(config)
            val fullContent = StringBuilder()

            provider.chatStream(context, config, if (tools.isEmpty()) null else tools)
                .collect { token ->
                    fullContent.append(token)
                    emit(AgentEvent.Token(token))
                }

            // 流结束后，需要非流式调用获取 tool_calls
            // (SSE 流中 tool_calls 解析复杂，先用非流式兜底)
            val response = provider.chat(context, config, if (tools.isEmpty()) null else tools)

            if (response.toolCalls != null && response.toolCalls.isNotEmpty()) {
                handleToolCalls(response, history, config, personaId, maxHistoryTokens, 0)
                    .collect { emit(it) }
            } else {
                val finalMsg = response.copy(content = fullContent.toString())
                history.add(finalMsg)
                emit(AgentEvent.Done(finalMsg))

                chatRoundCount++
                if (chatRoundCount % MEMORY_EXTRACT_INTERVAL == 0) {
                    extractMemory(history)
                }
            }
        } catch (e: Exception) {
            emit(AgentEvent.Error(e.message ?: "Unknown error"))
        }
    }

    // ═══════════════════════════════════════════
    // 工具调用递归处理
    // ═══════════════════════════════════════════

    private fun handleToolCalls(
        response: Message,
        history: MutableList<Message>,
        config: ModelConfig,
        personaId: String?,
        maxHistoryTokens: Int,
        depth: Int
    ): Flow<AgentEvent> = flow {
        if (depth >= MAX_TOOL_DEPTH) {
            emit(AgentEvent.Error("Tool call depth limit reached ($MAX_TOOL_DEPTH)"))
            return@flow
        }

        // 添加 assistant 消息（带 tool_calls）
        history.add(response)

        // 逐个执行工具
        for (tc in response.toolCalls!!) {
            emit(AgentEvent.ToolCall(tc.id, tc.function.name, tc.function.arguments))

            val result = executeTool(tc.function.name, tc.id, tc.function.arguments)
            emit(AgentEvent.ToolResult(tc.id, tc.function.name, result))

            // 添加工具结果消息
            history.add(Message(
                role = MessageRole.TOOL,
                content = result,
                toolCallId = tc.id,
                name = tc.function.name
            ))
        }

        // 递归调用 LLM
        val context = buildContext(history, personaId)
        val tools = getToolDefinitions()
        val provider = createProvider(config)

        try {
            val nextResponse = provider.chat(context, config, if (tools.isEmpty()) null else tools)

            if (nextResponse.toolCalls != null && nextResponse.toolCalls.isNotEmpty()) {
                handleToolCalls(nextResponse, history, config, personaId, maxHistoryTokens, depth + 1)
                    .collect { emit(it) }
            } else {
                history.add(nextResponse)
                emit(AgentEvent.Done(nextResponse))
            }
        } catch (e: Exception) {
            emit(AgentEvent.Error(e.message ?: "Tool call recursion error"))
        }
    }

    // ═══════════════════════════════════════════
    // 工具执行 (先查 MCP，再查内置)
    // ═══════════════════════════════════════════

    private suspend fun executeTool(name: String, callId: String, argsJson: String): String {
        // 1. MCP 工具
        if (mcpManager.isToolMCP(name)) {
            val args = try {
                com.google.gson.JsonParser.parseString(argsJson).asJsonObject
                    .entrySet().associate { (k, v) ->
                        k to when {
                            v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asNumber
                            v.isJsonPrimitive && v.asJsonPrimitive.isBoolean -> v.asBoolean
                            else -> v.asString
                        }
                    }
            } catch (e: Exception) { emptyMap() }
            val result = mcpManager.callTool(name, args)
            return result.content
        }

        // 2. 内置工具
        return toolRegistry.execute(name, callId, argsJson)
    }

    // ═══════════════════════════════════════════
    // 获取所有工具定义
    // ═══════════════════════════════════════════

    private fun getToolDefinitions(): List<ToolDefinition> {
        val builtin = toolRegistry.getDefinitions()
        val mcp = mcpManager.getToolDefinitions()
        return builtin + mcp
    }

    // ═══════════════════════════════════════════
    // 历史裁剪
    // ═══════════════════════════════════════════

    private fun truncateHistory(history: MutableList<Message>, maxTokens: Int) {
        // 硬上限
        while (history.size > MAX_MESSAGES) {
            val removed = history.removeAt(0)
            if (history.isNotEmpty()) {
                val summary = "[Earlier: ${removed.content.take(50)}...]"
                // 如果第一消息不是系统消息，插入摘要
                if (history[0].role != MessageRole.SYSTEM) {
                    history.add(0, Message(role = MessageRole.ASSISTANT, content = summary))
                }
            }
        }

        // 字符上限
        val charLimit = (maxTokens * CHAR_LIMIT_RATIO).toInt()
        var totalChars = history.sumOf { it.content.length }

        while (totalChars > charLimit && history.size > 1) {
            // 找到第一个非系统消息移除
            val idx = history.indexOfFirst { it.role != MessageRole.SYSTEM }
            if (idx >= 0) {
                totalChars -= history[idx].content.length
                history.removeAt(idx)
            } else break
        }
    }

    // ═══════════════════════════════════════════
    // 上下文构建
    // ═══════════════════════════════════════════

    private fun buildContext(history: List<Message>, personaId: String?): List<Message> {
        val context = mutableListOf<Message>()

        // 1. 系统提示 (persona)
        val persona = if (personaId != null) {
            PersonaManager.getById(personaId)
        } else {
            PersonaManager.getActive()
        }

        if (persona != null && persona.systemPrompt.isNotEmpty()) {
            val tools = getToolDefinitions()
            var systemPrompt = persona.systemPrompt

            // 附加可用工具列表
            if (tools.isNotEmpty()) {
                val toolList = tools.joinToString("\n") { t ->
                    "- ${t.function.name}: ${t.function.description}"
                }
                systemPrompt += "\n\nYou have access to these tools:\n$toolList"
            }

            context.add(Message(role = MessageRole.SYSTEM, content = systemPrompt))
        }

        // 2. 历史消息
        context.addAll(history.filter { it.role != MessageRole.SYSTEM })

        return context
    }

    // ═══════════════════════════════════════════
    // Provider 创建
    // ═══════════════════════════════════════════

    private fun createProvider(config: ModelConfig): ProviderAdapter {
        return when {
            config.provider.contains("anthropic", ignoreCase = true) ||
            config.model.contains("claude", ignoreCase = true) -> AnthropicProvider()
            else -> OpenAIProvider()
        }
    }

    // ═══════════════════════════════════════════
    // 记忆提取
    // ═══════════════════════════════════════════

    private suspend fun extractMemory(history: List<Message>) {
        val recentUserMessages = history
            .filter { it.role == MessageRole.USER }
            .takeLast(5)
            .joinToString("\n") { it.content }

        if (recentUserMessages.isNotEmpty()) {
            memoryManager?.extractAndSave(listOf(recentUserMessages))
        }
    }

    // ═══════════════════════════════════════════
    // 查询记忆 (用于 RAG 注入)
    // ═══════════════════════════════════════════

    suspend fun queryMemory(query: String, limit: Int = 3): List<String> {
        return memoryManager?.searchMemory(query, limit)?.map { it.content } ?: emptyList()
    }
}