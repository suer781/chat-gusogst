/**
 * Persona Theme Schema
 * 双模式：AI 推导（默认）+ 手动覆盖（优先级更高）
 */

export interface ColorScheme {
  primary: string; primaryLight: string; primaryDark: string
  accent: string; background: string; surface: string
  textPrimary: string; textSecondary: string; border: string
}

export type BubbleShape = 'round' | 'soft' | 'sharp' | 'pill' | 'playful'
export type BubbleStyle = 'filled' | 'glass' | 'outline' | 'shadow' | 'gradient'

export interface BubbleConfig {
  shape: BubbleShape; style: BubbleStyle
  radius?: number; maxWidth?: number; tail?: boolean; shadow?: string
}

export type AnimationPreset = 'none' | 'subtle' | 'soft' | 'bouncy' | 'dramatic' | 'romantic'

export interface AnimationConfig {
  preset: AnimationPreset
  messageEnter?: 'fade' | 'slide' | 'scale' | 'bounce' | 'typewriter'
  typingIndicator?: 'dots' | 'pulse' | 'wave' | 'heartbeat'
  pageTransition?: 'fade' | 'slide' | 'zoom' | 'none'
  emojiAnimation?: 'none' | 'pop' | 'bounce' | 'float'
  speed?: number
}

export type FontFamily = 'system' | 'rounded' | 'sans' | 'serif' | 'mono' | 'handwriting'

export interface FontConfig {
  family: FontFamily; size?: number; lineHeight?: number
  weight?: number; letterSpacing?: string
}

export interface ChatTheme {
  userBubble: BubbleConfig; aiBubble: BubbleConfig
  inputBox: { radius: number; background: string; border: string; placeholder?: string }
  showAiAvatar: boolean; showUserAvatar: boolean; messageGap?: number
}

export interface SpecialEffects {
  particles?: 'none' | 'hearts' | 'stars' | 'sparkles' | 'petals' | 'snow'
  backgroundPattern?: 'none' | 'dots' | 'waves' | 'gradient' | 'stars' | 'clouds'
  cursor?: 'default' | 'pointer' | 'custom'
  selectionColor?: string; scrollbar?: 'hidden' | 'thin' | 'rounded'
}

export interface PersonaTheme {
  colors: ColorScheme; chat: ChatTheme; animation: AnimationConfig
  font: FontConfig; effects: SpecialEffects; tags?: string[]
}

