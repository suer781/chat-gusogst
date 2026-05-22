# 代码地图 (CODEMAP)

> 最后更新：2026-05-17

本文件是项目的导航指南，说明每块代码在哪里、为什么在那里、怎么用。

---

## 两个目录的关系

```
chat-gusogst/
├── app/src/          ← 【唯一编译源码】tsconfig include 只包含这里
├── _our_modules/     ← 【上游参考】完整 Agent 实现，被复制到 app/src/agent/
└── hermes-backend/   ← 【Python 后端】Chaquopy 打包用，不参与 TS 编译
```

### 规则
- **所有 import 必须指向 `app/src/` 内的文件**（因为 `@/*` 映射到 `app/src/*`）
- `_our_modules/` 不参与编译，只作参考备份
- 修改代码只改 `app/src/`，不要改 `_our_modules/`
- ~~根目录 `src/`~~ 已删除，Provider 注册表已搬到 `app/src/providers/`

---

## app/src/ — 应用源码（唯一编译目录）

### 入口

| 文件 | 作用 |
|------|------|
| `main.tsx` | Vite 入口，渲染 `<App />` |
| `bridge.ts` | UI ↔ Agent 桥接层，ChatView 通过它调用 Agent |

### UI 层 (`ui/`)

| 文件 | 作用 |
|------|------|
| `ui/App.tsx` | 主布局 + 底部 4 Tab 导航 |
| `ui/stores.ts` | Zustand 全局状态 |
| `ui/types.ts` | UI 层类型定义 |
| `ui/i18n.ts` | 中英双语 40+ 条翻译 |
| `ui/chat/ChatView.tsx` | 消息气泡、Markdown 渲染 |
| `ui/init.ts` | Agent 初始化（config → store 映射） |
| `ui/settings/SettingsView.tsx` | 设置页主入口（6 卡片导航） |
| `ui/settings/BasicSettings.tsx` | 基础设置（语言、护眼、主题、毛玻璃、字体） |
| `ui/settings/ModelSettings.tsx` | 模型参数（温度、Token、Thinking） |
| `ui/settings/MemorySettings.tsx` | 记忆开关配置 |
| `ui/settings/SearchSettings.tsx` | 搜索设置 |
| `ui/settings/PlatformSettings.tsx` | 平台设置 |
| `ui/settings/AboutSettings.tsx` | 关于页面 |
| `ui/persona/PersonaView.tsx` | 6 个预设角色 + 搜索 |
| `ui/persona/PersonaProfileView.tsx` | 角色详情页 |
| `ui/persona/PersonaSettingsModal.tsx` | 角色编辑弹窗 |
| `ui/components/TestDisclaimer.tsx` | 测试免责声明组件 |
| `ui/providers/ProviderSettings.tsx` | 129 供应商 + 4774 模型选择 |

### Provider 注册表 (`providers/`) ← 新架构，从根目录 src/ 搬入

| 文件 | 行数 | 作用 |
|------|------|------|
| `providers/definitions/agent-core.ts` | 657 | 129 个供应商注册表（带 transport/auth/baseUrl） |
| `providers/index.ts` | 15 | 导出 PROVIDER_DEFINITIONS + findProviderDef |

### Agent 层 (`agent/`)

| 文件 | 作用 | 来源 |
|------|------|------|
| `agent/core/agent.ts` | Agent 主类（219 行） | 从 `_our_modules/agent/` 复制 |
| `agent/core/persona.ts` | 角色人设管理 | 同上 |
| `agent/providers/index.ts` | Provider 适配器注册表（运行时用） | 同上 |
| `agent/providers/openai.ts` | OpenAI 兼容适配器 | 同上 |
| `agent/providers/anthropic.ts` | Anthropic 适配器 | 同上 |
| `agent/tools/registry.ts` | 工具注册表 | 同上 |
| `agent/tools/search.ts` | 联网搜索工具 | 同上 |
| `agent/mcp/client.ts` | MCP 单服务器连接 | 同上 |
| `agent/mcp/manager.ts` | MCP 多服务器管理 | 同上 |
| `agent/mcp/types.ts` | MCP 类型 | 同上 |
| `agent/mcp/index.ts` | MCP 导出 | 同上 |
| `agent/memory/manager.ts` | 长期记忆（IndexedDB） | 同上 |
| `agent/hermes/bridge.ts` | Hermes 桥接 | 同上 |
| `agent/hermes/client.ts` | Hermes 客户端 | 同上 |
| `agent/hermes/connector.ts` | 平台连接流程数据 | 同上 |
| `agent/hermes/index.ts` | Hermes 导出 | 同上 |
| `agent/hermes/platform_connect_tool.ts` | 平台连接工具 | 同上 |
| `agent/hermes/types.ts` | Hermes 类型 | 同上 |

### 共享类型 (`shared/`)

| 文件 | 作用 |
|------|------|
| `shared/agent-types.ts` | Agent 核心类型 + Provider 类型（94+45 行） |
| `shared/types.ts` | re-export 统一入口（`export * from './agent-types'`） |
| `shared/module-types.ts` | UI 模块类型 |

---

## _our_modules/ — 上游参考代码

**不要直接 import 这里的文件。** 它是 `app/src/agent/` 的源，改了 agent 代码要同步回来。

| 目录 | 作用 | 与 app/src/ 的关系 |
|------|------|-------------------|
| `_our_modules/agent/` | 完整 Agent（18 个 ts 文件） | = `app/src/agent/` 的复制源 |
| `_our_modules/bridge/` | Bridge 原版 | = `app/src/bridge.ts` 的修改源 |
| `_our_modules/shared/types/` | 原版类型定义 | = `app/src/shared/agent-types.ts` 的复制源 |

---

## hermes-backend/ — Python 后端

| 目录 | 作用 |
|------|------|
| `hermes-backend/agent/` | Agent 核心（run_agent.py, 12 模块） |
| `hermes-backend/tools/ | 70+ 工具（web, file, browser, tts, vision...） |
| `hermes-backend/plugins/ | 插件（memory, kanban, image_gen, model...） |
| `hermes-backend/skills/ | 25 个分类技能库 |
| `hermes-backend/gateway/ | 20+ 聊天平台适配器 |
| `hermes-backend/.plans/ | 开发计划文档 |

---

## 依赖关系图

```
ui/chat/ChatView.tsx
  └── import from '@/bridge'              → app/src/bridge.ts
        └── import from '@/agent/core/agent'  → app/src/agent/core/agent.ts
              └── import from '@/shared/types' → app/src/shared/types.ts
                    └── export * from './agent-types' → app/src/shared/agent-types.ts

ui/providers/ProviderSettings.tsx
  └── import from '@/providers'           → app/src/providers/index.ts
        └── PROVIDER_DEFINITIONS (657 行供应商注册表)

agent/providers/index.ts
  └── OpenAIProvider, AnthropicProvider (2 个运行时适配器)
```

---

## 两套 Provider 的区别

| 位置 | 内容 | 用途 |
|------|------|------|
| `app/src/providers/` (657 行) | 129 个供应商的**定义数据**（名称、baseUrl、transport、auth） | UI 展示、Provider 选择、端点嗅探 |
| `app/src/agent/providers/` (24 行) | 2 个**运行时适配器**（OpenAI、Anthropic） | Agent 实际调用 LLM API |

两套不冲突：`providers/` 是数据，`agent/providers/` 是执行逻辑。
