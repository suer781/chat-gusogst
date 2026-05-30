# HDR 标准与版本全集：结构化摘要

来源：通义千问 MOS 文档
日期：2026-05-30

## 一、核心结论概览

- **六大主流标准**：HDR10、Dolby Vision、HDR10+、HLG、DisplayHDR、HDR Vivid。
- **"HDR10 Pro"并非独立标准**，仅为厂商营销用语。
- **关键技术分水岭**：
  - **动态元数据**（Dolby Vision、HDR10+、HDR Vivid）可逐帧优化；**静态元数据**（HDR10）全片统一。
  - **峰值亮度**跨度：400 尼特（DisplayHDR 400）至 10000 尼特（Dolby Vision Level 7）。
  - **色域**普遍要求 DCI-P3 或 Rec.2020。
- **设备支持高度分化**：电视最全面，显示器按 VESA 等级，手机以旗舰为主，流媒体是内容分发枢纽。

### 六大标准核心参数对比表

| 标准 | 推广组织 | 元数据 | 峰值亮度 | 色深 | 色域 | 主要应用场景 |
|------|----------|--------|----------|------|------|--------------|
| **HDR10** | CTA | 静态 | 最高 10000 尼特（常以 1000 实现） | 10-bit | BT.2020 | 流媒体、蓝光、游戏主机 |
| **Dolby Vision** | 杜比实验室 | 动态 | 最高 10000 尼特 | 12-bit | DCI-P3 | 高端电视、Apple 生态、UHD 蓝光、影院 |
| **HDR10+** | 三星、亚马逊等 | 动态 | 最高 4000 尼特 | 10-bit | Rec.2020, DCI-P3 | 三星电视、Prime Video、移动设备 |
| **HLG** | BBC、NHK | 无 | 不限（相对亮度） | 10-bit | BT.2020 | 广播电视、体育直播、SDR/HDR 兼容 |
| **DisplayHDR** | VESA | N/A（硬件认证） | 400–1400 尼特 | 8–10 bit | sRGB 至 DCI-P3 | PC 显示器、笔记本电脑 |
| **HDR Vivid** | UWA 联盟 | 动态 | 最高 10000 尼特 | 10/12-bit | Rec.2020 | 中国 4K 电视、华为/小米手机、腾讯/爱奇艺 |

## 二、核心标准细节

### HDR10
- **元数据**：静态（MaxCLL, MaxFALL），基于 SMPTE ST 2084 (PQ)。
- **规格**：10-bit, Rec.2020, 峰值亮度常达 1000 尼特。
- **兼容性**：所有 HDR 电视、游戏主机（PS5, Xbox Series X/S）、流媒体设备均支持。

### Dolby Vision
- **元数据**：动态（SMPTE ST 2094），逐帧/逐场景优化。
- **规格**：12-bit, DCI-P3, 最高 10000 尼特（实际制作以 4000 为主）。
- **版本**：Profile 5/7, Level 5/6, Dolby Vision IQ, DV Game, DV 2 Max
- **设备**：Apple 全系、LG/Sony 高端 OLED、Netflix、UHD 蓝光播放器

### HDR Vivid
- **元数据**：动态，逐帧优化（智能映射引擎）。
- **规格**：10/12-bit, Rec.2020, 最高 10000 尼特。
- **标准化**：GY/T 358-2022（行业标准）、GB/T 46269.1-2025（国家标准）。
- **设备**：华为/小米/OPPO/一加/三星手机及电视；腾讯视频、爱奇艺、咪咕、B 站

---

## 三、chat-gusogst Android 实现参考

### 3.1 理念对齐

chat-gusogst 的 Android HDR v4.0 实现借鉴了 HDR10/Dolby Vision 的核心思想，将其映射到 SDR 显示器的 GUI 渲染层：

