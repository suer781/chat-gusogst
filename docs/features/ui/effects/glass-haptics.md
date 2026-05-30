# 玻璃触感 + HDR 动效升级

> 状态：已完成 | 创建：2026-05-19 | Android HDR v4.0 已实现 (2026-05-30)
> 前置：[UI_DESIGN.md](./UI_DESIGN.md) [ANIMATION_SYSTEM.md](./ANIMATION_SYSTEM.md)

---

## 一、升级总览

两个方向：
1. **清脆玻璃触感** — 线性马达震动从"嗡嗡"改为"叮叮"
2. **HDR 动态生命感** — UI 有光影流动、呼吸脉动的灵动感

跨平台实现矩阵：

| 平台 | HDR 实现 | 玻璃效果 | 触感反馈 |
|------|----------|----------|----------|
| **Web** | CSS `hdr_v3.css` (P3/oklch 广色域) | `tailwind.css` backdrop-filter | `haptics.ts` |
| **Android Native** | `HdrHelper.kt` v4.0 (7 层视觉特效) | `HdrHelper.kt` LayerDrawable 玻璃层 | `HapticsHelper.kt` |

---

## 二、Android 原生 HDR 系统 (HdrHelper.kt)

> 文件：`android-native/app/src/main/java/com/gusogst/chat/util/HdrHelper.kt` (890 行)

### 2.1 颜色方案

`HdrColors` 数据类，DARK + LIGHT 两套色值，精确匹配 Web `hdr_v3.css`：

```kotlin
data class HdrColors(
    val glowBase: Int,           // 辉光基础色
    val glowAccent: Int,         // 辉光强调色
    val glowWhite: Int,          // 纯白高光
    val borderHighlight: Int,    // 边框高光色
    val shadowGlow: Int,         // 彩色阴影辉光
    val bgTint: Int,             // 玻璃底色染
    val cardBorder: Int,         // 卡片边框色
    val headerBg: Int,           // Header 背景色
    val navBg: Int,              // 导航栏背景色
    val bubbleTint: Int,         // 气泡着色
    val buttonGlow: Int,         // 按钮辉光
    val indicatorGlow: Int,      // 导航指示器辉光
    val inputFocusGlow: Int,     // 输入框聚焦辉光
    val reflectionHighlight: Int // 对角线反光
)
```

**暗色主题 (DARK)** — 精确匹配 `hdr_v3.css [data-hdr="on"][data-theme="dark"]`：
- `glowAccent = Color.argb(230, 220, 100, 140)` = `rgba(220,100,140,0.9)`
- `shadowGlow = Color.argb(64, 200, 100, 150)` = `rgba(200,100,150,0.25)`
- `bgTint = Color.argb(15, 180, 120, 200)` = `rgba(180,120,200,0.06)`

**亮色主题 (LIGHT)** — 精确匹配 `hdr_v3.css [data-hdr="on"][data-theme="light"]`：
- `glowAccent = Color.argb(217, 180, 60, 100)` = `rgba(180,60,100,0.85)`
- `shadowGlow = Color.argb(38, 180, 80, 140)` = `rgba(180,80,140,0.15)`

### 2.2 七层视觉效果

通过自定义 `Drawable` 子类逐层叠加，使用 `LayerDrawable` 组合为 GPU 单次合成：

| # | 效果 | Drawable 类 | 对应 Web CSS | 物理模拟 |
|---|------|-------------|-------------|----------|
| 1 | **底色染** (bgTint) | `GradientDrawable` | `background-color: var(--hdr-bg-tint)` | 玻璃本身的轻微着色 |
| 2 | **内散射辉光** (InsetScatterGlow) | 自定义 `RadialGradient` 从中心扩散 | `inset 0 0 30px var(--hdr-shadow-glow)` | 光线进入玻璃内部后的散射 |
| 3 | **顶部 1px 高光线** (TopEdgeHighlight) | 自定义 `drawRoundRect` 1dp 厚度 | `inset 0 1px 0 var(--hdr-glow-white)` | 光源在玻璃上缘的反射 |
| 4 | **上边缘彩色边** | `CombinedGlassEdges` 合并绘制 | `border-top: 1px solid var(--hdr-card-border)` | 玻璃切面折射的高光边缘 |
| 5 | **左边缘白色微光** | `CombinedGlassEdges` 同层绘制 | `border-left: 1px solid rgba(255,255,255,0.06)` | 侧向环境光反射 |
| 6 | **对角线表面反光** | `GradientDrawable(Orientation.TL_BR)` | 无直接 CSS 对应（Native 独有增强） | 环境光在玻璃表面的镜面反射 |
| 7 | **微纹理噪点** (NoiseDrawable) | 16×16 预生成 `BitmapShader` 平铺 | `--glass-noise` SVG feTurbulence | 玻璃微观瑕疵模拟 |

