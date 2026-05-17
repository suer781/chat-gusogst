import type { ProviderAdapter } from '../../shared/types'
export type { ProviderAdapter, ModelConfig, Message, ToolDefinition } from '../../shared/types'
import { OpenAIProvider } from './openai'
import { AnthropicProvider } from './anthropic'

const providers: Record<string, ProviderAdapter> = {}

function register(p: ProviderAdapter) { providers[p.name] = p }

register(new OpenAIProvider())
register(new AnthropicProvider())

export function getProvider(name: string): ProviderAdapter {
  const p = providers[name]
  if (!p) throw new Error(`Unknown provider: ${name}. Available: ${Object.keys(providers).join(', ')}`)
  return p
}

export function listProviders(): string[] { return Object.keys(providers) }

export function registerProvider(p: ProviderAdapter) { register(p) }
