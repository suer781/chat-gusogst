/**
 * Bridge — 连接 Agent 和 UI
 * 在 Android (Capacitor) 和桌面 (Electron) 中统一使用
 */
import { Agent } from '../agent/core/agent'
import type { AgentConfig, AgentEvent, Persona } from '../shared/types'

class AgentBridge {
  private agent: Agent | null = null
  private config: AgentConfig | null = null

  async init(config: AgentConfig): Promise<void> {
    this.config = config
    this.agent = new Agent(config)
  }

  isReady(): boolean { return this.agent !== null }

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

// 单例
export const bridge = new AgentBridge()
export type { AgentBridge }
