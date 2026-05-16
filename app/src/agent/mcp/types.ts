/**
 * MCP (Model Context Protocol) types
 * Lightweight JSON-RPC 2.0 based protocol for tool integration
 */

export interface MCPServerConfig {
  name: string
  url: string
  headers?: Record<string, string>
  enabled?: boolean
  timeout?: number  // ms, default 30000
}

export interface MCPToolDefinition {
  name: string
  description?: string
  inputSchema: {
    type: 'object'
    properties?: Record<string, any>
    required?: string[]
  }
}

export interface MCPToolResult {
  content: MCPContent[]
  isError?: boolean
}

export type MCPContent =
  | { type: 'text'; text: string }
  | { type: 'image'; data: string; mimeType: string }
  | { type: 'resource'; resource: { uri: string; text?: string; mimeType?: string } }

// JSON-RPC 2.0
export interface JSONRPCRequest {
  jsonrpc: '2.0'
  id: number
  method: string
  params?: Record<string, any>
}

export interface JSONRPCResponse {
  jsonrpc: '2.0'
  id: number
  result?: any
  error?: { code: number; message: string; data?: any }
}

export type MCPConnectionState = 'disconnected' | 'connecting' | 'connected' | 'error'

export interface MCPServerStatus {
  name: string
  state: MCPConnectionState
  toolCount: number
  error?: string
  lastConnected?: number
}
