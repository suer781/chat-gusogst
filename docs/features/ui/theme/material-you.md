# Material You 生命力升级

> 状态：进行中 | 创建：2026-05-19
> 目标：Google Material You 级别的弹性物理、环境光、微交互
> 前置：[GLASS_HAPTICS_UPGRADE.md](./GLASS_HAPTICS_UPGRADE.md)

---

## 模块清单

### 🔄 M6: 弹性缓动曲线升级

**目标：** 所有 transition 从线性 ease 换成 Google 风格弹性曲线

| 旧值 | 新值 | 用途 |
|------|------|------|
| `ease` | `var(--ease-spring)` `cubic-bezier(0.2, 0, 0, 1)` | 默认过渡 |
| `ease-out` | `var(--ease-decelerate)` `cubic-bezier(0, 0, 0, 1)` | 入场动画 |
| `ease-in` | `var(--ease-accelerate)` `cubic-bezier(0.3, 0, 1, 1)` | 出场动画 |
| — | `var(--ease-spring-bounce)` `cubic-bezier(0.34, 1.56, 0.64, 1)` | 弹性回弹 |

影响范围：所有 button、card、nav、input 的 transition

---

### 🔄 M7: 按钮弹性按压

**目标：** 按下缩小，松开弹回带过冲

```css
/* 旧 */ button:active { transform: scale(0.96); transition: 0.08s ease; }
/* 新 */ button:active { transform: scale(0.94); transition: 0.1s var(--ease-spring); }
/* 新 */ button:not(:active) { transition: 0.35s var(--ease-spring-bounce); }
```

---

### 🔄 M8: 动态涟漪效果

**目标：** 从触摸点扩散的涟漪，而不是静态 radial-gradient

方案：CSS `@keyframes rippleExpand` + `::after` 伪元素动画

---

### 🔄 M9: 过度滚动辉光

**目标：** 滚动到边界时的弹性光晕（Google 签名效果）

方案：`overscroll-bounce` 动画 + 顶部/底部渐变光晕

---

### 🔄 M10: 环境光背景

**目标：** 主容器背景微妙的渐变流动，像呼吸一样

方案：`@keyframes ambientShift` — 背景色缓慢漂移

---

### 🔄 M11: 卡片触摸提升

**目标：** 触摸卡片时阴影升起，松开回落

方案：`:active` 时 `box-shadow` 扩大 + `translateY(-1px)`

---

### 🔄 M12: 页面切换过渡

**目标：** 视图切换时的 crossfade + 微位移

方案：`@keyframes viewEnter` / `@keyframes viewExit`

---

## 实现顺序

M6 → M7 → M8 → M9 → M10 → M11 → M12

依赖关系：M7/M11 依赖 M6 的缓动曲线

## 回滚

```bash
cd ~/project/github.com/chat-gusogst
git checkout HEAD~1 -- app/src/ui/tailwind.css
```