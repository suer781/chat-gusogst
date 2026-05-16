import type {
  MCPServerConfig,
  MCPToolDefinition,
  MCPToolResult,
  JSONRPCRequest,
  JSONRPCResponse,
  MCPConnectionState,
  MCPServerStatus,
} from './types'

export class MCPClient {
  private config: MCPServerConfig
  private state: MCPConnectionState = 'disconnected'
  private tools: MCPToolDefinition[] = []
  private requestId = 0
  private timeout: number
  private serverInfo: { name?: string; version?: string } = {}

  constructor(config: MCPServerConfig) {
    this.config = config
    this.timeout = config.timeout ?? 30000
  }

  get name(): string { return this.config.name }
  get isConnected(): boolean { return this.state === 'connected' }
  get toolList(): MCPToolDefinition[] { return [...this.tools] }

  getStatus(): MCPServerStatus {
    return {
      name: this.config.name,
      state: this.state,
      toolCount: this.tools.length,
      lastConnected: this.state === 'connected' ? Date.now() : undefined,
    }
  }

  // ── Connection ──

  async connect(): Promise<void> {
    if (this.state === 'connected') return
    this.state = 'connecting'

    try {
      // Step 1: Initialize
      const initResult = await this.rpc('initialize', {
        protocolVersion: '2024-11-05',
        capabilities: { tools: {} },
        clientInfo: { name: 'chat-gusogst', version: '0.1.0' },
      })

      this.serverInfo = {
        name: initResult?.serverInfo?.name,
        version: initResult?.serverInfo?.version,
      }

      // Step 2: Notify initialized
      await this.notify('notifications/initialized', {})

      // Step 3: List tools
      const toolsResult = await this.rpc('tools/list', {})
      this.tools = toolsResult?.tools ?? []

      this.state = 'connected'
      console.log(`[MCP] ${this.config.name}: connected, ${this.tools.length} tools`)
    } catch (error: any) {
      this.state = 'error'
      console.error(`[MCP] ${this.config.name}: connect failed`, error.message)
      throw error
    }
  }

  disconnect(): void {
    this.state = 'disconnected'
    this.tools = []
  }

  // ── Tool execution ──

  async callTool(name: string, args: Record<string, any>): Promise<MCPToolResult> {
    if (this.state !== 'connected') {
      throw new Error(`MCP server ${this.config.name} not connected`)
    }

    const result = await this.rpc('tools/call', { name, arguments: args })
    return {
      content: result?.content ?? [{ type: 'text', text: JSON.stringify(result) }],
      isError: result?.isError ?? false,
    }
  }

  // ── JSON-RPC transport ──

  private nextId(): number {
    return ++this.requestId
  }

  private async rpc(method: string, params: Record<string, any>): Promise<any> {
    const request: JSONRPCRequest = {
      jsonrpc: '2.0',
      id: this.nextId(),
      method,
      params,
    }

    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), this.timeout)

    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        Accept: 'application/json, text/event-stream',
        ...this.config.headers,
      }

      const response = await fetch(this.config.url, {
        method: 'POST',
        headers,
        body: JSON.stringify(request),
        signal: controller.signal,
      })

      if (!response.ok) {
        const text = await response.text().catch(() => '')
        throw new Error(`HTTP ${response.status}: ${text}`)
      }

      const contentType = response.headers.get('content-type') ?? ''

      // Handle SSE response (Streamable HTTP)
      if (contentType.includes('text/event-stream')) {
        return await this.parseSSEResponse(response, request.id)
      }

      // Handle JSON response
      const json: JSONRPCResponse = await response.json()
      if (json.error) {
        throw new Error(`RPC ${method}: ${json.error.message}`)
      }
      return json.result
    } finally {
      clearTimeout(timer)
    }
  }

  private async parseSSEResponse(response: Response, expectedId: number): Promise<any> {
    const text = await response.text()
    const lines = text.split('\n')
    let currentEvent = ''
    let currentData = ''

    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.slice(6).trim()
      } else if (line.startsWith('data:')) {
        currentData = line.slice(5).trim()
      } else if (line === '' && currentData) {
        if (currentEvent === 'message') {
          try {
            const json: JSONRPCResponse = JSON.parse(currentData)
            if (json.id === expectedId) {
              if (json.error) throw new Error(json.error.message)
              return json.result
            }
          } catch (e) {
            // not our message, skip
          }
        }
        currentEvent = ''
        currentData = ''
      }
    }

    if (currentData && currentEvent === 'message') {
      const json: JSONRPCResponse = JSON.parse(currentData)
      if (json.error) throw new Error(json.error.message)
      return json.result
    }

    throw new Error('No valid response in SSE stream')
  }

  // Fire-and-forget notification (no response expected)
  private async notify(method: string, params: Record<string, any>): Promise<void> {
    const notification = {
      jsonrpc: '2.0',
      method,
      params,
    }

    try {
      await fetch(this.config.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...this.config.headers,
        },
        body: JSON.stringify(notification),
      })
    } catch (e) {
      // Notifications are fire-and-forget, ignore errors
    }
  }
}
