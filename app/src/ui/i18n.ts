export type Lang = 'zh' | 'en'

const translations: Record<string, Record<Lang, string>> = {
  // App.tsx
  'nav.chat': { zh: '聊天', en: 'Chat' },
  'nav.persona': { zh: '角色', en: 'Persona' },
  'nav.providers': { zh: '供应商', en: 'Providers' },
  'nav.settings': { zh: '设置', en: 'Settings' },
  'btn.back': { zh: '返回', en: 'Back' },

  // ChatView
  'chat.aiAssistant': { zh: 'AI 助手', en: 'AI Assistant' },
  'chat.startTitle': { zh: '开始对话', en: 'Start a conversation' },
  'chat.startHint': { zh: '输入消息开始聊天', en: 'Type a message below to begin' },
  'chat.placeholder': { zh: '输入消息...', en: 'Type a message...' },
  'chat.thinking': { zh: '思考中...', en: 'Thinking...' },
  'chat.newChat': { zh: '新对话', en: 'New Chat' },
  'chat.noBackend': { zh: 'Hermes 后端尚未连接。请先在供应商页面配置模型。', en: 'Hermes backend not yet connected. Configure a model provider in Providers.' },

  // SettingsView
  'settings.model': { zh: '模型', en: 'Model' },
  'settings.noModel': { zh: '未选择模型', en: 'No model selected' },
  'settings.persona': { zh: '角色', en: 'Persona' },
  'settings.customPersona': { zh: '自定义角色', en: 'Custom persona' },
  'settings.features': { zh: '功能', en: 'Features' },
  'settings.memory': { zh: '记忆', en: 'Memory' },
  'settings.memoryDesc': { zh: '跨对话记住关键信息', en: 'Remember facts across conversations' },
  'settings.search': { zh: '外界数据来源', en: 'External Data' },
  'settings.searchDesc': { zh: '联网搜索、网页抓取等外部数据', en: 'Web search, web scraping and external data' },
  'settings.advanced': { zh: '高级设置', en: 'Advanced' },
  'settings.temperature': { zh: '温度', en: 'Temperature' },
  'settings.maxTokens': { zh: '最大令牌数', en: 'Max Tokens' },
  'settings.language': { zh: '语言', en: 'Language' },

  // PersonaView
  'persona.search': { zh: '搜索角色...', en: 'Search personas...' },
  'persona.create': { zh: '创建自定义角色', en: 'Create Custom Persona' },

  // ProviderSettings
  'provider.dataFrom': { zh: '数据来源 models.dev', en: 'Powered by models.dev' },
  'provider.search': { zh: '搜索供应商或模型...', en: 'Search providers or models...' },
  'provider.fetchLive': { zh: '拉取实时列表', en: 'Fetch Live' },
  'provider.docs': { zh: '文档', en: 'Docs' },
  'provider.searchModels': { zh: '个模型...', en: ' models...' },
  'provider.noMatch': { zh: '没有匹配的供应商', en: 'No providers match your search' },
  'provider.apiKey': { zh: 'API 密钥', en: 'API Key' },
  'provider.baseUrl': { zh: '基础地址', en: 'Base URL' },
  'provider.providers': { zh: '个供应商', en: ' Providers' },
  'provider.models': { zh: '个模型', en: ' Models' },

  // Preset personas
  'persona.hermes.desc': { zh: '你是一个乐于助人的 AI 助手。', en: 'You are Hermes, a helpful AI assistant.' },
  'persona.muse.desc': { zh: '你是一个创意写作助手，帮助用户创作优美的散文、诗歌和故事。', en: 'You are Muse, a creative writing assistant.' },
  'persona.hephaestus.desc': { zh: '你是一个专业程序员，帮助用户编写、调试和理解代码。', en: 'You are Hephaestus, an expert programmer.' },
  'persona.athena.desc': { zh: '你是一个敏锐的分析思考者，帮助用户分析数据、解决问题。', en: 'You are Athena, a sharp analytical thinker.' },
  'persona.socrates.desc': { zh: '你是一个耐心的老师，通过提问和解释引导用户学习。', en: 'You are Socrates, a patient teacher.' },
  'persona.companion.desc': { zh: '你是一个温暖、有同理心的朋友，善于倾听和支持。', en: 'You are a warm, empathetic friend.' },
}

let currentLang: Lang = 'zh'

export function setLang(lang: Lang) { currentLang = lang }
export function getLang(): Lang { return currentLang }

export function t(key: string): string {
  const entry = translations[key]
  if (!entry) return key
  return entry[currentLang] || entry.zh || key
}

type Listener = () => void
const listeners: Set<Listener> = new Set()
export function onLangChange(fn: Listener) { listeners.add(fn); return () => listeners.delete(fn) }
export function notifyLangChange() { listeners.forEach((fn) => fn()) }
