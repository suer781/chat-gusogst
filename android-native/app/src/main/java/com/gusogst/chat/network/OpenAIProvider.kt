package com.gusogst.chat.network

// ═══════════════════════════════════════════════
// OpenAI 兼容适配器
// 移植自 main 分支 providers/openai.ts
// 支持: OpenAI, DeepSeek, OpenRouter, Moonshot 等
// ═══════════════════════════════════════════════

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
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

interface ProviderAdapter {
    val name: String
    suspend fun chat(messages: List<Message>, config: ModelConfig, tools: List<ToolDefinition>? = null): Message
    fun chatStream(messages: List<Message>, config: ModelConfig, tools: List<ToolDefinition>? = null): Flow<String>
}

class OpenAIProvider : ProviderAdapter {
    override val name = "openai"

    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val streamClient = OkHttpClient.Builder()
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
        val body = buildRequestBody(messages, config, tools, stream = false)
        val request = Request.Builder()
            .url("${config.apiHost}/v1/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw IOException("API request failed: ${response.code} - $errorBody")
        }

        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        parseResponse(responseBody)
    }

    // ─── 流式聊天 ───
    override fun chatStream(
        messages: List<Message>,
        config: ModelConfig,
        tools: List<ToolDefinition>?
    ): Flow<String> = flow {
        val body = buildRequestBody(messages, config, tools, stream = true)
        val request = Request.Builder()
            .url("${config.apiHost}/v1/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON))
            .build()

        val response = streamClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            throw IOException("Stream request failed: ${response.code} - $errorBody")
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (!l.startsWith("data: ")) continue
                val data = l.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                try {
                    val chunk = JsonParser.parseString(data).asJsonObject
                    val choices = chunk.getAsJsonArray("choices") ?: continue
                    if (choices.size() == 0) continue
                    val delta = choices[0].asJsonObject.getAsJsonObject("delta") ?: continue

                    // 普通内容
                    if (delta.has("content") && !delta.get("content").isJsonNull) {
                        emit(delta.get("content").asString)
                    }

                    // 思考内容 (DeepSeek/OpenRouter)
                    if (delta.has("reasoning_content") && !delta.get("reasoning_content").isJsonNull) {
                        // 暂不处理思考内容流
                    }
                } catch (e: Exception) {
                    // 解析失败跳过该行
                }
            }
        } finally {
            reader.close()
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    // ─── 构建请求体 ───
    private fun buildRequestBody(
        messages: List<Message>,
        config: ModelConfig,
        tools: List<ToolDefinition>?,
        stream: Boolean
    ): JsonObject {
        val body = JsonObject()
        body.addProperty("model", config.model)
        body.addProperty("stream", stream)
        config.topP?.let { body.addProperty("top_p", it) }

        // 温度：o1/o3 系列不支持 temperature
        if (!config.model.startsWith("o1") && !config.model.startsWith("o3")) {
            body.addProperty("temperature", config.temperature)
            body.addProperty("max_tokens", config.maxTokens)
        }

        // 消息
        val msgs = com.google.gson.JsonArray()
        for (msg in messages) {
            val msgObj = JsonObject()
            msgObj.addProperty("role", msg.role.name.lowercase())

            if (msg.role == MessageRole.TOOL) {
                msgObj.addProperty("content", msg.content)
                msgObj.addProperty("tool_call_id", msg.toolCallId)
            } else if (msg.toolCalls != null) {
                msgObj.addProperty("content", msg.content.ifEmpty { null })
                val tcArray = com.google.gson.JsonArray()
                for (tc in msg.toolCalls) {
                    val tcObj = JsonObject()
                    tcObj.addProperty("id", tc.id)
                    tcObj.addProperty("type", tc.type)
                    val fnObj = JsonObject()
                    fnObj.addProperty("name", tc.function.name)
                    fnObj.addProperty("arguments", tc.function.arguments)
                    tcObj.add("function", fnObj)
                    tcArray.add(tcObj)
                }
                msgObj.add("tool_calls", tcArray)
            } else {
                msgObj.addProperty("content", msg.content)
                msg.name?.let { msgObj.addProperty("name", it) }
            }
            msgs.add(msgObj)
        }
        body.add("messages", msgs)

        // 工具定义
        if (tools != null && tools.isNotEmpty()) {
            val toolsArray = com.google.gson.JsonArray()
            for (tool in tools) {
                val toolObj = JsonObject()
                toolObj.addProperty("type", tool.type)
                val fnObj = JsonObject()
                fnObj.addProperty("name", tool.function.name)
                fnObj.addProperty("description", tool.function.description)
                fnObj.add("parameters", tool.function.parameters)
                toolObj.add("function", fnObj)
                toolsArray.add(toolObj)
            }
            body.add("tools", toolsArray)
        }

        return body
    }

    // ─── 解析响应 ───
    private fun parseResponse(responseBody: String): Message {
        val json = JsonParser.parseString(responseBody).asJsonObject
        val choices = json.getAsJsonArray("choices")
        if (choices == null || choices.size() == 0) {
            throw IOException("No choices in response")
        }

        val messageObj = choices[0].asJsonObject.getAsJsonObject("message")
        val content = if (messageObj.has("content") && !messageObj.get("content").isJsonNull) {
            messageObj.get("content").asString
        } else ""

        // 解析思考内容
        val thinking = if (messageObj.has("reasoning_content") && !messageObj.get("reasoning_content").isJsonNull) {
            messageObj.get("reasoning_content").asString
        } else null

        // 解析 tool_calls
        val toolCalls = if (messageObj.has("tool_calls") && !messageObj.get("tool_calls").isJsonNull) {
            val tcArray = messageObj.getAsJsonArray("tool_calls")
            tcArray.map { tcElement ->
                val tcObj = tcElement.asJsonObject
                val fnObj = tcObj.getAsJsonObject("function")
                ToolCall(
                    id = tcObj.get("id").asString,
                    type = tcObj.get("type").asString,
                    function = ToolCallFunction(
                        name = fnObj.get("name").asString,
                        arguments = fnObj.get("arguments").asString
                    )
                )
            }
        } else null

        return Message(
            role = MessageRole.ASSISTANT,
            content = content,
            toolCalls = toolCalls,
            thinking = thinking
        )
    }

    // ─── 辅助：估算 token 数 ───
    companion object {
        fun estimateTokens(text: String): Int {
            val chineseChars = text.count { it.code in 0x4e00..0x9fff }
            val otherChars = text.length - chineseChars
            return chineseChars + (otherChars / 4)
        }
    }
}