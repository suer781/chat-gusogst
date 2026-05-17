# Chaquopy 集成方案

> 更新时间：2026-05-17
> 状态：设计完成，待实现

---

## 概述

**Chaquopy** 是 Android Gradle 插件，允许在 Android APK 中嵌入 Python 解释器。
chat-gusogst 使用 Chaquopy 将 Hermes Python 后端（1,635 个文件）打包进 APK，
实现应用内直接调用 Python 代码，无需独立后端服务。

---

## 为什么选 Chaquopy

| 方案 | 优点 | 缺点 |
|------|------|------|
| **HTTP Server** | 实现简单 | 同进程走网络浪费、需保活、端口冲突 |
| **Termux 外挂** | Python 全功能 | 用户需装 Termux、配置复杂 |
| **Chaquopy** ✅ | 进程内直调、零配置、用户无感 | 需处理 Python 依赖打包 |

---

## 架构

```
Android App Process
  ├── WebView (Capacitor)
  │   ├── React UI
  │   └── TypeScript Agent
  │       └── bridge.ts
  │           │ JS Bridge (Chaquopy JS ↔ Java)
  ├── Java Layer
  │   └── PythonBridge.java
  │       │ py.getModule().callAttr()
  └── Python Runtime (Chaquopy)
      └── hermes-backend/
          ├── chaquopy_entry.py
          ├── run_agent.py
          ├── agent/
          ├── tools/
          ├── plugins/
          └── skills/
```

---

## 通信协议

### 同步调用（TS → Python）

```typescript
// bridge.ts
const result = await Capacitor.Plugins.Chaquopy.call({
  module: 'chaquopy_entry',
  function: 'chat',
  args: [JSON.stringify({ messages: [...], tools: [...], config: {...} })]
})
```

```python
# chaquopy_entry.py
import json
def chat(request_json: str) -> str:
    request = json.loads(request_json)
    result = run_agent(request)
    return json.dumps(result)
```

### 异步通知（Python → TS）

Python 写入共享文件，TS 轮询或监听：

```python
# Python 侧
with open('/data/data/com.chatgusogst/files/events.jsonl', 'a') as f:
    f.write(json.dumps({'type': 'tool_call', 'name': 'search'}) + '\n')
```

```typescript
// TS 侧
const watcher = setInterval(async () => {
  const content = await readFile('events.jsonl')
  // 解析新行并处理
}, 100)
```

---

## Gradle 配置

```groovy
plugins {
    id 'com.chaquo.python' version '15.0.1'
}
android {
    defaultConfig {
        ndk { abiFilters 'arm64-v8a', 'x86_64' }
        python {
            version '3.11'
            pip {
                install 'aiohttp'
                install 'openai'
                install 'anthropic'
            }
        }
    }
}
```

---

## 实现阶段

| Phase | 内容 | 状态 |
|-------|------|------|
| 1 | 基础 IPC（Java ↔ Python 同步调用） | ⏳ |
| 2 | Agent 集成（消息传递 + 工具回传） | ⏳ |
| 3 | 流式输出（共享缓冲区 + 轮询） | ⏳ |
| 4 | 依赖优化（APK < 100MB） | ⏳ |

---

## 已知挑战

| 挑战 | 应对方案 |
|------|----------|
| Python 冷启动 2-3s | 前台 Service 预热 + .pyc 缓存 |
| APK 体积增大 | 只打包必要依赖，裁剪标准库 |
| 内存占用 | 共享进程，不用子进程 |
| 依赖冲突 | 使用 Chaquopy 虚拟环境 |
| 文件 I/O 权限 | 使用 App 私有目录 |

---

## TS Agent vs Python Agent

| 能力 | TS Agent（轻量） | Python Agent（完整） |
|------|------------------|--------------------|
| 基础对话 | ✅ | ✅ |
| 流式输出 | ✅ | ✅ |
| 内置工具 | search, time | 70+ 完整工具 |
| MCP 扩展 | ✅ | ✅ |
| 记忆系统 | IndexedDB | SQLite + 向量检索 |
| 联网搜索 | DuckDuckGo | Tavily + DDG + 更多 |
| 语音 TTS | ❌ | ✅ |
| 图片生成 | ❌ | ✅ |
| 平台网关 | ❌ | 20+ 平台 |

**策略**：Chaquopy 未就绪前用 TS Agent 提供基础对话。就绪后 Python Agent 接管全部能力。
