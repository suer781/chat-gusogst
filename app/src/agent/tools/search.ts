import { ToolRegistry } from './registry'
import { SearchDispatcher } from '../../search/dispatcher'
import type { SearchOptions } from '../../search/types'

const searchInputSchema = {
  type: 'object' as const,
  properties: {
    query: {
      type: 'string',
      description: '搜索关键词（支持中英文）'
    },
    count: {
      type: 'number',
      description: '返回结果数量，默认 5',
      default: 5
    },
    timeRange: {
      type: 'string',
      enum: ['day', 'week', 'month', 'year'],
      description: '时间范围过滤'
    },
    language: {
      type: 'string',
      enum: ['zh', 'en'],
      description: "语言偏好，如 'zh' 或 'en'"
    }
  },
  required: ['query']
}

/** 创建搜索工具 */
function createSearchTool(dispatcher: SearchDispatcher) {
  return {
    name: 'search',
    description: '联网搜索：搜索互联网获取实时信息（新闻、百科、技术文档等）',
    parameters: searchInputSchema,
    execute: async (args: Record<string, unknown>) => {
      try {
        const query = String(args.query ?? '')
        const options: SearchOptions = {
          count: typeof args.count === 'number' ? args.count : 5,
          timeRange: args.timeRange as 'day' | 'week' | 'month' | 'year' | undefined,
          language: args.language as string | undefined
        }
        const results = await dispatcher.search(query, options)
        return formatResults(results)
      } catch (err) {
        return `搜索失败: ${String(err)}`
      }
    }
  }
}

function formatResults(results: Array<{ title: string; url: string; snippet: string; source?: string }>): string {
  if (!results.length) return '未找到相关结果'
  return results.map((r, i) =>
    `${i + 1}. ${r.title}\n   ${r.url}\n   ${r.snippet}${r.source ? `\n   来源: ${r.source}` : ''}`
  ).join('\n\n')
}

/** 注册搜索工具到 ToolRegistry */
export function registerSearchTools(
  registry: ToolRegistry,
  options: { engine?: string; apiKey?: string } = {}
): void {
  const dispatcherOpts: Record<string, unknown> = {}
  if (options.engine && options.apiKey) {
    dispatcherOpts.engineConfigs = {
      [options.engine]: { id: options.engine, name: options.engine, apiKey: options.apiKey }
    }
  }
  const dispatcher = new SearchDispatcher(dispatcherOpts)
  const tool = createSearchTool(dispatcher)
  registry.register(tool.name, tool.description, tool.parameters as any, tool.execute)
}
