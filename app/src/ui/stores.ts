import { create } from 'zustand'
import type { Message, Persona, AgentConfig } from './types'
import type { Lang } from './i18n'
import { setLang, notifyLangChange } from './i18n'

export type ChatState = {
  messages: Message[]
  streaming: boolean
  error: string | null
  addMessage: (msg: Message) => void
  clearMessages: () => void
  setStreaming: (s: boolean) => void
  setError: (e: string | null) => void
}

export const useChatStore = create<ChatState>()((set) => ({
  messages: [],
  streaming: false,
  error: null,
  addMessage: (msg) => set((s) => ({ messages: [...s.messages, msg] })),
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
}

const defaultPersona: Persona = {
  id: 'default', name: 'Hermes', systemPrompt: 'You are Hermes, a helpful AI assistant.', tags: ['general'],
}

export const useSettingsStore = create<SettingsState>()((set) => ({
  config: {
    model: { provider: 'openai', model: 'gpt-4o', apiKey: '', baseUrl: '', apiHost: '', temperature: 0.7, maxTokens: 4096 },
    persona: defaultPersona, searchEnabled: false, channel: 'default', maxRounds: 10, memoryEnabled: true, maxHistoryTokens: 100000,
  },
  language: 'zh' as Lang,
  setModel: (provider, model, apiKey, baseUrl) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, provider, model, ...(apiKey && { apiKey }), ...(baseUrl && { baseUrl }) } } })),
  setApiKey: (key) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, apiKey: key } } })),
  setBaseUrl: (url) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, baseUrl: url } } })),
  setApiHost: (host) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, apiHost: host } } })),
  setPersona: (persona) => set((s) => ({ config: { ...s.config, persona } })),
  setSearchEnabled: (enabled) => set((s) => ({ config: { ...s.config, searchEnabled: enabled } })),
  setMemoryEnabled: (enabled) => set((s) => ({ config: { ...s.config, memoryEnabled: enabled } })),
  setTemperature: (t) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, temperature: t } } })),
  setMaxTokens: (t) => set((s) => ({ config: { ...s.config, model: { ...s.config.model, maxTokens: t } } })),
  setLanguage: (lang) => {
    setLang(lang)
    notifyLangChange()
    set({ language: lang })
  },
}))
