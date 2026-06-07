# AndroidUI 分支后端移植指南

> 基于 main 分支 (TypeScript) → AndroidUI 分支 (Kotlin) 的完整移植方案
> 编写时间：2026-06-07

---

## 1. 架构总览

### 1.1 Main 分支数据流

```
用户输入
  │
  ▼
Agent.sendMessage(content)
  │
  ├─→ buildContext()        ← Persona 系统提示 + 历史消息
  ├─→ truncateHistory()     ← 裁剪到 maxHistoryTokens
  │
  ▼
ProviderAdapter.chat()/chatStream()
  │
  ├─→ OpenAI 适配器        ← /v1/chat/completions
  ├─→ Anthropic 适配器      ← /v1/messages
  │
  ▼
响应处理
  ├─→ 普通文本 → yield token → yield done
  ├─→ 工具调用 → ToolRegistry.execute() → 递归 sendMessage
  │
  ▼
MemoryManager.extractAndSave()  ← 每10轮自动提取记忆
```

### 1.2 模块依赖图

```
┌─────────────────────────────────────────────┐
│                 Agent (核心)                  │
│  agent.ts ← persona.ts ← agent-types.ts     │
├──────────┬──────────┬──────────┬────────────┤
│ Provider │  Tools   │  Memory  │    MCP     │
│ System   │ Registry │  System  │  Manager   │
├──────────┼──────────┼──────────┼────────────┤
│ openai   │ search   │ vector   │ client     │
│ anthropic│          │ store    │ manager    │
│ registry │          │ manager  │            │
│ endpoint │          │          │            │
│ manager  │          │          │            │
└──────────┴──────────┴──────────┴────────────┘
```

### 1.3 AndroidUI 分支现状对比

| Main 模块 | Main 行数 | AndroidUI 对应 | AndroidUI 行数 | 状态 |
|-----------|----------|---------------|---------------|------|
| endpoint-types.ts | 72 | AgentTypes.kt | 76 | ⚠️ 部分匹配 |
| provider-registry.ts | 208 | ProviderRegistry.kt | 39 | ❌ 骨架 |
| endpoint-manager.ts | 440 | EndpointKB.kt + EndpointScorer.kt | 320 | ⚠️ 部分实现 |
| openai.ts | 132 | ApiService.kt | 26 | ❌ 极简 |
| anthropic.ts | 128 | 无 | 0 | ❌ 缺失 |
| agent.ts | 231 | ChatViewModel.kt 散落 | — | ⚠️ 架构不同 |
| persona.ts | 110 | PersonaFragment.kt (UI) | — | ⚠️ 只有UI |
| vectorStore.ts | 262 | VectorStore.kt | 204 | ✅ 已移植 |
| manager.ts | 218 | MemoryManager.kt | 263 | ✅ 已移植 |
| bridge.ts | 74 | AgentBridge.kt | 16 | ❌ 空壳 |
| tools/registry.ts | 89 | 无 | 0 | ❌ 缺失 |
| mcp/manager.ts | ~150 | 无 | 0 | ❌ 缺失 |
| shared/types.ts | ~200 | Models.kt | 179 | ⚠️ 部分匹配 |

---

## 2. 模块详解

### 2.1 类型系统 (agent-types.ts → Models.kt / AgentTypes.kt)

**Main 核心类型：**

```typescript
// 消息
Message { role, content, tool_calls?, tool_call_id?, name?, timestamp? }
ToolCall { id, type:'function', function: { name, arguments } }
ToolResult { tool_call_id, name, content, is_error? }

// 配置
ModelConfig { provider, model, apiKey, apiHost?, temperature?, maxTokens?, topP? }
AgentConfig { model, persona?, provider?, memory?, mcpServers?, search?, maxHistoryTokens? }
SearchConfig { engine, tavilyApiKey? }
MemoryConfig { enabled }
MCPServerConfig { name, url, headers?, enabled, timeout? }

// 事件流
AgentEvent = { type:'token', content } | { type:'tool_call', ... } | { type:'done', message } | ...

// Provider
ProviderAdapter { name, chat(), chatStream() }
ToolDefinition { type:'function', function: { name, description, parameters } }

// Persona
Persona { id, name, systemPrompt, avatar?, emoji?, tags?, isDefault?, builtIn?, personality?, modelParamsConfig? }

// 记忆
MemoryEntry { id, content, type, importance, created_at, last_accessed, access_count, tags, embedding? }

// Provider 定义 (UI用)
ProviderDef { id, name, transport, apiKeyEnvVars, baseUrl?, authType?, isAggregator?, aliases?, doc?, source? }
```

