# AI 协作交接文档

> 本文件供两个 AI 协作开发时使用，记录已完成的工作、待办事项和注意事项。
> 更新时间：2026-06-02 02:55 (Asia/Shanghai)

## 已完成的修复（@osbot.main 完成）

### 编译错误修复

| 文件 | 问题 | 修复内容 |
|------|------|----------|
| Models.kt | 缺少 `ChatResponse`/`Choice` 类定义 | 新增 `ChatResponse` 和 `Choice` data class |
| Models.kt | 缺少 `ApiMessage` import | 添加 `import com.gusogst.chat.data.ApiMessage` |
| AgentTypes.kt | 缺少 `ApiRequestMessage` 类 | 新增 `ApiRequestMessage(role, content)` data class |
| MemorySettingsFragment.kt | `MemoryManager(requireContext())` 参数错误 | 改为 `MemoryManager()` |
| MemorySettingsFragment.kt | `stats.totalEntries` 不存在 | 改为 `stats.totalMemories` |
| BasicSettingsFragment.kt | 文件为空，无 class 定义 | 创建空壳 Fragment |
| PlatformSettingsFragment.kt | 文件为空，无 class 定义 | 创建空壳 Fragment |
| MarkdownRenderer.kt | 缺少 R import | 已添加 |
| SettingsFragment.kt | 缺少 BasicSettings/PlatformSettings import | 已添加 |
| 多个文件 | 缺少 ContextCompat import | 已批量修复 |

### 之前修复的

- 主题切换动画 bug：删除 `overridePendingTransition` 调用
- Gradle wrapper 版本：8.9 → 8.11.1
- Chaquopy Python 路径：软链接修复
- colors.xml 格式错误：删除多余闭合标签
- SettingItem.color 类型：String → Int

## 待办事项

### 高优先级

1. **BasicSettingsFragment / PlatformSettingsFragment** 需要填充实际 UI 内容
   - 目前是空壳 Fragment，只有 ScrollView + LinearLayout
   - SettingsFragment 里通过 ViewPager2 Tab 导航到它们
   - 参考其他 Settings 子页面的 UI 风格（黑色背景 + 白色文字 + 卡片）

2. **MemoryManager 当前是存根实现**
   - `memory/MemoryManager.kt` 只有空方法，无实际存储
   - 需要实现：向量化存储、持久化、检索
   - RAG memory 的完整实现在之前的工作中被覆盖了

3. **HermesBridge 协程警告**
   - `emit` 在 synchronized 块内，有 suspension point in critical section 警告
   - 不影响编译但可能导致运行时问题

### 低优先级

- 各种过时 API 警告（onActivityCreated、getColor 等）
- Gradle deprecation warnings

## 项目结构

```
android-native/app/src/main/java/com/gusogst/chat/
├── model/Models.kt          # ChatRequest, ChatResponse, Choice, Delta, SettingItem 等
├── data/AgentTypes.kt        # AiModel, Preset, Agent, ApiMessage, ApiRequestMessage 等
├── data/memory/MemoryManager.kt  # 当前是存根
├── network/ApiService.kt     # Retrofit 接口
├── network/StreamProcessor.kt # SSE 流式处理
├── viewmodel/ChatViewModel.kt   # 注意路径是 viewmodel/ 不是 ui/chat/
├── ui/settings/SettingsFragment.kt
├── ui/settings/MemorySettingsFragment.kt
├── ui/settings/BasicSettingsFragment.kt    # 空壳，待填充
├── ui/settings/PlatformSettingsFragment.kt # 空壳，待填充
├── ui/chat/MarkdownRenderer.kt
└── core/hermes/HermesBridge.kt
```

## 注意事项

- **ChatViewModel 路径**：`viewmodel/ChatViewModel.kt`，不是 `ui/chat/ChatViewModel.kt`
- **MemoryManager 构造函数**：当前无参 `MemoryManager()`，存根中所有方法都是空实现
- **CI 触发**：推代码到 AndroidUI 分支会自动触发 GitHub Actions 构建
- **分支保护**：目前没有，可以直接 push
