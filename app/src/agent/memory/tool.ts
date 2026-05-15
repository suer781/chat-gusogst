/**
 * Memory Tool — Agent 可调用的记忆工具
 * 基于 Hermes memory.py 重写
 */
import type { ToolDefinition, MemoryCategory } from '../../shared/types'
import type { MemoryManager } from './manager'

// ── 工具参数类型 ──────────────────────

interface MemoryToolArgs {
  action: 'save' | 'search' | 'get' | 'update' | 'delete' | 'list'
  bank?: 'long_term' | 'daily' | 'profile'
  content?: string
  old_content?: string
  query?: string
  category?: MemoryCategory
  tags?: string[]
}

// ── 工具 schema（暴露给模型）──────────────────────

export const MEMORY_TOOL_SCHEMA: ToolDefinition = {
  name: 'memory',
  description: `持久化记忆存储。跨会话保存重要信息。

动作：
- save: 保存一条记忆
- search: 搜索记忆
- get: 获取记忆详情
- update: 更新记忆内容
- delete: 删除记忆
- list: 列出记忆

记忆库：
- long_term: 长期记忆（跨会话保留）
- daily: 每日记忆（当天事件）
- profile: 用户档案（基本信息）

分类：
- personal: 个人信息
- preference: 偏好
- event: 事件
- relationship: 人际关系`,
  parameters: [
    {
      name: 'action',
      type: 'string',
      description: '操作类型',
      required: true,
      enum: ['save', 'search', 'get', 'update', 'delete', 'list'],
    },
    {
      name: 'bank',
      type: 'string',
      description: '记忆库',
      required: false,
      enum: ['long_term', 'daily', 'profile'],
    },
    {
      name: 'content',
      type: 'string',
      description: '记忆内容（save/update 时必填）',
      required: false,
    },
    {
      name: 'old_content',
      type: 'string',
      description: '旧内容（update 时用于匹配）',
      required: false,
    },
    {
      name: 'query',
      type: 'string',
      description: '搜索关键词（search 时必填）',
      required: false,
    },
    {
      name: 'category',
      type: 'string',
      description: '记忆分类',
      required: false,
      enum: ['personal', 'preference', 'event', 'relationship'],
    },
    {
      name: 'tags',
      type: 'array',
      description: '标签列表',
      required: false,
    },
  ],
  execute: async (args: Record<string, any>) => {
    // 这是一个占位实现，实际执行时会被 createMemoryToolExecutor 替换
    throw new Error('Memory tool executor not initialized. Use createMemoryToolExecutor to create an instance.')
  },
}

// ── 工具执行器 ──────────────────────

export function createMemoryToolExecutor(manager: MemoryManager) {
  return async (args: Record<string, unknown>): Promise<string> => {
    const toolArgs: MemoryToolArgs = {
      action: args.action as any,
      bank: args.bank as any,
      content: args.content as string | undefined,
      old_content: args.old_content as string | undefined,
      query: args.query as string | undefined,
      category: args.category as any,
      tags: args.tags as string[] | undefined,
    }
    const result = await manager.handleToolCall(toolArgs as any)
    return JSON.stringify(result)
  }
}

// ── 用户反馈工具（用户手动标记记忆有用/无用）──────────────────────

export const MEMORY_FEEDBACK_TOOL_SCHEMA: ToolDefinition = {
  name: 'memory_feedback',
  description: '标记一条记忆是否有用。当用户的反应表明你的记忆引用有帮助或不准确时调用。',
  parameters: [
    {
      name: 'content',
      type: 'string',
      description: '记忆内容',
      required: true,
    },
    {
      name: 'helpful',
      type: 'boolean',
      description: '是否有用',
      required: true,
    },
  ],
  execute: async (args: Record<string, any>) => {
    // 这是一个占位实现，实际执行时会被 createMemoryFeedbackToolExecutor 替换
    throw new Error('Memory feedback tool executor not initialized.')
  },
}

export function createMemoryFeedbackToolExecutor(manager: MemoryManager) {
  return async (args: Record<string, unknown>): Promise<string> => {
    const content = args.content as string
    const helpful = args.helpful as boolean
    const result = await manager.handleFeedback(content, helpful)
    return JSON.stringify(result)
  }
}
