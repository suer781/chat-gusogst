/**
 * Chaquopy 直连层 — 替代 HTTP bridge + HTTP client
 *
 * 通信方式：TypeScript → Capacitor Plugin → Chaquopy → Python 函数
 * 零 HTTP、零轮询、零序列化开销（同进程 IPC）
 */

export interface ChaquopyPlugin {
  initialize(): Promise<void>
  chat(opts: { message: string; history: any[]; tools?: any[] }): Promise<any>
  callTool(opts: { name: string; args: Record<string, any> }): Promise<any>
  connectPlatform(opts: { name: string; config?: Record<string, any> }): Promise<any>
  disconnectPlatform(opts: { name: string }): Promise<void>
  getPlatformStatus(): Promise<any>
  getPersona(): Promise<any>
  setPersona(opts: { id: string }): Promise<any>
  getStatus(): Promise<any>
}

export type ChaquopyEventType =
  | 'message' | 'thinking' | 'tool_call' | 'tool_result'
  | 'platform_event' | 'error' | 'status'

export interface ChaquopyEvent {
  type: ChaquopyEventType
  data: any
  timestamp: number
}

type Listener = (event: ChaquopyEvent) => void

export class ChaquopyClient {
  private plugin: ChaquopyPlugin | null = null
  private initialized = false
  private listeners = new Map<ChaquopyEventType, Set<Listener>>()

  constructor(plugin?: ChaquopyPlugin) {
    if (plugin) this.plugin = plugin
  }

  setPlugin(plugin: ChaquopyPlugin): void {
    this.plugin = plugin
  }

  async initialize(): Promise<void> {
    if (!this.plugin) {
      console.warn('[Chaquopy] No plugin, using mock mode')
      this.plugin = createMockPlugin()
    }
    await this.plugin.initialize()
    this.initialized = true
  }

  get isInitialized(): boolean { return this.initialized }

  async chat(message: string, history: any[] = [], tools?: any[]): Promise<any> {
    this.ensureReady()
    return this.plugin!.chat({ message, history, tools })
  }

  async callTool(name: string, args: Record<string, any> = {}): Promise<any> {
    this.ensureReady()
    return this.plugin!.callTool({ name, args })
  }

  async connectPlatform(name: string, config?: Record<string, any>): Promise<any> {
    this.ensureReady()
    const result = await this.plugin!.connectPlatform({ name, config })
    this.emit({ type: 'platform_event', data: { action: 'connected', platform: name, ...result }, timestamp: Date.now() })
    return result
  }

  async disconnectPlatform(name: string): Promise<void> {
    this.ensureReady()
    await this.plugin!.disconnectPlatform({ name })
    this.emit({ type: 'platform_event', data: { action: 'disconnected', platform: name }, timestamp: Date.now() })
  }

  async getPlatformStatus(): Promise<Record<string, any>> {
    this.ensureReady()
    return this.plugin!.getPlatformStatus()
  }

  async getPersona(): Promise<any> {
    this.ensureReady()
    return this.plugin!.getPersona()
  }

  async setPersona(id: string): Promise<any> {
    this.ensureReady()
    return this.plugin!.setPersona({ id })
  }

  async getStatus(): Promise<any> {
    this.ensureReady()
    return this.plugin!.getStatus()
  }

  on(type: ChaquopyEventType, listener: Listener): () => void {
    if (!this.listeners.has(type)) this.listeners.set(type, new Set())
    this.listeners.get(type)!.add(listener)
    return () => this.listeners.get(type)?.delete(listener)
  }

  onAny(listener: Listener): () => void {
    return this.on('*' as ChaquopyEventType, listener)
  }

  handleEvent(event: ChaquopyEvent): void { this.emit(event) }

  private emit(event: ChaquopyEvent): void {
    this.listeners.get(event.type)?.forEach(fn => fn(event))
    this.listeners.get('*' as ChaquopyEventType)?.forEach(fn => fn(event))
  }

  private ensureReady(): void {
    if (!this.initialized) throw new Error('[Chaquopy] Not initialized')
  }
}

let _instance: ChaquopyClient | null = null
export function getChaquopyClient(): ChaquopyClient {
  if (!_instance) _instance = new ChaquopyClient()
  return _instance
}

function createMockPlugin(): ChaquopyPlugin {
  return {
    async initialize() { console.log('[Mock] Initialized') },
    async chat({ message }) {
      return { role: 'assistant', content: '[Mock] ' + message, timestamp: Date.now() }
    },
    async callTool({ name, args }) { return { ok: true, tool: name } },
    async connectPlatform({ name }) { return { success: true, platform: name } },
    async disconnectPlatform() {},
    async getPlatformStatus() { return {} },
    async getPersona() { return { id: 'default', name: 'AI' } },
    async setPersona({ id }) { return { id } },
    async getStatus() { return { state: 'ready' } },
  }
}
