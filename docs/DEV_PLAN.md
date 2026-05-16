# chat-gusogst 开发文档

> 更新时间：2026-05-17

## 项目定位

安卓 AI 虚拟恋人应用，融合 Agent 能力，后台常驻。用户零配置，APK 安装即用。

---

## 当前架构

```
app/
├── src/
│   ├── main.tsx              # 应用入口，加载 UI
│   ├── ui/                   # ★ 新 UI 层（Tailwind CSS）
│   │   ├── App.tsx           # 主布局 + 底部导航 + 路由
│   │   ├── stores.ts         # Zustand 全局状态（chat + settings + language）
│   │   ├── types.ts          # 自包含类型定义（Message, Persona, AgentConfig）
│   │   ├── i18n.ts           # 国际化（中/英，40+ 条翻译，t() 函数）
│   │   ├── tailwind.css      # Tailwind v4 入口 + 全局样式
│   │   ├── chat/ChatView.tsx      # 聊天界面（消息气泡、输入框、流式状态）
│   │   ├── settings/SettingsView.tsx  # 设置页（语言/记忆/搜索/高级参数）
│   │   ├── persona/PersonaView.tsx    # 角色选择页（6 预设 + 搜索）
│   │   └── providers/ProviderSettings.tsx  # 供应商配置（129 供应商/4774 模型）
│   ├── agent/                # Agent 核心逻辑
│   │   ├── core/             # Agent 主循环
│   │   ├── providers/        # 多模型 API 适配
│   │   ├── memory/           # 记忆系统
│   │   └── tools/            # 工具注册与调用
│   ├── bridge.ts             # 前后端桥接（Chaquopy IPC）
│   └── data/
│       ├── providers-registry.json  # 129 供应商 + 4774 模型数据
│       └── models-dev-raw.json      # models.dev 原始数据
├── _legacy/                  # 旧版 Chatbox UI 代码备份
├── capacitor.config.ts       # Capacitor 配置
└── package.json

hermes-backend/               # Python Agent 后端
├── run_agent.py              # Agent 主循环
├── chaquopy_entry.py         # Chaquopy 入口
├── providers/                # AI 模型调用
├── tools/                    # 80+ 工具（搜索/文件/智能家居等）
├── memory/                   # 记忆管理
└── persona/                  # 人设系统

capacitor-app/                # Capacitor 精简版（暂搁置）
src/shared/                   # 前后端共享类型
```

---

## 文件功能说明

### UI 层（`app/src/ui/`）— Tailwind CSS，无第三方 UI 库

| 文件 | 功能 | 状态 |
|------|------|------|
| `App.tsx` | 顶栏标题 + 4 Tab 底部导航 + 视图路由 | ✅ |
| `stores.ts` | Zustand：聊天消息流、模型配置、语言切换 | ✅ |
| `types.ts` | Message、Persona、AgentConfig 类型 | ✅ |
| `i18n.ts` | 中/英双语，`t(key)` 取文案，`setLang()` 切换 | ✅ |
| `tailwind.css` | Tailwind v4 入口 + 全局暗色主题 | ✅ |
| `chat/ChatView.tsx` | 聊天：消息列表 + Markdown 渲染 + 流式状态 + 输入框 | ✅ |
| `settings/SettingsView.tsx` | 设置：语言切换 + 记忆/搜索开关 + 高级参数 | ✅ |
| `persona/PersonaView.tsx` | 角色：6 预设（Hermes/Muse/Hephaestus 等）+ 搜索 | ✅ |
| `providers/ProviderSettings.tsx` | 供应商：129 个，展开看模型列表，支持实时拉取 | ✅ |

### Agent 核心（`app/src/agent/`）

| 文件 | 功能 | 状态 |
|------|------|------|
| `core/agent.ts` | Agent 主循环（消息→人设→模型→流式返回） | 🚧 |
| `providers/registry.ts` | 模型提供商注册表 | 🚧 |
| `memory/index.ts` | IndexedDB 长期记忆 + 会话上下文 | 🚧 |
| `tools/index.ts` | MCP 工具注册与调用 | 🚧 |

### 数据文件（`app/src/data/`）

| 文件 | 内容 |
|------|------|
| `providers-registry.json` | 129 个 AI 供应商 + 4774 个模型（含上下文长度、价格） |
| `models-dev-raw.json` | models.dev 原始 API 数据 |

---

## 技术栈

| 层 | 技术 |
|----|------|
| 前端框架 | React + TypeScript |
| UI | Tailwind CSS v4 + Lucide Icons |
| 状态管理 | Zustand |
| 国际化 | 自研 i18n（40+ 条翻译，中/英） |
| Markdown | react-markdown |
| 打包 | Capacitor + Vite |
| 后端 | Python（Chaquopy 嵌入 Android） |
| CI/CD | GitHub Actions → APK |

---

## 开发阶段

### ✅ 已完成
- [x] Tailwind CSS 新 UI（8 个文件，零 Mantine 依赖）
- [x] 国际化系统（中/英，设置页切换）
- [x] 129 供应商 + 4774 模型配置页
- [x] 聊天界面（消息气泡、Markdown、流式状态）
- [x] 角色选择页（6 预设 + 搜索）
- [x] 设置页（语言/记忆/搜索/高级参数）
- [x] 旧版 UI 备份到 `_legacy/`
- [x] CI/CD 工作流

### 🚧 进行中
- [ ] Agent 核心代码串联（providers → tools → memory → persona）
- [ ] 前后端桥接（Chaquopy IPC）

### 📋 待开发（按优先级）

**P1 — Agent 能力**
- Agent 主循环（消息→人设→调模型→流式返回）
- PersonaEngine（加载/切换/管理系统提示词）
- MemoryStore（IndexedDB 长期记忆）
- ToolRegistry（MCP 工具注册/调用）
- 思考过程 UI（`<think>` 折叠、工具调用状态）

**P2 — Chaquopy 嵌入**
- Python 后端通过 Chaquopy 嵌入 Android
- 同步调用：`py.getModule().callAttr()`
- 异步持久化：共享 JSON/SQLite

**P3 — 前台服务 & 流式输出**
- Android 前台服务保活
- 聊天页接入 Agent 后端（实时流式输出）
- 工具调用状态展示

**P4 — 体验优化**
- 页面切换动画
- 消息入场动画
- 打字指示器

**P5 — 主动消息**
- 基于记忆的主动触发
- 情感状态检测
- 定时关怀推送

---

## 构建与部署

- **⚠️ 禁止本地构建 APK**（不要装 Android SDK/Gradle）
- **✅ 正确流程**：推代码到 GitHub → Actions 自动构建 → 下载 artifact
- **CI 工作流**：`.github/workflows/build-apk.yml`
- **触发条件**：push 到 main，或手动 workflow_dispatch

---

## Git 信息

- **主仓库**：`ghfast.top/https://github.com/suer781/chat-gusogst.git`
- **工作分支**：`feat/agent-core-integration`
- **推送方式**：GitHub MCP 工具（`github__push_files`）或脚本（`do_push.sh`）
