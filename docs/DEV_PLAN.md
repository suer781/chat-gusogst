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
