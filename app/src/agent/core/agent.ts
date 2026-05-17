import { getProvider } from '../providers'
import type { Persona, ProviderAdapter, ToolDefinition } from '../../shared/types'
import { MemoryManager } from '../memory/manager'
import { ToolRegistry } from '../tools/registry'
import { registerSearchTools, type SearchConfig } from '../tools/search'
import { MCPManager } from '../mcp/manager'
import type { MCPServerConfig } from '../mcp/types'
import { createPlatformConnectTool } from '../hermes/platform_connect_tool'
import type { HermesBridge } from '../hermes/bridge'

export interface Message {
  role: 'system' | 'user' | 'assistant' | 'tool'
  content: string
  tool_calls?: { id: string; type: 'function'; function: { name: string; arguments: string } }[]
  tool_call_id?: string
  name?: string
  timestamp?: number
}

export interface ModelConfig {
  provider: string
  model: string
  apiKey: string
  apiHost?: string
  temperature?: number
  maxTokens?: number
  topP?: number
}

export interface AgentConfig {
  model: ModelConfig
  persona?: Persona
  memory?: { enabled: boolean }
  mcpServers?: MCPServerConfig[]
  search?: SearchConfig
  maxHistoryTokens?: number
}

export type AgentEvent =
  | { type: 'token'; content: string }
  | { type: 'thinking'; content: string }
  | { type: 'tool_call'; id: string; name: string; arguments: any }
  | { type: 'tool_result'; id: string; name: string; result: string }
  | { type: 'error'; error: string }
  | { type: 'done'; message: Message }

// Helper to build ToolDefinition in the nested format registry expects
function makeToolDef(name: string, description: string, parameters: Record<string, any>): ToolDefinition {
  return { type: 'function', function: { name, description, parameters } }
}

export class Agent {
  private provider!: ProviderAdapter
  private modelConfig: ModelConfig
  private tools: ToolRegistry
  private memory: MemoryManager
  private config: AgentConfig
  private history: Message[] = []
  private persona: Persona | null
  private mcpManager: MCPManager
  private mcpInitialized = false
  private bridge: HermesBridge | undefined
  private aborted = false
  private maxHistoryTokens: number

  constructor(config: AgentConfig) {
    this.config = config
    this.modelConfig = config.model
    this.persona = config.persona || null
    this.maxHistoryTokens = config.maxHistoryTokens || 8000
    this.provider = getProvider(config.model.provider)
    this.tools = new ToolRegistry()
    this.memory = new MemoryManager()
    this.mcpManager = new MCPManager()
    this.registerBuiltinTools()
  }

  private registerBuiltinTools() {
    const searchConfig = this.config.search || { engine: 'auto' as const }
    registerSearchTools(this.tools, searchConfig)

    // Time tool
    this.tools.register(
      makeToolDef('get_current_time', 'Get the current date and time', {}),
      async () => {
        const now = new Date()
        return JSON.stringify({ datetime: now.toISOString(), timezone: Intl.DateTimeFormat().resolvedOptions().timeZone, formatted: now.toLocaleString('zh-CN') })
      }
    )

    // Memory save
    this.tools.register(
      makeToolDef('memory_save', 'Save important information to long-term memory', {
        content: { type: 'string', description: 'Information to remember' },
        type: { type: 'string', enum: ['fact', 'preference', 'event', 'emotion', 'context'] },
        importance: { type: 'number', description: '0-1, default 0.5' },
        tags: { type: 'array', items: { type: 'string' } },
      }),
      async (_n: string, args: any) => {
        const entry = await this.memory.add(args.content, args.type || 'fact', args.importance ?? 0.5, args.tags || [])
        return JSON.stringify({ id: entry.id, saved: true })
      }
    )

    // Memory search
    this.tools.register(
      makeToolDef('memory_search', 'Search long-term memory for previously saved information', {
        query: { type: 'string', description: 'Search keywords' },
        limit: { type: 'number', description: 'Max results, default 5' },
      }),
      async (_n: string, args: any) => {
        const results = await this.memory.search(args.query, args.limit || 5)
        if (results.length === 0) return 'No memories found'
        return results.map((r, i) => (i + 1) + '. [' + r.type + '] ' + r.content).join('\n')
      }
    )

    // Calculator
    this.tools.register(
      makeToolDef('calculator', 'Evaluate a math expression', {
        expression: { type: 'string', description: 'Math expression, e.g. 2+3*4, sqrt(16)' },
      }),
      async (_n: string, args: any) => {
        try {
          const safe = args.expression.replace(/[^0-9+\-*/().%\s,a-z]/gi, '')
          const defs = 'const sqrt=Math.sqrt,pow=Math.pow,abs=Math.abs,ceil=Math.ceil,floor=Math.floor,round=Math.round,PI=Math.PI,E=Math.E,sin=Math.sin,cos=Math.cos,tan=Math.tan,log=Math.log,min=Math.min,max=Math.max'
          return String(new Function(defs + ';return ' + safe)())
        } catch (e: any) { return 'Error: ' + e.message }
      }
    )

    // Platform connect (full version with bridge status)
    const platTool = createPlatformConnectTool(this.bridge)
    this.tools.register(platTool.definition, platTool.handler)
  }

