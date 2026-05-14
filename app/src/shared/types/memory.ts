// ============================================
// 记忆系统类型定义
// 移植自 Hermes memory_tool.py + holographic store
// 适配浏览器环境（IndexedDB）
// ============================================

// ── 记忆条目 ──────────────────────

export interface MemoryEntry {
  id: string
  /** 存储分区：agent=AI自己的记忆，user=AI对用户的了解 */
  bank: 'agent' | 'user'
  /** 记忆内容（一行或多行） */
  content: string
  /** 分类标签 */
  category: MemoryCategory
  /** 标签 */
  tags: string[]
  /** 信任评分 0.0-1.0，被检索/标记有用时上升 */
  trustScore: number
  /** 检索次数 */
  retrievalCount: number
  /** 标记有用次数 */
  helpfulCount: number
  /** 创建时间 */
  createdAt: number
  /** 最后访问时间 */
  lastAccessed: number
  /** 关联人设 ID（空=所有人设共享） */
  personaId?: string
}

export type MemoryCategory =
  | 'fact'       // 客观事实（用户住在哪、做什么工作）
  | 'preference' // 偏好（喜欢什么口味、沟通风格）
  | 'emotion'    // 情感状态（最近压力大、开心的事）
  | 'habit'      // 习惯（每天几点起床、通勤方式）
  | 'relationship' // 人际关系（同事名字、朋友关系）
  | 'project'    // 项目相关（正在做什么、进度）
  | 'opinion'    // 观点/态度（对某事的看法）
  | 'context'    // 上下文（当前在聊什么、上次聊到哪）

// ── 记忆存储配置 ──────────────────────

export interface MemoryConfig {
  /** agent 记忆字符上限（对应 Hermes MEMORY.md 的 2200） */
  agentCharLimit: number
  /** user 记忆字符上限（对应 Hermes USER.md 的 1375） */
  userCharLimit: number
  /** 信任评分：标记有用时增加 */
  helpfulDelta: number
  /** 信任评分：标记无用时减少 */
  unhelpfulDelta: number
  /** 最低信任评分，低于此值不返回 */
  minTrustThreshold: number
  /** 上下文压缩阈值（token 占比） */
  compressionThreshold: number
  /** 最大上下文注入记忆条数 */
  maxInjectedMemories: number
  /** 是否启用语义搜索（需要 embedding） */
  enableSemanticSearch: boolean
}

export const DEFAULT_MEMORY_CONFIG: MemoryConfig = {
  agentCharLimit: 2200,
  userCharLimit: 1375,
  helpfulDelta: 0.05,
  unhelpfulDelta: -0.10,
  minTrustThreshold: 0.2,
  compressionThreshold: 0.75,
  maxInjectedMemories: 10,
  enableSemanticSearch: false,
}

// ── 记忆工具操作 ──────────────────────

export type MemoryAction = 'add' | 'replace' | 'remove' | 'read' | 'search'

export interface MemoryToolArgs {
  action: MemoryAction
  bank?: 'agent' | 'user'
  content?: string
  /** replace/remove 时匹配旧内容的子字符串 */
  old_content?: string
  /** search 时的查询关键词 */
  query?: string
  category?: MemoryCategory
  tags?: string[]
}

export interface MemoryToolResult {
  success: boolean
  error?: string
  entries?: string
  char_count?: number
  char_limit?: number
  results?: MemoryEntry[]
}

// ── 上下文压缩 ──────────────────────

export interface CompressedMessage {
  role: 'system'
  content: string
  /** 标记为压缩摘要 */\  isSummary: true
  /** 原始消息范围 [start, end) */
  originalRange?: [number, number]
}

// ── 对话历史记录（零 token 记录）──────────────────────

export interface ChatRecord {
  id: string
  sessionId: string
  personaId: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  /** 是否已被压缩进摘要 */
  compressed?: boolean
  /** 摘要 ID（如果此消息已被压缩） */
  summaryId?: string
}

export interface SessionSummary {
  id: string
  sessionId: string
  personaId: string
  /** 摘要内容 */
  summary: string
  /** 覆盖的消息 ID 范围 */
  messageIds: string[]
  createdAt: number
  /** 此摘要覆盖的消息时间范围 */
  timeRange: [number, number]
}
