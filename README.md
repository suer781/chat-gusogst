# chat-gusogst

> ⚠️ **项目仍在早期开发阶段，尚未可用。**
> 核心代码框架已搭建完成，但尚未经过实际运行测试，不包含可直接安装的 APK。
> 详见 [开发规划](docs/DEV_PLAN.md) 了解当前进度。

AI 虚拟恋人 Android 应用。TypeScript Agent + Hermes Python 后端，129 供应商 4774 模型。

---

## 特性

- 💬 **129 供应商 4774 模型**（OpenAI / Anthropic / 自定义 / Ollama 等）
- 🔍 **联网搜索**（Tavily + DuckDuckGo 免费方案）
- 🧠 **记忆系统**（IndexedDB + 两层提取 + 信任评分）
- 🎭 **人设系统**（6 预设 + 自定义系统提示词）
- 🛠️ **MCP 工具扩展**（SSE 连接外部服务）
- 🌐 **端点嗅探**（填域名自动探测 API 路径 + 信誉度排序）
- 📱 **Android 优先**（Capacitor + React + Tailwind CSS v4）

---

## 分支策略

三个核心规则：

1. **永不合并** — main（Capacitor + Web）和 AndroidUI（纯原生 Android）两条分支各走各路，禁止 merge/rebase 互通
2. **版本进度同步** — 共享版本号和功能规划，里程碑保持一致
3. **平台差异各自消化** — 不支持的功能跳过就行，不硬凑

## 架构

```
┌─────────────────────────────────┐
│        Android App (APK)         │
│                                  │
│  React UI ── bridge.ts ── Agent  │
│  (Tailwind)   (事件转换)  (TS核心) │
│                        │         │
│              Chaquopy IPC        │
│                        │         │
│              Python Backend      │
│              (Hermes 1635文件)    │
│              70+ 工具 | 25 技能   │
└─────────────────────────────────┘
```

详见 [ARCHITECTURE.md](docs/ARCHITECTURE.md)

---

## 开发

```bash
cd app
npm install
npx tsc --noEmit          # 类型检查
npx vite --port 5176      # 本地预览
```

```bash
# 构建 APK（通过 CI，不要本地构建）
git push → GitHub Actions → 下载 artifact
```

详见 [BUILD_DEPLOY.md](docs/BUILD_DEPLOY.md)

---

## 项目结构

```
app/src/
├── agent/          # TS Agent 核心
│   ├── core/       #   Agent 主类
│   ├── providers/  #   模型供应商（129 个）
│   ├── tools/      #   工具系统
│   ├── mcp/        #   MCP 扩展
│   ├── memory/     #   记忆管理
│   └── hermes/     #   平台连接器
├── ui/             # React UI（新 Tailwind 版）
├── bridge.ts       # Agent ↔ UI 桥接
└── shared/         # 类型定义

hermes-backend/     # Python 后端（1,635 文件）
├── agent/          #   Agent 核心
├── tools/          #   70+ 工具
├── plugins/        #   插件
├── skills/         #   25 个分类
└── gateway/        #   20+ 聊天平台
```

---

## 文档

| 文档 | 内容 |
|------|------|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | 整体架构设计 |
| [DEV_PLAN.md](docs/DEV_PLAN.md) | 7 Phase 开发路线图（50+ 功能） |
| [AGENT_CORE.md](docs/AGENT_CORE.md) | TS Agent 核心模块详解 |
| [CHAQUOPY_INTEGRATION.md](docs/CHAQUOPY_INTEGRATION.md) | Python 嵌入 Android 方案 |
| [ENDPOINT_PROBER.md](docs/ENDPOINT_PROBER.md) | 端点嗅探 + 信誉度系统 |
| [BUILD_DEPLOY.md](docs/BUILD_DEPLOY.md) | CI/CD 构建部署 |
| [UI_ADAPTER.md](docs/UI_ADAPTER.md) | UI 隔离层接口设计 |

---

## 致谢

- [Hermes Agent](https://github.com/NousResearch/hermes-agent) — Agent 核心参考

## 许可证

[Apache License 2.0](LICENSE)
