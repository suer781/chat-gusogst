import { ProviderPreset, ProviderType, ModelProvider } from '../types';

/** Chatbox-style: 提供方预设（原生实现，非复制） */
export const PROVIDER_PRESETS: ProviderPreset[] = [
  { type: 'openai', name: 'OpenAI', icon: '🟢', defaultUrl: 'https://api.openai.com', defaultModel: 'gpt-4o-mini', models: ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'o1-mini'] },
  { type: 'anthropic', name: 'Anthropic', icon: '🟤', defaultUrl: 'https://api.anthropic.com', defaultModel: 'claude-sonnet-4-20250514', models: ['claude-sonnet-4-20250514', 'claude-3-5-haiku-20241022'] },
  { type: 'deepseek', name: 'DeepSeek', icon: '🔵', defaultUrl: 'https://api.deepseek.com', defaultModel: 'deepseek-chat', models: ['deepseek-chat', 'deepseek-reasoner'] },
  { type: 'gemini', name: 'Gemini', icon: '🔴', defaultUrl: 'https://generativelanguage.googleapis.com', defaultModel: 'gemini-2.0-flash', models: ['gemini-2.0-flash', 'gemini-1.5-pro'] },
  { type: 'custom', name: '自定义', icon: '⚪', defaultUrl: '', defaultModel: '', models: [] },
];

export function getPreset(type: ProviderType) {
  return PROVIDER_PRESETS.find(p => p.type === type);
}

export function createProvider(type: ProviderType, apiKey: string, overrides?: Partial<ModelProvider>): ModelProvider {
  const p = getPreset(type);
  return { id: 'p_' + Date.now(), name: p?.name || '自定义', type, apiKey, apiUrl: p?.defaultUrl || '', model: p?.defaultModel || '', enabled: true, ...overrides };
}
