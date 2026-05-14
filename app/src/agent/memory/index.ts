// 记忆系统统一导出
export { MemoryManager } from './manager'
export { MemoryStore } from './store'
export { MEMORY_TOOL_SCHEMA, MEMORY_FEEDBACK_TOOL_SCHEMA, createMemoryToolExecutor } from './tool'
export { quickExtract, deepExtract, compressConversation, adjustTrust } from './extractor'
export type {
  MemoryEntry, MemoryCategory, MemoryConfig,
  MemoryAction, MemoryToolArgs, MemoryToolResult,
  ChatRecord, SessionSummary, CompressedMessage,
} from '../../shared/types/memory'