**Kotlin 移植建议：**
- 用 `data class` 替代 interface
- 用 `sealed class` 替代联合类型 (AgentEvent)
- 用 `enum class` 替代字符串字面量 (role, type)
- 用 `kotlinx.serialization` 或 Gson 做 JSON 序列化
- `AsyncGenerator` → Kotlin `Flow`

```kotlin
// 示例骨架
enum class MessageRole { SYSTEM, USER, ASSISTANT, TOOL }

data class Message(
    val role: MessageRole,
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AgentEvent {
    data class Token(val content: String) : AgentEvent()
    data class Thinking(val content: String) : AgentEvent()
    data class ToolCall(val id: String, val name: String, val arguments: String) : AgentEvent()
    data class ToolResult(val toolCallId: String, val name: String, val content: String) : AgentEvent()
    data class Error(val message: String) : AgentEvent()
    data class Done(val message: Message) : AgentEvent()
}

interface ProviderAdapter {
    val name: String
    suspend fun chat(messages: List<Message>, config: ModelConfig, tools: List<ToolDefinition>? = null): Message
    fun chatStream(messages: List<Message>, config: ModelConfig, tools: List<ToolDefinition>? = null): Flow<String>
}
```

---

### 2.2 Provider 系统

#### 2.2.1 ProviderRegistry (provider-registry.ts → ProviderRegistry.kt)

**Main 逻辑：**
1. 从 `providers-registry.json` 加载供应商数据
2. 构建向量索引（用 VectorStore）
3. 三层匹配：精确域名 → 精确ID/名称 → 向量语义搜索
4. 导出 `smartMatch()`, `getEndpointUrl()`, `getAllProviders()`, `getProviderById()`

**AndroidUI 现状：** 只有 39 行的硬编码静态列表，无向量搜索，无匹配逻辑。

**移植要点：**
- 加载 providers-registry.json（放在 assets 或从网络拉取）
- 接入已有的 VectorStore.kt 做语义搜索
- 实现 `smartMatch()` 三层匹配
- 暴露 `getEndpointUrl()` 拼接完整 URL

```kotlin
// 骨架
class ProviderRegistry(private val vectorStore: VectorStore) {
    private val providers = mutableListOf<ProviderEntry>()
    
    suspend fun init(context: Context) {
        // 从 assets 或网络加载 providers-registry.json
        // 构建向量索引
    }
    
    fun smartMatch(input: String): MatchResult? {
        // 1. 精确域名匹配
        // 2. 精确ID/名称匹配
        // 3. 向量语义搜索
    }
    
    fun getEndpointUrl(providerId: String, endpointIndex: Int = 0): String
    fun getAllProviders(): List<ProviderEntry>
    fun getProviderById(id: String): ProviderEntry?
}
```

#### 2.2.2 EndpointManager (endpoint-manager.ts → EndpointKB.kt + EndpointScorer.kt)

**Main 逻辑：**
1. **评分系统**：累积评分 C_m，学习率 k_m = 12/(m+15)
   - 成功：ΔC = +100（首次 +500）
   - 失败：ΔC = -200（连续错误放大）
2. **搭便车测试**：发最小请求 (gpt-3.5-turbo, max_tokens=1) 测试端点
3. **网络判定**：ping favicon.ico 区分网络问题 vs 端点问题
4. **故障转移**：按评分降序逐个尝试候选端点
5. **持久化**：localStorage 存评分和聊天计数

