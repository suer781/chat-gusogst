# 命名规范与类型统一

> **目的：** 消除 API 层与 UI 层的命名冲突，减少绕路代码，提升可维护性
> **涉及文件：** AgentTypes.kt、AgentBridge.kt、Models.kt

---

## 问题

项目有两个 `Message` 类：

| 位置 | 用途 | 字段示例 |
|------|------|---------|
| `model/Models.kt` | UI 层消息展示 | id, role, content, status, thinking... |
| `data/AgentTypes.kt` | API 请求/响应 | id, role, content, toolCalls, toolCallId... |

同名的 `ToolCall` 也重复出现：

| 位置 | 用途 | 字段 |
|------|------|------|
| `model/Models.kt` | UI 层 | id, name, arguments, result |
| `data/AgentTypes.kt` | API 层 | id, type, function(ToolFunction) |

**后果：**

```kotlin
// AgentBridge.kt — 被迫写全限定名绕路
// [NOTE] 不能直接用 Message（会跟本地 data class Message 冲突）
history: List<com.gusogst.chat.model.UIMessage>

// Models.kt — 被迫加 typealias
// [NOTE] 删除会导致 AgentBridge 编译失败
typealias UIMessage = Message
```

每次新增文件都要小心别 import 错 `Message`，心智负担大。

---

## 修复方案

约定：**API 层类型加 `Api` 前缀**，UI 层类型保持原名。

| 旧名（API 层） | 新名 | 原因 |
|----------------|------|------|
| `Message` | `ApiMessage` | 与 UI 层 `model.Message` 无冲突 |
| `ToolCall` | `ApiToolCall` | 同上 |
| `ToolFunction` | `ApiToolFunction` | 同上 |

### 改动后

```kotlin
// AgentBridge.kt — 不再需要全限定名
import com.gusogst.chat.model.Message
import com.gusogst.chat.data.ApiMessage

fun buildMessages(
    systemPrompt: String,
    history: List<Message>     // ← 直接引用 model.Message
): List<ApiMessage> {          // ← 明确是 API 层类型
    ...
}
```

```kotlin
// Models.kt — 不再需要 typealias
// 删除: typealias UIMessage = Message
```

```kotlin
// AgentTypes.kt — 命名自解释
data class ApiMessage(...)
data class ApiToolCall(...)
data class ApiToolFunction(...)
```

### 效率提升

| 指标 | 改动前 | 改动后 |
|------|--------|--------|
| typealias 绕路 | 1 处（UIMessage） | 0 |
| 全限定名引用 | 1 处（com.gusogst.chat.model.UIMessage） | 0 |
| import 歧义 | 两个 Message 同包不同文件 | ApiMessage vs Message 一目了然 |
| 新增文件心智负担 | 需留意 import 哪个 Message | 无歧义，直接按需 import |

---

## 命名规范

```
com.gusogst.chat.model         — UI / 业务逻辑层
  Message, ToolCall, Persona, Conversation, ChatRequest...

com.gusogst.chat.data          — API / 数据传输层
  ApiMessage, ApiToolCall, AgentConfig, StreamEvent...
```

- **不要** 在 API 层使用与 UI 层相同的类名
- **不要** 用 typealias 绕路——改名更直接
- **不要** 在两个包之间互相引用 data class（除非是 Bridge）
- **推荐** API 层类型加 `Api` 前缀，一目了然

---

## 相关文件

| 文件 | 改动 |
|------|------|
| `android-native/.../data/AgentTypes.kt` | `Message`→`ApiMessage`, `ToolCall`→`ApiToolCall` |
| `android-native/.../data/AgentBridge.kt` | 简化 import，去掉全限定名 |
| `android-native/.../model/Models.kt` | 删除 `typealias UIMessage = Message` |
