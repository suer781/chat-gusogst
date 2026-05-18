/**
 * 设计令牌常量
 * 所有值引用 CSS 变量，统一在 tailwind.css :root 中定义
 * 组件内使用：import { c, t, s } from '../theme'
 */

/** 颜色 — c.xxx */
export const c = {
  // 背景层级
  bg: {
    primary:   'var(--bg-primary)',
    secondary: 'var(--bg-secondary)',
    tertiary:  'var(--bg-tertiary)',
    elevated:  'var(--bg-elevated)',
    overlay:   'var(--bg-overlay)',
  },
  // 文字层级
  text: {
    primary:   'var(--text-primary)',
    secondary: 'var(--text-secondary)',
    tertiary:  'var(--text-tertiary)',
    disabled:  'var(--text-disabled)',
    inverse:   'var(--text-inverse)',
  },
  // 主题色
  accent:       'var(--accent)',
  accentHover:  'var(--accent-hover)',
  accentSoft:   'var(--accent-soft)',
  accentGlow:   'var(--accent-glow)',
  // 功能色
  purple:      'var(--purple)',
  purpleSoft:  'var(--purple-soft)',
  yellow:      'var(--yellow)',
  yellowSoft:  'var(--yellow-soft)',
  success:     'var(--success)',
  successSoft: 'var(--success-soft)',
  danger:      'var(--danger)',
  dangerSoft:  'var(--danger-soft)',
  warning:     'var(--warning)',
  warningSoft: 'var(--warning-soft)',
  teal:        'var(--teal)',
  tealSoft:    'var(--teal-soft)',
  blue:        'var(--blue)',
  blueSoft:    'var(--blue-soft)',
  // 边框
  border:      'var(--border)',
  borderFocus: 'var(--border-focus)',
  borderSoft:  'var(--border-soft)',
  divider:     'var(--divider)',
  // 灰度
  gray50:  'var(--gray-50)',
  gray100: 'var(--gray-100)',
  gray200: 'var(--gray-200)',
  gray300: 'var(--gray-300)',
  gray400: 'var(--gray-400)',
  gray500: 'var(--gray-500)',
  gray600: 'var(--gray-600)',
  gray700: 'var(--gray-700)',
  gray800: 'var(--gray-800)',
  gray900: 'var(--gray-900)',
} as const;

/** 字号 — t.xxx */
export const t = {
  '2xs': 'var(--text-2xs)',
  xs:  'var(--text-xs)',
  sm:  'var(--text-sm)',
  base: 'var(--text-base)',
  md:  'var(--text-md)',
  lg:  'var(--text-lg)',
  xl:  'var(--text-xl)',
  '2xl': 'var(--text-2xl)',
  '3xl': 'var(--text-3xl)',
  '4xl': 'var(--text-4xl)',
  '5xl': 'var(--text-5xl)',
  '6xl': 'var(--text-6xl)',
} as const;

/** 间距 — s.xxx */
export const s = {
  0:  'var(--space-0)',
  1:  'var(--space-1)',
  2:  'var(--space-2)',
  3:  'var(--space-3)',
  4:  'var(--space-4)',
  5:  'var(--space-5)',
  6:  'var(--space-6)',
  8:  'var(--space-8)',
  10: 'var(--space-10)',
  12: 'var(--space-12)',
  16: 'var(--space-16)',
} as const;

/** 圆角 — r.xxx */
export const r = {
  xs:   'var(--radius-xs)',
  sm:   'var(--radius-sm)',
  md:   'var(--radius-md)',
  lg:   'var(--radius-lg)',
  xl:   'var(--radius-xl)',
  full: 'var(--radius-full)',
} as const;

/** 阴影 — sh.xxx */
export const sh = {
  sm:   'var(--shadow-sm)',
  md:   'var(--shadow-md)',
  lg:   'var(--shadow-lg)',
  glow: 'var(--shadow-glow)',
} as const;

/** 字重 — w.xxx */
export const w = {
  normal:   'var(--font-normal)',
  medium:   'var(--font-medium)',
  semibold: 'var(--font-semibold)',
  bold:     'var(--font-bold)',
} as const;

/** 动效 — anim.xxx */
export const anim = {
  easeDefault: 'var(--ease-default)',
  easeIn:      'var(--ease-in)',
  easeOut:     'var(--ease-out)',
  easeBounce:  'var(--ease-bounce)',
  fast:        'var(--duration-fast)',
  normal:      'var(--duration-normal)',
  slow:        'var(--duration-slow)',
} as const;

/** 平台品牌色 — 保持硬编码（品牌色不随主题变） */
export const platform = {
  telegram:  '#0088cc',
  discord:   '#5865F2',
  qqbot:     '#12B7F5',
  wechat:    '#07C160',
  slack:     '#4A154B',
  whatsapp:  '#25D366',
  twitter:   '#1DA1F2',
  email:     '#EA4335',
  line:      '#00B900',
  irc:       '#CCCCCC',
} as const;
