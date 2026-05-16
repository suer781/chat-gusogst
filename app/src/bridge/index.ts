import { Agent } from '../agent/core/agent'
import { ToolRegistry } from '../agent/tools/registry'
import type { AgentConfig, AgentEvent, Persona } from '../shared/types'

class AgentBridge {
  private agent: Agent | null = null
  private config: AgentConfig | null = null
  private toolRegistry: ToolRegistry | null = null

  async init(config: AgentConfig, toolRegistry?: ToolRegistry): Promise<void> {
    this.config = config
    this.toolRegistry = toolRegistry || new ToolRegistry()
    this.agent = new Agent(config, this.toolRegistry)
  }

  isReady(): boolean { return this.agent !== null }
  getToolRegistry(): ToolRegistry | null { return this.toolRegistry }

  updateConfig(patch: Partial<AgentConfig>) {
    if (!this.agent || !this.config) return
    Object.assign(this.config, patch)
    this.agent.updateConfig(patch)
  }

  async *chat(message: string): AsyncGenerator<AgentEvent> {
    if (!this.agent) throw new Error('Agent not initialized')
    yield* this.agent.sendMessage(message)
  }

  abort() { this.agent?.abort() }
  clearHistory() { this.agent?.clearHistory() }
  getHistory() { return this.agent?.getHistory() ?? [] }

  switchPersona(persona: Persona) {
    if (!this.config) return
    this.config.persona = persona
    this.agent?.updateConfig({ persona })
  }
}

export const bridge = new AgentBridge()
export type { AgentBridge }
