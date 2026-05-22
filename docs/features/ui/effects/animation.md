# 动画系统与玻璃材质设计文档

> 项目：chat-gusogst
> 创建：2026-05-14
> 状态：设计完成，待实现

---

## 概述

本项目在 Electron/WebView（软件级别）实现类系统级的流畅动画体验，参考 OPPO ColorOS 动效设计理念，但完全基于标准 Web API 实现，不涉及任何系统级引擎。

**核心原则：**
- 所有动画在通用设置中**可开可关**
- 关闭后零性能开销（CSS class 切换，不加载动画逻辑）
- 动画不影响功能，关闭后 UI 和交互完全正常

---

## 一、动画开关（通用设置）

### 设置项

```typescript
// ui/settings/types.ts
interface AppSettings {
  // ... 已有设置
  animations: {
    enabled: boolean;           // 总开关，默认 true
    reducedMotion: boolean;     // 跟随系统减弱动态（无障碍），默认 false
    intensity: 'full' | 'subtle' | 'off'; // 强度：完整 / 微妙 / 关闭
  };
  glassEffect: boolean;        // 玻璃材质开关，默认 true
}
```

### 设置界面 UI

```
🎨 通用设置
│
├── 动画效果
│   ├── 动画总开关  [✓]
│   ├── 动画强度    [完整 / 微妙 / 关闭]
│   └── 跟随系统减弱动态  [ ]
│
└── 视觉效果
    └── 玻璃材质    [✓]
```

### 实现方式

```typescript
// ui/styles/animations.ts
const ANIMATION_CSS_CLASS = 'animations-enabled';
const GLASS_CSS_CLASS = 'glass-enabled';

function applyAnimationSettings(settings: AnimationSettings) {
  const root = document.documentElement;
  if (settings.enabled && settings.intensity !== 'off') {
    root.classList.add(ANIMATION_CSS_CLASS);
    root.dataset.animationIntensity = settings.intensity; // 'full' | 'subtle'
  } else {
    root.classList.remove(ANIMATION_CSS_CLASS);
  }
}

// CSS 中用 .animations-enabled 前缀控制所有动画是否生效
```

---

## 二、动画清单与技术参数

### 2.1 聊天界面动画

#### 消息气泡入场
```css
/* 弹性入场：从底部弹上来 */
@keyframes bubbleIn {
  0% {
    opacity: 0;
    transform: translateY(20px) scale(0.95);
  }
  60% {
    transform: translateY(-4px) scale(1.02); /* overshoot */
  }
  80% {
    transform: translateY(2px) scale(0.99);  /* settle back */
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.animations-enabled .message-bubble {
  animation: bubbleIn 350ms cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

/* 微妙模式：缩短时长，减弱 overshoot */
[data-animation-intensity="subtle"] .message-bubble {
  animation: bubbleIn 200ms cubic-bezier(0.25, 1, 0.5, 1) both;
}
```

#### 打字指示器
```css
@keyframes typingDot {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

.animations-enabled .typing-indicator .dot:nth-child(1) { animation: typingDot 1.4s infinite 0ms; }
.animations-enabled .typing-indicator .dot:nth-child(2) { animation: typingDot 1.4s infinite 200ms; }
.animations-enabled .typing-indicator .dot:nth-child(3) { animation: typingDot 1.4s infinite 400ms; }
```

#### 消息列表滚动
```typescript
// smooth-scroll.ts
function smoothScrollToBottom(container: HTMLElement, duration = 300) {
  const start = container.scrollTop;
  const end = container.scrollHeight - container.clientHeight;
  const distance = end - start;
  const startTime = performance.now();

  function step(currentTime: number) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    // easeOutCubic: 先快后慢，自然减速
    const ease = 1 - Math.pow(1 - progress, 3);
    container.scrollTop = start + distance * ease;
    if (progress < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}
```

