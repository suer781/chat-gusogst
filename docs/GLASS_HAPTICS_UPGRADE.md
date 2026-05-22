# 玻璃触感 + HDR 动效升级

> 状态：进行中 | 创建：2026-05-19
> 前置：[UI_DESIGN.md](./UI_DESIGN.md) [ANIMATION_SYSTEM.md](./ANIMATION_SYSTEM.md)

---

## 一、升级总览

两个方向：
1. **清脆玻璃触感** — 线性马达震动从"嗡嗡"改为"叮叮"
2. **HDR 动态生命感** — UI 有光影流动、呼吸脉动的灵动感

---

## 二、模块清单

### ✅ M0: haptics.ts 核心重写 — 已完成

文件：`app/src/ui/haptics.ts`

| 函数 | 旧实现 | 新实现 | 状态 |
|------|--------|--------|:----:|
| `glassTap()` | 单次 10ms Light | 双击 `[5, 30, 5]` 清脆弹跳 | ✅ |
| `glassPress()` | ❌ 不存在 | Light→Medium 渐强确认感 | ✅ 新增 |
| `glassSlide()` | ❌ 不存在 | 3ms 极短脉冲，60ms 节流 | ✅ 新增 |
| `sendPulse()` | 单次 25ms Medium | Medium + 60ms后 Light 弹射 | ✅ |
| `unfold()` | 单次 10ms Light | 双击 `[5, 50, 5]` 折纸感 | ✅ |

架构：Capacitor Haptics API 优先，Web Vibrate API 回退。

---

### ✅ M1: CSS HDR 动态光效 — 已完成

文件：`app/src/ui/tailwind.css`（追加在文件末尾）

5 组 `@keyframes` + P3 广色域支持：

| 动画 | 效果 | 选择器 | 周期 |
|------|------|--------|------|
| `prismaticShift` | P3 棱镜折射光 | `.glass-card::after`, `.glass-panel::after` | 8s |
| `auroraBreath` | conic-gradient 极光辉光 | `.glass-card::before` | 6s |
| `depthBreath` | 微缩放脉动 scale(1→1.003) | `.glass-card` | 5s |
| `spectrumDrift` | 边框光谱漂移 | `.glass-panel` | 10s |
| `glossFlow` | 顶部高光流过 overlay | `.glass-panel::before` | 6s |

暗色增强：`brightness(1.08) contrast(1.03)` blur(12px) saturate(1.4)
亮色柔和：P3 `multiply` 混合，12s 慢速
无障碍：`prefers-reduced-motion: reduce` 时全部禁用

---

### ✅ M2: App.tsx import 更新 — 已完成

文件：`app/src/ui/App.tsx`

```tsx
import { light as hapticLight, glassTap, glassPress, setHapticEnabled } from './haptics'
```

---

### ✅ M3: 组件 import 更新 — 已完成

| 文件 | 新增导入 |
|------|----------|
| `chat/ChatView.tsx` | `glassTap, glassPress` |
| `settings/BasicSettings.tsx` | `glassTap, glassPress` |
| `settings/SettingsView.tsx` | `glassTap` |

---

### 🔄 M4: 组件 onClick 替换 — 待做

**目标：** 把组件里的 `hapticLight()` 调用替换为 `glassTap()` / `glassPress()`

需要逐文件检查 `onClick` 里的 haptic 调用，按语义替换：

| 文件 | 替换规则 |
|------|----------|
| `App.tsx` | 所有 `hapticLight()` → `glassTap()` |
| `ChatView.tsx` | 复制/新建聊天按钮 → `glassTap()`；重新生成 → `glassPress()` |
| `BasicSettings.tsx` | 返回按钮 → `glassTap()`；主题/玻璃/HDR 开关 → `glassPress()`；展开选项 → `glassTap()` |
| `SettingsView.tsx` | 完成按钮 → `glassTap()` |
| `ModelSettings.tsx` | provider 选择 → `glassTap()`；返回 → `glassTap()` |
| `PlatformSettings.tsx` | 返回 → `glassTap()` |

**执行方式：**
```bash
# 在 Termux 中逐文件 grep 确认 hapticLight 调用位置
cd ~/project/github.com/chat-gusogst/app/src/ui
grep -n 'hapticLight()' *.tsx components/*.tsx 2>/dev/null
```

---

### 📋 M5: 推送 + CI 构建 — 待做

1. `cd ~/project/github.com/chat-gusogst && git add -A && git commit`
2. 用 `do_push.sh` 推送到 GitHub（ghfast.top 镜像）
3. 等 CI 构建 APK
4. 安装测试

---

## 三、技术参考

### 清脆触感原理
- iOS Taptic Engine / 小米线性马达 的最佳"叮"感来自 **双击短脉冲 + 间隙**
- 间隙 30-50ms 时人感知为单次清脆点击（而非两次分离震动）
- 间隙 >80ms 时感知为两次独立事件

