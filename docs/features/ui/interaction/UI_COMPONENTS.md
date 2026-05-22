# UI 组件接口定义

> 状态：待实施 | 创建：2026-05-18
> 设计令牌见 → [UI_DESIGN.md](./UI_DESIGN.md)
> 组件目录：`app/src/ui/components/common/`

---

## 使用约定

1. **所有组件只消费 CSS 变量**，不硬编码颜色/字号/间距
2. Props 用 TypeScript interface 导出，方便外部覆盖
3. 样式统一用 inline style，不混用 className
4. 组件内不引入外部 CSS 文件，所有样式自包含
5. 组件支持 `style?: React.CSSProperties` 透传覆盖

---

## Button 按钮

**文件**：`components/common/Button.tsx`

```tsx
export interface ButtonProps {
  /** 外观变体 */
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'success';
  /** 尺寸 */
  size?: 'sm' | 'md' | 'lg';
  /** 左侧图标 URL 或 emoji */
  icon?: string;
  /** 图标位置 */
  iconPosition?: 'left' | 'right';
  /** 加载态（禁用 + 显示 spinner） */
  loading?: boolean;
  /** 禁用 */
  disabled?: boolean;
  /** 铺满父容器宽度 */
  fullWidth?: boolean;
  /** 点击回调 */
  onClick?: () => void;
  /** 透传样式覆盖 */
  style?: React.CSSProperties;
  children: React.ReactNode;
}
```

**样式映射**：

| variant | background | border | color | hover |
|---------|-----------|--------|-------|-------|
| primary | `var(--accent)` | none | `#fff` | `var(--accent-hover)` |
| secondary | `var(--bg-tertiary)` | `1px solid var(--border)` | `var(--text-primary)` | `var(--bg-elevated)` |
| ghost | transparent | none | `var(--text-secondary)` | `var(--accent-soft)` |
| danger | `var(--danger)` | none | `#fff` | `#d32f2f` |
| success | `var(--success)` | none | `#fff` | `#388e3c` |

**尺寸映射**：

| size | height | fontSize | padding | iconSize | radius |
|------|--------|---------|---------|----------|--------|
| sm | 30px | `var(--text-sm)` | `0 12px` | 14px | `var(--radius-sm)` |
| md | 38px | `var(--text-base)` | `0 16px` | 18px | `var(--radius-md)` |
| lg | 46px | `var(--text-lg)` | `0 24px` | 22px | `var(--radius-md)` |

---

## Input 输入框

**文件**：`components/common/Input.tsx`

