# chat-gusogst 整体架构

> 更新时间：2026-05-17

---

## 一句话概括

**Android 原生 App（Capacitor + React）+ TypeScript Agent 引擎 + Hermes Python 后端（Chaquopy 嵌入），三者通过应用内 IPC 直连，不走网络。**

---

## 架构总览

```
┌─────────────────────────────────────────────────┐
│                Android App (APK)                 │
│                                                  │
│  ┌──────────────────────────────────────────┐   │
│  │         Capacitor WebView Layer           │   │
│  │  ┌────────────────────────────────────┐  │   │
│  │  │     React UI (app/src/ui/)         │  │   │
│  │  │  Tailwind CSS v4 · i18n · Zustand  │  │   │
│  │  └──────────┬─────────────────────────┘  │   │
│  │             │ UIAdapter 接口              │   │
│  │  ┌──────────▼─────────────────────────┐  │   │
│  │  │     Bridge Layer (bridge.ts)        │  │   │
│  │  │  UI 事件 ↔ Agent 事件 双向转换      │  │   │
│  │  └──────────┬─────────────────────────┘  │   │
│  └─────────────┼────────────────────────────┘   │
│                │ JS Bridge / Chaquopy IPC        │
│  ┌─────────────▼────────────────────────────┐   │
│  │       TypeScript Agent (app/src/agent/)   │   │
│  │  ┌──────────┐ ┌────────┐ ┌────────────┐  │   │
│  │  │ Providers │ │ Tools  │ │    MCP     │  │   │
│  │  │ (129个)   │ │ (内置)  │ │  (外部扩展) │  │   │
│  │  └──────────┘ └────────┘ └────────────┘  │   │
│  │  ┌──────────┐ ┌────────────────────────┐  │   │
│  │  │  Memory  │ │ Hermes Connector       │  │   │
│  │  │(IndexedDB)│ │ (平台连接流程数据)      │  │   │
│  │  └──────────┘ └────────────────────────┘  │   │
│  └───────────────────────────────────────────┘   │
│                                                  │
│  ┌───────────────────────────────────────────┐   │
│  │    Python Backend (hermes-backend/)        │   │
│  │    通过 Chaquopy 嵌入 APK 进程内            │   │
│  │  ┌──────────┐ ┌────────┐ ┌────────────┐  │   │
│  │  │  Agent   │ │ Tools  │ │  Gateway   │  │   │
│  │  │  (核心)   │ │ (70+)  │ │  (20+ 平台) │  │   │
│  │  └──────────┘ └────────┘ └────────────┘  │   │
│  │  ┌──────────┐ ┌────────┐ ┌────────────┐  │   │
│  │  │  Skills  │ │Plugins │ │  Endpoints │  │   │
│  │  │  (25类)   │ │ (记忆等) │ │  Prober   │  │   │
│  │  └──────────┘ └────────┘ └────────────┘  │   │
│  └───────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

---

## 三层职责

| 层级 | 技术栈 | 职责 | 文件位置 |
|------|--------|------|----------|
| **UI 层** | React + Tailwind + Zustand | 渲染界面、处理用户交互、展示消息气泡 | `app/src/ui/` |
| **Bridge 层** | TypeScript | UI 事件 ↔ Agent 事件双向转换，屏蔽后端实现细节 | `app/src/bridge.ts` |
| **Agent 层** | TypeScript + Python | 模型调用、工具执行、记忆管理、MCP 扩展 | `app/src/agent/` + `hermes-backend/` |

---

## 通信方式

### ❌ 不用 HTTP

早期方案（`127.0.0.1:8642`）已废弃。同一进程内走网络是浪费。

### ✅ 应用内 IPC

| 方向 | 机制 | 说明 |
|------|------|------|
| **TS → Python** | `Chaquopy Java ↔ Python` 直调 | 同步调用，`py.getModule().callAttr()` |
| **Python → TS** | 共享本地文件（JSON/SQLite） | 异步通知，Python 写文件 → TS 轮询/监听 |
| **UI ↔ Agent** | Bridge.ts 事件流 | `AsyncGenerator<AgentEvent>` |

---

## 前端 UI 架构

### 新版 UI（当前方向）

```
app/src/ui/
├── App.tsx              # 主布局 + 底部 4 Tab 导航
├── stores.ts            # Zustand 全局状态
├── types.ts             # UI 层类型定义
├── i18n.ts              # 中英双语 40+ 条翻译
├── chat/
│   └── ChatView.tsx     # 核心：消息气泡、Markdown、流式状态
├── settings/
│   └── SettingsView.tsx # 语言、记忆开关、温度/Token 滑块
├── persona/
│   └── PersonaView.tsx  # 6 个预设角色 + 搜索
└── providers/
    └── ProviderSettings.tsx  # 129 供应商 + 4774 模型
