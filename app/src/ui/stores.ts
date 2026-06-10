import { create } from 'zustand'
import type { UIMessage as Message, Persona, AppSettings } from './types'
import { setLang, notifyLangChange } from './i18n'

type Lang = 'zh' | 'en'
// ── Eye Care Color Mapping ──
export interface EyeCareMapping {
  id: string
  sourceColor: string
  targetColor: string
  label?: string
}

let _mappingId = Date.now()
export const genMappingId = () => `ec_${++_mappingId}`

export const DEFAULT_EYE_CARE_MAPPINGS: EyeCareMapping[] = [
  { id: genMappingId(), sourceColor: '#0000FF', targetColor: '#FF8C00', label: '纯蓝→暖橙' },
  { id: genMappingId(), sourceColor: '#000000', targetColor: '#1A1412', label: '纯黑→深棕' },
  { id: genMappingId(), sourceColor: '#FFFFFF', targetColor: '#F5F0E8', label: '纯白→米黄' },
  { id: genMappingId(), sourceColor: '#808080', targetColor: '#8B7D6B', label: '中灰→暖灰' },
]


// ── Chat Store ──────────────────────────────────
interface ChatState {
  messages: Message[]
  streaming: boolean
  error: string | null
  addMessage: (msg: Message, upsert?: boolean) => void
  updateMessage: (id: string, patch: Partial<Message>) => void
  clearMessages: () => void
  setStreaming: (s: boolean) => void
  setError: (e: string | null) => void
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  streaming: false,
  error: null,
  addMessage: (msg, upsert) => set((s) => {
    if (upsert && msg.id) {
      const idx = s.messages.findIndex(m => m.id === msg.id)
      if (idx >= 0) {
        const next = [...s.messages]
        next[idx] = { ...next[idx], ...msg, timestamp: msg.timestamp ?? next[idx].timestamp }
        return { messages: next }
      }
    }
    return { messages: [...s.messages, msg] }
  }),
  updateMessage: (id, patch) => set((s) => ({
    messages: s.messages.map(m => m.id === id ? { ...m, ...patch } : m)
  })),
  clearMessages: () => set({ messages: [], error: null }),
  setStreaming: (streaming) => set({ streaming }),
  setError: (error) => set({ error }),
}))

// ── Settings Store ─────────────────────────────
const DEFAULT_PERSONA: Persona = {
  id: 'default',
  name: 'Hermes',
  systemPrompt: 'You are Hermes, a helpful AI assistant.',
  tags: ['general'],
}

const DEFAULT_SETTINGS: AppSettings = {
  model: {
    provider: 'openai',
    model: 'gpt-4o',
    apiKey: '',
    baseUrl: '',
    apiHost: '',
    temperature: 0.7,
    maxTokens: 4096,
  },
  persona: DEFAULT_PERSONA,
  searchEnabled: false,
  searchEngine: 'duckduckgo',
  searchApiKey: '',
  mcpServers: [],
  channel: 'default',
  maxRounds: 10,
  memoryEnabled: true,
  maxHistoryTokens: 100000,
  showThinking: false,
  showToolCalls: false,
  showMemoryHints: false,
  showSearchSources: false,
  showErrorDetails: false,
}

interface SettingsState extends AppSettings {
  // UI-only state
  language: Lang
  themeMode: string
  fontSize: number
  eyeCareEnabled: boolean
  eyeCareColors: EyeCareMapping[]
  eyeCareIntensity: number  // 0-100, 护眼强度
  glassEnabled: boolean
  glassOpacity: number  // 0-100, 毛玻璃透明度
  glassTier: 'auto' | 'full' | 'light' | 'off'  // 毛玻璃性能等级
  performanceHint: string  // 检测结果提示
  hapticEnabled: boolean  // 转子马达触觉反馈
  hdrEnabled: boolean  // HDR高动态玻璃质感

  // Model setters
  setModel: (provider: string, model: string, apiKey?: string, baseUrl?: string) => void
  setApiKey: (key: string) => void
  setBaseUrl: (url: string) => void
  setApiHost: (host: string) => void
  setTemperature: (t: number) => void
  setMaxTokens: (t: number) => void

  // Agent setters
  setPersona: (p: Persona) => void
  setSearchEnabled: (enabled: boolean) => void
  setSearchEngine: (engine: string) => void
  setSearchApiKey: (key: string) => void
  setMemoryEnabled: (enabled: boolean) => void
  setMaxRounds: (r: number) => void
  setMaxHistoryTokens: (t: number) => void

