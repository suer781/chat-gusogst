package com.gusogst.chat.data

// ═══════════════════════════════════════════════
// 工具注册与执行系统
// 移植自 main 分支 tools/registry.ts
// ═══════════════════════════════════════════════

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gusogst.chat.data.memory.MemoryManager
import com.gusogst.chat.model.*
import java.text.SimpleDateFormat
import java.util.*

typealias ToolHandler = suspend (callId: String, args: Map<String, Any>) -> Any

class ToolRegistry {
    private val tools = mutableMapOf<String, ToolDefinition>()
    private val handlers = mutableMapOf<String, ToolHandler>()
    var memoryManager: MemoryManager? = null
    private val gson = Gson()

    init {
        registerBuiltinTools()
    }

    // ─── 注册工具 ───
    fun register(definition: ToolDefinition, handler: ToolHandler) {
        tools[definition.function.name] = definition
        handlers[definition.function.name] = handler
    }

    // ─── 执行工具 ───
    suspend fun execute(name: String, callId: String, argsJson: String): String {
        val handler = handlers[name]
        if (handler == null) {
            return gson.toJson(mapOf("error" to "Tool not found: $name"))
        }

        return try {
            val args = parseArgs(argsJson)
            val result = handler(callId, args)
            when (result) {
                is String -> result
                else -> gson.toJson(result)
            }
        } catch (e: Exception) {
            gson.toJson(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    // ─── 获取所有工具定义 (用于发送给 LLM) ───
    fun getDefinitions(): List<ToolDefinition> {
        return tools.values.toList()
    }

    // ─── 注册内置工具 ───
    private fun registerBuiltinTools() {
        // 1. get_current_time
        register(
            ToolDefinition(
                function = ToolFunctionDef(
                    name = "get_current_time",
                    description = "Get the current date and time",
                    parameters = JsonObject().apply {
                        addProperty("type", "object")
                        add("properties", JsonObject())
                    }
                )
            )
        ) { _, _ ->
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss EEEE", Locale.getDefault())
            mapOf("datetime" to sdf.format(Date()))
        }

        // 2. memory_save
        register(
            ToolDefinition(
                function = ToolFunctionDef(
                    name = "memory_save",
                    description = "Save a memory entry for long-term recall",
                    parameters = JsonObject().apply {
                        addProperty("type", "object")
                        add("properties", JsonObject().apply {
                            add("content", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "The memory content to save")
                            })
                            add("type", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "Memory type: fact, preference, context, instruction")
                            })
                            add("importance", JsonObject().apply {
                                addProperty("type", "number")
                                addProperty("description", "Importance 0-1, default 0.5")
                            })
                        })
                        add("required", com.google.gson.JsonArray().apply { add("content") })
                    }
                )
            )
        ) { _, args ->
            val content = args["content"] as? String ?: return@register mapOf("error" to "content required")
            val type = args["type"] as? String ?: "fact"
            val importance = (args["importance"] as? Number)?.toFloat() ?: 0.5f
            memoryManager?.saveMemory(content, type, importance)
            mapOf("success" to true, "content" to content)
        }

        // 3. memory_search
        register(
            ToolDefinition(
                function = ToolFunctionDef(
                    name = "memory_search",
                    description = "Search memories by query",
                    parameters = JsonObject().apply {
                        addProperty("type", "object")
                        add("properties", JsonObject().apply {
                            add("query", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "Search query")
                            })
                            add("limit", JsonObject().apply {
                                addProperty("type", "integer")
                                addProperty("description", "Max results, default 3")
                            })
                        })
                        add("required", com.google.gson.JsonArray().apply { add("query") })
                    }
                )
            )
        ) { _, args ->
            val query = args["query"] as? String ?: return@register mapOf("error" to "query required")
            val limit = (args["limit"] as? Number)?.toInt() ?: 3
            val results = memoryManager?.searchMemory(query, limit) ?: emptyList()
            mapOf("results" to results.map { it.content })
        }

        // 4. calculator
        register(
            ToolDefinition(
                function = ToolFunctionDef(
                    name = "calculator",
                    description = "Evaluate a math expression",
                    parameters = JsonObject().apply {
                        addProperty("type", "object")
                        add("properties", JsonObject().apply {
                            add("expression", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "Math expression to evaluate")
                            })
                        })
                        add("required", com.google.gson.JsonArray().apply { add("expression") })
                    }
                )
            )
        ) { _, args ->
            val expr = args["expression"] as? String ?: return@register mapOf("error" to "expression required")
            try {
                val result = evaluateSimpleMath(expr)
                mapOf("result" to result)
            } catch (e: Exception) {
                mapOf("error" to "Failed to evaluate: ${e.message}")
            }
        }
    }

    // ─── 简单数学求值 (安全,无eval) ───
    private fun evaluateSimpleMath(expr: String): Double {
        val cleaned = expr.replace(" ", "")
        // 只支持基本四则运算
        val tokens = mutableListOf<String>()
        var current = ""
        for (c in cleaned) {
            if (c in "+-*/()") {
                if (current.isNotEmpty()) tokens.add(current)
                tokens.add(c.toString())
                current = ""
            } else {
                current += c
            }
        }
        if (current.isNotEmpty()) tokens.add(current)

        return try {
            evaluateTokens(tokens.iterator()).toDouble()
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot evaluate: $expr")
        }
    }

    private fun evaluateTokens(tokens: Iterator<String>): Int {
        val numbers = mutableListOf<Int>()
        val ops = mutableListOf<String>()

        while (tokens.hasNext()) {
            val token = tokens.next()
            when {
                token.toIntOrNull() != null -> numbers.add(token.toInt())
                token in "+-*/" -> ops.add(token)
            }
        }

        var result = numbers.getOrElse(0) { 0 }
        for (i in ops.indices) {
            val b = numbers.getOrElse(i + 1) { 0 }
            when (ops[i]) {
                "+" -> result += b
                "-" -> result -= b
                "*" -> result *= b
                "/" -> result = if (b != 0) result / b else 0
            }
        }
        return result
    }

    private fun parseArgs(argsJson: String): Map<String, Any> {
        return try {
            val json = JsonParser.parseString(argsJson).asJsonObject
            json.entrySet().associate { (k, v) ->
                k to when {
                    v.isJsonPrimitive && v.asJsonPrimitive.isNumber -> v.asNumber
                    v.isJsonPrimitive && v.asJsonPrimitive.isBoolean -> v.asBoolean
                    else -> v.asString
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}