import { HermesClient } from './client'
import type { HermesConfig, PlatformInfo } from './types'

export interface BridgeConfig {
  port: number
  entryScript: string
  startupTimeoutMs: number
}

const DEFAULT_CONFIG: BridgeConfig = {
  port: 8642,
  entryScript: 'hermes_server.py',
  startupTimeoutMs: 15000,
}

export type BridgeState = 'stopped' | 'starting' | 'running' | 'error'

export class HermesBridge {
  private config: BridgeConfig
  private client: HermesClient
  private state: BridgeState = 'stopped'
  private error: string | null = null

  constructor(config?: Partial<BridgeConfig>) {
    this.config = { ...DEFAULT_CONFIG, ...config }
    this.client = new HermesClient({
      baseUrl: `http://127.0.0.1:${this.config.port}`,
      timeout: 10000,
    })
  }

  async start(): Promise<void> {
    if (this.state === 'running') return
    this.state = 'starting'
    this.error = null
    try {
      await this.startPython()
      await this.waitForReady()
      this.state = 'running'
      console.log('[HermesBridge] started')
    } catch (e: any) {
      this.state = 'error'
      this.error = e.message
      throw e
    }
  }

  async stop(): Promise<void> {
    if (this.state === 'stopped') return
    try { await this.stopPython() } catch {}
    this.state = 'stopped'
  }

  async restart(): Promise<void> {
    await this.stop()
    await this.start()
  }

  getClient(): HermesClient { return this.client }

  async isOnline(): Promise<boolean> { return this.client.isOnline() }

  async getPlatforms(): Promise<PlatformInfo[]> { return this.client.getPlatforms() }

  async togglePlatform(name: string, enabled: boolean): Promise<boolean> {
    return this.client.togglePlatform(name, enabled)
  }

  private async startPython(): Promise<void> {
    // @ts-ignore - Capacitor plugin
    const { HermesPlugin } = window.Capacitor?.Plugins ?? {}
    if (!HermesPlugin) throw new Error('HermesPlugin not available')
    await HermesPlugin.start({ script: this.config.entryScript, port: this.config.port })
  }

  private async stopPython(): Promise<void> {
    // @ts-ignore
    const { HermesPlugin } = window.Capacitor?.Plugins ?? {}
    if (HermesPlugin) await HermesPlugin.stop()
  }

  private async waitForReady(): Promise<void> {
    const deadline = Date.now() + this.config.startupTimeoutMs
    while (Date.now() < deadline) {
      try {
        if (await this.client.isOnline()) return
      } catch {}
      await new Promise(r => setTimeout(r, 500))
    }
    throw new Error('Hermes server not ready')
  }

  getState(): BridgeState { return this.state }
  getError(): string | null { return this.error }
}