```

### 旧版 UI（已废弃）

`app/_legacy/renderer/` — 不再使用，保留作参考。

---

## Agent 核心架构

### TypeScript Agent（app/src/agent/）

```
app/src/agent/
├── core/agent.ts        # Agent 主类
├── providers/           # 模型供应商适配器
│   ├── index.ts         #   注册表
│   ├── openai.ts        #   OpenAI 兼容（128+ 供应商）
│   └── anthropic.ts     #   Anthropic Claude
├── tools/               # 工具系统
│   ├── registry.ts      #   注册表
│   └── search.ts        #   联网搜索
├── mcp/                 # MCP 扩展
│   ├── client.ts        #   单服务器连接
│   ├── manager.ts       #   多服务器管理
│   └── types.ts         #   类型
├── memory/              # 记忆系统
│   └── manager.ts       #   长期记忆
├── hermes/              # Hermes 平台连接器
│   └── connector.ts     #   连接流程数据
└── shared/              # 类型定义
    ├── agent-types.ts   #   Agent 核心类型
    ├── module-types.ts  #   UI 模块类型
    └── types.ts         #   re-export 统一入口
```

### Python 后端（hermes-backend/）

```
hermes-backend/
├── agent/               # Agent 核心（run_agent.py、12 个模块）
├── tools/               # 70+ 工具（web、file、browser、tts、vision…）
├── plugins/             # 插件（memory、kanban、image_gen、model…）
├── skills/              # 25 个分类技能库
├── gateway/
│   ├── platforms/       # 20+ 聊天平台适配器
│   ├── run.py           # 网关运行器
│   └── config.py        # 网关配置
├── web/                 # Web UI
├── ui-tui/              # 终端 UI
└── .plans/              # 开发计划文档
```

---

## 模型供应商支持

| 类型 | 数量 | 实现 |
|------|------|------|
| OpenAI 兼容 | 128 个 | `providers/openai.ts` 统一适配 |
| Anthropic | 1 个 | `providers/anthropic.ts` 独立实现 |
| **合计** | **129 个** | 覆盖 4774 个模型 |

供应商列表从上游 [chatbox 项目](https://github.com/chatboxai/chatbox) 同步，包含 API 基础 URL 和模型名称映射。

---

## 端点嗅探系统

用户只需填域名 + API Key，系统自动探测可用路径：

1. 尝试已知 API 路径（`/v1/chat/completions`、`/v1/messages` 等）
2. 根据响应自动推断模型格式（openai / anthropic / custom）
3. 用信誉度公式动态排序端点优先级
4. 失败降级、成功上位，状态持久化

详见 [ENDPOINT_PROBER.md](./ENDPOINT_PROBER.md)

---

## 构建与部署

```
代码推送 → GitHub Actions CI → 自动构建 APK → 下载 artifact
```

- **禁止本地构建**（手机上装 Android SDK 占 2.4GB+）
- CI 工作流：`.github/workflows/build-apk.yml`
- 详见 [BUILD_DEPLOY.md](./BUILD_DEPLOY.md)

---

## 数据流示例：用户发一条消息

```
1. 用户在 ChatView 输入文字
      ↓
2. stores.ts 更新消息列表
      ↓
3. bridge.ts 调用 agent.sendMessage(content)
      ↓
4. Agent 构建 messages 数组（含 persona 系统提示词 + 历史 + 新消息）
      ↓
5. Provider 调用 LLM API（流式）
      ↓
6. AgentEvent 逐个 yield 出来（thinking / content / tool_call / tool_result / done）
      ↓
7. bridge.ts 将 AgentEvent 转为 UI 事件
      ↓
8. ChatView 实时更新消息气泡
```

---

## 文件索引

| 文档 | 内容 |
|------|------|
| [DEV_PLAN.md](./DEV_PLAN.md) | 7 Phase 开发路线图（50+ 功能） |
| [AGENT_CORE.md](./AGENT_CORE.md) | TS Agent 核心模块详解 |
| [CHAQUOPY_INTEGRATION.md](./CHAQUOPY_INTEGRATION.md) | Python 嵌入 Android 方案 |
| [ENDPOINT_PROBER.md](./ENDPOINT_PROBER.md) | 端点嗅探 + 信誉度系统 |
| [BUILD_DEPLOY.md](./BUILD_DEPLOY.md) | CI/CD 构建部署 |
| [UI_ADAPTER.md](./UI_ADAPTER.md) | UI 隔离层接口设计 |
