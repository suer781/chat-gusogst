// Module-level type declarations
export interface ModuleConfig {
  name: string
  enabled: boolean
  config?: Record<string, unknown>
}
