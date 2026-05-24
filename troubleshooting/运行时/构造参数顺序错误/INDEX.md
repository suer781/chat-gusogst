# 构造参数顺序错误导致 role 字段错位

> **状态：** 已修复
> **涉及文件：** ChatViewModel.kt、AgentTypes.kt

---

## 症状

AI 回复异常或完全无响应。API 请求已发出但返回内容不对，或模型忽略用户的 system prompt。

---

## 原因

`ApiMessage`（原 `Message`）的构造函数参数顺序是：

```kotlin
data class ApiMessage(
    val id: String? = null,    // 第 1 位
    val role: String,           // 第 2 位
    val content: String,        // 第 3 位
    ...
)
```

但 `ChatViewModel.kt` 中用了**位置参数**而非命名参数，导致 role 和 content 错位：

```kotlin
// 错误：位置参数
ApiMessage("system", it)
// 实际: id="system", role=【prompt内容】 ← role 被赋值为 prompt！

ApiMessage(it.role.name, it.content)
// 实际: id=【role名】, role=【消息内容】 ← role 被赋值为聊天内容！
```

API 收到后，所有消息的 `role` 字段都是错的值——服务器无法区分 system/user/assistant，对话逻辑完全混乱。

---

## 修复

改用**命名参数**，确保每个字段赋值到正确的位置：

```kotlin
// 正确：命名参数
ApiMessage(role = "system", content = it)
ApiMessage(role = it.role.name, content = it.content)
```

### 为什么之前没发现

改名前 `data class Message` 和 `model.Message` 同名（不同包），位置参数调用不易被注意到。改名后 `ApiMessage` 与 `Message` 名称不同，调用处的潜规则暴露了出来。

### 教训

- Kotlin 中 data class 参数多的，**永远用命名参数**，不要依赖位置
- 重命名类型时，检查所有调用处的位置参数
- `id` 有默认值 `null` 应该放在最后一位，减少误传概率

---

## 相关文件

| 文件 | 改动 |
|------|------|
| `android-native/.../viewmodel/ChatViewModel.kt` | 第 140、145 行：位置参数 → 命名参数 |
| `android-native/.../data/AgentTypes.kt` | `ApiMessage` 定义（未改动，但暴露了调用处的问题） |
