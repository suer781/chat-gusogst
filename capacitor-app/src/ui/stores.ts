import { create } from 'zustand'
import type { Message, Persona, AgentConfig } from './types'
import type { Lang } from './i18n'
import { setLang, notifyLangChange } from './i18n'

export type ChatState = {
  messages: Message[]
  streaming: boolean
  error: string | null
  addMessage: (msg: Message, upsert?: boolean) => void
  updateMessage: (id: string, patch: Partial<Message>) => void
  clearMessages: () => void
  setStreaming: (s: boolean) => void
  setError: (e: string | null) => void
}

export const useChatStore = create<ChatState>()((set) => ({
  messages: [],
  streaming: false,
  error: null,
  addMessage: (msg, upsert) => set((s) => {
    if (upsert && s.messages.some(m => m.id === msg.id)) {
      return { messages: s.messages.map(m => m.id === msg.id ? { ...m, ...msg, timestamp: msg.timestamp ?? m.timestamp } : m) }
    }
    return { messages: [...s.messages, msg] }
  }),
  updateMessage: (id, patch) => set((s) => ({
    messages: s.messages.map(m => m.id === id ? { ...m, ...patch } : m)
  })),
  clearMessages: () => set({ messages: [] }),
  setStreaming: (streaming) => set({ streaming }),
  setError: (error) => set({ error }),
}))

export type SettingsState = {
  config: AgentConfig
  language: Lang
  setModel: (provider: string, model: string, apiKey?: string, baseUrl?: string) => void
  setApiKey: (key: string) => void
  setBaseUrl: (url: string) => void
  setApiHost: (host: string) => void
  setPersona: (p: Persona) => void
  setSearchEnabled: (enabled: boolean) => void
  setMemoryEnabled: (enabled: boolean) => void
  setTemperature: (t: number) => void
  setMaxTokens: (t: number) => void
  setLanguage: (lang: Lang) => void
  setShowThinking: (v: boolean) => void
  setShowToolCalls: (v: boolean) => void
  setShowMemoryHints: (v: boolean) => void
  setShowSearchSources: (v: boolean) => void
  setShowErrorDetails: (v: boolean) => void
  themeMode: string
  setThemeMode: (m: string) => void
  fontSize: number
  setFontSize: (s: number) => void
  eyeCareEnabled: boolean
  setEyeCareEnabled: (v: boolean) => void
  eyeCareColors: Record<string, string>
  setEyeCareColors: (c: Record<string, string>) => void
  glassEnabled: boolean
  setGlassEnabled: (v: boolean) => void
}

export const useSettingsStore = create<SettingsState>()((set) => ({
  config: {
    model: { provider: 'openai', model: 'gpt-4o', apiKey: '', baseUrl: '', apiHost: '', temperature: 0.7, maxTokens: 4096 },
    persona: { id: 'default', name: 'Hermes', systemPrompt: 'You are Hermes, a helpful AI assistant.', tags: ['general'] },
    searchEnabled: false,
    channel: 'default',
    maxRounds: 10,
    memoryEnabled: true,
    maxHistoryTokens: 100000,
    showThinking: false,
    showToolCalls: false,
    showMemoryHints: false,
    showSearchSources: false,
    showErrorDetails: false,
  },
  themeMode: 'dark',
  fontSize: 14,
  eyeCareEnabled: false,
  eyeCareColors: { white: '#F5F0E8', lightGray: '#D4C9B8', darkGray: '#1A1A1A', black: '#000000' },
  glassEnabled: true,
  language: 'zh' as Lang,
  setModel: (provider, model, apiKey, baseUrl) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, provider, model, ...(apiKey !== undefined ? { apiKey } : {}), ...(baseUrl !== undefined ? { baseUrl } : {}) } } })),
  setApiKey: (key) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, apiKey: key } } })),
  setBaseUrl: (url) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, baseUrl: url } } })),
  setApiHost: (host) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, apiHost: host } } })),
  setPersona: (persona) => set((s) => ({ config: { ...s.config, persona } })),
  setSearchEnabled: (enabled) => set((s) => ({ config: { ...s.config, searchEnabled: enabled } })),
  setMemoryEnabled: (enabled) => set((s) => ({ config: { ...s.config, memoryEnabled: enabled } })),
  setTemperature: (t) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, temperature: t } } })),
  setMaxTokens: (t) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, maxTokens: t } } })),
  setShowThinking: (v) => set((s) => ({ config: { ...s.config, showThinking: v } })),
  setShowToolCalls: (v) => set((s) => ({ config: { ...s.config, showToolCalls: v } })),
  setShowMemoryHints: (v) => set((s) => ({ config: { ...s.config, showMemoryHints: v } })),
  setShowSearchSources: (v) => set((s) => ({ config: { ...s.config, showSearchSources: v } })),
  setShowErrorDetails: (v) => set((s) => ({ config: { ...s.config, showErrorDetails: v } })),
  setThemeMode: (m) => set({ themeMode: m }),
  setFontSize: (s) => set({ fontSize: s }),
  setEyeCareEnabled: (v) => set({ eyeCareEnabled: v }),
  setEyeCareColors: (c) => set({ eyeCareColors: c }),
  setGlassEnabled: (v) => set({ glassEnabled: v }),
  setLanguage: (lang) => {
    setLang(lang)
    notifyLangChange()
    set({ language: lang })
  },
}))
