// ============================================
// Memory Tool — 模型可调用的记忆工具
// 移植自 Hermes memory_tool.py
// 单工具接口：add / replace / remove / read / search
// ============================================

import type { ToolDefinition } from '../../shared/types'
import type { MemoryToolArgs } from '../../shared/types/memory'
import { MemoryManager } from './manager'

// ── 工具 schema（暴露给模型）──────────────────────

export const MEMORY_TOOL_SCHEMA: ToolDefinition = {
  type: 'function',
  function: {
    name: 'memory',
    description: `持久化记忆存储。跨会话保存重要信息。

使用场景：
- 用户告诉你个人信息（名字、职业、偏好）→ add 到 user bank
- 你发现了重要的项目约定或配置 → add 到 agent bank
- 需要回忆之前的信息 → search 或 read
- 用户纠正了之前的信息 → replace 更新
- 信息不再有效 → remove 删除

两个存储区：
- agent: AI 自己的记忆（项目约定、工具特性、学到的东西）
- user: AI 对用户的了解（偏好、个人信息、习惯）

重要：add 时用简洁的事实描述，不要加前缀（如"用户喜欢"），直接写事实（如"喜欢辣的食物"）。`,
    parameters: {
      type: 'object',
      properties: {
        action: {
          type: 'string',
          enum: ['add', 'replace', 'remove', 'read', 'search'],
          description: '操作类型',
        },
        bank: {
          type: 'string',
          enum: ['agent', 'user'],
          description: '存储区。agent=AI的记忆，user=对用户的了解。add/replace/read 时必填。',
        },
        content: {
          type: 'string',
          description: 'add 时：新记忆内容。replace 时：替换后的新内容。',
        },
        old_content: {
          type: 'string',
          description: 'replace/remove 时：匹配要修改/删除的记忆的子字符串（不需要完全匹配，只要包含即可）。',
        },
        query: {
          type: 'string',
          description: 'search 时：搜索关键词。',
        },
        category: {
          type: 'string',
          enum: ['fact', 'preference', 'emotion', 'habit', 'relationship', 'project', 'opinion', 'context'],
          description: '记忆分类（add 时可选）。',
        },
        tags: {
          type: 'array',
          items: { type: 'string' },
          description: '标签（add 时可选）。',
        },
      },
      required: ['action'],
    },
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
    const result = await manager.handleToolCall(toolArgs)
    return JSON.stringify(result)
  }
}

// ── 用户反馈工具（用户手动标记记忆有用/无用）──────────────────────

export const MEMORY_FEEDBACK_TOOL_SCHEMA: ToolDefinition = {
  type: 'function',
  function: {
    name: 'memory_feedback',
    description: '标记一条记忆是否有用。当用户的反应表明你的记忆引用有帮助或不准确时调用。',
    parameters: {
      type: 'object',
      properties: {
        memory_id: {
          type: 'string',
          description: '记忆条目 ID',
        },
        helpful: {
          type: 'boolean',
          description: 'true=有用，false=无用/过时',
        },
      },
      required: ['memory_id', 'helpful'],
    },
  },
}
