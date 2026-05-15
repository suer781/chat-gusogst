export type ToolExecuteResult = {
  content: { type: "text"; text: string }[]
  isError?: boolean
}

/**
 * Tool Registry — 管理和执行工具
 * 基于 Hermes toolsets.py 重写
 */
import type { ToolDefinition } from '../../shared/types'

type ToolHandler = (args: Record<string, unknown>) => Promise<string>

interface RegisteredTool {
  definition: ToolDefinition
  handler: ToolHandler
}

export class ToolRegistry {
  private tools: Map<string, RegisteredTool> = new Map()

  register(name: string, description: string, parameters: Record<string, unknown>, handler: ToolHandler) {
    this.tools.set(name, {
      definition: {
        type: 'function',
        function: { name, description, parameters },
      },
      handler,
    })
  }

  getToolDefinitions(): ToolDefinition[] {
    return Array.from(this.tools.values()).map(t => t.definition)
  }

  async execute(name: string, args: Record<string, unknown>): Promise<string> {
    const tool = this.tools.get(name)
    if (!tool) return JSON.stringify({ error: `Unknown tool: ${name}` })
    try {
      return await tool.handler(args)
    } catch (err: any) {
      return JSON.stringify({ error: err.message })
    }
  }

  has(name: string): boolean { return this.tools.has(name) }
  list(): string[] { return Array.from(this.tools.keys()) }
}