  async initMCP(configs?: MCPServerConfig[]) {
    if (this.mcpInitialized) return
    const servers = configs || this.config.mcpServers || []
    if (servers.length === 0) return
    try {
      await this.mcpManager.loadConfigs(servers)
      await this.mcpManager.connectAll()
      this.tools.setMCPManager(this.mcpManager)
      this.tools.registerMCPTools()
      this.mcpInitialized = true
    } catch (e) { console.warn('MCP init failed:', e) }
  }

  getMCPManager() { return this.mcpManager }
  setBridge(bridge: HermesBridge) {
    this.bridge = bridge
    const platTool = createPlatformConnectTool(bridge)
    this.tools.register(platTool.definition, platTool.handler)
  }

  private truncateHistory(): void {
    let totalChars = 0
    const maxChars = this.maxHistoryTokens * 2
    const systemMsgs = this.history.filter(m => m.role === 'system')
    const nonSystem = this.history.filter(m => m.role !== 'system')
    let keepFrom = nonSystem.length
    for (let i = nonSystem.length - 1; i >= 0; i--) {
      totalChars += (nonSystem[i].content || '').length
      if (totalChars > maxChars) { keepFrom = i + 1; break }
    }
    if (keepFrom < nonSystem.length) {
      const dropped = nonSystem.slice(0, keepFrom)
      const summary = dropped.slice(-3).map(m => (m.content || '').substring(0, 50)).join('; ')
      this.history = [...systemMsgs, { role: 'system' as const, content: '[Earlier: ' + summary + ']', timestamp: Date.now() }, ...nonSystem.slice(keepFrom)]
    }
  }

  private buildContext(): Message[] {
    const messages: Message[] = []
    let sys = this.persona ? this.persona.systemPrompt + '\nYour name is ' + this.persona.name + '.' : 'You are a helpful AI assistant.'
    const toolNames = this.tools.names
    if (toolNames.length > 0) sys += '\nAvailable tools: ' + toolNames.join(', ') + '.'
    messages.push({ role: 'system', content: sys, timestamp: Date.now() })
    messages.push(...this.history)
    return messages
  }

  async *sendMessage(content: string): AsyncGenerator<AgentEvent> {
    this.aborted = false
    await this.initMCP()
    if (content) this.history.push({ role: 'user', content, timestamp: Date.now() })
    this.truncateHistory()
    const messages = this.buildContext()
    const toolDefs = this.tools.getDefinitions()
    try {
      const response = await this.provider.chat(messages, this.modelConfig, toolDefs.length > 0 ? toolDefs : undefined)
      if (response.tool_calls && response.tool_calls.length > 0) {
        this.history.push({ role: 'assistant', content: response.content || '', tool_calls: response.tool_calls, timestamp: Date.now() })
        if (response.content) yield { type: 'thinking', content: response.content }
        for (const tc of response.tool_calls) {
          if (this.aborted) break
          yield { type: 'tool_call', id: tc.id, name: tc.function.name, arguments: tc.function.arguments }
          let result: string
          try {
            const args = typeof tc.function.arguments === 'string' ? JSON.parse(tc.function.arguments) : tc.function.arguments
            result = await this.tools.execute(tc.function.name, args)
          } catch (e: any) { result = 'Error: ' + e.message }
          yield { type: 'tool_result', id: tc.id, name: tc.function.name, result }
          this.history.push({ role: 'tool', content: result, tool_call_id: tc.id, name: tc.function.name, timestamp: Date.now() })
        }
        if (!this.aborted) yield* this.sendMessage('')
        return
      }
      const msg: Message = { role: 'assistant', content: response.content || '', timestamp: Date.now() }
      this.history.push(msg)
      if (response.content) yield { type: 'token', content: response.content }
      yield { type: 'done', message: msg }
      if (this.config.memory?.enabled && this.history.length % 10 === 0) this.memory.extractAndSave(this.history).catch(() => {})
    } catch (e: any) {
      if (!this.aborted) yield { type: 'error', error: e.message }
    }
  }

  abort() { this.aborted = true }
  clearHistory() { this.history = [] }
  getHistory(): Message[] { return [...this.history] }
  updateConfig(patch: Partial<AgentConfig>) {
    if (patch.model) { this.modelConfig = patch.model; this.provider = getProvider(patch.model.provider) }
    if (patch.persona) this.persona = patch.persona
    if (patch.search) registerSearchTools(this.tools, patch.search)
  }
  getMemory() { return this.memory }
  getTools() { return this.tools }
}