**AndroidUI 现状：**
- EndpointKB.kt (200行) — 有基础的端点知识库
- EndpointScorer.kt (120行) — 有评分逻辑但不完整

**移植要点：**
- 合并 EndpointKB + EndpointScorer 为完整的 EndpointManager
- 实现完整的评分公式（学习率 + 累积评分 + 错误放大）
- 用 SharedPreferences 替代 localStorage 做持久化
- 实现搭便车测试（OkHttp 最小请求）
- 网络判定用 OkHttp ping 替代浏览器 img 标签
- 实现故障转移循环

```kotlin
// 骨架
class EndpointManager(private val context: Context) {
    private val ratings = mutableMapOf<String, EndpointRating>()
    private val chatCounts = mutableMapOf<String, Int>()
    
    suspend fun selectEndpoint(
        userInput: String,
        apiKey: String,
        customEndpoint: String? = null
    ): EndpointSelection? {
        // 1. smartMatch 匹配供应商
        // 2. 构建候选列表，按评分降序
        // 3. 逐个 testEndpoint，成功则返回
        // 4. 网络问题则直接返回 null
    }
    
    private suspend fun testEndpoint(endpoint: String, apiKey: String): EndpointTestResult
    private fun recordSuccess(endpoint: String, providerId: String)
    private fun recordFailure(endpoint: String, providerId: String)
    private suspend fun judgeNetwork(domain: String): Boolean // true = 网络问题
}
```

#### 2.2.3 OpenAI 适配器 (openai.ts)

**Main 逻辑：**
- 端点：`{apiHost}/v1/chat/completions`
- 认证：`Authorization: Bearer {apiKey}`
- 请求体：model, messages, temperature, max_tokens, top_p, tools, stream
- 流式：SSE 解析，逐块 yield delta.content
- 超时：非流式 60s，流式 120s
- 支持 tool_calls 解析

**移植要点：**
- OkHttp + SSE 实现流式
- 协程替代 Promise
- 解析 tool_calls 返回

```kotlin
// 骨架
class OpenAIProvider : ProviderAdapter {
    override val name = "openai"
    private val client = OkHttpClient()
    
    override suspend fun chat(messages: List<Message>, config: ModelConfig, tools: List<ToolDefinition>?): Message {
        val body = buildBody(messages, config, tools, stream = false)
        val request = Request.Builder()
            .url("${config.apiHost}/v1/chat/completions")
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(body.toRequestBody(jsonMediaType))
            .build()
        // 执行请求，解析响应
    }
    
    override fun chatStream(messages: List<Message>, config: ModelConfig, tools: List<ToolDefinition>?): Flow<String> = flow {
        val body = buildBody(messages, config, tools, stream = true)
        // SSE 流式解析
    }
}
```

#### 2.2.4 Anthropic 适配器 (anthropic.ts)

**Main 逻辑：**
- 端点：`{apiHost}/v1/messages`
- 认证：`x-api-key: {apiKey}` + `anthropic-version: 2023-06-01`
- 消息格式转换：system 消息提取为顶层字段，tool 结果转 user 消息
- 响应解析：content 数组中 text 和 tool_use 两种类型
- 流式：当前实现为伪流式（直接调 chat 再 yield）
- 超时：120s

**移植要点：**
- 消息格式转换是重点（和 OpenAI 差异大）
- tool_use 的解析
- 流式可以先用伪流式，后续补真流式

```kotlin
// 骨架
class AnthropicProvider : ProviderAdapter {
    override val name = "anthropic"
    
    private fun convertMessages(messages: List<Message>): Pair<String?, List<Map<String, Any>>> {
        // 提取 system 消息，转换 tool 结果
    }
    
    private fun convertTools(tools: List<ToolDefinition>?): List<Map<String, Any>>?
}
```

---

### 2.3 Agent 核心

**Main 逻辑 (agent.ts)：**