export const THEME_PRESETS: Record<string, Partial<PersonaTheme>> = {
  sweet: {
    tags: ['sweet', 'cute', 'pastel', 'warm'],
    colors: { primary: '#ff6b9d', primaryLight: '#ffb3cc', primaryDark: '#e84580', accent: '#ffd1dc', background: '#fff5f7', surface: '#ffffff', textPrimary: '#4a3040', textSecondary: '#8a6070', border: '#ffd1dc' },
    chat: { userBubble: { shape: 'round', style: 'filled', maxWidth: 0.7, tail: true }, aiBubble: { shape: 'round', style: 'glass', maxWidth: 0.8, tail: true }, inputBox: { radius: 24, background: '#fff0f3', border: '1px solid #ffd1dc' }, showAiAvatar: true, showUserAvatar: false, messageGap: 12 },
    animation: { preset: 'bouncy', messageEnter: 'bounce', typingIndicator: 'heartbeat' },
    font: { family: 'rounded', lineHeight: 1.7 },
    effects: { particles: 'hearts', backgroundPattern: 'dots', scrollbar: 'rounded' },
  },
  cool: {
    tags: ['cool', 'tech', 'dark', 'sharp'],
    colors: { primary: '#4a90d9', primaryLight: '#6ab0f9', primaryDark: '#2a70b9', accent: '#00d4aa', background: '#0d1117', surface: '#161b22', textPrimary: '#e6edf3', textSecondary: '#8b949e', border: '#30363d' },
    chat: { userBubble: { shape: 'sharp', style: 'filled', maxWidth: 0.7, tail: false }, aiBubble: { shape: 'soft', style: 'outline', maxWidth: 0.85, tail: false }, inputBox: { radius: 8, background: '#161b22', border: '1px solid #30363d' }, showAiAvatar: true, showUserAvatar: false, messageGap: 8 },
    animation: { preset: 'subtle', messageEnter: 'fade', typingIndicator: 'pulse' },
    font: { family: 'mono', size: 14, lineHeight: 1.5 },
    effects: { scrollbar: 'thin' },
  },
  professional: {
    tags: ['professional', 'formal', 'clean'],
    colors: { primary: '#1a73e8', primaryLight: '#4a9af5', primaryDark: '#0d5bbd', accent: '#fbbc04', background: '#ffffff', surface: '#f8f9fa', textPrimary: '#202124', textSecondary: '#5f6368', border: '#dadce0' },
    chat: { userBubble: { shape: 'soft', style: 'filled', maxWidth: 0.7, tail: false }, aiBubble: { shape: 'soft', style: 'filled', maxWidth: 0.85, tail: false }, inputBox: { radius: 12, background: '#f8f9fa', border: '1px solid #dadce0' }, showAiAvatar: true, showUserAvatar: false, messageGap: 10 },
    animation: { preset: 'subtle', messageEnter: 'fade', typingIndicator: 'dots' },
    font: { family: 'sans', size: 15 },
    effects: { scrollbar: 'thin' },
  },
  playful: {
    tags: ['playful', 'fun', 'energetic'],
    colors: { primary: '#ff6b35', primaryLight: '#ff9a6c', primaryDark: '#e04d1a', accent: '#ffd23f', background: '#fffef5', surface: '#ffffff', textPrimary: '#2d2013', textSecondary: '#7a6b5d', border: '#ffe0b2' },
    chat: { userBubble: { shape: 'playful', style: 'gradient', maxWidth: 0.7, tail: true }, aiBubble: { shape: 'round', style: 'shadow', maxWidth: 0.8, tail: true }, inputBox: { radius: 20, background: '#fff8e1', border: '2px solid #ffd23f' }, showAiAvatar: true, showUserAvatar: true, messageGap: 16 },
    animation: { preset: 'bouncy', messageEnter: 'scale', typingIndicator: 'wave', emojiAnimation: 'bounce' },
    font: { family: 'rounded', lineHeight: 1.8 },
    effects: { particles: 'sparkles', backgroundPattern: 'dots', scrollbar: 'rounded' },
  },
  gentle: {
    tags: ['gentle', 'warm', 'soft', 'calm'],
    colors: { primary: '#7c9a6e', primaryLight: '#a3c494', primaryDark: '#5a7a4e', accent: '#d4a574', background: '#faf8f5', surface: '#ffffff', textPrimary: '#3d3830', textSecondary: '#8a8070', border: '#e8e0d4' },
    chat: { userBubble: { shape: 'soft', style: 'filled', maxWidth: 0.7, tail: true }, aiBubble: { shape: 'round', style: 'glass', maxWidth: 0.8, tail: true }, inputBox: { radius: 20, background: '#f5f0ea', border: '1px solid #e8e0d4' }, showAiAvatar: true, showUserAvatar: false, messageGap: 14 },
    animation: { preset: 'soft', messageEnter: 'fade', typingIndicator: 'dots', emojiAnimation: 'float' },
    font: { family: 'serif', lineHeight: 1.8 },
    effects: { backgroundPattern: 'waves', scrollbar: 'rounded' },
  },
  mysterious: {
    tags: ['mysterious', 'dark', 'elegant'],
    colors: { primary: '#9b59b6', primaryLight: '#bb77d6', primaryDark: '#7d3c98', accent: '#2c3e50', background: '#1a1a2e', surface: '#16213e', textPrimary: '#eaeaea', textSecondary: '#a0a0b0', border: '#2a2a4a' },
    chat: { userBubble: { shape: 'soft', style: 'outline', maxWidth: 0.7, tail: false }, aiBubble: { shape: 'soft', style: 'glass', maxWidth: 0.85, tail: false }, inputBox: { radius: 12, background: '#16213e', border: '1px solid #2a2a4a' }, showAiAvatar: true, showUserAvatar: false, messageGap: 10 },
    animation: { preset: 'soft', messageEnter: 'fade', typingIndicator: 'pulse' },
    font: { family: 'serif', size: 15 },
    effects: { particles: 'sparkles', backgroundPattern: 'stars', scrollbar: 'hidden' },
  },
}