#### 关键 Drawable 实现细节

**CombinedGlassEdges** (性能优化 — 原 3 层合并为 1 层)：
```kotlin
// 原先 3 个独立的 LayerDrawable 层（TopEdgeHighlight×2 + LeftEdgeHighlight）被合并
// 为单次 drawRoundRect×3，减少 LayerDrawable 合成开销约 29%
private class CombinedGlassEdges(
    topGlowColor: Int, borderColor: Int, leftGlowColor: Int, density: Float, cornerRadius: Float
) : Drawable() {
    // 三层 paint 各自独立着色（绘制顺序：白线 → 彩边 → 左边缘）
    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(l, t, r, t + thickness, rr, rr, topGlowPaint)      // 1) 白色高光线
        canvas.drawRoundRect(l, t, r, t + thickness, rr, rr, borderPaint)       // 2) 边框彩色
        canvas.drawRoundRect(l, t, l + thickness, b, rr, rr, leftPaint)         // 3) 左边缘
    }
}
```

**NoiseDrawable** — 匹配 Web `--glass-noise` SVG feTurbulence：
```kotlin
// 预生成 16x16 随机噪点 Bitmap（固定种子 Random(42)）
// API 29+ 使用 BlendMode.OVERLAY 匹配 Web mix-blend-mode: overlay
// 暗色: opacity 0.04，亮色: opacity 0.025
// 单例模式缓存 dark/light 两个 NoiseDrawable，所有 view 共用
```

**InsetScatterGlow** — 内散射辉光（缓存 Shader 避免每帧重建）：
```kotlin
// RadialGradient 从 view 中心扩散到 70% 对角线
// 颜色: glowColor → TRANSPARENT
// 仅当 view 尺寸变化时重建 Shader
```

### 2.3 公开 API

所有方法均支持 `enabled` 参数控制开关，关闭时自动还原原始背景：

| 方法 | 用途 | 目标 View | 对应 Web CSS |
|------|------|-----------|-------------|
| `applyGlassWithHdr()` | 根视图玻璃 + HDR 叠加 | `android.R.id.content` | `.app-header` + content area |
| `applyHeaderGlow()` | Header 辉光 | `R.id.header` | `.app-header` |
| `applyNavGlow()` | 导航栏辉光 | `R.id.bottomNav` | `.app-nav` |
| `applyCardGlow()` | 玻璃卡片效果 | 任意卡片 View | `.glass-card` |
| `applyBubbleGlow()` | 消息气泡效果 | 聊天气泡 View | `.msg-bubble` |
| `applyButtonGlow()` | 按钮按下效果 | 按钮 View | `.btn-accent` / `button:active` |
| `applyIndicatorGlow()` | 导航指示器 | `R.id.navIndicator` | `.nav-indicator>div` |
| `applyInputGlow()` | 输入框聚焦效果 | EditText | `input:focus` / `textarea:focus` |
| `applyToggleGlow()` | Toggle 开关效果 | Switch/CheckBox | `.toggle-active` |

### 2.4 HDR / Glass 开关行为矩阵

| 状态 | 效果叠加 | elevation | 阴影颜色 |
|------|----------|-----------|----------|
| HDR=off, Glass=off | 还原原始背景 | 0 | 默认 |
| HDR=on, Glass=off | 对角线反光 + 彩色辉光阴影 + 彩色边框 | 3dp (根视图) | `shadowGlow` 彩色 |
| HDR=off, Glass=on | 底色染 + 顶部高光 + 边缘高光线 + 内散射辉光 + 噪点纹理 | 2dp | 默认 |
| HDR=on, Glass=on | 以上全部 + 底部额外内散射辉光 | 6dp (卡片) | `shadowGlow` 增强 |