1. **初始化**：加载 provider、注册内置工具、初始化 memory/mcp
2. **sendMessage(content) → AsyncGenerator<AgentEvent>**：
   - 重置状态，加入用户消息到历史
   - truncateHistory() 裁剪
   - buildContext() 构建上下文（persona 系统提示 + 历史）
   - 调用 provider.chat()
   - 如果返回 tool_calls：
     - 逐个执行工具
     - 递归调用 sendMessage（深度限制 10）
   - 如果返回文本：
     - yield token + done
     - 每 10 轮自动提取记忆
3. **truncateHistory()**：
   - 硬上限 100 条消息
   - 字符上限 = maxHistoryTokens * 1.5
   - 系统消息始终保留
   - 丢弃的消息生成摘要
4. **buildContext()**：
   - 系统消息 = persona.systemPrompt + 可用工具列表
   - 合并历史消息

**AndroidUI 现状：** 逻辑散落在 ChatViewModel.kt 中，架构不同。

**移植方案：**
- 不要直接移植 Agent 类（Android 的 ViewModel 架构不同）
- 把 Agent 的核心逻辑融入 ChatViewModel
- 重点移植：
  - sendMessage 的工具调用递归循环
  - truncateHistory 的裁剪算法
  - buildContext 的上下文构建
  - 记忆提取触发逻辑

```kotlin
// ChatViewModel 中需要补的核心逻辑
private suspend fun handleToolCalls(
    response: Message,
    tools: List<ToolDefinition>
): Flow<AgentEvent> = flow {
    response.toolCalls?.forEach { tc ->
        emit(AgentEvent.ToolCall(tc.id, tc.function.name, tc.function.arguments))
        val result = toolRegistry.execute(tc.function.name, parseArgs(tc.function.arguments))
        emit(AgentEvent.ToolResult(tc.id, tc.function.name, result.toString()))
        // 加入历史
    }
    // 递归（深度检查）
    if (currentDepth < MAX_TOOL_DEPTH) {
        currentDepth++
        emitAll(sendMessage(""))  // 空内容触发下一轮
    }
}
```

---

### 2.4 人设系统 (persona.ts)

**Main 逻辑：**
- 6 个预设人设（温柔、傲娇、元气、深夜、学习、治愈）
- 持久化到 localStorage
- CRUD：增删改查 + 切换
- 预设不可删除

**AndroidUI 现状：** 有 PersonaFragment/PersonaProfileFragment/PersonaSettingsDialog（UI层），但缺少底层 PersonaManager。

**移植要点：**
- 实现 PersonaManager 类
- 用 SharedPreferences 或 Room 持久化
- 预设人设数据
- 与 UI Fragment 对接

```kotlin
// 骨架
class PersonaManager(private val context: Context) {
    private val personas = mutableListOf<Persona>()
    private var activeId: String
    
    fun getActive(): Persona
    fun switchTo(id: String): Persona
    fun listAll(): List<Persona>
    fun add(persona: Persona): Persona
    fun update(id: String, patch: Map<String, Any>): Boolean
    fun delete(id: String): Boolean  // 预设不可删
}
```

---

### 2.5 记忆系统

**AndroidUI 现状：** ✅ 已基本移植完成
- VectorStore.kt (204行) — TF-IDF + 余弦相似度
- MemoryManager.kt (263行) — 混合搜索 + 自动提取

**需要做的：**
- ❗ 接线到 ChatViewModel（3 根线，见 RAG_INTEGRATION.md）
- 用 Room 或 SharedPreferences 替代 IndexedDB（已有实现则验证）
- 确保 extractAndSave 在每 10 轮对话后触发

---

### 2.6 工具系统 (tools/registry.ts)

**Main 逻辑：**
- ToolRegistry 类：注册工具定义 + 处理函数
- 执行时先查 MCP 工具，再查内置工具
- 内置工具：get_current_time, memory_save, memory_search, calculator, 搜索工具

**AndroidUI 现状：** ❌ 完全缺失

**移植要点：**
- 实现 ToolRegistry 类
- 内置工具可以简化（记忆工具直接调 MemoryManager，时间工具直接获取）
- MCP 工具集成看下面 2.7