#### 长按菜单展开
```css
@keyframes circularReveal {
  from {
    clip-path: circle(0% at var(--click-x) var(--click-y));
    opacity: 0;
  }
  to {
    clip-path: circle(150% at var(--click-x) var(--click-y));
    opacity: 1;
  }
}

.animations-enabled .context-menu {
  animation: circularReveal 250ms ease-out both;
}
```

#### 表情面板
```css
@keyframes slideUpBounce {
  0% { transform: translateY(100%); }
  70% { transform: translateY(-5%); }   /* overshoot */
  100% { transform: translateY(0); }
}

.animations-enabled .emoji-panel {
  animation: slideUpBounce 400ms cubic-bezier(0.22, 1, 0.36, 1) both;
}
```

#### 图片预览（SharedElement 风格）
```css
@keyframes imageZoomIn {
  from {
    transform: scale(0.3);
    border-radius: 12px;
    opacity: 0;
  }
  to {
    transform: scale(1);
    border-radius: 0;
    opacity: 1;
  }
}

.animations-enabled .image-preview-overlay {
  animation: imageZoomIn 300ms cubic-bezier(0.16, 1, 0.3, 1) both;
}
```

#### 消息发送动画
```css
@keyframes sendPulse {
  0% { transform: scale(1); }
  30% { transform: scale(0.9); }   /* 按下 */
  60% { transform: scale(1.1); }   /* 弹起 overshoot */
  100% { transform: scale(1); }
}

.animations-enabled .send-button.sending {
  animation: sendPulse 400ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes bubbleGrow {
  from {
    max-height: 0;
    opacity: 0;
    transform: scaleY(0.8);
  }
  to {
    max-height: 500px;
    opacity: 1;
    transform: scaleY(1);
  }
}

.animations-enabled .message-bubble.sending {
  animation: bubbleGrow 300ms ease-out both;
  transform-origin: bottom center;
}
```

### 2.2 界面切换动画

#### 页面切换（左滑进入 / 右滑返回）
```css
/* 进入 */
@keyframes slideInRight {
  from { transform: translateX(30%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

/* 返回 */
@keyframes slideOutRight {
  from { transform: translateX(0); opacity: 1; }
  to { transform: translateX(30%); opacity: 0; }
}

.animations-enabled .page-enter {
  animation: slideInRight 300ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.animations-enabled .page-exit {
  animation: slideOutRight 250ms cubic-bezier(0.4, 0, 1, 1) both;
}
```

#### 设置面板（从右侧滑入）
```css
@keyframes slideInFromRight {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.animations-enabled .settings-panel {
  animation: slideInFromRight 300ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

/* 半透明遮罩同步淡入 */
.animations-enabled .settings-overlay {
  animation: fadeIn 300ms ease both;
}
```

#### 对话列表 ↔ 聊天页（头像过渡）
```css
/* SharedElement 风格：头像从列表位置飞到聊天页顶部 */
@keyframes avatarTransition {
  from {
    position: fixed;
    top: var(--avatar-list-y);
    left: var(--avatar-list-x);
    width: 40px;
    height: 40px;
    border-radius: 50%;
  }
  to {
    position: fixed;
    top: var(--avatar-chat-y);
    left: var(--avatar-chat-x);
    width: 48px;
    height: 48px;
    border-radius: 50%;
  }
}

.animations-enabled .avatar-transition {
  animation: avatarTransition 350ms cubic-bezier(0.16, 1, 0.3, 1) both;
}
```

#### 退出动画（托举效果）
```css
@keyframes liftExit {
  0% {
    transform: scale(1);
    filter: blur(0px) brightness(1);
    opacity: 1;
  }
  50% {
    transform: scale(0.92);
    filter: blur(4px) brightness(1.05);
    opacity: 0.7;
  }
  100% {
    transform: scale(0.85);
    filter: blur(8px) brightness(1.1);
    opacity: 0;
  }
}

.animations-enabled .app-exit {
  animation: liftExit 400ms cubic-bezier(0.4, 0, 0.2, 1) both;
}
```

