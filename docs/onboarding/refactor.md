# chat-gusogst refactor status

**last updated**: 2026-05-18 03:00
**compile status**: ✅ 0 errors (tsc --noEmit)

## summary

统一了整个项目的类型体系，消灭了 50 个编译错误。
核心改动：一个类型源（shared/agent-types.ts），Agent 是 Ground Truth，UI 通过扩展类型适配。

## completed changes

### phase 1 — type unification
- shared/agent-types.ts 重写，对齐 Agent.ts 实际类型
- shared/types.ts 重写，re-export from agent-types
- ui/types.ts 重写，exports AppSettings + UIToolCall/UIMessage 扩展类型
- ui/stores.ts flat 结构，用 UIMessage

### phase 2 — bridge rewrite
- bridge.ts 重写，settingsToAgentConfig 用 SearchConfig/MemoryConfig
- AgentEvent 映射对齐 event.name/event.result/event.arguments

### phase 3 — settings 链路修复
- 8 个 settings/persona 文件全局替换 settings.config. → settings.
- Persona 类型加回 personality/modelParamsConfig
- SettingsView.tsx 语法修复

### phase 4 — ChatView 适配
- ChatView.tsx import 改用 UIMessage/UIToolCall
- ToolCallCard 适配 UIToolCall（tool/input/output/status 字段）
- 流式事件处理适配 UIMessage（thinking/error/toolCalls）
- JSX 语法修复（双大括号问题）

### phase 5 — agent.ts 统一
- agent/core/agent.ts 删掉本地 Message/ModelConfig/AgentConfig/AgentEvent 定义
- 改为从 shared/agent-types.ts import
- SearchConfig/MCPServerConfig 也从 shared 导入

### phase 6 — search/mcp 类型统一
- agent/tools/search.ts：删掉本地 SearchConfig，从 shared 导入
- agent/mcp/types.ts：删掉本地 MCPServerConfig，从 shared 导入 + re-export
- 全项目所有核心类型唯一源头：shared/agent-types.ts

## documentation

- docs/UI_ADAPTER.md — 接口规范 + 类型映射 + 类型冲突记录 + 修改规则 + 文件地图
- docs/REFACTOR_STATUS.md — 本文件

## key decisions

- shared/agent-types.ts 是唯一类型源，Agent.ts 是 Ground Truth
- UI 通过 UIToolCall/UIMessage 扩展类型适配，不污染 shared
- bridge.ts 负责 AppSettings ↔ AgentConfig 转换
- Persona 用 application data 而非发给 LLM 的 system prompt

## known issues

- UIToolCall 和 OpenAI ToolCall 是两个独立类型，bridge 侧做映射
- tc 和 th 在 ChatView 里用 any 标注，后续可以细化
- provider adapter 尚未接入真实 API 调用
- MCP 集成尚未测试
- chat-adapter.ts 是旧架构残留，已从 imports 移除但文件仍在
