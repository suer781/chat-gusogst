package com.gusogst.chat.data.mcp

// ═══════════════════════════════════════════════
// MCP 管理器
// 移植自 main 分支 mcp/manager.ts
// ═══════════════════════════════════════════════

import com.gusogst.chat.model.MCPServerConfig
import com.gusogst.chat.model.MCPToolResult
import com.gusogst.chat.model.ToolDefinition
import com.gusogst.chat.model.ToolFunctionDef
import com.google.gson.JsonObject
import kotlinx.coroutines.*

class MCPManager {
    private val clients = mutableMapOf<String, MCPClient>()
    private val toolToServer = mutableMapOf<String, String>()
    private val serverConfigs = mutableMapOf<String, MCPServerConfig>()

    // ─── 注册服务器配置 ───
    fun registerServer(name: String, config: MCPServerConfig) {
        serverConfigs[name] = config
    }

    // ─── 连接所有服务器 ───
    suspend fun connectAll() = coroutineScope {
        serverConfigs.map { (name, config) ->
            async(Dispatchers.IO) {
                try {
                    val client = MCPClient(config)
                    val success = client.connect()
                    if (success) {
                        clients[name] = client
                        // 建立工具到服务器的映射
                        for (tool in client.getToolDefinitions()) {
                            toolToServer[tool.name] = name
                        }
                    }
                    name to success
                } catch (e: Exception) {
                    name to false
                }
            }
        }.awaitAll()
    }

    // ─── 断开所有 ───
    fun disconnectAll() {
        clients.values.forEach { it.disconnect() }
        clients.clear()
        toolToServer.clear()
    }

    // ─── 断开指定服务器 ───
    fun disconnectServer(name: String) {
        clients[name]?.disconnect()
        clients.remove(name)
        toolToServer.entries.removeAll { it.value == name }
    }

    // ─── 调用工具 ───
    suspend fun callTool(toolName: String, args: Map<String, Any>): MCPToolResult {
        val serverName = toolToServer[toolName]
            ?: return MCPToolResult(content = "Tool not found: $toolName", isError = true)

        val client = clients[serverName]
            ?: return MCPToolResult(content = "Server not connected: $serverName", isError = true)

        return client.callTool(toolName, args)
    }

    // ─── 获取所有 MCP 工具定义 (用于发送给 LLM) ───
    fun getToolDefinitions(): List<ToolDefinition> {
        return clients.values.flatMap { client ->
            client.getToolDefinitions().map { mcpTool ->
                ToolDefinition(
                    function = ToolFunctionDef(
                        name = mcpTool.name,
                        description = mcpTool.description,
                        parameters = mcpTool.inputSchema
                    )
                )
            }
        }
    }

    // ─── 查询 ───
    fun isToolMCP(toolName: String): Boolean = toolToServer.containsKey(toolName)
    fun getToolNames(): List<String> = toolToServer.keys.toList()
    fun getConnectedServers(): List<String> = clients.keys.toList()
    fun getServerStatus(): Map<String, Boolean> = serverConfigs.mapValues { (name, _) ->
        clients[name]?.isConnected() ?: false
    }
}