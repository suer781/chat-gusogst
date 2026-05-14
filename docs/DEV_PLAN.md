# chat-gusogst 开发规划

## 项目概述

**chat-gusogst** — AI 虚拟恋人 Android 应用，基于两个开源项目拆解重组。

### 定位
对标猫箱/星野等移动端 AI 陪伴应用，但完全开源、本地化、无付费。主打便携，Android 优先。

### 核心能力目标
- 💬 AI 聊天：多模型支持（OpenAI/Anthropic/自定义端点），流式输出，人设系统
- 🔍 联网搜索：Tavily + DuckDuckGo 免费方案，可扩展
- 🧠 记忆系统：长期记忆、偏好学习、情感追踪
- 🛠️ MCP 工具：可扩展的工具系统，支持自定义 MCP Server
- 🌐 社交连接：微信等社交平台接入（第二阶段）
- ⚙️ 全参数可调：模型、温度、搜索、人设、记忆等全部可在 UI 中配置

### 技术路线
- TypeScript 全栈重写，不依赖 Python 运行时
- React + Vite 构建 UI
- Capacitor 打包 Android APK
- 用户安装即用，无需 Termux 或编译环境

---

## 架构设计

### 父项目来源

| 父项目 | 仓库 | 提供什么 |
|--------|------|--------|
| Hermes Agent | NousResearch/hermes-agent | Agent 循环、多模型适配、记忆、人设、社交连接器 |
| Chatbox | chatboxai/chatbox | UI 框架、Provider 系统、设置界面、Electron 结构 |

两个父项目仅作为参考和代码提取来源，不作为运行依赖。

### 目录结构

```
chat-gusogst/
├── app/                          ← 新项目（唯一需要的目录）
│   ├── src/
│   │   ├── agent/                ← Agent 核心（从 Hermes 重写）
│   │   │   ├── core/
│   │   │   │   ├── agent.ts      ← 主循环
│   │   │   │   └── persona.ts    ← 人设管理
│   │   │   ├── providers/        ← 模型适配器
│   │   │   ├── tools/            ← 工具系统
│   │   │   └── memory/
│   │   │       └── manager.ts    ← 记忆系统
│   │   ├── ui/                   ← React UI
│   │   ├── bridge/               ← Agent ↔ UI 桥接
│   │   └── shared/types/         ← 全局类型
│   ├── android/                  ← Capacitor Android 项目
│   ├── package.json / capacitor.config.ts / tsconfig.json
│   └── index.html
├── hermes-backend/               ← 参考来源（只读）
├── chatbox-frontend/             ← 参考来源（只读）
├── docs/DEV_PLAN.md             ← 本文件
└── README.md
```

---

## 当前状态（2026-05-14）

### ✅ 已完成

| 模块 | 文件 | 状态 |
|------|------|------|
| 类型定义 | shared/types/index.ts | ✅ |
| OpenAI Provider | agent/providers/openai.ts | ✅ 流式 |
| Anthropic Provider | agent/providers/anthropic.ts | ✅ |
| Provider 注册表 | agent/providers/index.ts | ✅ |
| Agent 核心循环 | agent/core/agent.ts | ✅ 含 tool call loop |
| 人设系统 | agent/core/persona.ts | ✅ 6预设+自定义 |
| 记忆系统 | agent/memory/manager.ts | ✅ |
| 工具注册表 | agent/tools/registry.ts | ✅ |
| 搜索工具 | agent/tools/search.ts | ✅ Tavily+DDG |
| Bridge | bridge/index.ts | ✅ |
| Zustand Stores | ui/stores.ts | ✅ |
| 聊天界面 | ui/chat/ChatView.tsx | ✅ Markdown |
| 设置界面 | ui/settings/SettingsView.tsx | ✅ 全参数 |
| 人设界面 | ui/persona/PersonaView.tsx | ✅ |
| CSS 主题 | ui/styles.css | ✅ 深色+移动端 |
| TypeScript 编译 | — | ✅ 0错误 |
| Vite 构建 | — | ✅ 290KB JS |

### ⏳ 待完成

#### P0 — 能跑起来
- [ ] pnpm dev 实际运行
- [ ] 端到端测试
- [ ] 修复运行时 bug

#### P1 — Android APK
- [ ] Capacitor Android 项目
- [ ] 原生适配（状态栏/键盘/返回键）
- [ ] 签名打包
- [ ] 真机测试

#### P2 — 核心完善
- [ ] 流式输出优化
- [ ] MCP 工具集成
- [ ] 聊天记录持久化（IndexedDB）
- [ ] 记忆系统增强（向量搜索）
- [ ] Anthropic 流式 SSE 完整实现
- [ ] 上下文窗口精确管理

#### P3 — 差异化
- [ ] 社交平台连接器（微信）
- [ ] 多模态支持
- [ ] 主动消息
- [ ] 离线模型

---

## 技术决策

### 为什么重写？
Hermes 是 Python。Android 上跑 Python 需要 Chaquopy（兼容性差）或 Termux（体验差）。TypeScript 重写 = 纯 JS 栈 = Capacitor 直接打包 = 用户安装即用。

### 数据存储
| 数据 | 当前 | 后续 |
|------|------|------|
| 设置 | localStorage | localStorage |
| 记忆 | localStorage | IndexedDB |
| 聊天 | 内存 | IndexedDB |

### 搜索引擎
- DuckDuckGo：免费，无需 Key
- Tavily：AI 优化，需 Key，有免费额度
