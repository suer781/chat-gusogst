# 开发路线规划

---

## 已完成

### 前端基础架构
- Capacitor + React + TypeScript（18 个页面）
- 深色主题 UI、设置面板、MCP 状态页
- 本地聊天界面（气泡、输入框、Markdown 渲染）

### 后端 Agent 核心
- 多模型 API 支持（OpenAI/Anthropic/Ollama 等）
- 联网搜索集成（Tavily + DuckDuckGo，22 引擎框架）
- MCP 工具集成
- 人设系统（PersonaManager + AI 分析 + 手动配置）
- 记忆系统（MemoryManager + IndexedDB + 两层提取）
- Hermes 平台连接器（QQ/微信/Telegram 等）
- GitHub CI/CD（push to main -> Actions -> APK）

### 人设管理 UI（部分完成）
- 列表视图、添加自定义、编辑、删除
- 详情 Tab（搜索/采样/AI 分析）待完成

---

## 进行中

### PersonaView UI 完善
- 详情 Tab 页（搜索配置开关、采样参数滑块、AI 分析结果展示）
- 原因中断：heredoc 写入复杂 JSX 被安全系统拦截
- 方案：用 write_file 或手动粘贴完成

---

## 待开发

### P1 - Chaquopy 嵌入 Android
- Python 后端通过 Chaquopy 嵌入 Android
- 同步调用：py.getModule().callAttr() 直调
- 异步持久化：共享本地文件（JSON/SQLite）
- 替代已废弃的 Flask HTTP 方案

### P2 - Agent 主循环串联
- providers -> tools -> memory -> persona 串联成完整 Agent 循环
- 消息 -> 人设注入 -> 模型调用 -> 工具执行 -> 记忆写入 -> 回复

### P3 - 前台服务与 UI 接入
- Android 前台服务保活 + 通知栏常驻
- 聊天页面接入 Agent 后端（实时流式输出）
- 工具调用状态展示

### P4 - 动画系统与视觉体验
> 详见 docs/ANIMATION_SYSTEM.md
- AnimationManager 框架 + CSS 变量 + 设置开关
- 聊天动画（气泡入场、打字指示器、发送动画、平滑滚动）
- 玻璃材质 Glassmorphism（顶栏/底栏/面板/菜单，深色适配）
- 页面切换（slideIn/Out、设置面板、退出托举）
- 微动效（按钮回弹、开关、下拉刷新、错误抖动）
- 通用设置中动画效果和玻璃材质可开可关
- 无障碍适配（prefers-reduced-motion）

### P5 - 主动消息
- 基于记忆的主动触发（生日、纪念日、待办提醒）
- 情感状态变化检测
- 定时关怀推送

### P6 - 高级功能
- 语音输入/输出
- 图片/文件分享
- 多人设同时对话
- 插件市场

---

## UI 反馈修复（2026-05-16）

### P0 — 立即修
- [ ] **UI 动画**：页面切换过渡、按钮 hover/active、列表展开收起动画
- [ ] **按钮样式**：统一按钮组件（圆角、hover 效果、active 压感）

### P1 — 近期
- [ ] **模型预设补全**：对齐 Hermes 24 个 Provider（补 zai/stepfun/copilot/kilocode/huggingface/xai/xiaomi/nvidia/mistral/perplexity/cohere）
- [ ] **人设智能搜索推荐**：LLM 分析人设特征 → 推荐匹配引擎组合

### P2 — 架构
- [ ] **第三方平台 Channel 系统**：参考 Hermes 19 个平台，设计 ChannelAdapter 接口
  - 国内：微信个人号/企微/QQ/飞书/钉钉
  - 海外：Telegram/Discord/Slack/WhatsApp/Signal/Matrix
  - 其他：Email/SMS/Mattermost/BlueBubbles/Yuanbao

---

## ⚠️ 前后端差距分析（2026-05-16 凌晨审计）

### 背景
用户部署 chatbox-frontend 到浏览器查看，发现功能比预期少。经逐文件审计，发现严重断层：

### 1. chatbox-frontend（浏览器版本）实际状态