### 2.3 细节微动效

#### 按钮点击回弹
```css
.animations-enabled .btn-interactive {
  transition: transform 150ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
.animations-enabled .btn-interactive:active {
  transform: scale(0.95);
}
```

#### 开关切换
```css
.animations-enabled .toggle-thumb {
  transition: transform 200ms cubic-bezier(0.34, 1.56, 0.64, 1),
              background-color 200ms ease;
}
```

#### 下拉刷新
```css
@keyframes pullStretch {
  0% { transform: scaleY(1); }
  50% { transform: scaleY(1.15); }
  100% { transform: scaleY(1); }
}

.animations-enabled .pull-refresh {
  animation: pullStretch 400ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
```

#### 侧滑返回（跟手弹性）
```css
@keyframes swipeBack {
  0% { transform: translateX(0); }
  100% { transform: translateX(100%); opacity: 0; }
}

.animations-enabled .swipe-back-active {
  transition: transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
```

#### 错误提示抖动
```css
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-8px); }
  40% { transform: translateX(8px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
}

.animations-enabled .error-shake {
  animation: shake 400ms ease both;
}
```

#### 消息收藏（星星爆开）
```css
@keyframes starBurst {
  0% { transform: scale(0) rotate(-30deg); opacity: 0; }
  50% { transform: scale(1.3) rotate(10deg); opacity: 1; }
  100% { transform: scale(1) rotate(0deg); opacity: 1; }
}

.animations-enabled .star-burst {
  animation: starBurst 350ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
```

---

## 三、玻璃材质（Glassmorphism）

### 3.1 设计规范

玻璃材质用于浮层、面板、模态框等覆盖在内容之上的 UI 元素，营造通透感和层次感。

#### 基础变量
```css
:root {
  /* 玻璃材质变量 */
  --glass-bg: rgba(255, 255, 255, 0.72);
  --glass-blur: 20px;
  --glass-saturate: 180%;
  --glass-border: 1px solid rgba(255, 255, 255, 0.18);
  --glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  --glass-radius: 16px;
}

/* 深色模式 */
[data-theme="dark"] {
  --glass-bg: rgba(30, 30, 30, 0.75);
  --glass-border: 1px solid rgba(255, 255, 255, 0.08);
  --glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}
```

#### 基础 mixin
```css
.glass {
  background: var(--glass-bg);
  backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
  border: var(--glass-border);
  box-shadow: var(--glass-shadow);
  border-radius: var(--glass-radius);
}

/* 关闭玻璃材质时降级为纯色背景 */
:not(.glass-enabled) .glass {
  background: var(--bg-primary);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  border: 1px solid var(--border-color);
}
```

### 3.2 应用场景

| 组件 | 玻璃参数 | 说明 |
|------|----------|------|
| **顶栏/Header** | blur=12, opacity=0.85 | 半透明，能看到背后聊天内容滚动 |
| **底栏/输入框** | blur=16, opacity=0.8 | 输入区域浮在聊天上方 |
| **设置面板** | blur=20, opacity=0.75 | 侧滑面板，背后内容模糊可见 |
| **上下文菜单** | blur=24, opacity=0.85 | 长按弹出的菜单 |
| **图片预览遮罩** | blur=30, opacity=0.6 | 大图预览时背景重度模糊 |
| **浮窗/卡片** | blur=16, opacity=0.78 | 日程卡片、天气卡片等 |
| **表情面板** | blur=20, opacity=0.72 | 底部弹出的表情选择器 |
| **Toast 提示** | blur=10, opacity=0.9 | 轻量提示条 |

### 3.3 渐变高光效果

```css
.glass-highlight {
  position: relative;
  overflow: hidden;
}

/* 顶部高光线 */
.glass-highlight::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg,
    transparent,
    rgba(255, 255, 255, 0.4),
    transparent
  );
}

/* 微妙的光泽反射 */
.glass-highlight::after {
  content: '';
  position: absolute;
  top: 0;
  left: -50%;
  width: 200%;
  height: 50%;
  background: linear-gradient(180deg,
    rgba(255, 255, 255, 0.06),
    transparent
  );
  pointer-events: none;
}
```

