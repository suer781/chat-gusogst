# chat-gusogst interface specification

**last updated**: 2026-05-18 03:00
**compile status**: ✅ 0 errors

## architecture overview

```
AppSettings (UI store, flat)
    | bridge.ts settingsToAgentConfig()
AgentConfig (agent type, nested)
    | Agent.sendMessage()
AgentEvent (agent output)
    | bridge.ts event mapping
StreamEvent (UI consumption)
    | ChatView.tsx
UIMessage / UIToolCall (UI display)
```

## type hierarchy

### shared/agent-types.ts (canonical source, Agent is ground truth)

- **Message** — OpenAI format: role, content (non-null string), tool_calls?, tool_call_id?, name?, timestamp?, id?
- **ToolCall** — id, type, function: { name, arguments }
- **ModelConfig** — provider, model, apiKey, apiHost?, baseUrl?, temperature?, maxTokens?, topP?
- **SearchConfig** — engine: 'tavily' | 'duckduckgo' | 'auto', tavilyApiKey?
- **MemoryConfig** — enabled: boolean
- **MCPServerConfig** — name, url, headers?, enabled?, timeout?
- **AgentConfig** — model, persona?, provider?, memory?, mcpServers?, search?, maxHistoryTokens?
- **AgentEvent** — token | thinking | tool_call(id,name,arguments) | tool_result(id,name,result) | error | done
- **Persona** — id, name, systemPrompt, avatar?, emoji?, tags[], isDefault?, builtIn?, personality?, modelParamsConfig?

### ui/types.ts (UI extensions, does NOT modify shared types)

- **UIToolCall** — id, tool, input (any), output?, status: running|done|error
- **UIMessage** extends Message — thinking?: ThinkingBlock[], error?, toolCalls?: UIToolCall[]
- **AppSettings** — flat UI config: model, persona, searchEnabled, searchEngine, searchApiKey, channel, maxRounds, memoryEnabled, maxHistoryTokens, showThinking, showToolCalls, showMemoryHints, showSearchSources, showErrorDetails

### bridge.ts (conversion layer)

- **settingsToAgentConfig(s: AppSettings) → AgentConfig**
  - s.searchEnabled → config.search: SearchConfig
  - s.memoryEnabled → config.memory: MemoryConfig
  - Other fields mapped directly

- **AgentEvent → StreamEvent mapping**
  - event.name → data.tool
  - event.arguments → data.input (JSON.parse)
  - event.result → data.output

## file map

| file | role | imports from |
|------|------|-------------|
| shared/agent-types.ts | canonical types | none (source of truth) |
| shared/types.ts | re-export barrel | agent-types.ts |
| agent/core/agent.ts | agent runtime | shared/types |
| agent/tools/search.ts | search tools | shared/types (SearchConfig) |
| agent/mcp/types.ts | MCP types | shared/types (MCPServerConfig) |
| bridge.ts | config/event conversion | shared/types, ui/types |
| ui/types.ts | UI types + re-export | shared/types |
| ui/stores.ts | zustand store | ui/types (UIMessage) |
| ui/chat/ChatView.tsx | chat UI | ui/types (UIMessage, UIToolCall) |
| ui/views/Settings*.tsx | settings UI | ui/types (AppSettings) |
| ui/views/Persona*.tsx | persona UI | ui/types (Persona) |

## rules for modifying types

1. **Agent.ts is ground truth** — never change shared/agent-types.ts to contradict Agent.ts
2. **UI extensions go in ui/types.ts** — UIToolCall, UIMessage, never pollute shared
3. **bridge.ts handles conversion** — AppSettings ↔ AgentConfig, AgentEvent ↔ StreamEvent
4. **AppSettings is flat, AgentConfig is nested** — bridge does the nesting
5. **Persona is application data** — personality and modelParamsConfig are for UI, not sent to LLM

## known type conflicts (resolved)

| conflict | old | new | status |
|----------|-----|-----|--------|
| AgentConfig.search | searchEnabled/searchEngine/searchApiKey | search?: SearchConfig | ✅ resolved |
| AgentConfig.memory | memoryEnabled | memory?: MemoryConfig | ✅ resolved |
| AgentEvent.tool_call | name, args, arguments? | id, name, arguments | ✅ resolved |
| AgentEvent.tool_result | name, content | id, name, result | ✅ resolved |
| Message.content | string | null | string (non-null) | ✅ resolved |
| ToolCall format | tool, input, output, status | id, type, function | ✅ resolved |
| Persona.personality | not in type | added as optional | ✅ resolved |
| Persona.modelParamsConfig | not in type | added as optional | ✅ resolved |
| MCPServerConfig duplicate | local def in mcp/types.ts | import from shared | ✅ resolved |
| SearchConfig duplicate | local def in search.ts | import from shared | ✅ resolved |

## pending work

- [ ] provider adapter 接入真实 API 调用
- [ ] MCP 集成测试
- [ ] ChatView.tsx tc/th 的 any 细化
- [ ] chat-adapter.ts 旧架构清理
- [ ] UIAdapter 接口实现（替代 bridge.ts 临时方案）
- [ ] Chaquopy 嵌入 Android