```kotlin
// 骨架
class ToolRegistry {
    private val tools = mutableMapOf<String, ToolDefinition>()
    private val handlers = mutableMapOf<String, suspend (String, Map<String, Any>) -> Any>()
    private var mcpManager: MCPManager? = null
    
    fun register(tool: ToolDefinition, handler: (suspend (String, Map<String, Any>) -> Any)? = null)
    suspend fun execute(name: String, args: Map<String, Any>): Any
    fun getDefinitions(): List<ToolDefinition>
}
```

---

### 2.7 MCP 系统 (mcp/)

**Main 逻辑：**
- MCPClient：连接单个 MCP 服务器（SSE/HTTP）
- MCPManager：管理多个客户端，工具路由
- 连接：`connectAll()` 并行连接，构建 toolToServer 映射
- 调用：`callTool()` 通过映射路由到对应客户端

**AndroidUI 现状：** ❌ 完全缺失

**移植优先级：** P2（增强功能）
- 先让基础聊天 + 工具调用跑通，再接 MCP
- Android 上可以用 OkHttp 做 SSE 客户端

```kotlin
// 骨架
class MCPClient(private val config: MCPServerConfig) {
    suspend fun connect()
    fun disconnect()
    suspend fun callTool(name: String, args: Map<String, Any>): MCPToolResult
    fun getToolDefinitions(): List<ToolDefinition>
}

class MCPManager {
    private val clients = mutableMapOf<String, MCPClient>()
    private val toolToServer = mutableMapOf<String, String>()
    
    suspend fun connectAll()
    suspend fun callTool(toolName: String, args: Map<String, Any>): MCPToolResult
    fun getToolNames(): List<String>
}
```

---

### 2.8 Hermes 桥接 (hermes/)

**Main 逻辑：**
- HermesBridge：启动/停止 Python 进程
- HermesClient：HTTP 客户端，调用 Hermes API
- 状态管理：stopped → starting → running → error

**AndroidUI 现状：** AgentBridge.kt 只有 16 行空壳

**移植优先级：** P3（锦上添花）
- Android 上没有 Capacitor 插件，需要通过 Termux 或其他方式启动 Python
- 可以先跳过，后面再做

---

## 3. 移植优先级

### P0 — 必须先做（基础能跑）
1. **类型系统完善** — Models.kt / AgentTypes.kt 补齐所有类型
2. **OpenAI 适配器** — ApiService.kt 扩展为完整的 OpenAI 适配器
3. **ChatViewModel 接线** — 工具调用递归循环 + 历史裁剪 + 上下文构建

### P1 — 核心功能（体验完整）
4. **Anthropic 适配器** — 支持第二种 provider
5. **ProviderRegistry 完善** — 从骨架升级为完整实现
6. **EndpointManager 合并** — EndpointKB + EndpointScorer 合并为完整版
7. **PersonaManager** — 底层管理类，对接已有 UI
8. **ToolRegistry** — 工具注册 + 执行框架
9. **RAG 接线** — VectorStore + MemoryManager 接入 ChatViewModel

### P2 — 增强功能（高级特性）
10. **MCP 系统** — MCPClient + MCPManager
11. **内置工具** — get_current_time, memory_save, memory_search, calculator
12. **搭便车测试** — 端点可用性验证

### P3 — 锦上添花
13. **Hermes 桥接** — Python 进程管理
14. **搜索工具** — Tavily/DuckDuckGo 集成
15. **providers-registry.json** — 完整供应商数据

---

## 4. Kotlin 移植注意事项

### 4.1 TS → Kotlin 常见转换

| TypeScript | Kotlin |
|-----------|--------|
| `interface` | `data class` 或 `interface` |
| `Promise<T>` | `suspend fun` + 协程 |
| `AsyncGenerator<T>` | `Flow<T>` |
| `localStorage` | `SharedPreferences` 或 `DataStore` |
| `IndexedDB` | `Room` 数据库 |
| `Map<K,V>` | `MutableMap<K,V>` |
| `Array` | `List` / `MutableList` |
| `fetch()` | `OkHttp` |
| `AbortController` | `OkHttp` 内置超时 |
| `JSON.parse/stringify` | `Gson` 或 `kotlinx.serialization` |
| `setTimeout/setInterval` | `CoroutineScope` + `delay` |
| 联合类型 `A | B` | `sealed class` |
| `window.Capacitor` | Android 原生 API |