| HDR 标准概念 | chat-gusogst Android 映射 | 实现位置 |
|-------------|--------------------------|----------|
| **宽色域 (DCI-P3/Rec.2020)** | 亮/暗双套 HdrColors，颜色值匹配 Web P3/oklch | `HdrHelper.kt` → `HdrColors` |
| **动态元数据 (逐帧调整)** | 运行时 HDR/Glass 开关，实时切换 LayerDrawable 组合 | `HdrHelper.kt` → `apply*()` 方法 |
| **高峰值亮度 (1000-4000 nits)** | elevation 增强 + 彩色辉光阴影 (`outlineSpotShadowColor`) | `HdrHelper.kt` → `setGlowShadow()` |
| **10/12-bit 色深** | ARGB_8888 + 精细 alpha 通道控制 (argb with transparency) | `HdrColors` 各字段 |
| **环境光自适应 (Dolby Vision IQ)** | 三椭圆 RadialGradient 环境光系统，HDR 时饱和度 ×1.3 | `MaterialAnimator.kt` → `AmbientDrawable` |
| **色域映射 (tone mapping)** | 暗/亮主题颜色独立配置，fallback 透明色安全值 | `HdrColors.DARK` / `HdrColors.LIGHT` |

### 3.2 实现文件清单

| 文件 | 路径 | 职责 |
|------|------|------|
| **HdrHelper.kt** | `app/src/main/java/com/gusogst/chat/util/HdrHelper.kt` | HDR v4.0 核心引擎 — 颜色定义、7 层视觉特效、Drawable 构建、API 暴露 |
| **MaterialAnimator.kt** | `app/src/main/java/com/gusogst/chat/util/MaterialAnimator.kt` | 环境光系统、入场/出场动画、主题过渡、Material You 动态曲线 |
| **Models.kt** | `app/src/main/java/com/gusogst/chat/model/Models.kt` | `UISettings` 数据类 — `hdrEnabled`, `glassEnabled`, `glassOpacity` |
| **MainActivity.kt** | `app/src/main/java/com/gusogst/chat/ui/MainActivity.kt` | 集成点 — settings observer 驱动 HDR/Glass/环境光更新 |
| **hdr_v3.css** (Web) | `app/src/ui/hdr_v3.css` | Web 端 HDR v3 CSS 变量 + 选择器规则，Native 颜色的 source of truth |
| **tailwind.css** (Web) | `app/src/ui/tailwind.css` | Web 端玻璃效果 CSS (`backdrop-filter`, `--glass-noise`)，Native 玻璃参数对齐参考 |

### 3.3 颜色方案对齐 HDR 理念

chat-gusogst 的 `HdrColors.DARK` 和 `HdrColors.LIGHT` 颜色选择遵循以下原则：

- **高饱和度强调色** — `glowAccent` 使用 `rgba(220,100,140,0.9)`，模拟 Dolby Vision 中高色域内容的鲜艳度
- **广色域玻璃着色** — `bgTint`、`headerBg`、`navBg` 使用低透明度彩色 (α=0.04~0.08)，模拟玻璃对不同波长光的选择性吸收
- **微光细节** — TopEdgeHighlight 的纯白高光（α=0.95）对应 HDR10 的 specular highlight 概念
- **辉光阴影** — `shadowGlow` 彩色阴影模拟内容光晕扩散到 UI 边框的效果

### 3.4 与 Web CSS 的像素级对齐

Android 原生 HDR 实现的设计原则是**精确重现 Web CSS 的视觉效果**：

```
Web CSS                          Android Native
─────────────────────────────────────────────────────
--hdr-bg-tint (rgba)      →     GradientDrawable.setColor(c.bgTint)
inset 0 1px 0 glow-white  →     TopEdgeHighlight(c.glowWhite)
inset 0 0 30px shadow-glow →    InsetScatterGlow(c.shadowGlow)
border-top 1px card-border →    CombinedGlassEdges borderPaint
border-left 1px rgba()     →    CombinedGlassEdges leftPaint
0 0 20px shadow-glow       →    outlineSpotShadowColor = c.shadowGlow
glass-noise SVG            →    NoiseDrawable(16×16 bitmap, OVERLAY)
```

### 3.5 设备兼容性

| API 级别 | 功能支持 | 说明 |
|----------|---------|------|
| API 28+ (Pie) | 完整 HDR 体验 | 彩色辉光阴影可用 |
| API 29+ (Q) | 完整 HDR + Noise Overlay | `BlendMode.OVERLAY` 正确模拟 Web mix-blend-mode |
| API 21-27 | 降级 HDR | 彩色阴影静默跳过，仍保留 elevation + 对角线反光 + 边缘高光 |
| API < 21 | 最低支持 | `View.setTag()` 使用 AndroidX `View.generateViewId()` fallback |
