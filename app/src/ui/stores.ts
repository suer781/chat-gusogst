/**
 * Zustand Stores — 全局状态管理
 */
import { create } from 'zustand'
import type { AgentConfig, Message, Persona } from '../shared/types'
import { bridge } from '../bridge'
import { PersonaManager } from '../agent/core/persona'
import { MemoryManager } from '../agent/memory'
import { registerSearchTools } from '../agent/tools/search'
import { ToolRegistry } from '../agent/tools/registry'

// ── Chat Store ──────────────────────────────────────
interface ChatState {
  messages: Message[]
  isStreaming: boolean
  error: string | null
  sendMessage: (content: string) => Promise<void>
  abort: () => void
  clear: () => void
}

export const useChatStore = create<ChatState>((set, get) => ({
  messages: [],
  isStreaming: false,
  error: null,

  sendMessage: async (content: string) => {
    const userMsg: Message = { id: crypto.randomUUID(), role: 'user', content, timestamp: Date.now() }
    set(s => ({ messages: [...s.messages, userMsg], isStreaming: true, error: null }))

    let assistantContent = ''
    const assistantMsg: Message = { id: crypto.randomUUID(), role: 'assistant', content: '', timestamp: Date.now() }
    set(s => ({ messages: [...s.messages, assistantMsg] }))

    try {
      for await (const event of bridge.chat(content)) {
        switch (event.type) {
          case 'token':
            assistantContent += event.content || ''
            set(s => {
              const msgs = [...s.messages]
              msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], content: assistantContent }
              return { messages: msgs }
            })
            break
          case 'tool_call':
            set(s => {
              const msgs = [...s.messages]
              msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], content: assistantContent + `\n\n🔧 调用工具: ${event.name}` }
              return { messages: msgs }
            })
            break
          case 'tool_result':
            set(s => {
              const msgs = [...s.messages]
              msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], content: assistantContent + `\n\n📋 工具结果: ${event.message}` }
              return { messages: msgs }
            })
            break
          case 'error':
            set({ error: event.error })
            break
          case 'done':
            set(s => {
              const msgs = [...s.messages]
              msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], content: assistantContent || event.message || '' }
              return { messages: msgs, isStreaming: false }
            })
            break
        }
      }
    } catch (err: any) {
      set({ error: err.message, isStreaming: false })
    }
  },

  abort: () => {
    bridge.abort()
    set({ isStreaming: false })
  },

  clear: () => set({ messages: [], error: null }),
}))

// ── Settings Store ──────────────────────────────────────
interface SettingsState {
  config: AgentConfig
  personaManager: PersonaManager
  memoryManager: MemoryManager
  initialized: boolean
  theme: 'light' | 'dark' | 'auto'
  fontSize: 'small' | 'medium' | 'large'
  language: string
  init: () => Promise<void>
  updateConfig: (patch: Partial<AgentConfig>) => void
  switchPersona: (id: string) => void
  addCustomPersona: (name: string, prompt: string) => void
  setTheme: (theme: 'light' | 'dark' | 'auto') => void
  setFontSize: (size: 'small' | 'medium' | 'large') => void
  setLanguage: (lang: string) => void
}

const memoryMgr = new MemoryManager()
const personaMgr = new PersonaManager()

const DEFAULT_CONFIG: AgentConfig = {
  model: {
    provider: 'openai',
    model: 'gpt-4o-mini',
    apiKey: '',
    baseUrl: 'https://api.openai.com/v1',
    apiHost: '',
    temperature: 0.7,
    maxTokens: 4096,
  },
  persona: personaMgr.getActive(),
  searchEnabled: false,
  channel: 'app',
  maxRounds: 10,
  memoryEnabled: true,
  maxHistoryTokens: 8000,
}

export const useSettingsStore = create<SettingsState>((set, get) => ({
  config: DEFAULT_CONFIG,
  personaManager: personaMgr,
  memoryManager: memoryMgr,
  initialized: false,
  theme: 'auto',
  fontSize: 'medium',
  language: 'zh-CN',

  init: async () => {
    const state = get()
    if (state.initialized) return

    try {
      // 加载保存的配置
      const savedConfig = localStorage.getItem('chat-gusogst-config')
      let config = DEFAULT_CONFIG
      if (savedConfig) {
        config = { ...DEFAULT_CONFIG, ...JSON.parse(savedConfig) }
      }

      // 注册搜索工具
      const toolRegistry = new ToolRegistry()
      if (config.searchEnabled) {
        registerSearchTools(toolRegistry, config as any)
      }

      // 初始化 bridge
      await bridge.init(config)

      set({ config, initialized: true })
    } catch (err: any) {
      console.error('初始化失败:', err)
    }
  },

  updateConfig: (patch: Partial<AgentConfig>) => {
    const state = get()
    const newConfig = { ...state.config, ...patch }
    localStorage.setItem('chat-gusogst-config', JSON.stringify(newConfig))
    set({ config: newConfig })
  },

  switchPersona: (id: string) => {
    const state = get()
    const persona = state.personaManager.switchTo(id)
    state.updateConfig({ persona })
  },

  addCustomPersona: (name: string, prompt: string) => {
    const state = get()
    const persona = state.personaManager.add({ name, systemPrompt: prompt, tags: ['自定义'] })
    state.updateConfig({ persona })
  },

  setTheme: (theme: 'light' | 'dark' | 'auto') => set({ theme }),
  setFontSize: (fontSize: 'small' | 'medium' | 'large') => set({ fontSize }),
  setLanguage: (language: string) => set({ language }),
}))
