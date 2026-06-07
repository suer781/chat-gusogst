package com.gusogst.chat.data.mcp

// ═══════════════════════════════════════════════
// MCP 客户端
// 移植自 main 分支 mcp/client.ts
// ═══════════════════════════════════════════════

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gusogst.chat.model.MCPServerConfig
import com.gusogst.chat.model.MCPToolDef
import com.gusogst.chat.model.MCPToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class MCPClient(private val config: MCPServerConfig) {
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private var requestId = 0
    private val tools = mutableListOf<MCPToolDef>()
    private var connected = false

    private val client = OkHttpClient.Builder()
        .connectTimeout(config.timeout.toLong(), TimeUnit.SECONDS)
        .writeTimeout(config.timeout.toLong(), TimeUnit.SECONDS)
        .readTimeout(config.timeout.toLong(), TimeUnit.SECONDS)
        .build()

    // ─── 连接 ───
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 发送 initialize 请求
            val initResult = sendRequest("initialize", JsonObject().apply {
                addProperty("protocolVersion", "2024-11-05")
                add("capabilities", JsonObject())
                add("clientInfo", JsonObject().apply {
                    addProperty("name", "gusogst-android")
                    addProperty("version", "1.0.0")
                })
            })

            // 发送 initialized 通知
            sendNotification("notifications/initialized", JsonObject())

            // 获取工具列表
            val toolsResult = sendRequest("tools/list", JsonObject())
            parseToolsList(toolsResult)

            connected = true
            true
        } catch (e: Exception) {
            connected = false
            false
        }
    }

    fun disconnect() {
        connected = false
        tools.clear()
    }

    fun isConnected(): Boolean = connected

    fun getToolDefinitions(): List<MCPToolDef> = tools.toList()

    // ─── 调用工具 ───
    suspend fun callTool(name: String, args: Map<String, Any>): MCPToolResult = withContext(Dispatchers.IO) {
        try {
            val params = JsonObject().apply {
                addProperty("name", name)
                add("arguments", gson.toJsonTree(args).asJsonObject)
            }
            val result = sendRequest("tools/call", params)
            parseToolResult(result)
        } catch (e: Exception) {
            MCPToolResult(content = e.message ?: "Unknown error", isError = true)
        }
    }

    // ─── 发送 JSON-RPC 请求 ───
    private fun sendRequest(method: String, params: JsonObject): JsonObject {
        val id = ++requestId
        val request = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("id", id)
            addProperty("method", method)
            add("params", params)
        }

        val httpRequest = Request.Builder()
            .url(config.url)
            .apply {
                config.headers?.forEach { (k, v) -> header(k, v) }
            }
            .header("Content-Type", "application/json")
            .post(request.toString().toRequestBody(JSON))
            .build()

        val response = client.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            throw IOException("MCP request failed: ${response.code}")
        }

        val body = response.body?.string() ?: throw IOException("Empty response")
        val json = JsonParser.parseString(body).asJsonObject

        if (json.has("error")) {
            val error = json.getAsJsonObject("error")
            throw IOException("MCP error: ${error.get("message").asString}")
        }

        return json.getAsJsonObject("result") ?: JsonObject()
    }

    // ─── 发送通知 (无 id，无响应) ───
    private fun sendNotification(method: String, params: JsonObject) {
        val notification = JsonObject().apply {
            addProperty("jsonrpc", "2.0")
            addProperty("method", method)
            add("params", params)
        }

        val httpRequest = Request.Builder()
            .url(config.url)
            .apply {
                config.headers?.forEach { (k, v) -> header(k, v) }
            }
            .header("Content-Type", "application/json")
            .post(notification.toString().toRequestBody(JSON))
            .build()

        try {
            client.newCall(httpRequest).execute().close()
        } catch (e: Exception) {
            // 通知失败忽略
        }
    }

    // ─── 解析工具列表 ───
    private fun parseToolsList(result: JsonObject) {
        tools.clear()
        val toolsArray = result.getAsJsonArray("tools") ?: return
        for (toolElement in toolsArray) {
            val toolObj = toolElement.asJsonObject
            tools.add(MCPToolDef(
                name = toolObj.get("name").asString,
                description = if (toolObj.has("description")) toolObj.get("description").asString else "",
                inputSchema = if (toolObj.has("inputSchema")) toolObj.getAsJsonObject("inputSchema") else JsonObject()
            ))
        }
    }

    // ─── 解析工具调用结果 ───
    private fun parseToolResult(result: JsonObject): MCPToolResult {
        val content = result.getAsJsonArray("content")
        if (content != null && content.size() > 0) {
            val textBlock = content[0].asJsonObject
            val text = if (textBlock.has("text")) textBlock.get("text").asString else textBlock.toString()
            val isError = result.has("isError") && result.get("isError").asBoolean
            return MCPToolResult(content = text, isError = isError)
        }
        return MCPToolResult(content = result.toString())
    }
}