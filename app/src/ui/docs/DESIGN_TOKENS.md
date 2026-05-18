# 设计令牌与规范化指南

> 最后更新: 2026-05-18

## 核心原则

**所有视觉样式必须通过 CSS 变量或 theme.ts 常量引用，禁止硬编码颜色/字号/间距。**

---

## 一、设计令牌体系

### 1.1 颜色 (`c.xxx`)

| 令牌 | CSS 变量 | 默认值 | 用途 |
|------|----------|--------|------|
| `c.bg.primary` | `--bg-primary` | `#0d0d2b` | 最底层背景 |
| `c.bg.secondary` | `--bg-secondary` | `#151538` | 卡片、面板 |
| `c.bg.tertiary` | `--bg-tertiary` | `#1a1a3a` | 输入框、列表项 |
| `c.bg.elevated` | `--bg-elevated` | `#22224a` | 悬浮、弹窗 |
| `c.bg.overlay` | `--bg-overlay` | `rgba(0,0,0,0.6)` | 遮罩层 |
| `c.text.primary` | `--text-primary` | `#ffffff` | 主文字 |
| `c.text.secondary` | `--text-secondary` | `#a0a0b8` | 次文字 |
| `c.text.tertiary` | `--text-tertiary` | `#6b6b80` | 辅助文字 |
| `c.text.disabled` | `--text-disabled` | `#4a4a5e` | 禁用文字 |
| `c.accent` | `--accent` | `#e94560` | 主题红 |
| `c.accentHover` | `--accent-hover` | `#c73e54` | 主题红悬停 |
| `c.accentSoft` | `--accent-soft` | `rgba(233,69,96,0.12)` | 主题红淡底 |
| `c.accentGlow` | `--accent-glow` | `rgba(233,69,96,0.25)` | 主题红发光 |
| `c.purple` | `--purple` | `#6C5CE7` | 紫色强调 |
| `c.yellow` | `--yellow` | `#FDCB6E` | 黄色星星 |
| `c.success` | `--success` | `#4CAF50` | 成功 |
| `c.danger` | `--danger` | `#FF5252` | 危险 |
| `c.warning` | `--warning` | `#FF9800` | 警告 |
| `c.teal` | `--teal` | `#00b894` | 青色 |
| `c.blue` | `--blue` | `#3498db` | 蓝色 |
| `c.gray50~900` | `--gray-50~900` | 灰度阶梯 | 10级灰度 |
| `c.border` | `--border` | `#2a2a4a` | 边框 |
| `c.borderFocus` | `--border-focus` | `var(--accent)` | 聚焦边框 |
| `c.divider` | `--divider` | `rgba(255,255,255,0.06)` | 分割线 |

### 1.2 字号 (`t.xxx`)

| 令牌 | 值 | 用途 |
|------|----|------|
| `t['2xs']` | 9px | 极小角标、隐私标记 |
| `t.xs` | 10px | badge、小标签 |
| `t.sm` | 12px | 辅助文字、时间戳、placeholder |
| `t.base` | 14px | **正文默认** |
| `t.md` | 15px | 密集列表正文 |
| `t.lg` | 16px | 列表标题、设置项名称 |
| `t.xl` | 18px | 小节标题、卡片标题 |
| `t['2xl']` | 20px | 区块标题 |
| `t['3xl']` | 24px | 页面标题 |
| `t['4xl']` | 32px | 头像旁昵称、大数字 |
| `t['5xl']` | 40px | 统计大屏数字 |
| `t['6xl']` | 48px | 登录页品牌标题 |

### 1.3 间距 (`s[xxx]`)

| 令牌 | 值 | 常见用途 |
|------|----|----------|
| `s[0]` | 0 | — |
| `s[1]` | 4px | 紧凑间距 |
| `s[2]` | 8px | 图标与文字 |
| `s[3]` | 12px | 列表项内间距 |
| `s[4]` | 16px | **默认内边距** |
| `s[5]` | 20px | 区块间距 |
| `s[6]` | 24px | 卡片内边距 |
| `s[8]` | 32px | 大区块间距 |
| `s[10]` | 40px | 页面边距 |
| `s[12]` | 48px | 大留白 |
| `s[16]` | 64px | 超大留白 |

### 1.4 圆角 (`r.xxx`)

| 令牌 | 值 | 用途 |
|------|----|------|
| `r.xs` | 4px | 小按钮、标签 |
| `r.sm` | 6px | 输入框、小卡片 |
| `r.md` | 10px | **默认圆角** |
| `r.lg` | 16px | 大卡片、弹窗 |
| `r.xl` | 24px | 特殊卡片 |
| `r.full` | 9999px | 圆形、胶囊 |

### 1.5 阴影 (`sh.xxx`)

| 令牌 | 值 |
|------|----|
| `sh.sm` | `0 1px 3px rgba(0,0,0,0.3)` |
| `sh.md` | `0 4px 12px rgba(0,0,0,0.4)` |
| `sh.lg` | `0 8px 24px rgba(0,0,0,0.5)` |
| `sh.glow` | `0 0 20px var(--accent-glow)` |

