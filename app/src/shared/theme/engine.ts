/**
 * ThemeEngine — PersonaTheme → CSS 变量注入
 */
import type { PersonaTheme, BubbleConfig, ColorScheme, AnimationConfig, FontConfig, SpecialEffects } from './schema'

function injectColors(c: ColorScheme): Record<string, string> {
  return {
    '--color-primary': c.primary, '--color-primary-light': c.primaryLight, '--color-primary-dark': c.primaryDark,
    '--color-accent': c.accent, '--color-background': c.background, '--color-surface': c.surface,
    '--color-text-primary': c.textPrimary, '--color-text-secondary': c.textSecondary, '--color-border': c.border,
  }
}

function injectBubble(prefix: string, b: BubbleConfig): Record<string, string> {
  const vars: Record<string, string> = {}
  const shapeRadius: Record<string, string> = { round: '20px', soft: '12px', sharp: '4px', pill: '9999px', playful: '18px 20px 18px 4px' }
  vars['--' + prefix + '-radius'] = b.radius ? b.radius + 'px' : (shapeRadius[b.shape] || '12px')
  const styles: Record<string, () => Record<string, string>> = {
    filled: () => ({ ['--' + prefix + '-bg']: 'var(--color-primary)', ['--' + prefix + '-border']: 'none', ['--' + prefix + '-shadow']: 'none' }),
    glass: () => ({ ['--' + prefix + '-bg']: 'rgba(255,255,255,0.15)', ['--' + prefix + '-border']: '1px solid rgba(255,255,255,0.2)', ['--' + prefix + '-shadow']: '0 4px 12px rgba(0,0,0,0.08)' }),
    outline: () => ({ ['--' + prefix + '-bg']: 'transparent', ['--' + prefix + '-border']: '1.5px solid var(--color-primary)', ['--' + prefix + '-shadow']: 'none' }),
    shadow: () => ({ ['--' + prefix + '-bg']: 'var(--color-surface)', ['--' + prefix + '-border']: 'none', ['--' + prefix + '-shadow']: '0 2px 8px rgba(0,0,0,0.12)' }),
    gradient: () => ({ ['--' + prefix + '-bg']: 'linear-gradient(135deg, var(--color-primary), var(--color-accent))', ['--' + prefix + '-border']: 'none', ['--' + prefix + '-shadow']: '0 2px 12px rgba(0,0,0,0.1)' }),
  }
  Object.assign(vars, (styles[b.style] || styles.filled)())
  vars['--' + prefix + '-max-width'] = ((b.maxWidth || 0.75) * 100) + '%'
  return vars
}

function injectAnimation(a: AnimationConfig): Record<string, string> {
  const dur: Record<string, string> = { none: '0ms', subtle: '200ms', soft: '300ms', bouncy: '400ms', dramatic: '600ms', romantic: '800ms' }
  const ease: Record<string, string> = { none: 'linear', subtle: 'ease', soft: 'ease-out', bouncy: 'cubic-bezier(0.68,-0.55,0.27,1.55)', dramatic: 'cubic-bezier(0.175,0.885,0.32,1.275)', romantic: 'ease-in-out' }
  return { '--animation-duration': dur[a.preset] || '300ms', '--animation-easing': ease[a.preset] || 'ease-out' }
}

function injectFont(f: FontConfig): Record<string, string> {
  const families: Record<string, string> = {
    system: 'system-ui, sans-serif', rounded: '"Nunito", system-ui, sans-serif', sans: '"Inter", "Noto Sans SC", system-ui',
    serif: '"Noto Serif SC", Georgia, serif', mono: '"JetBrains Mono", monospace', handwriting: '"Ma Shan Zheng", cursive',
  }
  return { '--font-family': families[f.family] || families.system, '--font-size-base': (f.size || 15) + 'px', '--font-line-height': String(f.lineHeight || 1.6) }
}

function injectEffects(e: SpecialEffects): Record<string, string> {
  return { '--particles': e.particles || 'none', '--bg-pattern': e.backgroundPattern || 'none', '--scrollbar': e.scrollbar || 'thin' }
}

export function themeToCSSVariables(theme: PersonaTheme): Record<string, string> {
  return {
    ...injectColors(theme.colors), ...injectBubble('bubble-user', theme.chat.userBubble),
    ...injectBubble('bubble-ai', theme.chat.aiBubble), ...injectAnimation(theme.animation),
    ...injectFont(theme.font), ...injectEffects(theme.effects),
    '--chat-input-radius': theme.chat.inputBox.radius + 'px',
    '--chat-input-bg': theme.chat.inputBox.background, '--chat-input-border': theme.chat.inputBox.border,
    '--chat-message-gap': (theme.chat.messageGap || 10) + 'px',
  }
}

export function applyTheme(theme: PersonaTheme): void {
  const vars = themeToCSSVariables(theme)
  const root = document.documentElement
  for (const [k, v] of Object.entries(vars)) root.style.setProperty(k, v)
  root.setAttribute('data-theme-preset', theme.tags?.[0] || 'custom')
}

export async function deriveThemeFromIdentity(
  identity: string, llmCall: (prompt: string) => Promise<string>, manualOverride?: Partial<PersonaTheme>,
): Promise<PersonaTheme> {
  const prompt = '根据人物设定生成UI主题JSON：' + identity.slice(0, 500)
  try {
    const resp = await llmCall(prompt)
    const json = resp.match(/\{[\s\S]*\}/)
    if (!json) throw new Error('no JSON')
    const ai = JSON.parse(json[0]) as PersonaTheme
    return manualOverride ? deepMerge(ai, manualOverride) as PersonaTheme : ai
  } catch {
    const { THEME_PRESETS } = await import('./schema')
    return THEME_PRESETS.gentle as PersonaTheme
  }
}

function deepMerge(t: any, s: any): any {
  const r = { ...t }
  for (const k of Object.keys(s)) {
    r[k] = s[k] && typeof s[k] === 'object' && !Array.isArray(s[k]) && t[k] && typeof t[k] === 'object'
      ? deepMerge(t[k], s[k]) : s[k]
  }
  return r
}
