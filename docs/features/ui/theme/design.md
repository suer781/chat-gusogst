# UI 设计令牌与通用规范

> 状态：待实施 | 创建：2026-05-18
> 组件接口定义见 → [UI_COMPONENTS.md](./UI_COMPONENTS.md)

---

## 一、现状诊断

### 1.1 样式方式不统一

| 文件 | inline style | className | 混用？ |
|------|:-----------:|:---------:|:-----:|
| BasicSettings.tsx | ✅ | ❌ | — |
| ModelSettings.tsx | ✅ | ❌ | — |
| SearchSettings.tsx | ✅ | ❌ | — |
| MemorySettings.tsx | ✅ | ❌ | — |
| AboutSettings.tsx | ✅ | ❌ | — |
| PlatformSettings.tsx | ✅ | ❌ | — |
| PersonaView.tsx | ✅ | ❌ | — |
| PersonaProfileView.tsx | ✅ | ❌ | — |
| SettingsView.tsx | ✅ | ❌ | — |
| ProviderSettings.tsx | ⚠️ | ✅ Tailwind | **是** |
| ChatView.tsx | ⚠️ | ✅ Tailwind | **是** |
| MessageBubble.tsx | ⚠️ | ✅ Tailwind | **是** |

### 1.2 颜色硬编码

全局只定义了 5 个 CSS 变量，代码中硬编码了 **20+ 种颜色**：

| 颜色值 | 含义 | 出现次数 |
|--------|------|:-------:|
| `#e94560` | 主题红 | **43** |
| `#e9456020` | 主题红 12% | 5 |
| `#1a1a3a` | 深蓝背景 | **16** |
| `#2a2a4a` | 边框色 | **13** |
| `#c73e54` | 红色悬停 | 5 |
| `#6C5CE7` | 紫色强调 | 6 |
| `#FDCB6E` | 黄色星星 | 5 |
| `#4CAF50` / `#FF5252` / `#FF9800` | 功能色 | 各 2-3 |

### 1.3 字号 / 间距碎片化

- 字号：12 种无规律散落（10~48px）
- gap：3/6/8/10/12/14/16 px 无体系
- padding：8/12/16/20/24 px 随意取值

---

## 二、设计令牌

> 令牌定义已迁移至 **[`app/src/ui/docs/DESIGN_TOKENS.md`](../app/src/ui/docs/DESIGN_TOKENS.md)**，此处不再重复。
> 请查阅该文件获取完整的颜色/间距/字号/动效令牌表及使用方式。

## 三、实施计划

### Phase 1 — 令牌注入（低成本高收益）

- [ ] `tailwind.css` 的 `:root` 补全所有 CSS 变量
- [ ] 全局替换硬编码颜色（`#e94560` × 43、`#1a1a3a` × 16、`#2a2a4a` × 13 等）
- [ ] 散落 fontSize 值映射到 12 档变量
- [ ] 散落 gap/padding 值映射到间距阶梯

**字号映射表（旧值 → 新变量）：**

| 旧 fontSize | 现有用途 | 映射 |
|:-----------:|---------|------|
| 9 | — | `--text-2xs` |
| 10 | 隐私标记小字 | `--text-xs` |
| 11 | 小辅助文字 | `--text-sm`（升到 12） |
| 12 | 时间戳、placeholder | `--text-sm` |
| 13 | 密集正文 | `--text-base`（升到 14） |
| 14 | 正文、设置项 | `--text-base` |
| 15 | 列表正文 | `--text-md` |
| 16 | 设置标题 | `--text-lg` |
| 18 | 页面标题 | `--text-xl` |
| 20 | 区块标题 | `--text-2xl` |
| 22 | 卡片标题 | `--text-2xl`（合并到 20） |
| 24 | 大标题 | `--text-3xl` |
| 32 | 头像旁昵称 | `--text-4xl` |
| 48 | 品牌标题 | `--text-6xl` |

### Phase 2 — 公共组件抽取

见 [UI_COMPONENTS.md](./UI_COMPONENTS.md)，逐组件抽取并替换内联样式。

### Phase 3 — 混用修复

- [ ] ProviderSettings / ChatView / MessageBubble 的 `className` 混用问题
- [ ] 统一为 inline style + CSS 变量（或全面迁移到 Tailwind，二选一）

### Phase 4 — i18n 规范化

- [ ] 统一 key 命名：`{模块}_{组件}_{语义}`，全小写+下划线
- [ ] 合并重复 key（`about_project` / `about_title`）
- [ ] 排查组件中硬编码的中文字符串

---

## 四、完成自测检查清单

- [ ] `grep -r '#[0-9a-fA-F]\{6\}' app/src/ui/` — 无硬编码颜色
- [ ] `grep -rn 'fontSize:' app/src/ui/ | grep -v 'var(--text'` — 全部走变量
- [ ] `grep -rn 'className=' app/src/ui/providers/` — ProviderSettings 无混用
- [ ] 所有页面在深色主题下视觉一致
- [ ] 各组件通过 UI_COMPONENTS.md 中的 Props 定义可单独复用


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