### 1.6 字重 (`w.xxx`)

| 令牌 | 值 |
|------|----|
| `w.normal` | 400 |
| `w.medium` | 500 |
| `w.semibold` | 600 |
| `w.bold` | 700 |

### 1.7 动效 (`anim.xxx`)

| 令牌 | 值 |
|------|----|
| `anim.easeDefault` | `cubic-bezier(0.4,0,0.2,1)` |
| `anim.easeIn` | `cubic-bezier(0.4,0,1,1)` |
| `anim.easeOut` | `cubic-bezier(0,0,0.2,1)` |
| `anim.easeBounce` | `cubic-bezier(0.34,1.56,0.64,1)` |
| `anim.fast` | 150ms |
| `anim.normal` | 250ms |
| `anim.slow` | 400ms |

### 1.8 平台品牌色 (`platform.xxx`)

> ⚠️ 品牌色保持硬编码，不随主题变化

| 平台 | 颜色 |
|------|------|
| Telegram | `#0088cc` |
| Discord | `#5865F2` |
| QQ Bot | `#12B7F5` |
| 微信 | `#07C160` |
| Slack | `#4A154B` |
| WhatsApp | `#25D366` |
| Twitter/X | `#1DA1F2` |
| Email | `#EA4335` |
| LINE | `#00B900` |
| IRC | `#CCCCCC` |

---

## 二、使用方式

### 2.1 CSS 变量（推荐）

```tsx
// ✅ 正确
<div style={{ background: 'var(--bg-secondary)', color: 'var(--text-primary)' }} />

// ❌ 错误
<div style={{ background: '#151538', color: '#fff' }} />
```

### 2.2 theme.ts 常量

```tsx
import { c, t, s, r, sh, w, anim } from '../theme'

// ✅ 正确
<div style={{
  background: c.bg.secondary,
  color: c.text.primary,
  fontSize: t.base,
  padding: s[4],
  borderRadius: r.md,
  boxShadow: sh.md,
  fontWeight: w.medium,
  transition: `all ${anim.normal}ms ${anim.easeDefault}`
}} />
```

---

## 三、接口规范

### 3.1 组件 Props 命名

所有组件 Props 接口必须命名为 `组件名Props`：

```tsx
// ✅ 正确
interface EyeCareColorMapperProps { ... }
interface PersonaProfileViewProps { ... }

// ❌ 错误
interface Props { ... }
```

### 3.2 stores.ts 状态接口

| 接口 | 用途 |
|------|------|
| `ChatState` | 聊天相关状态 |
| `SettingsState` | 设置相关状态（扩展 AppSettings） |
| `EyeCareMapping` | 护眼模式颜色映射项 |

### 3.3 护眼模式数据结构

```typescript
interface EyeCareMapping {
  id: string           // 唯一标识（genMappingId() 生成）
  sourceColor: string  // 源色（被替换的颜色）
  targetColor: string  // 目标色（替换成的颜色）
  label?: string       // 可选标签
}

// 默认映射
DEFAULT_EYE_CARE_MAPPINGS: EyeCareMapping[]
```

---

## 四、已规范化文件清单

| 文件 | 规范化内容 | 日期 |
|------|-----------|------|
| `stores.ts` | EyeCareMapping 类型导出、eyeCareIntensity 状态 | 2026-05-18 |
| `App.tsx` | 硬编码颜色 → CSS 变量 | 2026-05-18 |
| `init.ts` | 状态栏颜色 → CSS 变量 | 2026-05-18 |
| `BasicSettings.tsx` | 15 处硬编码颜色 → CSS 变量 | 2026-05-18 |
| `AboutSettings.tsx` | 8 处硬编码颜色 → CSS 变量 | 2026-05-18 |
| `ChatView.tsx` | fill 属性 → CSS 变量 | 2026-05-18 |
| `EyeCareColorMapper.tsx` | 新建组件、Props 命名规范化 | 2026-05-18 |
| `PersonaProfileView.tsx` | Props 命名规范化 | 2026-05-18 |
| `PersonaSettingsModal.tsx` | Props 命名规范化 | 2026-05-18 |
| `ModelSettings.tsx` | 补充 `t()` 导入 | 2026-05-18 |
| `SearchSettings.tsx` | 补充 `t()` 导入 | 2026-05-18 |

---

## 五、例外情况

以下硬编码是**有意保留**的：

1. **平台品牌色** (`PlatformSettings.tsx`, `theme.ts platform`)
   - 品牌色不随主题变化，保持硬编码

2. **DEFAULT_EYE_CARE_MAPPINGS 默认值** (`stores.ts`)
   - 数据默认值，非样式值

3. **rgba 半透明层** (阴影、边框、背景遮罩)
   - 纯透明度层，不需要主题变量

4. **`#000000` 纯黑** (`BasicSettings.tsx` 护眼模式)
   - 功能色，表示"关闭像素"