### 4.2 异步模型
- TS 的 `Promise` → Kotlin 协程 `suspend`
- TS 的 `AsyncGenerator` → Kotlin `Flow`
- `yield` → `emit`
- `yield*` → `emitAll`
- `for await (const x of gen)` → `gen.collect { x -> }`

### 4.3 Android 特有限制
- **网络**：必须在协程/后台线程，需要 INTERNET 权限
- **存储**：用 Room/SharedPreferences/DataStore，不能用 IndexedDB
- **后台**：不能随意启动后台进程（Hermes 桥接需要特殊处理）
- **UI 更新**：必须在主线程，用 `viewModelScope` + `Dispatchers`

---

## 5. 文件结构规划

```
android-native/app/src/main/java/com/gusogst/chat/
├── data/
│   ├── AgentBridge.kt          # [改造] 完整桥接
│   ├── AgentTypes.kt            # [改造] 补齐所有类型
│   ├── ChatStore.kt             # [保留] 聊天存储
│   ├── ProviderRegistry.kt      # [改造] 完整注册表
│   ├── PersonaManager.kt        # [新增] 人设管理
│   ├── ToolRegistry.kt          # [新增] 工具注册
│   ├── memory/
│   │   ├── VectorStore.kt       # [保留] 已移植
│   │   └── MemoryManager.kt     # [保留] 已移植
│   └── mcp/
│       ├── MCPClient.kt         # [新增] P2
│       └── MCPManager.kt        # [新增] P2
├── network/
│   ├── ApiService.kt            # [改造] → ProviderAdapter
│   ├── ApiClient.kt             # [改造] → OkHttp 封装
│   ├── StreamProcessor.kt       # [保留] SSE 解析
│   ├── AutoRetryEngine.kt       # [保留] 重试逻辑
│   ├── EndpointKB.kt            # [合并] → EndpointManager
│   ├── EndpointScorer.kt        # [合并] → EndpointManager
│   ├── EndpointManager.kt       # [新增] 合并后的完整版
│   ├── OpenAIProvider.kt        # [新增] OpenAI 适配器
│   └── AnthropicProvider.kt     # [新增] Anthropic 适配器
├── model/
│   └── Models.kt                # [改造] 补齐类型
└── viewmodel/
    └── ChatViewModel.kt         # [改造] 融入 Agent 核心逻辑
```

---

## 6. 实施顺序

### 第一步：类型系统 (Models.kt + AgentTypes.kt)
- 补齐 Message, ToolCall, ToolResult, ModelConfig, AgentConfig, AgentEvent, ProviderAdapter, ToolDefinition, Persona, MemoryEntry
- 用 sealed class 实现 AgentEvent

### 第二步：OpenAI 适配器 (OpenAIProvider.kt)
- 实现 ProviderAdapter 接口
- 非流式 chat + 流式 chatStream
- SSE 解析复用 StreamProcessor
- tool_calls 解析

### 第三步：ChatViewModel 改造
- 融入 Agent 核心逻辑
- 工具调用递归循环
- 历史裁剪算法
- 上下文构建（persona + 工具列表）
- 接线 VectorStore + MemoryManager

### 第四步：PersonaManager
- 预设人设数据
- SharedPreferences 持久化
- 对接已有 UI Fragment

### 第五步：ToolRegistry
- 注册框架
- 内置工具（时间、记忆搜索、记忆保存、计算器）

### 第六步：Anthropic 适配器
- 消息格式转换
- tool_use 解析

### 第七步：ProviderRegistry + EndpointManager 完善
- smartMatch 三层匹配
- 评分系统完善
- 搭便车测试
- 故障转移

### 第八步（P2）：MCP 系统
- MCPClient
- MCPManager

### 第九步（P3）：Hermes 桥接
- 等基础功能稳定后再做