### HDR 动效原则
- 所有动画用 `ease-in-out` 或 `cubic-bezier(0.4, 0, 0.2, 1)` 避免机械感
- P3 广色域颜色只在 `@supports (color: display-p3)` 内使用
- 亮色模式用 `multiply` 混合，暗色用 `screen` 混合
- 动画周期 5-12s，避免太快导致眩晕
- `prefers-reduced-motion: reduce` 必须尊重

---

## 四、回滚方案

如果震动效果不理想：
```bash
cd ~/project/github.com/chat-gusogst
git checkout HEAD~1 -- app/src/ui/haptics.ts
```

如果 HDR 动效卡顿：
- 在设置中关闭 HDR 开关（已有 `data-hdr="off"` 控制）
- 或在 CSS 中删除/注释掉 M1 的 `@keyframes` 块

---

## 2026-05-22: `--glass-opacity` CSS 变量修复

### 问题
App.tsx 第 90 行设置了 `--glass-opacity` CSS 变量，但 `tailwind.css` 中所有毛玻璃元素的背景色都是硬编码的 rgba 值，从未引用该变量。用户调透明度滑块没有任何视觉变化。

### 根因
- JS 侧：`root.style.setProperty('--glass-opacity', String(glassOpacity / 100))` ✅ 设了
- CSS 侧：所有 `.glass-card`、`.glass-panel`、按钮、输入框的 `background` 都是硬编码 rgba ❌ 没读

### 修复

**CSS 侧 (`tailwind.css`)**：
- `:root` 新增默认值 `--glass-opacity: 1`
- 8 处加 `opacity: var(--glass-opacity)`：
  - dark: glass-card、glass-panel、buttons、inputs
  - light: glass-card、glass-panel、buttons、inputs

**JS 侧 (`App.tsx`)**：
- 映射从 `glassOpacity / 100`（0~1）改为 `0.2 + (glassOpacity / 100) * 0.8`（0.2~1.0）
- 保证滑块拉到 0% 时仍保留薄雾效果（opacity=0.2），不会完全透明

### 设计决策
- 用 CSS `opacity` 而非替换 rgba alpha：因为背景色包含复杂 gradient（多层叠加），无法简单用变量替换 alpha 通道
- opacity 0.2 作为下限：完全透明（0）会让玻璃卡片完全消失，用户会以为 bug
- 未来如果需要只调背景不调文字，可改用 `::before` 伪元素方案

### 额外修复
- 修复了 `[data-theme="light"]/*注释*/` 选择器语法 bug（注释卡在选择器中间），原本 light 主题的 glass-card 规则整段不生效


---

## 2026-05-22: Native 颜色同步

### 问题
Web CSS 在 commit `bb9863c` 中做了深色主题大改（deeper blacks, softer ambient light），但 Android 原生侧的 `colors.xml` 没有同步更新，导致 Native 和 Web 视觉不一致。

### 变更

**`android-native/app/src/main/res/values/colors.xml`**（全部对齐 Web CSS dark theme）：

| 变量 | 旧值 | 新值 | 说明 |
|------|------|------|------|
| bg_primary | `#0D0D2B` | `#08080F` | 主背景，更深 |
| bg_secondary | `#1A1A3A` | `#0E0E1A` | 次级背景 |
| bg_tertiary | `#2A2A4A` | `#141428` | 三级背景 |
| bg_elevated | `#3D3D5C` | `#1C1C30` | 悬浮卡片 |
| text_primary | `#FFFFFF` | `#E8E8EE` | 主文字，更柔和 |
| text_secondary | `#B0B0CC` | `#9090A8` | 次级文字 |
| accent_glow | `#40E94560` | `#30E94560` | 主题红光晕，更柔和 |
| gray_50~900 | 旧色阶 | 新色阶 | 全部同步 |

### 同步规则
- Web CSS (`tailwind.css`) 是颜色的 **source of truth**
- Native `colors.xml` 必须与 Web CSS 保持一致
- 未来改主题色时，两个文件都要改

### 注意
- `values-night/colors.xml` 目前只覆盖 3 个颜色，和默认值几乎一样，夜间模式无实际意义
- 如果未来要支持 light theme 切换，需要重新规划 `values/` vs `values-night/` 的结构


---

## 2026-05-22: CSS 变量设计决策

### `--glass-opacity` 工作机制

```
滑块 (0~100)
  → JS: 0.2 + (v/100) * 0.8  → CSS: --glass-opacity (0.2~1.0)
  → 所有 glass-card / glass-panel / button / input 的 opacity
```

- `:root` 默认值：`--glass-opacity: 1`
- JS 映射范围：0.2~1.0（不会完全透明）
- CSS 引用方式：`opacity: var(--glass-opacity)`
- 为什么用 opacity 而非 rgba alpha：背景色是多层 gradient，无法简单替换 alpha

### Web ↔ Native 颜色同步

- **Source of truth**：`app/src/ui/tailwind.css` 的 `[data-theme="dark"]` 变量
- **镜像**：`android-native/app/src/main/res/values/colors.xml`
- 改主题色时两个文件都要改
