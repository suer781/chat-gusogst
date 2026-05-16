import { getProvider } from '../providers/index'
import { MemoryManager } from '../memory/manager'
import { ToolRegistry } from '../tools/registry'
import { MCPManager } from '../mcp/manager'
import { matchPlatform, getConnectablePlatforms } from '../hermes/connector'
import type {
  AgentConfig, AgentEvent, Message, ToolDefinition,
  Persona, ProviderAdapter, ModelConfig
} from '../../shared/types'
import type { MCPServerConfig } from '../mcp/types'

export class Agent {
  private provider: ProviderAdapter
  private modelConfig: ModelConfig
  private tools: ToolRegistry
  private memory: MemoryManager
  private config: AgentConfig
  private history: Message[] = []
  private persona: Persona
  private mcpManager: MCPManager
  private mcpInitialized = false
  private aborted = false

  constructor(config: AgentConfig) {
    this.config = config
    this.modelConfig = config.model
    this.persona = config.persona
    this.provider = getProvider(config.model.provider)
    this.tools = new ToolRegistry()
    this.memory = new MemoryManager()
    this.mcpManager = new MCPManager()
    this.registerBuiltinTools()
  }

  // ── MCP ──
  async initMCP(configs?: MCPServerConfig[]): Promise<void> {
    if (this.mcpInitialized) return
    const serverConfigs = configs ?? []
    if (serverConfigs.length === 0) {
      this.mcpInitialized = true
      return
    }
    this.mcpManager.loadConfigs(serverConfigs)
    await this.mcpManager.connectAll()
    this.tools.setMCPManager(this.mcpManager)
    this.tools.registerMCPTools()
    this.mcpInitialized = true
  }

  getMCPManager(): MCPManager { return this.mcpManager }

  // ── 内置工具 ──
  private registerBuiltinTools() {
    this.tools.register(
      {
        type: 'function',
        function: {
          name: 'search',
          description: '搜索互联网获取最新信息',
          parameters: {
            type: 'object',
            properties: { query: { type: 'string', description: '搜索关键词' } },
            required: ['query']
          }
        }
      },
      async (_name, args) => ({ result: `搜索「${args.query}」— 搜索引擎待接入` })
    )

    this.tools.register(
      {
        type: 'function',
        function: {
          name: 'get_current_time',
          description: '获取当前时间',
          parameters: { type: 'object', properties: {}, required: [] }
        }
      },
      async () => ({ time: new Date().toISOString(), timezone: Intl.DateTimeFormat().resolvedOptions().timeZone })
    )

    this.tools.register(
      {
        type: 'function',
        function: {
          name: 'platform_connect',
          description: '引导用户连接社交平台（微信、QQ、Telegram、飞书、Discord、钉钉）。当用户说「加微信」「加QQ」时调用。返回步骤数据。',
          parameters: {
            type: 'object',
            properties: {
              platform: { type: 'string', description: '平台名：qq/weixin/telegram/feishu/discord/dingtalk' },
              action: { type: 'string', enum: ['get_steps', 'list_platforms', 'check_status'] },
            },
            required: ['action'],
          },
        },
      },
      async (_name, args) => {
        const { action, platform } = args
        if (action === 'list_platforms') {
          return { platforms: getConnectablePlatforms().map(f => ({ name: f.platform, displayName: f.displayName, icon: f.icon })) }
        }
        const flow = matchPlatform(platform ?? '')
        if (!flow) return { supported: ['qq', 'weixin', 'telegram', 'feishu', 'discord', 'dingtalk'] }
        return { platform: flow.displayName, icon: flow.icon, steps: flow.steps }
      }
    )
  }

  // ── 消息发送 ──
  async *sendMessage(content: string): AsyncGenerator<AgentEvent> {
    if (!this.mcpInitialized) await this.initMCP()
    this.aborted = false

    if (content) {
      const userMsg: Message = { role: 'user', content, timestamp: Date.now() }
      this.history.push(userMsg)
    }

    const context = await this.buildContext()
    const toolDefs = this.tools.getDefinitions()

    try {
      const response = await this.provider.chat(context, this.modelConfig, toolDefs)

      if (this.aborted) return

      // Handle tool calls
      if (response.tool_calls && response.tool_calls.length > 0) {
        yield { type: 'tool_call', name: response.tool_calls[0].function.name, args: JSON.parse(response.tool_calls[0].function.arguments) }

        for (const tc of response.tool_calls) {
          const parsedArgs = JSON.parse(tc.function.arguments)
          const result = await this.tools.execute(tc.function.name, parsedArgs)
          const resultStr = typeof result === 'string' ? result : JSON.stringify(result)

          yield { type: 'tool_result', name: tc.function.name, content: resultStr }

          this.history.push({
            role: 'tool', tool_call_id: tc.id, content: resultStr, timestamp: Date.now()
          })
        }

        // Recursive call for follow-up
        yield* this.sendMessage('')
        return
      }

      // Normal response
      if (response.content) {
        yield { type: 'token', content: response.content }
        this.history.push({ role: 'assistant', content: response.content, timestamp: Date.now() })
        yield { type: 'done', message: this.history[this.history.length - 1] }
      }

      // Memory extraction (background, non-blocking)
      if (this.config.memoryEnabled && this.history.length % 10 === 0) {
        this.memory.extractAndSave(this.history).catch(() => {})
      }
    } catch (error: any) {
      if (!this.aborted) {
        yield { type: 'error', error: error.message || String(error) }
      }
    }
  }

  private async buildContext(): Promise<Message[]> {
    const context: Message[] = []

    // System prompt with persona
    let systemPrompt = ''
    if (this.persona) {
      systemPrompt += `## 你是谁\n${this.persona.systemPrompt}\n\n`
      if (this.persona.name) systemPrompt += `你的名字是${this.persona.name}。\n\n`
    }
    const toolDefs = this.tools.getDefinitions()
    if (toolDefs.length > 0) {
      systemPrompt += '## 可用工具\n'
      toolDefs.forEach(def => { systemPrompt += `- ${def.function.name}: ${def.function.description}\n` })
    }
    if (systemPrompt) {
      context.push({ role: 'system', content: systemPrompt, timestamp: Date.now() })
    }

    // Memory context
    if (this.config.memoryEnabled) {
      const recent = this.history.slice(-3).map(m => m.content).join(' ')
      if (recent) {
        const memories = await this.memory.search(recent, 3)
        if (memories.length > 0) {
          context.push({
            role: 'system',
            content: `相关记忆：\n${memories.map(m => `- ${m.content}`).join('\n')}`,
            timestamp: Date.now()
          })
        }
      }
    }

    context.push(...this.history)
    return context
  }

  // ── Public API (bridge calls these) ──
  abort() { this.aborted = true }

  clearHistory() { this.history = [] }

  getHistory(): Message[] { return [...this.history] }

  updateConfig(patch: Partial<AgentConfig>) {
    if (patch.model) {
      this.modelConfig = patch.model
      this.provider = getProvider(patch.model.provider)
    }
    if (patch.persona) this.persona = patch.persona
    Object.assign(this.config, patch)
  }
}
