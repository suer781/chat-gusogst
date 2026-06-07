package com.gusogst.chat.network

// ═══════════════════════════════════════════════
// Anthropic Claude 适配器
// 移植自 main 分支 providers/anthropic.ts
// ═══════════════════════════════════════════════

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gusogst.chat.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class AnthropicProvider : ProviderAdapter {
    override val name = "anthropic"

    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // ─── 非流式聊天 ───
    override suspend fun chat(
        messages: List<Message>,
        config: ModelConfig,
        tools: List<ToolDefinition>?
    ): Message = withContext(Dispatchers.IO) {
        val (systemPrompt, convertedMessages) = convertMessages(messages)
        val body = buildRequestBody(convertedMessages, config, tools, systemPrompt, stream = false)

        val request = Request.Builder()
            .url("${config.apiHost}/v1/messages")
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw IOException("Anthropic API error: ${response.code} - $errorBody")
        }

        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        parseResponse(responseBody)
    }

    // ─── 流式聊天 (伪流式: 先请求再逐字 yield) ───
    override fun chatStream(
        messages: List<Message>,
        config: ModelConfig,
        tools: List<ToolDefinition>?
    ): Flow<String> = flow {
        val msg = chat(messages, config, tools)
        for (char in msg.content) {
            emit(char.toString())
        }
    }.flowOn(Dispatchers.IO)

    // ─── 消息格式转换 ───
    // Anthropic 要求 system 消息单独提取，tool 结果转为 user 消息
    private fun convertMessages(messages: List<Message>): Pair<String?, List<Message>> {
        var systemPrompt: String? = null
        val converted = mutableListOf<Message>()

        for (msg in messages) {
            when (msg.role) {
                MessageRole.SYSTEM -> {
                    systemPrompt = msg.content
                }
                MessageRole.TOOL -> {
                    // Anthropic 要求 tool 结果放在 user 消息中
                    converted.add(Message(
                        role = MessageRole.USER,
                        content = "",
                        toolCalls = null,
                        toolCallId = null
                    ))
                    // 使用特殊格式包装 tool 结果
                    converted.add(Message(
                        role = MessageRole.USER,
                        content = buildToolResultContent(msg.toolCallId ?: "", msg.name ?: "", msg.content)
                    ))
                }
                else -> converted.add(msg)
            }
        }

        return systemPrompt to converted
    }

    private fun buildToolResultContent(toolCallId: String, name: String, content: String): String {
        return """<tool_result>
<tool_call_id>$toolCallId</tool_call_id>
<tool_name>$name</tool_name>
<result>$content</result>
</tool_result>"""
    }

    // ─── 构建请求体 ───
    private fun buildRequestBody(
        messages: List<Message>,
        config: ModelConfig,
        tools: List<ToolDefinition>?,
        systemPrompt: String?,
        stream: Boolean
    ): JsonObject {
        val body = JsonObject()
        body.addProperty("model", config.model)
        body.addProperty("max_tokens", config.maxTokens)
        body.addProperty("stream", stream)
        config.topP?.let { body.addProperty("top_p", it) }

        // system prompt
        if (systemPrompt != null) {
            body.addProperty("system", systemPrompt)
        }

        // 消息
        val msgs = JsonArray()
        for (msg in messages) {
            val msgObj = JsonObject()
            msgObj.addProperty("role", msg.role.name.lowercase())

            if (msg.toolCalls != null) {
                // assistant 消息带 tool_calls
                val contentArray = JsonArray()
                for (tc in msg.toolCalls) {
                    val toolUse = JsonObject()
                    toolUse.addProperty("type", "tool_use")
                    toolUse.addProperty("id", tc.id)
                    toolUse.addProperty("name", tc.function.name)
                    toolUse.add("input", JsonParser.parseString(tc.function.arguments).asJsonObject)
                    contentArray.add(toolUse)
                }
                if (msg.content.isNotEmpty()) {
                    val textBlock = JsonObject()
                    textBlock.addProperty("type", "text")
                    textBlock.addProperty("text", msg.content)
                    contentArray.add(0, textBlock)
                }
                msgObj.add("content", contentArray)
            } else {
                msgObj.addProperty("content", msg.content)
            }
            msgs.add(msgObj)
        }
        body.add("messages", msgs)

        // 工具定义
        if (tools != null && tools.isNotEmpty()) {
            val toolsArray = JsonArray()
            for (tool in tools) {
                val toolObj = JsonObject()
                toolObj.addProperty("name", tool.function.name)
                toolObj.addProperty("description", tool.function.description)
                toolObj.add("input_schema", tool.function.parameters)
                toolsArray.add(toolObj)
            }
            body.add("tools", toolsArray)
        }

        return body
    }

    // ─── 解析响应 ───
    private fun parseResponse(responseBody: String): Message {
        val json = JsonParser.parseString(responseBody).asJsonObject
        val content = json.getAsJsonArray("content") ?: return Message(
            role = MessageRole.ASSISTANT, content = ""
        )

        var textContent = ""
        val toolCalls = mutableListOf<ToolCall>()

        for (block in content) {
            val blockObj = block.asJsonObject
            when (blockObj.get("type").asString) {
                "text" -> {
                    textContent += blockObj.get("text").asString
                }
                "tool_use" -> {
                    toolCalls.add(ToolCall(
                        id = blockObj.get("id").asString,
                        type = "function",
                        function = ToolCallFunction(
                            name = blockObj.get("name").asString,
                            arguments = blockObj.getAsJsonObject("input").toString()
                        )
                    ))
                }
            }
        }

        return Message(
            role = MessageRole.ASSISTANT,
            content = textContent,
            toolCalls = if (toolCalls.isEmpty()) null else toolCalls
        )
    }
}