# chat-gusogst

> ⚠️ **项目仍在早期开发阶段，尚未可用。**
> 核心代码框架已搭建完成（Agent 循环、UI 界面、模型适配），但尚未经过实际运行测试，
> 不包含可直接安装的 APK。欢迎关注，但现阶段请不要将其作为可用软件使用。
> 详见 [开发规划](docs/DEV_PLAN.md) 了解当前进度和待完成事项。

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

## 致谢

- [Hermes Agent](https://github.com/NousResearch/hermes-agent) — Agent 核心参考
- [Chatbox](https://github.com/chatboxai/chatbox) — UI 框架参考

## 许可证

[AGPL-3.0](LICENSE)