### 2.5 配置模型 (Models.kt)

```kotlin
data class UISettings(
    // ... 其他字段
    val hdrEnabled: Boolean = false,    // HDR 动态光效开关
    val glassEnabled: Boolean = false,  // 玻璃效果开关
    val glassOpacity: Int = 80,         // 玻璃透明度 (0-100)
    // ...
)
```

### 2.6 环境光系统 (MaterialAnimator.kt)

> 文件：`android-native/app/src/main/java/com/gusogst/chat/util/MaterialAnimator.kt` (373 行)

**`setAmbientBackground()`** — 设置/更新三椭圆径向渐变环境光叠加层：

- 匹配 Web `tailwind.css body::after` 的三椭圆径向渐变：
  - 顶部居中 (80% 50% at 50% 0%)
  - 左下角 (60% 40% at 20% 100%)
  - 右下角 (50% 35% at 80% 90%)
- `AmbientDrawable` 自定义 Drawable 缓存三个 `RadialGradient` Shader
- 支持 600ms 交叉淡入淡出动画（`PathInterpolator(0.4, 0, 0.2, 1)` + `AccelerateInterpolator`）
- HDR 开启时环境光饱和度 ×1.3 (alpha 通道乘 `hdrMul = 1.3f`)
- 纯黑模式 (`pureBlack`) 自动禁用环境光

### 2.7 依赖与兼容性

| 依赖 | 最低 API | 用途 |
|------|----------|------|
| `outlineSpotShadowColor` | API 28 (P) | 彩色辉光阴影（API < 28 静默跳过） |
| `outlineAmbientShadowColor` | API 28 (P) | 环境阴影颜色 |
| `BlendMode.OVERLAY` | API 29 (Q) | 噪点层混合模式（< 29 回退 DITHER + alpha） |
| `PathInterpolator` | API 21 (L) | 自定义贝塞尔缓动曲线 |
| AndroidX/Core | - | `View.generateViewId()`、`ViewCompat` |

**背景保存/还原机制**：
- 使用 `View.setTag(TAG_KEY_ORIGINAL_BG, originalBg)` 而非 `WeakHashMap<View, Drawable>` — 避免弱引用 GC 开销，O(1) 无锁直接查找
- 首次调用 `apply*()` 时保存原始背景，HDR/Glass 关闭时完全还原并重置 elevation + 阴影颜色

### 2.8 400ms 过渡动画

对齐 Web `hdr_v3.css` 第 57-59 行的 `transition: box-shadow 0.4s cubic-bezier(0.2,0,0,1)`：

```kotlin
// elevation 动画: ValueAnimator + PathInterpolator(0.2f, 0f, 0f, 1f)
private fun animateElevation(view: View, targetPx: Float) {
    ValueAnimator.ofFloat(current, targetPx).apply {
        duration = 400
        interpolator = PathInterpolator(0.2f, 0f, 0f, 1f)
        addUpdateListener { view.elevation = it.animatedValue as Float }
        start()
    }
}

// 背景交叉淡入淡出: alpha 0.85 → 1.0, 400ms
private fun crossFadeBackground(view: View) {
    view.alpha = 0.85f
    view.animate().alpha(1f).setDuration(400)
        .setInterpolator(PathInterpolator(0.2f, 0f, 0f, 1f)).start()
}
```

---

## 三、Web 实现（参考）

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

### ✅ M1: CSS HDR v3 — 已完成

文件：`app/src/ui/hdr_v3.css`

- HDR on 时通过 CSS 变量注入颜色：`[data-hdr="on"]` 定义 `--hdr-glow-accent`、`--hdr-shadow-glow`、`--hdr-bg-tint` 等 12 个变量
- P3 广色域增强：`@media (color-gamut: p3)` + oklch 提升色域覆盖率
- 0.4s cubic-bezier(0.2,0,0,1) 过渡：所有玻璃元素统一过渡 box-shadow、border-color、background
- Light/Dark 双主题：`[data-hdr="on"][data-theme="light"]` 独立颜色方案
- HDR+Glass 组合：`[data-hdr="on"][data-glass="on"]` 额外 `inset 0 0 30px` + `inset 0 -1px 20px`

### ✅ M2-M3: 组件 import 更新 — 已完成

具体见原文档 commit log。

---

## 四、技术参考

