# Agent 核心模块详解

> 更新时间：2026-05-17
> 位置：`app/src/agent/`

---

## 概述

chat-gusogst 的 Agent 核心是纯 TypeScript 实现，运行在 Android WebView 内。
从 Hermes Agent（Python）的设计理念移植而来，但代码完全重写。

```
app/src/agent/
├── core/agent.ts        # Agent 主类
├── providers/           # 模型供应商适配器
│   ├── index.ts         #   注册表
│   ├── openai.ts        #   OpenAI 兼容
│   └── anthropic.ts     #   Anthropic
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
    └── types.ts         #   re-export
```

---

## 1. Agent 主类 (core/agent.ts)

### 类结构

```typescript
class Agent {
  private provider: ProviderAdapter   // 当前模型适配器
  private modelConfig: ModelConfig     // 模型配置
  private tools: ToolRegistry          // 工具注册表
  private memory: MemoryManager        // 记忆管理器
  private config: AgentConfig          // 全局配置
  private history: Message[]           // 对话历史
  private persona: Persona             // 当前角色人设
  private mcpManager: MCPManager       // MCP 管理器
  private aborted: boolean             // 中断标记
}
```

### 核心方法

| 方法 | 功能 |
|------|------|
| `constructor(config)` | 初始化 Provider、ToolRegistry、Memory、MCP |
| `initMCP(configs?)` | 连接 MCP 服务器，注册远程工具 |
| `sendMessage(content)` | 主循环：构建消息 → 调用 LLM → 处理工具 → yield 事件 |
| `abort()` | 中断当前生成 |
| `getHistory()` | 获取对话历史 |
| `clearHistory()` | 清空对话 |

### 事件流

`sendMessage()` 是一个 AsyncGenerator，产出 `AgentEvent`：

```typescript
type AgentEvent =
  | { type: 'thinking'; content: string }     // 思考过程
  | { type: 'content'; content: string }       // 正文内容（流式）
  | { type: 'tool_call'; name: string; args: any }  // 调用工具
  | { type: 'tool_result'; name: string; result: any } // 工具结果
  | { type: 'done' }                           // 生成完成
  | { type: 'error'; error: string }           // 错误
```

### 消息循环流程

```
sendMessage(userContent)
  │
  ├─ 1. 构建 messages 数组
  │     ├─ system prompt（persona + 记忆上下文）
  │     ├─ history（之前的对话）
  │     └─ new user message
  │
  ├─ 2. 调用 provider.chat(messages, tools)
  │     └─ 流式返回 LLM 响应
  │
  ├─ 3. 解析响应
  │     ├─ 有文本内容 → yield { type: 'content' }
  │     └─ 有工具调用 → yield { type: 'tool_call' }
  │
  ├─ 4. 执行工具（如果有的话）
  │     ├─ 从 ToolRegistry 查找
  │     ├─ 调用 handler
  │     └─ yield { type: 'tool_result' }
  │
  ├─ 5. 将工具结果追加到 messages，回到步骤 2
  │     （多轮工具调用循环）
  │
  └─ 6. yield { type: 'done' }
```

---

## 2. 模型供应商 (providers/)

### Provider 注册表 (index.ts)

```typescript
const PROVIDERS: Record<string, ProviderAdapter> = {}
function getProvider(name: string): ProviderAdapter
function registerProvider(name: string, adapter: ProviderAdapter): void
```

### ProviderAdapter 接口

```typescript
interface ProviderAdapter {
  chat(
    messages: Message[],
    tools?: ToolDefinition[],
    config?: ModelConfig
  ): AsyncGenerator<ChatChunk>
}
```

### OpenAI 适配器 (openai.ts)

- 覆盖 128 个供应商（通过 `api_base` 区分）
- 支持 `/v1/chat/completions` 端点
- 支持 SSE 流式传输
- 自动处理 `<think>` 标签（思考过程）

### Anthropic 适配器 (anthropic.ts)

- 独立实现，不复用 OpenAI 格式
- 支持 `/v1/messages` 端点
- 支持 SSE 流式传输
- 处理 `thinking` 内容块

---

## 3. 工具系统 (tools/)

### ToolRegistry (registry.ts)

```typescript
class ToolRegistry {
  register(definition: ToolDefinition, handler: ToolHandler): void
  setMCPManager(manager: MCPManager): void
  registerMCPTools(): void
  getToolDefinitions(): ToolDefinition[]
  execute(name: string, args: any): Promise<any>
}
```

### 内置工具

| 工具 | 用途 |
|------|------|
| `search` | 联网搜索（search.ts） |
| `get_current_time` | 获取当前时间 |

> 更多工具通过 MCP 扩展接入。

---

## 4. MCP 扩展 (mcp/)

### 架构

```
MCPManager
  ├── MCPClient (server-1)  → 远程 MCP 服务器 1
  ├── MCPClient (server-2)  → 远程 MCP 服务器 2
  └── MCPClient (server-N)  → 远程 MCP 服务器 N
```

### MCPManager

```typescript
class MCPManager {
  loadConfigs(configs: MCPServerConfig[]): void
  async connectAll(): Promise<void>
  async disconnectAll(): Promise<void>
  getTools(): MCPToolInfo[]
  async callTool(serverName: string, toolName: string, args: any): Promise<any>
}
```

---

## 5. 记忆系统 (memory/)

基于 IndexedDB 的两层记忆提取：

```
用户消息
  ├─ 第一层：即时提取（关键词、实体、情感）
  │     └─ 存入 IndexedDB 短期记忆
  └─ 第二层：深度提取（偏好、关系、事件）
        └─ 存入 IndexedDB 长期记忆（带信任评分）
```

---

## 6. Hermes 连接器 (hermes/connector.ts)

6 个平台的连接流程数据（QQ / 微信 / Telegram / 飞书 / Discord / 钉钉）：

```typescript
function getConnectablePlatforms(): ConnectFlow[]
function matchPlatform(input: string): ConnectFlow | null
```

---

## 7. 类型系统 (shared/)

```typescript
// Agent 全局配置
interface AgentConfig {
  model: ModelConfig
  persona: Persona
  provider: string
  memory: MemoryConfig
}

// 模型配置
interface ModelConfig {
  provider: string
  model: string
  apiKey?: string
  apiBase?: string
  temperature?: number
  maxTokens?: number
  topP?: number
}

// 角色人设
interface Persona {
  id: string
  name: string
  emoji: string
  systemPrompt: string
  avatar?: string
}
```

### 类型文件关系

```
shared/agent-types.ts  ← Agent 核心类型
shared/module-types.ts ← UI 模块类型（旧版兼容）
shared/types.ts        ← re-export 统一入口
```

---

## 开发指南

### 添加新 Provider

1. 在 `providers/` 下创建新文件
2. 实现 `ProviderAdapter` 接口
3. 在 `index.ts` 中注册

### 添加新内置工具

1. 在 `core/agent.ts` 的 `registerBuiltinTools()` 中注册
2. 定义 `ToolDefinition`（JSON Schema 格式）
3. 实现 `ToolHandler` 异步函数

### 添加新 MCP 服务器

1. 创建 `MCPServerConfig`
2. 在 UI 设置页中配置
3. Agent 启动时自动连接