| 能力 | 状态 | 说明 |
|------|------|------|
| 基础聊天 | ✅ 完整 | 气泡、输入框、Markdown 渲染、消息列表 |
| LLM 直连调用 | ✅ 完整 | generateText / streamText / generateImage |
| 多 Provider 配置 | ✅ 完整 | 24+ Provider 定义、设置页、模型管理 |
| MCP 服务器配置 | ✅ 完整 | stdio + http 传输，状态监控 |
| 网络搜索设置 | ✅ 完整 | 搜索引擎配置 UI |
| 知识库管理 | ✅ 完整 | 文档上传、RAG 查询 |
| Chatbox AI 服务 | ✅ 完整 | 付费服务的登录/许可证管理 |
| 快捷键 | ✅ 完整 | 快捷键配置 |
| **Agent 核心** | ❌ **空文件** | `src/agent/core/agent.ts` = 0 字节 |
| **人设系统** | ❌ **空文件** | `src/agent/core/persona.ts` = 0 字节 |
| **记忆系统** | ❌ **空文件** | `src/agent/memory/store.ts` = 0 字节 |
| **工具注册** | ❌ **空文件** | `src/agent/tools/registry.ts` = 0 字节 |
| **思考过程展示** | ❌ **不存在** | 无 \`<think>\` 标签渲染 |
| **工具调用展示** | ❌ **不存在** | 无工具执行状态 UI |
| **Persona 切换** | ❌ **不存在** | 无多角色管理 |

### 2. capacitor-app 实际状态

| 能力 | 状态 | 说明 |
|------|------|------|
| ChatPage | ✅ 存在 | 基础聊天页面 |
| PersonaPage | ✅ 存在 | 人设管理页面（含 5 预设） |
| SettingsPage | ✅ 存在 | 设置页 |
| Agent 逻辑 | ❌ **不存在** | 无 agent 目录、无 agent 代码 |

### 3. hermes-backend（Python）实际状态

| 模块 | 状态 | 行数 | 说明 |
|------|------|------|------|
| run_agent.py | ✅ 完整 | 15,774 | Agent 主循环、工具调度、模型调用 |
| chaquopy_entry.py | ✅ 完整 | 176 | Android ↔ Python 桥接入口 |
| tools/ | ✅ 完整 | 80+ 文件 | 浏览器、代码执行、MCP、记忆、技能等 |
| providers/ | ✅ 存在 | base.py | 模型 Provider 基类 |
| 前端集成 | ❌ **不存在** | — | 无 HTTP/WebSocket 接口给前端调用 |

### 4. 核心问题总结

**三层断裂：**
```
[浏览器 UI] ──❌──→ [Agent 核心] ──❌──→ [Python 后端]
  chatbox-frontend     src/agent/          hermes-backend
  (有UI无脑)          (全是空文件)        (有脑无接口)
```

- chatbox-frontend 的 UI 是原版 Chatbox 的，支持直接调 LLM API，但 **没有 Agent 能力**
- agent/ 下的 4 个核心文件全是 **0 字节占位符**，从未写入代码
- hermes-backend 有完整的 Python Agent 能力，但 **没有暴露任何接口** 给前端

### 5. 修复计划（2026-05-16 待执行）

#### 阶段 A：让浏览器版能用 Agent（最高优先级）

| 任务 | 说明 | 估时 |
|------|------|------|
| A1. 写 agent.ts | Agent 主循环：接收消息 → 注入人设 → 调模型 → 流式返回 | 2h |
| A2. 写 persona.ts | PersonaEngine：加载/切换/管理系统提示词 | 1h |
| A3. 写 store.ts | MemoryStore：IndexedDB 长期记忆 + 会话上下文 | 1.5h |
| A4. 写 registry.ts | ToolRegistry：注册/调用 MCP 工具 + 搜索 | 1h |
| A5. 接入现有 UI | 在 chat 页面调用 agent 代替直连 LLM | 1h |
| A6. 思考过程 UI | \`<think>\` 标签折叠渲染、工具调用状态展示 | 1h |

#### 阶段 B：桥接 Python 后端（增强能力）

| 任务 | 说明 | 估时 |
|------|------|------|
| B1. Chaquopy IPC | 前端通过 Chaquopy 调 Python run_agent.py | 2h |
| B2. 工具结果回传 | Python 工具执行结果 → 前端展示 | 1h |
| B3. 记忆同步 | Python 记忆 ↔ 前端 IndexedDB 双向同步 | 1h |

#### 阶段 C：UI 体验优化

| 任务 | 说明 | 估时 |
|------|------|------|
| C1. Persona 切换 UI | 多角色管理、预设模板、自定义创建 | 1h |
| C2. MCP 状态展示 | 工具调用实时状态、结果折叠展示 | 1h |
| C3. 动画过渡 | 页面切换、消息入场、打字指示器 | 1.5h |

### 6. 注意事项

- **不要在本地构建 APK**（上次教训：1.1GB SDK 浪费）
- **代码推到 GitHub → Actions 自动构建**
- chatbox-frontend 原版 UI 不要大幅改动，只在必要处插入 Agent 能力
- capacitor-app 作为精简版可暂时搁置，优先修浏览器版
