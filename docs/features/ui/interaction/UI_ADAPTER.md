# UI Adapter Interface Contract

> Bridge layer between React UI and Agent core.
> Last updated: 2026-05-18

## Architecture

```
AppSettings (ui/stores.ts)
       │
       ▼ settingsToAgentConfig()
  AgentConfig (shared/agent-types.ts)
       │
       ▼ new Agent(config)
     Agent.sendMessage()
       │
       ▼ AgentEvent → StreamEvent (bridge.ts)
   ChatView.tsx (consumer)
```

## Type Definitions

### Source of Truth: `shared/agent-types.ts`

All agent-facing types are defined here. UI types in `ui/types.ts` extend them.

| Type | Owner | Description |
|------|-------|-------------|
| `AgentConfig` | agent-types.ts | Full agent configuration |
| `AgentEvent` | agent-types.ts | Events emitted by Agent |
| `Message` | agent-types.ts | Chat message with role, content, tool_calls |
| `ToolCall` | agent-types.ts | Function call from model |
| `ToolResult` | agent-types.ts | Tool execution result |
| `ModelConfig` | agent-types.ts | LLM provider/model/API settings |
| `SearchConfig` | agent-types.ts | Search engine config |
| `MCPServerConfig` | agent-types.ts | MCP server endpoint config |
| `MemoryConfig` | agent-types.ts | Memory system config |
| `Persona` | agent-types.ts | Character persona definition |
| `StreamEvent` | bridge.ts | UI-friendly event format (translated from AgentEvent) |
| `AppSettings` | ui/types.ts | User-facing settings (extends AgentConfig fields) |

### AgentConfig Fields

```typescript
interface AgentConfig {
  model: ModelConfig        // required
  persona?: Persona          // character persona
  provider?: ProviderAdapter // custom provider
  memory?: MemoryConfig      // { enabled: boolean }
  mcpServers?: MCPServerConfig[]  // MCP tool servers
  search?: SearchConfig            // web search config
  maxHistoryTokens?: number        // context window limit
}
```

### AppSettings → AgentConfig Mapping

| AppSettings field | AgentConfig field | Transform |
|-------------------|-------------------|-----------|
| `model.*` | `model.*` | Direct copy |
| `persona` | `persona` | Direct copy |
| `memoryEnabled` | `memory.enabled` | Wrapped in object |
| `searchEnabled` | `search` | If true: `{ engine, tavilyApiKey }`; if false: undefined |
| `searchEngine` | `search.engine` | Cast to union type |
| `searchApiKey` | `search.tavilyApiKey` | Only when searchEnabled |
| `mcpServers` | `mcpServers` | Filter out disabled servers |
| `maxHistoryTokens` | `maxHistoryTokens` | Direct copy |

### StreamEvent (bridge → ChatView)

Bridge translates `AgentEvent` → `StreamEvent`:

| AgentEvent | StreamEvent | Field mapping |
|------------|-------------|---------------|
| `{ type: 'token', content }` | `{ type: 'text_delta', data }` | content → data |
| `{ type: 'thinking', content }` | `{ type: 'thinking', data }` | content → data |
| `{ type: 'tool_call', id, name, arguments }` | `{ type: 'tool_use', data: { tool, input } }` | name→tool, arguments→input |
| `{ type: 'tool_result', id, name, result }` | `{ type: 'tool_result', data: { tool, output } }` | name→tool, result→output |
| `{ type: 'error', error }` | `{ type: 'error', data }` | error → data |
| `{ type: 'done', message }` | `{ type: 'done' }` | message stripped |

### Bridge Public API

```typescript
class Bridge {
  init(settings: AppSettings): boolean
  ensureInit(settings: AppSettings): boolean  // lazy init, skips if unchanged
  chat(content: string, settings?: AppSettings): AsyncGenerator<StreamEvent>
  abort(): void
  isReady(): boolean
  getAgent(): Agent | null
}
export const bridge: Bridge  // singleton
```

**Re-init trigger**: `ensureInit` compares `provider|model|apiKey|persona.id|memoryEnabled|searchEnabled|mcpServers` — any change triggers re-init.

## Consumer Contract (ChatView)

ChatView uses:
- `bridge.chat(content, settings)` — iterate StreamEvents
- `bridge.abort()` — cancel on unmount or user stop
- Events: `text_delta` (accumulate), `thinking` (show panel), `tool_use`/`tool_result` (show tool UI), `error` (show error), `done` (finalize)

ChatView builds the final assistant `Message` from accumulated `text_delta` data, not from the `done` event.

## File Map

| File | Role | Imports from |
|------|------|-------------|
| `shared/agent-types.ts` | Canonical types | None (source of truth) |
| `shared/types.ts` | Re-export barrel | agent-types.ts |
| `agent/core/agent.ts` | Agent runtime | shared/types |
| `agent/tools/search.ts` | Search tools | shared/types (SearchConfig) |
| `agent/mcp/types.ts` | MCP types | shared/types (MCPServerConfig) |
| `bridge.ts` | Config/event conversion | shared/types, ui/types |
| `ui/types.ts` | UI types + re-export | shared/types |
| `ui/stores.ts` | Zustand store | ui/types (AppSettings) |
| `ui/chat/ChatView.tsx` | Chat UI | ui/types (UIMessage, UIToolCall) |
| `ui/views/Settings*.tsx` | Settings UI | ui/types (AppSettings) |
| `ui/views/Persona*.tsx` | Persona UI | ui/types (Persona) |

## Rules

1. **Never define agent types in ui/** — always import from `shared/agent-types.ts`
2. **StreamEvent is the bridge boundary** — UI only consumes StreamEvent, never raw AgentEvent
3. **AppSettings owns user prefs** — bridge reads from it, never writes
4. **AgentConfig is the agent contract** — bridge constructs it, agent consumes it
5. **Agent.ts is ground truth** — never change shared/agent-types.ts to contradict Agent.ts
6. **UI extensions go in ui/types.ts** — UIToolCall, UIMessage, never pollute shared
7. **AppSettings is flat, AgentConfig is nested** — bridge does the nesting

## Known Type Conflicts (Resolved)

| Conflict | Old | New | Status |
|----------|-----|-----|--------|
| AgentConfig.search | searchEnabled/searchEngine/searchApiKey | `search?: SearchConfig` | ✅ resolved |
| AgentConfig.memory | memoryEnabled | `memory?: MemoryConfig` | ✅ resolved |
| AgentEvent.tool_call | name, args, arguments? | id, name, arguments | ✅ resolved |
| AgentEvent.tool_result | name, content | id, name, result | ✅ resolved |
| Message.content | string \| null | string (non-null) | ✅ resolved |
| ToolCall format | tool, input, output, status | id, type, function | ✅ resolved |
| Persona.personality | not in type | added as optional | ✅ resolved |
| Persona.modelParamsConfig | not in type | added as optional | ✅ resolved |
| MCPServerConfig duplicate | local def in mcp/types.ts | import from shared | ✅ resolved |
| SearchConfig duplicate | local def in search.ts | import from shared | ✅ resolved |

## Pending Work

- [ ] Provider adapter 接入真实 API 调用
- [ ] MCP 集成测试
- [ ] ChatView.tsx `tc`/`th` 的 any 细化
- [ ] chat-adapter.ts 旧架构清理
- [ ] UIAdapter 正式接口实现（替代 bridge.ts 临时方案）
- [ ] Chaquopy 嵌入 Android
