# Chat Gusogst — AI 伴侣 Agent

## 定位

星野的体验 + Agent 的大脑。下载 APK → 填 Key → 聊天，3 步搞定。

用户看到的是一个有温度的 AI 伴侣，背后是一个能记住你、理解你、主动关心你的 Agent。

## 架构：Hermes + Chatbox 缝合

| 来源 | 借鉴内容 |
|------|----------|
| **Hermes Agent** | turnStore 对话状态管理、memory 跨会话记忆、charms/fortunes 主动聊天内容、session 会话管理 |
| **Chatbox** | 多提供方管理模式、模型预设列表、连接测试、provider 设置 UI 模式 |
| **原创** | 情感化 UI、主动找你聊天调度、记忆注入 System Prompt、消息平台接入框架 |

## 文件结构（18 文件，1081 行）

```
src/
├── types/index.ts            # 融合 Hermes domain + Chatbox provider 类型
├── theme/colors.ts           # 温暖柔和情感化色板
├── services/
│   ├── llm.ts               # 多提供方流式调用（Chatbox 模式）
│   ├── memory.ts            # 跨会话记忆引擎（Hermes 模式）
│   ├── providers.ts         # 提供方预设（Chatbox 模式）
│   ├── charms.ts            # 主动聊天内容（Hermes charms/fortunes 移植）
│   └── proactive.ts         # 主动聊天调度（新功能）
├── stores/chatStore.ts       # Zustand 状态管理（Hermes turnStore 模式）
├── components/
│   ├── Avatar.tsx            # 角色头像
│   ├── ChatBubble.tsx        # 聊天气泡
│   └── MessageInput.tsx      # 消息输入
├── screens/
│   ├── ConversationList.tsx  # 对话列表 + 每日运势
│   ├── ChatScreen.tsx        # 聊天 + 记忆注入 + 流式
│   └── SettingsScreen.tsx    # Chatbox-style 提供方管理
└── app/                      # Expo Router
```

## 核心功能

- **多提供方** — OpenAI / Anthropic / DeepSeek / Gemini / 自定义
- **流式聊天** — SSE + 中途停止 + AbortController
- **跨会话记忆** — 关键词检索 + 重要性评分 + 记忆注入 Prompt
- **主动找你聊天** — 定时触发 + 时间感知问候 + AI 生成个性化消息
- **每日运势** — Hermes fortune 移植改造
- **连接测试** — Chatbox 模式，一键验证 API Key

## TODO

- [ ] SQLite + FTS5 替代 AsyncStorage
- [ ] 会话摘要自动生成
- [ ] Android 前台服务保活
- [ ] 通知监听接入微信/QQ
- [ ] 技能系统