### 3.4 深色模式适配

```css
[data-theme="dark"] .glass {
  background: rgba(30, 30, 30, 0.75);
}

[data-theme="dark"] .glass-highlight::before {
  background: linear-gradient(90deg,
    transparent,
    rgba(255, 255, 255, 0.12),
    transparent
  );
}
```

---

## 四、性能策略

### 4.1 性能保障

| 策略 | 实现 |
|------|------|
| **GPU 加速** | 所有动画属性使用 `transform` 和 `opacity`（触发 GPU 合成层） |
| **will-change 提示** | 动画元素提前设置 `will-change: transform, opacity` |
| **避免布局抖动** | 禁止动画中改变 `width/height/top/left` 等触发布局的属性 |
| **requestAnimationFrame** | JS 动画统一使用 rAF，不用 setInterval |
| **性能降级** | 低性能设备自动切换到 subtle 模式 |

### 4.2 无障碍

```css
/* 跟随系统「减少动态效果」设置 */
@media (prefers-reduced-motion: reduce) {
  .animations-enabled * {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

### 4.3 开关实现

```typescript
// ui/styles/animation-manager.ts
export class AnimationManager {
  private enabled: boolean = true;
  private intensity: 'full' | 'subtle' | 'off' = 'full';

  update(settings: AnimationSettings) {
    this.enabled = settings.enabled;
    this.intensity = settings.intensity;

    const root = document.documentElement;
    root.classList.toggle('animations-enabled', this.enabled);
    root.classList.toggle('glass-enabled', settings.glassEffect);
    root.dataset.animationIntensity = this.intensity;
  }

  // 代码中调用：如果动画关闭则直接设置终态
  animate(el: HTMLElement, from: Keyframe, to: Keyframe, duration = 300) {
    if (!this.enabled) {
      Object.assign(el.style, to);
      return Promise.resolve();
    }
    return el.animate([from, to], {
      duration: this.intensity === 'subtle' ? duration * 0.6 : duration,
      easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
      fill: 'forwards'
    }).finished;
  }
}
```

---

## 五、实现优先级

| 阶段 | 内容 | 工作量 |
|------|------|--------|
| **P0 — 基础框架** | AnimationManager + CSS 变量 + 设置开关 | 0.5天 |
| **P1 — 聊天动画** | 气泡入场 + 打字指示器 + 发送动画 + 平滑滚动 | 1天 |
| **P2 — 玻璃材质** | 基础 glass mixin + 顶栏/底栏/面板应用 + 深色适配 | 0.5天 |
| **P3 — 页面切换** | slideIn/Out + 设置面板滑入 + 退出动画 | 1天 |
| **P4 — 微动效** | 按钮回弹 + 开关 + 下拉刷新 + 错误抖动 + 收藏星星 | 1天 |
| **P5 — 优化打磨** | 性能调优 + 无障碍 + 低端机降级 | 0.5天 |

**总计：约 4.5 天**

---

## 六、关键 cubic-bezier 参考表

| 名称 | 参数 | 手感 | 适用场景 |
|------|------|------|----------|
| **easeOutBack** | 0.34, 1.56, 0.64, 1 | 弹性 overshoot | 气泡入场、按钮、开关 |
| **easeOutExpo** | 0.16, 1, 0.3, 1 | 快速缓出 | 页面切换、面板滑入 |
| **easeOutQuart** | 0.25, 1, 0.5, 1 | 平滑减速 | 微妙模式下的所有动画 |
| **easeInQuad** | 0.4, 0, 1, 1 | 自然加速 | 退出/消失动画 |
| **springApprox** | 0.22, 1, 0.36, 1 | 类弹簧 | 表情面板、弹窗 |