  // Display setters
  setShowThinking: (v: boolean) => void
  setShowToolCalls: (v: boolean) => void
  setShowMemoryHints: (v: boolean) => void
  setShowSearchSources: (v: boolean) => void
  setShowErrorDetails: (v: boolean) => void

  // UI setters
  setThemeMode: (m: string) => void
  setFontSize: (s: number) => void
  setEyeCareEnabled: (v: boolean) => void
  setEyeCareColors: (c: EyeCareMapping[]) => void
  setEyeCareIntensity: (v: number) => void
  setGlassEnabled: (v: boolean) => void
  setGlassOpacity: (v: number) => void
  setGlassTier: (t: 'auto' | 'full' | 'light' | 'off') => void
  setPerformanceHint: (h: string) => void
  setHapticEnabled: (v: boolean) => void
  setHdrEnabled: (v: boolean) => void
  setLanguage: (lang: Lang) => void

  // Bulk update
  updateSettings: (patch: Partial<AppSettings>) => void
}

export const useSettingsStore = create<SettingsState>((set) => ({
  ...DEFAULT_SETTINGS,

  // UI-only defaults
  language: 'zh',
  themeMode: 'system',
  fontSize: 14,
  eyeCareEnabled: false,
  eyeCareColors: DEFAULT_EYE_CARE_MAPPINGS,
  eyeCareIntensity: 30,
  glassEnabled: true,
  glassOpacity: 80,
  glassTier: 'auto',
  performanceHint: '',
  hapticEnabled: true,
  hdrEnabled: true,

  // Model
  setModel: (provider, model, apiKey, baseUrl) => set((s) => ({
    model: { ...s.model, provider, model, ...(apiKey !== undefined ? { apiKey } : {}), ...(baseUrl !== undefined ? { baseUrl } : {}) }
  })),
  setApiKey: (key) => set((s) => ({ model: { ...s.model, apiKey: key } })),
  setBaseUrl: (url) => set((s) => ({ model: { ...s.model, baseUrl: url } })),
  setApiHost: (host) => set((s) => ({ model: { ...s.model, apiHost: host } })),
  setTemperature: (t) => set((s) => ({ model: { ...s.model, temperature: t } })),
  setMaxTokens: (t) => set((s) => ({ model: { ...s.model, maxTokens: t } })),

  // Agent
  setPersona: (persona) => set({ persona }),
  setSearchEnabled: (searchEnabled) => set({ searchEnabled }),
  setSearchEngine: (searchEngine) => set({ searchEngine }),
  setSearchApiKey: (searchApiKey) => set({ searchApiKey }),
  setMemoryEnabled: (memoryEnabled) => set({ memoryEnabled }),
  setMaxRounds: (maxRounds) => set({ maxRounds }),
  setMaxHistoryTokens: (maxHistoryTokens) => set({ maxHistoryTokens }),

  // Display
  setShowThinking: (showThinking) => set({ showThinking }),
  setShowToolCalls: (showToolCalls) => set({ showToolCalls }),
  setShowMemoryHints: (showMemoryHints) => set({ showMemoryHints }),
  setShowSearchSources: (showSearchSources) => set({ showSearchSources }),
  setShowErrorDetails: (showErrorDetails) => set({ showErrorDetails }),

  // UI
  setThemeMode: (themeMode) => set({ themeMode }),
  setFontSize: (fontSize) => set({ fontSize }),
  setEyeCareEnabled: (eyeCareEnabled) => set({ eyeCareEnabled }),
  setEyeCareColors: (eyeCareColors) => set({ eyeCareColors }),
  setEyeCareIntensity: (eyeCareIntensity) => set({ eyeCareIntensity }),
  setGlassEnabled: (glassEnabled) => set({ glassEnabled }),
  setGlassOpacity: (glassOpacity) => set({ glassOpacity }),
  setGlassTier: (glassTier) => set({ glassTier }),
  setPerformanceHint: (performanceHint) => set({ performanceHint }),
  setHapticEnabled: (hapticEnabled) => set({ hapticEnabled }),
  setHdrEnabled: (hdrEnabled) => set({ hdrEnabled }),
  setLanguage: (lang) => {
    setLang(lang)
    notifyLangChange()
    set({ language: lang })
  },

  // Bulk
  updateSettings: (patch) => set((s) => ({ ...s, ...patch })),
}))
