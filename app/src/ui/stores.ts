/**
 * Zustand Stores — 全局状态管理
 */
import { create } from 'zustand'
import type { AgentConfig, Message, Persona } from '../shared/types'
import { bridge } from '../bridge'
import { PersonaManager } from '../agent/core/persona'
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
    const userMsg: Message = { role: 'user', content, timestamp: Date.now() }
    set(s => ({ messages: [...s.messages, userMsg], isStreaming: true, error: null }))

    let assistantContent = ''
    const assistantMsg: Message = { role: 'assistant', content: '', timestamp: Date.now() }
    set(s => ({ messages: [...s.messages, assistantMsg] }))

    try {
      for await (const event of bridge.chat(content)) {
        switch (event.type) {
          case 'token':
            assistantContent += event.content
            set(s => {
              const msgs = [...s.messages]
              msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], content: assistantContent }
              return { messages: msgs }
            })
            break
          case 'tool_call':
            assistantContent += `\n🔧 调用 ${event.name}...`
            set(s => {
              const msgs = [...s.messages]
              msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], content: assistantContent }
              return { messages: msgs }
            })
            break
          case 'tool_result':
            assistantContent += event.result ? ` [${event.tool}: ${String(event.result).slice(0, 100)}]` : ` ✅`
            set(s => {
              const msgs = [...s.messages]
              msgs[msgs.length - 1] = { ...msgs[msgs.length - 1], content: assistantContent }
              return { messages: msgs }
            })
            break
          case 'error':
            set({ error: event.error })
            break
          case 'done':
            set(s => {
              const msgs = [...s.messages]
              msgs[msgs.length - 1] = { ...event.message, content: assistantContent || event.message.content }
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

  clear: () => {
    bridge.clearHistory()
    set({ messages: [], error: null })
  },
}))

// ── Settings Store ──────────────────────────────────
interface SettingsState {
  config: AgentConfig
  personaManager: PersonaManager
  initialized: boolean
  theme: 'light' | 'dark'
  fontSize: number
  language: string
  init: () => Promise<void>
  updateConfig: (patch: Partial<AgentConfig>) => void
  switchPersona: (id: string) => void
  addCustomPersona: (name: string, prompt: string) => void
  setTheme: (theme: 'light' | 'dark') => void
  setFontSize: (size: number) => void
  setLanguage: (lang: string) => void
}

const personaMgr = new PersonaManager()

const DEFAULT_CONFIG: AgentConfig = {
  model: {
    provider: 'openai',
    model: 'gpt-4o-mini',
    apiKey: '',
    apiHost: '',
    temperature: 0.7,
    maxTokens: 4096,
  },
  persona: personaMgr.getActive(),
  memoryEnabled: true,
  maxRounds: 10,
  maxHistoryTokens: 8000,
  searchEnabled: false,
  searchEngine: 'duckduckgo',
  channel: 'app',
}

function loadConfig(): AgentConfig {
  try {
    const raw = localStorage.getItem('chat-gusogst-config')
    if (raw) {
      const saved = JSON.parse(raw)
      return { ...DEFAULT_CONFIG, ...saved, persona: personaMgr.getActive() }
    }
  } catch {}
  return DEFAULT_CONFIG
}

export const useSettingsStore = create<SettingsState>((set, get) => ({
  config: loadConfig(),
  personaManager: personaMgr,
  initialized: false,
  theme: 'light' as const,
  fontSize: 14,
  language: 'zh-CN',
  setTheme: (theme) => set({ theme }),
  setFontSize: (fontSize) => set({ fontSize }),
  setLanguage: (language) => set({ language }),

  init: async () => {
    const { config } = get()
    const tools = new ToolRegistry()
    if (config.searchEnabled) {
      registerSearchTools(tools, { engine: config.searchEngine, apiKey: config.searchApiKey })
    }
    await bridge.init(config)
    set({ initialized: true })
  },

  updateConfig: (patch) => {
    set(s => {
      const config = { ...s.config, ...patch }
      localStorage.setItem('chat-gusogst-config', JSON.stringify(config))
      bridge.updateConfig(patch)
      return { config }
    })
  },

  switchPersona: (id) => {
    const p = personaMgr.switchTo(id)
    set(s => ({ config: { ...s.config, persona: p } }))
    bridge.switchPersona(p)
  },

  addCustomPersona: (name, prompt) => {
    personaMgr.add({ name, systemPrompt: prompt, tags: ['自定义'] })
  },
}))
