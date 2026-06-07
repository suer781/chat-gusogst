# ChatViewModel 接入指南

> 新模块已写好，需要在 ChatViewModel 中接入。以下是具体的代码改动。

---

## 新增依赖

```kotlin
import com.gusogst.chat.network.AgentEngine
import com.gusogst.chat.network.OpenAIProvider
import com.gusogst.chat.network.AnthropicProvider
import com.gusogst.chat.data.PersonaManager
import com.gusogst.chat.data.ToolRegistry
import com.gusogst.chat.data.mcp.MCPManager
import com.gusogst.chat.model.*
```

## 新增成员变量

```kotlin
// 在 ChatViewModel 类中添加
private val toolRegistry = ToolRegistry()
private val mcpManager = MCPManager()
private val agentEngine by lazy {
    AgentEngine(memoryManager, toolRegistry, mcpManager)
}
```

## 改造 callAiApi 方法

**原来的逻辑**：直接用 ApiClient + StreamProcessor 调用

**新的逻辑**：用 AgentEngine 统一处理

```kotlin
private fun callAiApi() {
    // ... 原有的准备阶段保留 ...

    // 初始化 PersonaManager
    PersonaManager.init(getApplication())

    // 构建 ModelConfig
    val modelConfig = ModelConfig(
        provider = providerId,
        model = selectedModel,
        apiKey = apiKey,
        apiHost = resolveEndpointPath(baseUrl),
        temperature = 0.7f,
        maxTokens = 1024
    )

    // 使用 AgentEngine 发送消息
    viewModelScope.launch {
        var aiMsg = Message(id = generateId(), role = "assistant", ...)
        messages = messages + aiMsg
        _messages.value = messages
        _isStreaming.value = true

        agentEngine.sendMessageStream(
            content = lastUserMsg.content,
            history = messages.toMutableList().filter { it.status != "error" }.map {
                Message(
                    role = when(it.role) {
                        "user" -> MessageRole.USER
                        "assistant" -> MessageRole.ASSISTANT
                        "tool" -> MessageRole.TOOL
                        else -> MessageRole.SYSTEM
                    },
                    content = it.content,
                    toolCallId = it.toolCallId
                )
            }.toMutableList(),
            config = modelConfig,
            personaId = activeConversation?.personaId
        ).collect { event ->
            when (event) {
                is AgentEvent.Token -> {
                    aiMsg = aiMsg.copy(content = aiMsg.content + event.content)
                    messages = messages.map { if (it.id == aiMsg.id) aiMsg else it }
                    _messages.value = messages
                }
                is AgentEvent.Thinking -> {
                    aiMsg = aiMsg.copy(thinking = (aiMsg.thinking ?: "") + event.content)
                }
                is AgentEvent.ToolCall -> {
                    // 可选：显示工具调用状态
                    aiMsg = aiMsg.copy(content = aiMsg.content + "\n🔧 调用工具: ${event.name}...")
                    messages = messages.map { if (it.id == aiMsg.id) aiMsg else it }
                    _messages.value = messages
                }
                is AgentEvent.ToolResult -> {
                    // 可选：显示工具结果
                }
                is AgentEvent.Done -> {
                    aiMsg = aiMsg.copy(
                        content = event.message.content,
                        status = "ready",
                        thinking = event.message.thinking
                    )
                    messages = messages.map { if (it.id == aiMsg.id) aiMsg else it }
                    _messages.value = messages
                    _isStreaming.value = false
                    // 更新对话
                    activeConversation = activeConversation?.copy(updatedAt = System.currentTimeMillis())
                    store.saveConversations(conversations)
                }
                is AgentEvent.Error -> {
                    aiMsg = aiMsg.copy(
                        content = aiMsg.content + "\n❌ ${event.message}",
                        status = "error"
                    )
                    messages = messages.map { if (it.id == aiMsg.id) aiMsg else it }
                    _messages.value = messages
                    _isStreaming.value = false
                }
            }
        }
    }
}
```

## 改造要点总结

| 原来 | 现在 |
|------|------|
| `ApiClient.getService().chatCompletionsStream()` | `agentEngine.sendMessageStream()` |
| `streamProcessor.processStream()` | `agentEngine 返回的 Flow<AgentEvent>` |
| `retryEngine.findFallbacks()` | `EndpointManager.selectEndpoint()` (可选接入) |
| 手动构建消息列表 | `agentEngine.buildContext()` 自动处理 |
| 无工具调用 | `ToolRegistry` + `MCPManager` 自动处理 |
| `autoExtractMemories()` 手动触发 | `agentEngine` 每10轮自动触发 |

## 文件清单

已创建的新文件：

```
android-native/app/src/main/java/com/gusogst/chat/
├── model/Models.kt              ✅ 完整类型系统 (282行)
├── network/
│   ├── OpenAIProvider.kt        ✅ OpenAI 适配器 (248行)
│   ├── AnthropicProvider.kt     ✅ Anthropic 适配器 (200行)
│   ├── AgentEngine.kt           ✅ Agent 核心引擎 (282行)
│   └── EndpointManager.kt       ✅ 端点管理器 (300行)
├── data/
│   ├── ToolRegistry.kt          ✅ 工具注册 (209行)
│   ├── PersonaManager.kt        ✅ 人设管理 (213行)
│   └── mcp/
│       ├── MCPClient.kt         ✅ MCP 客户端 (168行)
│       └── MCPManager.kt        ✅ MCP 管理器 (93行)
└── docs/
    ├── ANDROIDUI_PORTING_GUIDE.md   ✅ 移植指南
    └── CHATVIEWMODEL_INTEGRATION.md ✅ 接入指南
```

## 初始化顺序

在 Application 或 MainActivity 中：

```kotlin
// 1. PersonaManager 初始化
PersonaManager.init(context)

// 2. ToolRegistry 已自动注册内置工具
// toolRegistry 已在 ChatViewModel 中创建

// 3. MCPManager 可选初始化
// mcpManager.registerServer("name", MCPServerConfig(url = "..."))
// mcpManager.connectAll()
```

## 注意事项

1. **Models.kt 和原有 Models 可能冲突** — 需要统一，建议用新的 Models.kt 替换旧的
2. **消息 ID 字段** — 原 Message 有 `id` 字段，新的 Models.kt 没有，需要对齐
3. **编译依赖** — 需要 `com.google.code.gson:gson:2.10.1` 和 `com.squareup.okhttp3:okhttp:4.12.0`
4. **测试** — 先用 OpenAI 兼容端点测试，Anthropic 需要单独测试消息格式转换
