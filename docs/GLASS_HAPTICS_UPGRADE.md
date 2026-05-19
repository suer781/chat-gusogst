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