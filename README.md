# chat-gusogst

AI 虚拟恋人 Android 应用。基于 Chatbox UI + Hermes Agent 拆解重组，TypeScript 全栈重写。

## 特性

- 💬 多模型支持（OpenAI/Anthropic/自定义端点）
- 🔍 联网搜索（Tavily/DuckDuckGo）
- 🧠 记忆系统（偏好学习、情感追踪）
- 🎭 人设系统（6 预设 + 自定义）
- 🛠️ MCP 工具扩展
- ⚙️ 全参数可调
- 📱 Android 优先，安装即用

## 开发

```bash
cd app
pnpm install
pnpm dev          # 开发模式
pnpm build        # 构建
```

## 架构

```
app/src/
├── agent/        ← Agent 核心（从 Hermes 重写）
├── ui/           ← React UI（从 Chatbox 改造）
├── bridge/       ← Agent ↔ UI 桥接
└── shared/       ← 类型定义
```

详见 [docs/DEV_PLAN.md](docs/DEV_PLAN.md)
