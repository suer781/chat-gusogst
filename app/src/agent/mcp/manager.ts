import { MCPClient } from './client'
import type { MCPServerConfig, MCPServerStatus, MCPToolResult } from './types'

export class MCPManager {
  private clients: Map<string, MCPClient> = new Map()
  private toolToServer: Map<string, string> = new Map()

  loadConfigs(configs: MCPServerConfig[]): void {
    for (const config of configs) {
      if (config.enabled === false) continue
      const client = new MCPClient(config)
      this.clients.set(config.name, client)
    }
  }

  async connectAll(): Promise<void> {
    await Promise.allSettled(
      Array.from(this.clients.values()).map(async (client) => {
        try {
          await client.connect()
        } catch (e: any) {
          console.warn(`[MCP] Failed to connect ${client.name}: ${e.message}`)
        }
      })
    )

    // Build tool -> server mapping
    this.toolToServer.clear()
    for (const [name, client] of this.clients) {
      if (client.isConnected) {
        for (const tool of client.toolList) {
          if (this.toolToServer.has(tool.name)) {
            console.warn(`[MCP] Duplicate tool name '${tool.name}' from ${name}, skipping`)
            continue
          }
          this.toolToServer.set(tool.name, name)
        }
      }
    }

    console.log(`[MCP] Connected ${this.connectedCount}/${this.clients.size} servers, ${this.toolToServer.size} tools`)
  }

  disconnectAll(): void {
    for (const client of this.clients.values()) {
      client.disconnect()
    }
    this.toolToServer.clear()
  }

  async callTool(toolName: string, args: Record<string, any>): Promise<MCPToolResult> {
    const serverName = this.toolToServer.get(toolName)
    if (!serverName) {
      throw new Error(`MCP tool '${toolName}' not found`)
    }

    const client = this.clients.get(serverName)
    if (!client || !client.isConnected) {
      throw new Error(`MCP server '${serverName}' not connected`)
    }

    return client.callTool(toolName, args)
  }

  getToolNames(): string[] {
    return Array.from(this.toolToServer.keys())
  }

  getToolDefinition(toolName: string): { name: string; description: string; parameters: any } | null {
    const serverName = this.toolToServer.get(toolName)
    if (!serverName) return null

    const client = this.clients.get(serverName)
    if (!client) return null

    const tool = client.toolList.find(t => t.name === toolName)
    if (!tool) return null

    return {
      name: tool.name,
      description: tool.description ?? `MCP tool from ${serverName}`,
      parameters: tool.inputSchema,
    }
  }

  getAllToolDefinitions(): Array<{ name: string; description: string; parameters: any }> {
    const defs: Array<{ name: string; description: string; parameters: any }> = []
    for (const toolName of this.toolToServer.keys()) {
      const def = this.getToolDefinition(toolName)
      if (def) defs.push(def)
    }
    return defs
  }

  get connectedCount(): number {
    return Array.from(this.clients.values()).filter(c => c.isConnected).length
  }

  getStatuses(): MCPServerStatus[] {
    return Array.from(this.clients.values()).map(c => c.getStatus())
  }

  getClient(name: string): MCPClient | undefined {
    return this.clients.get(name)
  }
}
