// ============================================
// 搜索工具 — Agent 可调用的 Tool 定义
// 底层走 SearchDispatcher 调度
// ============================================

import { z } from 'zod'
import { zodToJsonSchema } from 'zod-to-json-schema'
import type { SearchDispatcher } from '../../search/dispatcher'
import type { SearchResult } from '../../search/types'
import type { ToolDefinition, ToolExecuteResult } from './registry'

// ── Zod Schema ──────────────────────────

const SearchInputSchema = z.object({
  query: z.string().describe('搜索关键词，支持中文或英文'),
  count: z.number().optional().default(5).describe('返回结果数量，默认 5'),
  timeRange: z.enum(['day', 'week', 'month', 'year']).optional().describe('时间范围过滤'),
  language: z.string().optional().describe('语言偏好，如 zh、en'),
})

// ── Tool 定义 ──────────────────────────

export function createSearchTool(dispatcher: SearchDispatcher): ToolDefinition {
  return {
    id: 'search',
    name: '联网搜索',
    description: '搜索互联网获取实时信息。可以搜索新闻、百科、技术文档等。',
    inputSchema: zodToJsonSchema(SearchInputSchema),
    async execute(input: unknown): Promise<ToolExecuteResult> {
      const parsed = SearchInputSchema.safeParse(input)
      if (!parsed.success) {
        return { content: [{ type: 'text', text: `参数错误: ${parsed.error.message}` }] }
      }

      const { query, count, timeRange, language } = parsed.data

      try {
        const results = await dispatcher.search(query, { count, timeRange, language })
        return {
          content: [{ type: 'text', text: formatResults(results) }],
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err)
        return {
          content: [{ type: 'text', text: `搜索失败: ${msg}` }],
          isError: true,
        }
      }
    },
  }
}

// ── 格式化输出 ──────────────────────────

function formatResults(results: SearchResult[]): string {
  if (results.length === 0) return '未找到相关结果'

  return results
    .map((r, i) => {
      let text = `【${i + 1}】${r.title}`
      if (r.url) text += `\n${r.url}`
      if (r.snippet) text += `\n${r.snippet}`
      if (r.source) text += `\n[来源: ${r.source}]`
      return text
    })
    .join('\n\n')
}