### 清脆触感原理 (Web + Android)
- iOS Taptic Engine / 小米线性马达 的最佳"叮"感来自 **双击短脉冲 + 间隙**
- 间隙 30-50ms 时人感知为单次清脆点击（而非两次分离震动）
- 间隙 >80ms 时感知为两次独立事件
- Android 侧通过 `HapticsHelper.kt` 封装系统 `Vibrator` API，实现相同的脉冲序列

### HDR 效果层级映射 (Web ↔ Android)

| Web CSS | Android Drawable | 光学原理 |
|---------|-----------------|----------|
| `background-color: var(--hdr-bg-tint)` | `GradientDrawable` 背景色 | 玻璃基底着色 |
| `inset 0 1px 0 var(--hdr-glow-white)` | `TopEdgeHighlight` | 上缘高光反射 |
| `inset 0 0 30px var(--hdr-shadow-glow)` | `InsetScatterGlow` | 内散射辉光 |
| `border-top: 1px solid var(--hdr-card-border)` | `CombinedGlassEdges` borderPaint | 上边缘彩色折射 |
| `border-left: 1px solid rgba(255,255,255,0.06)` | `CombinedGlassEdges` leftPaint | 左边缘微光 |
| `box-shadow: 0 0 20px var(--hdr-shadow-glow)` | `outlineSpotShadowColor` | 彩色辉光阴影 |
| `--glass-noise` SVG feTurbulence | `NoiseDrawable` BitmapShader | 玻璃微观纹理 |
| `background: linear-gradient(135deg, ...)` | `GradientDrawable(TL_BR)` | 对角线表面反光 |

---

## 五、性能注意事项

### Android 原生 HDR 性能优化

1. **LayerDrawable 组合** — 所有 5-7 层效果组合为单个 `LayerDrawable`，由 GPU 一次合成，避免多层 View 叠加带来的 layout/draw 开销
2. **NoiseDrawable 单例缓存** — dark/light 各一个全局单例 `NoiseDrawable`，所有 View 共用同一个 `BitmapShader` (TileMode.REPEAT)，避免重复创建 Bitmap
3. **CombinedGlassEdges 合并** — 将原本 3 个独立 Drawable 层（TopEdgeHighlight + borderTop + LeftEdgeHighlight）合并为单层 `drawRoundRect×3`，减少 LayerDrawable 层数从 7→5（约 29% 合成更快）
4. **Shader 缓存** — `InsetScatterGlow` 和 `BottomInsetGlow` 仅在 View 尺寸变化时重建 `RadialGradient` Shader，避免每帧 `new`
5. **API 常量缓存** — `Build.VERSION.SDK_INT >= P` 结果缓存为 `HAS_OUTLINE_SHADOW` 常量，避免重复系统调用
6. **View.setTag 替代 WeakHashMap** — 背景保存/还原使用 `View.setTag()` 直接查找（SparseArray O(1)），避免 `WeakHashMap` 弱引用 GC 开销
7. **硬件加速层** — 需要频繁重绘 HDR 效果的 View 启用 `View.LAYER_TYPE_HARDWARE`，将绘制结果缓存到 GPU 纹理
8. **列表滚动优化** — 避免在 `RecyclerView.onBindViewHolder` 中重建 LayerDrawable；消息气泡的 HDR 效果应在绑定视图时一次性应用，不应在滚动回调中重新构建

### 低端设备建议

- **HDR 默认关闭** — 低端设备（<= 4GB RAM, API < 29）建议默认 `hdrEnabled = false`
- **Glass 默认开启** — 玻璃效果无 `RadialGradient` 复杂 Shader 开销，性能影响较小
- **彩色阴影 API 28+** — 低于 API 28 静默跳过 `outlineSpotShadowColor`，保持默认阴影
- **NoiseDrawable Overlay 混合 API 29+** — 低于 API 29 回退为 DITHER + alpha 方案
- **环境光叠加层** — 纯黑模式自动跳过 `AmbientDrawable`，省去 3 个 RadialGradient 绘制

---

## 六、历史记录

### 2026-05-22: `--glass-opacity` CSS 变量修复

### 2026-05-22: Native 颜色同步

### 2026-05-22: CSS 变量设计决策

*(详细内容见文档早期版本或 git log，此处仅保留摘要索引)*
