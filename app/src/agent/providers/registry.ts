import type { ProviderAdapter } from '../../shared/agent-types'
import { OpenAIProvider } from './openai'
import { AnthropicProvider } from './anthropic'

type ProviderFactory = () => ProviderAdapter

const providers = new Map<string, ProviderFactory>()

// Register built-in providers
providers.set('openai', () => new OpenAIProvider())
providers.set('deepseek', () => new OpenAIProvider())
providers.set('groq', () => new OpenAIProvider())
providers.set('together', () => new OpenAIProvider())
providers.set('siliconflow', () => new OpenAIProvider())
providers.set('anthropic', () => new AnthropicProvider())

export class ProviderRegistry {
  static register(name: string, factory: ProviderFactory) {
    providers.set(name, factory)
  }

  static get(name: string): ProviderAdapter {
    const factory = providers.get(name)
    if (!factory) {
      throw new Error(`Unknown provider: ${name}. Available: ${[...providers.keys()].join(', ')}`)
    }
    return factory()
  }

  static list(): string[] {
    return [...providers.keys()]
  }
}

export function getProvider(name: string): ProviderAdapter {
  return ProviderRegistry.get(name)
}
