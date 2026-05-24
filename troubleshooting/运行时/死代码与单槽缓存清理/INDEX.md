# 死代码、单槽缓存、TODO 遗留

> **状态：** 已清理
> **涉及文件：** AgentBridge.kt、ApiClient.kt、ChatViewModel.kt

---

## 1. AgentBridge 死代码

`AgentBridge.buildMessages()` 和 `AgentBridge.settingsToConfig()` 从未被调用。
ChatViewModel 直接在自己的 `callAiApi()` 中构建 `ApiMessage` 列表。

**清理前：** 56 行代码 + import + 注释，0 次调用
**清理后：** 仅保留 object 声明 + 说明注释

> 如果未来需要统一消息构建逻辑，将 ChatViewModel 的构建代码迁入即可。

---

## 2. ApiClient 单槽缓存

旧实现：

```kotlin
private var currentBaseUrl: String = ""
private var currentRetrofit: Retrofit? = null

@Synchronized
fun getService(baseUrl: String): ApiService {
    if (normalized != currentBaseUrl) {
        currentBaseUrl = normalized
        currentRetrofit = Retrofit.Builder()...build()
    }
    return currentRetrofit!!.create(...)
}
```

每切一次供应商就重建 Retrofit（`@Synchronized` 还阻塞并发）。改为 `ConcurrentHashMap`：

```kotlin
private val retrofitCache = ConcurrentHashMap<String, Retrofit>()

fun getService(baseUrl: String): ApiService {
    return retrofitCache.getOrPut(normalized) {
        Retrofit.Builder()...build()
    }.create(...)
}
```

| 指标 | 单槽 | Map 缓存 |
|------|------|----------|
| 跨供应商切换 | 重建 Retrofit | 命中缓存 |
| 并发安全 | `@Synchronized`（阻塞） | `ConcurrentHashMap`（无锁读） |
| 可扩展 | 1 个槽 | 不限 |

---

## 3. ChatViewModel.setModel 空实现

`setModel(providerId, modelId)` 只有一个 `// TODO`，调用后无任何效果。

实现为更新当前对话的 `providerId` 和 `modelId` 并持久化：

```kotlin
fun setModel(providerId: String, modelId: String) {
    val conv = _activeConversation.value ?: return
    conv.providerId = providerId
    conv.modelId = modelId
    conv.updatedAt = System.currentTimeMillis()
    _activeConversation.value = conv
    store.saveConversations(_conversations.value.orEmpty())
}
```

---

## 文件变更清单

| 文件 | 改动 |
|------|------|
| `data/AgentBridge.kt` | 删除 `settingsToConfig`、`buildMessages`，保留空壳 |
| `network/ApiClient.kt` | `ConcurrentHashMap` 替换单槽 + `@Synchronized` |
| `viewmodel/ChatViewModel.kt` | `setModel` 实现 TODO → 更新 providerId/modelId 并持久化 |
