# Chat Gusogst — AI 记忆聊天伴侣

## 一句话定位

一个装在 Android 手机上的 AI Agent 应用，拥有持久记忆、情感陪伴，未来能接管微信/QQ 等消息平台，成为用户的智能助手。

---

## 当前状态（2026-05-06）

### 已完成

- **Expo Router 6 项目骨架** — 基于 expo-54，TypeScript，file-based routing
- **基础 UI** — 三个页面：对话列表 / 聊天界面 / 设置页
- **聊天气泡组件** — 用户（右）+ AI（左），渐变尾巴装饰
- **消息输入组件** — 文本输入 + 发送按钮，底线装饰
- **角色头像组件** — 动漫风格 SVG 头像（猫耳女孩）
- **设置页** — API Key 输入（OpenAI/Azure/DeepSeek）
- **LLM 服务** — OpenAI SDK 封装，支持流式回复
- **内存记忆（store）** — Zustand store，conversation + messages，AsyncStorage 持久化
- **TTS 语音** — expo-speech 集成

### 当前文件结构

```
chat-gusogst/
├── App.tsx                        # 入口（加载字体）
├── index.ts                       # 注册根组件
├── app.json                       # Expo 配置（com.chatgusogst.app）
├── package.json                   # 依赖清单
├── src/
│   ├── app/                       # Expo Router 页面
│   │   ├── _layout.tsx            # 根布局（Stack）
│   │   ├── index.tsx              # 首页 → 跳转路由
│   │   ├── chat/[id].tsx          # 聊天页
│   │   └── settings.tsx           # 设置页
│   ├── screens/                   # 业务页面组件
│   │   ├── ConversationList.tsx   # 对话列表
│   │   ├── ChatScreen.tsx         # 聊天界面
│   │   └── SettingsScreen.tsx     # 设置界面
│   ├── components/                # 通用组件
│   │   ├── Avatar.tsx             # 角色头像
│   │   ├── ChatBubble.tsx         # 聊天气泡
│   │   └── MessageInput.tsx       # 消息输入
│   ├── services/
│   │   └── llm.ts                 # LLM API 调用
│   ├── stores/
│   │   └── chatStore.ts           # Zustand 状态管理
│   ├── theme/
│   │   └── colors.ts              # 品牌色板
│   └── types/
│       └── index.ts               # TypeScript 类型
└── assets/                        # 图标、启动图
```

---

## 架构目标（两阶段进化）

### 阶段一：AI 记忆伴侣 App（当前）

普通聊天应用，但 AI 拥有持久记忆和情感。

```
┌─────────────────────────┐
│    Chat Gusogst (Expo)  │
│  ┌───────────────────┐  │
│  │  UI (Expo Router) │  │
│  ├───────────────────┤  │
│  │  Hermes Memory    │  │  ← 记忆引擎（TypeScript 移植）
│  │  SQLite + FTS5    │  │
│  ├───────────────────┤  │
│  │  LLM Service      │  │  ← OpenAI / DeepSeek / 本地模型
│  └───────────────────┘  │
└─────────────────────────┘
```

**核心升级清单**：
- [ ] Zustand → **SQLite + FTS5**（expo-sqlite）
- [ ] 原始 JSON → **Hermes 记忆框架**（摘要、重要性评分、向量召回）
- [ ] 裸 LLM 调用 → **System Prompt + 记忆注入**（人设、关系、偏好）

### 阶段二：Android Agent 机器人（终极目标）

后台常驻的 AI Agent，接管用户消息，自主处理事务。

```
┌──────────────────────────────────────────┐
│           Chat Gusogst APK               │
│                                          │
│  ┌──────────┐  ┌──────────────────────┐  │
│  │ Expo UI  │  │ Android FG Service   │  │
│  │ (聊天界面)│  │ (前台服务,永不被杀)   │  │
│  └─────┬────┘  └──────┬───────────────┘  │
│        │              │                  │
│  ┌─────▼──────────────▼───────────────┐  │
│  │       Hermes Agent Engine          │  │
│  │  记忆 / 技能 / 工具调用 / 自改进    │  │
│  └─────────────┬──────────────────────┘  │
│                │                         │
│  ┌─────────────▼──────────────────────┐  │
│  │        Platform Adapters           │  │
│  │  通知监听 │ 无障碍 │ RPC桥接        │  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

**关键技术栈**：
- **后台保活**：Android Foreground Service + 通知常驻
- **消息接入**：
  - NotificationListenerService（读取微信/QQ 通知）
  - AccessibilityService（读取 + 模拟回复）
  - 未来：微信桌面协议桥接（需电脑中转）
- **Hermes 引擎**：Pyodide（WASM 跑 Python）或 Kotlin/TS 重写核心

---

## Hermes 记忆框架参考

参考项目：`~/project/hermes-agent/`（NousResearch/HermesAgent）

核心概念：
- **FTS5 全文搜索** → 跨会话记忆召回
- **LLM 摘要压缩** → 长期记忆提炼
- **Honcho 辩证用户建模** → 自动理解用户偏好和关系
- **自主技能创建** → Agent 自己写工具代码
- **自我改进闭环** → 从失败中学习

移植到 Expo 的方案：
- expo-sqlite 支持 FTS5（需确认 Android 端）
- 向量相似度可用 sqlite-vss 或简单关键词匹配替代
- LLM 摘要逻辑直接调 OpenAI API

---

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 框架 | Expo (React Native) | 54 |
| 路由 | Expo Router | 6 |
| 语言 | TypeScript | 5.8 |
| 状态 | Zustand + AsyncStorage | — |
| LLM | OpenAI SDK | — |
| 后端 | 无（纯客户端） | — |
| 数据库 | AsyncStorage（待升级 SQLite） | — |

---

## 开发规范

### 环境
- 开发设备：Xiaomi 15Pro，Android 15
- 开发方式：通过 Termux MCP server 在手机上写代码
- 项目路径：`~/project/github.com/chat-gusogst/`

### Git
- 本地仓库，暂无远端
- commit 风格：`feat: xxx` / `fix: xxx` / `refactor: xxx`

### 代码风格
- 所有文本用中文
- 组件用函数式 + Hooks
- 文件命名：组件 PascalCase，工具 camelCase

### 修改规则（给 AI 的指引）
- 改代码时先 `cat` 目标文件确认当前内容
- 用 `sed -i 's/旧/新/g'` 精确替换，避免 `echo >` 覆盖
- 改完后 `cat` 验证修改结果
- 重要改动前先 `git add -A && git commit -m "backup: before xxx"`
- **不要在 Termux 里运行 `npx expo start`**——会卡死终端

---

## 关联项目

| 项目 | 路径 | 关系 |
|------|------|------|
| hermes-agent | `~/project/hermes-agent/` | 记忆框架参考 |
| hermes-patch | `~/project/hermes-patch/` | Hermes 补丁 |
| hermes-skill-dev | `~/project/hermes-skill-dev/` | 技能开发参考 |
| tts-server-android | `~/project/tts-server-android/` | TTS 服务参考 |
| lovemo-fork | `~/project/github.com/lovemo-fork/` | 对标竞品（AI 聊天伴侣） |

---

## 遗留问题 & TODO

- [ ] 首页对话列表无数据时显示空状态（已修复，但需验证字体加载）
- [ ] App.tsx 加载字体时 expo-asset 模块找不到（可能需要 npx expo install expo-asset）
- [ ] 移动端键盘弹起时聊天气泡偏移（需 KeyboardAvoidingView）
- [ ] 三个页面中的文本硬编码，后续需国际化或统一管理
- [ ] 测试端口冲突（8081/8082）导致的问题
