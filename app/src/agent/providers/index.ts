import type { ProviderAdapter } from '../../shared/types'
import { OpenAIProvider } from './openai'
import { AnthropicProvider } from './anthropic'
import { PROVIDER_PRESETS, findPreset, type ProviderPreset } from './presets'

const providers: Record<string, ProviderAdapter> = {}

function register(p: ProviderAdapter) {
  providers[p.name] = p
}

// Register built-in providers
register(new OpenAIProvider() as any)
register(new AnthropicProvider() as any)

// Auto-register all Hermes presets as OpenAI-compatible providers
for (const preset of PROVIDER_PRESETS) {
  if (preset.type === 'skip') continue
  if (providers[preset.name]) continue // don't override built-in

  if (preset.type === 'anthropic') {
    // Anthropic uses its own provider class
    register(new AnthropicProvider(preset.baseUrl))
  } else {
    // OpenAI-compatible (including gemini via OpenAI compat layer)
    register(new OpenAIProvider(preset.baseUrl))
  }

  // Also register aliases
  for (const alias of preset.aliases) {
    if (!providers[alias]) {
      if (preset.type === 'anthropic') {
        register(new AnthropicProvider(preset.baseUrl))
      } else {
        register(new OpenAIProvider(preset.baseUrl))
      }
    }
  }
}

export function getProvider(name: string): any {
  const p = providers[name]
  if (!p) throw new Error(`Unknown provider: ${name}. Available: ${Object.keys(providers).join(', ')}`)
  return p
}

export function listProviders(): string[] {
  return Object.keys(providers)
}

export function registerProvider(p: ProviderAdapter) {
  register(p)
}

export { findPreset, PROVIDER_PRESETS, type ProviderPreset }
export { fetchModels, fetchChatModels } from './fetch-models'
export type { FetchedModel } from './fetch-models'

export type { ApiType } from './fetch-models'
