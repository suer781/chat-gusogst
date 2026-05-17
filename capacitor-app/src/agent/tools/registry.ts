import { ToolDefinition } from '../../shared/types'
import type { MCPManager } from '../mcp/manager'

export class ToolRegistry {
  private tools = new Map<string, ToolDefinition>()
  private handlers = new Map<string, (name: string, args: any) => Promise<any>>()
  private mcpManager: MCPManager | null = null

  setMCPManager(manager: MCPManager): void {
    this.mcpManager = manager
  }

  registerMCPTools(): void {
    if (!this.mcpManager) return

    for (const def of this.mcpManager.getAllToolDefinitions()) {
      this.register({
        type: 'function',
        function: {
          name: def.name,
          description: def.description,
          parameters: def.parameters,
        }
      })
    }
  }

  register(tool: ToolDefinition, handler?: (name: string, args: any) => Promise<any>): void {
    this.tools.set(tool.function.name, tool)
    if (handler) {
      this.handlers.set(tool.function.name, handler)
    }
  }

  registerWithHandler(name: string, handler: (name: string, args: any) => Promise<any>): void {
    this.handlers.set(name, handler)
  }

  get(name: string): ToolDefinition | undefined {
    return this.tools.get(name)
  }

  has(name: string): boolean {
    return this.tools.has(name)
  }

  getDefinitions(): ToolDefinition[] {
    return Array.from(this.tools.values())
  }

  get names(): string[] {
    return Array.from(this.tools.keys())
  }

  async execute(name: string, args: any): Promise<any> {
    // Check if this is an MCP tool
    if (this.mcpManager?.getToolNames().includes(name)) {
      try {
        const result = await this.mcpManager.callTool(name, args)
        if (result.isError) {
          const errorText = result.content
            .filter((c: any) => c.type === 'text')
            .map((c: any) => c.text)
            .join('\n')
          return { error: errorText }
        }
        const textParts = result.content.filter((c: any) => c.type === 'text')
        if (textParts.length === result.content.length) {
          return textParts.map((c: any) => c.text).join('\n')
        }
        return result.content
      } catch (error: any) {
        return { error: `MCP tool '${name}' failed: ${error.message}` }
      }
    }

    // Built-in handler
    const handler = this.handlers.get(name)
    if (!handler) {
      return { error: `Tool '${name}' not found` }
    }

    try {
      return await handler(name, args)
    } catch (error: any) {
      return { error: `Tool '${name}' failed: ${error.message}` }
    }
  }
}
