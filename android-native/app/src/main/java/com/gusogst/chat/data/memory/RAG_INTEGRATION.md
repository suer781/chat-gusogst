# RAG 集成待办（main → AndroidUI 移植）

## ✅ 已完成

### VectorStore.kt
- TF-IDF 分词（英文 word regex + 中文 unigram/bigram）
- IDF 逆文档频率计算
- 余弦相似度搜索
- SharedPreferences + Gson 持久化
- 公开 API：add / remove / search / getStats / clear / rebuild

### MemoryManager.kt
- 混合检索：向量 50% + 关键词 50%
- 自动淘汰（>200 条时按 importance/accessCount/timestamp 排序淘汰）
- 记忆导出/导入
- 公开 API：addMemory / removeMemory / search / getContextStrings / getStats / clear

## ⏳ 待集成

### ChatViewModel.kt（主线程改动）

1. **初始化 MemoryManager**
```kotlin
private val memoryManager = MemoryManager(getApplication())
```

2. **发消息时注入记忆上下文**（在 callAiApi 的 systemMessages 构建处）
```kotlin
// 在 existing systemMessages 之后追加
val memoryContext = memoryManager.getContextStrings(userMessage, 5)
if (memoryContext.isNotEmpty()) {
    systemMessages.add(ChatMessage(
        role = "system",
        content = "以下是与用户相关的记忆，可用于个性化回复：\n" +
            memoryContext.joinToString("\n")
    ))
}
```

3. **AI 回复后提取记忆**（在 handleApiResponse 之后）
```kotlin
// 调用 AI 自动提取记忆（需额外 API 调用，或先跳过等人工写入）
// 简单方案：用户手动通过设置页管理记忆
```

4. **MemorySettingsFragment.kt 接入**
- 记忆总数：`memoryManager.getStats().totalEntries`
- 词汇量：`memoryManager.getStats().vectorStats.vocabularySize`
- 清除：`memoryManager.clear()`

## 📌 注意
- vectorStore 和 memoryManager 的 SharedPreferences 与 ChatStore 独立
- 不需要 Gson 新依赖（已有）
- 运行时无额外权限
- 自动提取记忆需要额外 API 调用（可用 AI 或规则引擎），初期可先跳过