```tsx
export interface InputProps {
  /** 标签文字 */
  label?: string;
  /** 占位符 */
  placeholder?: string;
  /** 值 */
  value: string;
  /** 值变更回调 */
  onChange: (value: string) => void;
  /** 输入类型 */
  type?: 'text' | 'password' | 'number' | 'email' | 'url';
  /** 多行模式（为 true 时渲染 textarea） */
  multiline?: boolean;
  /** 多行行数 */
  rows?: number;
  /** 错误提示（红色显示在下方） */
  error?: string;
  /** 辅助说明（灰色显示在下方） */
  hint?: string;
  /** 前缀文字/图标 */
  prefix?: string;
  /** 后缀文字/图标 */
  suffix?: string;
  /** 禁用 */
  disabled?: boolean;
  /** 只读 */
  readOnly?: boolean;
  /** 自动聚焦 */
  autoFocus?: boolean;
  /** 最大长度（显示计数器） */
  maxLength?: number;
  /** 键盘确认回调 */
  onEnter?: () => void;
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**统一样式**：
- 容器：`display: flex; flex-direction: column; gap: var(--space-1)`
- 标签：`fontSize: var(--text-sm); color: var(--text-secondary); fontWeight: var(--font-medium)`
- 输入框本体：`background: var(--bg-tertiary); border: 1px solid var(--border); borderRadius: var(--radius-md); padding: var(--space-3) var(--space-4); fontSize: var(--text-base); color: var(--text-primary)`
- 聚焦态：`borderColor: var(--accent); boxShadow: 0 0 0 2px var(--accent-soft)`
- 错误态：`borderColor: var(--danger)` + 下方提示文字 `color: var(--danger)`
- 禁用态：`opacity: 0.5; cursor: not-allowed`

---

## Toggle 开关

**文件**：`components/common/Toggle.tsx`

```tsx
export interface ToggleProps {
  /** 当前开关状态 */
  value: boolean;
  /** 状态变更回调 */
  onChange: (value: boolean) => void;
  /** 标签（显示在左侧） */
  label?: string;
  /** 辅助说明（标签下方） */
  description?: string;
  /** 禁用 */
  disabled?: boolean;
  /** 尺寸 */
  size?: 'sm' | 'md';
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**样式映射**：
- 轨道：宽 44px / 高 24px（sm: 36×20），圆角 `var(--radius-full)`
- 关闭态轨道：`background: var(--gray-600)`
- 开启态轨道：`background: var(--accent)`
- 圆形滑块：宽高 = 轨道高度 - 4px，`background: #fff`，`boxShadow: var(--shadow-sm)`
- 过渡：`transition: all var(--duration-fast) var(--ease-default)`

---

## Select 下拉选择

**文件**：`components/common/Select.tsx`

```tsx
export interface SelectOption {
  label: string;
  value: string;
  /** 可选描述（灰色小字） */
  description?: string;
  /** 可选图标 */
  icon?: string;
  /** 是否禁用此选项 */
  disabled?: boolean;
}

export interface SelectProps {
  /** 选项列表 */
  options: SelectOption[];
  /** 当前选中值 */
  value: string;
  /** 变更回调 */
  onChange: (value: string) => void;
  /** 标签 */
  label?: string;
  /** 未选中时的占位文字 */
  placeholder?: string;
  /** 禁用 */
  disabled?: boolean;
  /** 是否支持搜索过滤 */
  searchable?: boolean;
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**行为**：
- 点击触发器展开下拉面板，再点击或点外部关闭
- 面板：`background: var(--bg-elevated); border: 1px solid var(--border); borderRadius: var(--radius-md); boxShadow: var(--shadow-lg)`
- 选项 hover：`background: var(--accent-soft)`
- 选中项：左侧显示 ✓ 或 accent 色高亮
- 支持键盘上下选择 + Enter 确认

---

## Badge 标签

**文件**：`components/common/Badge.tsx`

```tsx
export interface BadgeProps {
  /** 颜色变体 */
  variant?: 'default' | 'accent' | 'success' | 'warning' | 'danger' | 'purple' | 'blue';
  /** 尺寸 */
  size?: 'sm' | 'md';
  /** 是否带圆点（纯指示） */
  dot?: boolean;
  /** 透传样式 */
  style?: React.CSSProperties;
  children?: React.ReactNode;
}
```

**样式映射**：

| variant | background | color |
|---------|-----------|-------|
| default | `var(--gray-700)` | `var(--text-secondary)` |
| accent | `var(--accent-soft)` | `var(--accent)` |
| success | `var(--success-soft)` | `var(--success)` |
| warning | `var(--warning-soft)` | `var(--warning)` |
| danger | `var(--danger-soft)` | `var(--danger)` |
| purple | `var(--purple-soft)` | `var(--purple)` |
| blue | `var(--blue-soft)` | `var(--blue)` |

尺寸：sm = `fontSize: var(--text-2xs); padding: 1px 6px`，md = `fontSize: var(--text-xs); padding: 2px 10px`
圆角：`var(--radius-full)`

---

## Tooltip 提示气泡

**文件**：`components/common/Tooltip.tsx`

```tsx
export interface TooltipProps {
  /** 触发元素 */
  children: React.ReactElement;
  /** 提示内容 */
  content: string | React.ReactNode;
  /** 弹出方向 */
  placement?: 'top' | 'bottom' | 'left' | 'right';
  /** 延迟显示（ms） */
  delay?: number;
  /** 最大宽度 */
  maxWidth?: number;
}
```

**样式**：
- 容器：`background: var(--gray-800); color: var(--text-primary); fontSize: var(--text-sm); padding: var(--space-2) var(--space-3); borderRadius: var(--radius-sm); boxShadow: var(--shadow-md)`
- 箭头：8px 三角形，颜色继承背景
- 动画：淡入 `opacity 0→1`，`var(--duration-fast)`
- z-index：`var(--z-tooltip)` (9000)

---

## Card 卡片

**文件**：`components/common/Card.tsx`

```tsx
export interface CardProps {
  /** 外观变体 */
  variant?: 'default' | 'elevated' | 'outlined' | 'glass';
  /** 内边距 */
  padding?: 'none' | 'sm' | 'md' | 'lg';
  /** 是否可点击（显示 hover 效果） */
  clickable?: boolean;
  /** 点击回调 */
  onClick?: () => void;
  /** 头部区域 */
  header?: React.ReactNode;
  /** 底部区域 */
  footer?: React.ReactNode;
  /** 透传样式 */
  style?: React.CSSProperties;
  children: React.ReactNode;
}
```

**样式映射**：

| variant | background | border | boxShadow |
|---------|-----------|--------|----------|
| default | `var(--bg-secondary)` | `1px solid var(--border)` | none |
| elevated | `var(--bg-secondary)` | none | `var(--shadow-md)` |
| outlined | transparent | `1px solid var(--border)` | none |
| glass | `rgba(255,255,255,0.05)` | `1px solid rgba(255,255,255,0.08)` | `var(--shadow-sm)` |

**内边距**：

| padding | 值 |
|---------|---|
| none | 0 |
| sm | `var(--space-3)` |
| md | `var(--space-4)` |
| lg | `var(--space-6)` |

**可点击**：`cursor: pointer` + hover `borderColor: var(--accent)` / `background: var(--bg-elevated)`

---

## Section 分组区块

**文件**：`components/common/Section.tsx`

```tsx
export interface SectionProps {
  /** 区块标题 */
  title: string;
  /** 副标题/描述 */
  description?: string;
  /** 标题右侧操作区（如「添加」按钮） */
  action?: React.ReactNode;
  /** 折叠控制 */
  collapsible?: boolean;
  /** 默认是否展开（配合 collapsible） */
  defaultExpanded?: boolean;
  /** 透传样式 */
  style?: React.CSSProperties;
  children: React.ReactNode;
}
```

**样式**：
- 标题：`fontSize: var(--text-lg); fontWeight: var(--font-semibold); color: var(--text-primary)`
- 描述：`fontSize: var(--text-sm); color: var(--text-secondary); marginTop: var(--space-1)`
- 分隔线：`height: 1px; background: var(--divider); margin: var(--space-6) 0`
- 内容区：`paddingTop: var(--space-4)`

---

## Modal / Dialog 模态弹窗

**文件**：`components/common/Modal.tsx`

```tsx
export interface ModalProps {
  /** 是否可见 */
  visible: boolean;
  /** 关闭回调（点击遮罩或按 Esc） */
  onClose: () => void;
  /** 弹窗标题 */
  title?: string;
  /** 宽度（px 或 CSS 值） */
  width?: number | string;
  /** 最大高度（超出滚动） */
  maxHeight?: string;
  /** 是否显示关闭按钮（右上角 ×） */
  showClose?: boolean;
  /** 是否点击遮罩关闭 */
  maskClosable?: boolean;
  /** 是否显示遮罩 */
  showMask?: boolean;
  /** 底部操作区 */
  footer?: React.ReactNode;
  /** 透传样式 */
  style?: React.CSSProperties;
  children: React.ReactNode;
}
```

**样式**：
- 遮罩：`position: fixed; inset: 0; background: var(--bg-overlay); z-index: var(--z-modal)`
- 容器：`background: var(--bg-secondary); borderRadius: var(--radius-lg); boxShadow: var(--shadow-lg); maxWidth: 90vw; maxHeight: 85vh; overflow: auto`
- 标题：`fontSize: var(--text-xl); fontWeight: var(--font-semibold); padding: var(--space-6); borderBottom: 1px solid var(--divider)`
- 内容区：`padding: var(--space-6)`
- 底部区：`padding: var(--space-4) var(--space-6); borderTop: 1px solid var(--divider); display: flex; justify-content: flex-end; gap: var(--space-3)`
- 动画：弹入 `scale 0.95→1 + opacity 0→1`，`var(--duration-normal) var(--ease-out)`

---

## Toast 轻提示

**文件**：`components/common/Toast.tsx`

```tsx
export type ToastType = 'info' | 'success' | 'warning' | 'error';

export interface ToastProps {
  /** 提示类型（控制颜色和图标） */
  type?: ToastType;
  /** 文字内容 */
  message: string;
  /** 持续时间（ms），0 = 不自动关闭 */
  duration?: number;
  /** 关闭回调 */
  onClose?: () => void;
  /** 操作按钮（如「撤销」） */
  action?: { label: string; onClick: () => void };
}
```

**样式映射**：

| type | background | icon |
|------|-----------|------|
| info | `var(--bg-elevated)` | ℹ️ |
| success | `var(--success)` | ✅ |
| warning | `var(--warning)` | ⚠️ |
| error | `var(--danger)` | ❌ |

**容器**：`position: fixed; bottom: var(--space-8); left: 50%; transform: translateX(-50%); padding: var(--space-3) var(--space-5); borderRadius: var(--radius-md); boxShadow: var(--shadow-lg); fontSize: var(--text-sm); z-index: var(--z-toast)`

**动画**：弹入 `translateY(20px)→0 + opacity 0→1`，退出反转，`var(--duration-normal)`

**全局调用**：通过 `ToastManager.show({ type, message, duration })` 静态方法触发，内部维护 Toast 队列（最多同时 3 条）。

---

## Spinner 加载指示器

**文件**：`components/common/Spinner.tsx`

```tsx
export interface SpinnerProps {
  /** 尺寸（px） */
  size?: number;
  /** 颜色（默认 accent） */
  color?: string;
  /** 加载文案（显示在下方） */
  label?: string;
  /** 全屏居中模式 */
  fullscreen?: boolean;
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**样式**：
- 圆环：`border: 2px solid var(--border); borderTopColor: var(--accent); borderRadius: 50%; animation: spin 0.8s linear infinite`
- 尺寸默认：24px
- fullscreen 模式：`position: fixed; inset: 0; display: flex; align-items: center; justify-content: center; background: var(--bg-overlay); z-index: var(--z-loading)`
- label：`fontSize: var(--text-sm); color: var(--text-secondary); marginTop: var(--space-3)`

---

## EmptyState 空状态

**文件**：`components/common/EmptyState.tsx`

```tsx
export interface EmptyStateProps {
  /** 图标 URL 或 emoji */
  icon?: string;
  /** 标题 */
  title: string;
  /** 描述文字 */
  description?: string;
  /** 操作按钮（如「新建对话」） */
  action?: { label: string; onClick: () => void };
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**样式**：
- 容器：`display: flex; flex-direction: column; align-items: center; justify-content: center; padding: var(--space-12) var(--space-6); textAlign: center`
- 图标：`fontSize: 48px; marginBottom: var(--space-4); opacity: 0.6`
- 标题：`fontSize: var(--text-lg); fontWeight: var(--font-medium); color: var(--text-secondary)`
- 描述：`fontSize: var(--text-sm); color: var(--text-tertiary); marginTop: var(--space-2); maxWidth: 280px`
- 操作按钮：`marginTop: var(--space-6)`，使用 Button 组件 `variant="primary" size="md"`

---

## Divider 分割线

**文件**：`components/common/Divider.tsx`

```tsx
export interface DividerProps {
  /** 方向 */
  direction?: 'horizontal' | 'vertical';
  /** 是否带文字（如「或」） */
  label?: string;
  /** 间距 */
  spacing?: 'sm' | 'md' | 'lg';
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**样式**：
- 水平线：`height: 1px; background: var(--divider)`
- 垂直线：`width: 1px; height: 100%; background: var(--divider)`
- 带文字：左右各一条线，中间放 label，`fontSize: var(--text-sm); color: var(--text-tertiary); padding: 0 var(--space-3)`
- 间距：sm = `margin: var(--space-2) 0`，md = `var(--space-4)`，lg = `var(--space-6)`

---

## Icon 图标

**文件**：`components/common/Icon.tsx`

```tsx
export interface IconProps {
  /** 图标源：URL 图片、emoji 字符、或 SVG 路径 */
  src: string;
  /** 尺寸（px） */
  size?: number;
  /** 颜色（仅对 SVG/emoji 生效） */
  color?: string;
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**行为**：
- emoji / 文字：`fontSize: {size}px; lineHeight: 1`
- URL 图片：`<img width={size} height={size} />`，`object-fit: contain`
- 尺寸默认：20px
- 不做额外包装，直接渲染内联元素

---

## Avatar 头像

**文件**：`components/common/Avatar.tsx`

```tsx
export interface AvatarProps {
  /** 图片 URL */
  src?: string;
  /** 首字母（无图片时显示） */
  fallback?: string;
  /** 尺寸 */
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** 在线状态指示点 */
  online?: boolean;
  /** 点击回调 */
  onClick?: () => void;
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**尺寸映射**：

| size | 宽高 | fontSize |
|------|------|----------|
| sm | 28px | `var(--text-xs)` |
| md | 36px | `var(--text-sm)` |
| lg | 48px | `var(--text-lg)` |
| xl | 64px | `var(--text-2xl)` |

**样式**：
- 容器：`borderRadius: var(--radius-full); overflow: hidden; flex-shrink: 0`
- 图片：`width: 100%; height: 100%; object-fit: cover`
- fallback 背景：`var(--accent-soft)`，文字色：`var(--accent)`
- 在线点：右下角绝对定位，`width: 10px; height: 10px; background: var(--success); border: 2px solid var(--bg-secondary); borderRadius: 50%`

---

## Slider 滑块

**文件**：`components/common/Slider.tsx`

```tsx
export interface SliderProps {
  /** 当前值 */
  value: number;
  /** 值变更回调 */
  onChange: (value: number) => void;
  /** 最小值 */
  min?: number;
  /** 最大值 */
  max?: number;
  /** 步长 */
  step?: number;
  /** 标签 */
  label?: string;
  /** 显示当前值 */
  showValue?: boolean;
  /** 值格式化 */
  formatValue?: (value: number) => string;
  /** 禁用 */
  disabled?: boolean;
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**样式**：
- 轨道：`height: 4px; background: var(--gray-700); borderRadius: var(--radius-full)`
- 填充：`background: var(--accent); borderRadius: var(--radius-full)`
- 滑块：`width: 18px; height: 18px; background: #fff; borderRadius: 50%; boxShadow: var(--shadow-sm); cursor: pointer`
- hover：滑块放大 `transform: scale(1.2)`
- 标签：`fontSize: var(--text-sm); color: var(--text-secondary)`
- 值显示：`fontSize: var(--text-sm); color: var(--text-primary); fontWeight: var(--font-medium); minWidth: 32px; textAlign: right`

---

## Tabs 标签页

**文件**：`components/common/Tabs.tsx`

```tsx
export interface TabItem {
  /** 标签唯一 key */
  key: string;
  /** 显示文字 */
  label: string;
  /** 可选图标 */
  icon?: string;
  /** 角标数字（如未读消息） */
  badge?: number;
  /** 禁用此标签 */
  disabled?: boolean;
}

export interface TabsProps {
  /** 标签列表 */
  items: TabItem[];
  /** 当前激活 key */
  activeKey: string;
  /** 切换回调 */
  onChange: (key: string) => void;
  /** 外观风格 */
  variant?: 'underline' | 'pill' | 'card';
  /** 尺寸 */
  size?: 'sm' | 'md';
  /** 透传样式 */
  style?: React.CSSProperties;
}
```

**样式映射**：

| variant | 容器 | 激活态 | 未激活 |
|---------|------|--------|--------|
| underline | `borderBottom: 1px solid var(--border)` | `borderBottom: 2px solid var(--accent); color: var(--accent)` | `color: var(--text-secondary)` |
| pill | `background: var(--bg-tertiary); borderRadius: var(--radius-full); padding: 3px` | `background: var(--accent); borderRadius: var(--radius-full); color: #fff` | `color: var(--text-secondary)` |
| card | `gap: var(--space-1)` | `background: var(--bg-secondary); borderRadius: var(--radius-sm); border: 1px solid var(--accent)` | `color: var(--text-secondary)` |

badge 角标：`background: var(--danger); color: #fff; fontSize: var(--text-2xs); borderRadius: var(--radius-full); padding: 0 5px; marginLeft: var(--space-1)`

---

## z-index 层级常量

在 `tailwind.css` 或共享常量文件中定义，避免 z-index 混战：

```css
:root {
  --z-base:      0;
  --z-dropdown:  1000;
  --z-sticky:    2000;
  --z-drawer:    5000;
  --z-modal:     6000;
  --z-tooltip:   7000;
  --z-toast:     8000;
  --z-loading:   9000;
}
```

---

## 组件速查表

| 组件 | 文件名 | 核心 Props | 场景 |
|------|--------|-----------|------|
| Button | Button.tsx | `variant, size, icon, loading` | 所有可点击操作 |
| Input | Input.tsx | `label, type, multiline, error` | 表单、搜索、设置项 |
| Toggle | Toggle.tsx | `value, label, description` | 设置开关 |
| Select | Select.tsx | `options, value, searchable` | 模型选择、语言选择 |
| Badge | Badge.tsx | `variant, dot` | 状态标记、版本号 |
| Tooltip | Tooltip.tsx | `content, placement` | 图标说明、长文本省略提示 |
| Card | Card.tsx | `variant, padding, clickable` | 消息卡片、设置分组 |
| Section | Section.tsx | `title, description, collapsible` | 设置页分组 |
| Modal | Modal.tsx | `visible, title, footer` | 确认框、表单弹窗 |
| Toast | Toast.tsx | `type, message, duration` | 操作反馈 |
| Spinner | Spinner.tsx | `size, fullscreen, label` | 加载中 |
| EmptyState | EmptyState.tsx | `icon, title, action` | 空列表、首次使用 |
| Divider | Divider.tsx | `direction, label` | 区域分隔 |
| Icon | Icon.tsx | `src, size` | 图标显示 |
| Avatar | Avatar.tsx | `src, fallback, online` | 用户头像、角色头像 |
| Slider | Slider.tsx | `value, min, max, step` | 音量、透明度调节 |
| Tabs | Tabs.tsx | `items, activeKey, variant` | 设置页切换、会话切换 |

---

## 新增组件 Checklist

实现时逐个打勾：

- [ ] Button
- [ ] Input
- [ ] Toggle
- [ ] Select
- [ ] Badge
- [ ] Tooltip
- [ ] Card
- [ ] Section
- [ ] Modal
- [ ] Toast
- [ ] Spinner
- [ ] EmptyState
- [ ] Divider
- [ ] Icon
- [ ] Avatar
- [ ] Slider
- [ ] Tabs
