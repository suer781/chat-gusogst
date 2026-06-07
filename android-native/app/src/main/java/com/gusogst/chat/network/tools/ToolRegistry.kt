package com.gusogst.chat.network.tools

import android.util.Log
import com.gusogst.chat.network.mcp.MCPManager
import com.gusogst.chat.network.mcp.MCPTool
import com.gusogst.chat.network.mcp.MCPToolCallResult
import kotlinx.coroutines.flow.StateFlow

/**
 * 统一工具注册表 — 移植自 main 分支 tools/registry.ts
 * 合并内置工具 + MCP 工具，统一调用接口
 */
class ToolRegistry(
    private val mcpManager: MCPManager? = null
) {
    companion object {
        private const val TAG = "ToolRegistry"
    }

    // 内置工具定义
    data class BuiltinTool(
        val name: String,
        val description: String,
        val parameters: Map<String, ToolParameter> = emptyMap(),
        val handler: suspend (Map<String, Any>) -> ToolResult
    )

    data class ToolParameter(
        val type: String,
        val description: String,
        val required: Boolean = false,
        val enum: List<String>? = null
    )

    data class ToolResult(
        val success: Boolean,
        val content: String,
        val mimeType: String = "text/plain"
    )

    private val builtinTools = mutableMapOf<String, BuiltinTool>()

    // ===== 内置工具注册 =====

    fun registerBuiltin(tool: BuiltinTool) {
        builtinTools[tool.name] = tool
        Log.d(TAG, "Registered builtin tool: ${tool.name}")
    }

    fun registerBuiltins(tools: List<BuiltinTool>) {
        tools.forEach { registerBuiltin(it) }
    }

    // ===== 工具列表 =====

    /**
     * 获取所有可用工具（内置 + MCP），返回 OpenAI function 格式
     */
    fun getToolDefinitions(): List<Map<String, Any>> {
        val tools = mutableListOf<Map<String, Any>>()

        // 内置工具
        builtinTools.values.forEach { tool ->
            tools.add(mapOf(
                "type" to "function",
                "function" to mapOf(
                    "name" to tool.name,
                    "description" to tool.description,
                    "parameters" to mapOf(
                        "type" to "object",
                        "properties" to tool.parameters.mapValues { (_, p) ->
                            buildMap {
                                put("type", p.type)
                                put("description", p.description)
                                p.enum?.let { put("enum", it) }
                            }
                        },
                        "required" to tool.parameters.filter { it.value.required }.keys.toList()
                    )
                )
            ))
        }

        // MCP 工具
        mcpManager?.allTools?.value?.forEach { mcpTool ->
            tools.add(mcpToolToOpenAI(mcpTool))
        }

        return tools
    }

    /**
     * 获取 MCP 工具列表（用于 Gemini/Anthropic 格式）
     */
    fun getMCPToolDefinitions(): List<MCPTool> {
        return mcpManager?.allTools?.value ?: emptyList()
    }

    // ===== 工具调用 =====

    suspend fun callTool(name: String, arguments: Map<String, Any>): ToolResult {
        // 1. 先查内置工具
        val builtin = builtinTools[name]
        if (builtin != null) {
            return try {
                builtin.handler(arguments)
            } catch (e: Exception) {
                Log.e(TAG, "Builtin tool error: ${e.message}")
                ToolResult(false, "Tool execution failed: ${e.message}")
            }
        }

        // 2. 再查 MCP 工具
        if (mcpManager != null) {
            val mcpResult = mcpManager.callTool(name, arguments)
            return if (mcpResult.isSuccess) {
                val result = mcpResult.getOrNull()!!
                val text = result.content.joinToString("\n") { it.text ?: it.data ?: "" }
                val err = result.component2(); ToolResult(!err, text)
            } else {
                ToolResult(false, "MCP tool error: ${mcpResult.exceptionOrNull()?.message}")
            }
        }

        return ToolResult(false, "Tool not found: $name")
    }

    // ===== 工具搜索 =====

    fun searchTools(query: String): List<Map<String, Any>> {
        val queryLower = query.lowercase()
        return getToolDefinitions().filter { tool ->
            val func = tool["function"] as? Map<*, *> ?: return@filter false
            val name = (func["name"] as? String ?: "").lowercase()
            val desc = (func["description"] as? String ?: "").lowercase()
            queryLower in name || queryLower in desc
        }
    }

    // ===== 工具统计 =====

    fun getToolCount(): Pair<Int, Int> {
        val builtinCount = builtinTools.size
        val mcpCount = mcpManager?.allTools?.value?.size ?: 0
        return builtinCount to mcpCount
    }

    fun hasTool(name: String): Boolean {
        return builtinTools.containsKey(name) || (mcpManager?.findTool(name) != null)
    }

    // ===== 内置工具预注册 =====

    fun registerDefaultTools() {
        registerBuiltins(listOf(
            BuiltinTool(
                name = "web_search",
                description = "Search the web for information",
                parameters = mapOf(
                    "query" to ToolParameter("string", "Search query", required = true)
                ),
                handler = { args ->
                    val query = args["query"] as? String ?: ""
                    ToolResult(true, "Search results for: $query (web_search not yet implemented)")
                }
            ),
            BuiltinTool(
                name = "get_current_time",
                description = "Get the current date and time",
                parameters = emptyMap(),
                handler = {
                    ToolResult(true, java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date()))
                }
            ),
            BuiltinTool(
                name = "calculate",
                description = "Evaluate a mathematical expression",
                parameters = mapOf(
                    "expression" to ToolParameter("string", "Math expression to evaluate", required = true)
                ),
                handler = { args ->
                    val expr = args["expression"] as? String ?: ""
                    ToolResult(true, "Expression: $expr (calculator not yet implemented)")
                }
            )
        ))
        Log.i(TAG, "Registered ${builtinTools.size} default tools")
    }

    // ===== 工具格式转换 =====

    private fun mcpToolToOpenAI(tool: MCPTool): Map<String, Any> {
        return mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to tool.name,
                "description" to tool.description,
                "parameters" to mapOf(
                    "type" to tool.inputSchema.type,
                    "properties" to tool.inputSchema.properties.mapValues { (_, p) ->
                        buildMap {
                            put("type", p.type)
                            put("description", p.description)
                            p.enum?.let { put("enum", it) }
                        }
                    },
                    "required" to tool.inputSchema.required
                )
            )
        )
    }

    // ===== 清理 =====

    fun clear() {
        builtinTools.clear()
    }
}
