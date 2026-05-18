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

### 2.1 颜色系统

```css
:root {
  /* ── 背景层级 ── */
  --bg-primary:     #0d0d2b;   /* 最底层 */
  --bg-secondary:   #151538;   /* 卡片、面板 */
  --bg-tertiary:    #1a1a3a;   /* 输入框、列表项 */
  --bg-elevated:    #22224a;   /* 悬浮、弹窗 */
  --bg-overlay:     rgba(0, 0, 0, 0.6);

  /* ── 文字层级 ── */
  --text-primary:   #ffffff;
  --text-secondary: #a0a0b8;
  --text-tertiary:  #6b6b80;
  --text-disabled:  #4a4a5e;
  --text-inverse:   #0d0d2b;

  /* ── 主题色 ── */
  --accent:         #e94560;
  --accent-hover:   #c73e54;
  --accent-soft:    rgba(233, 69, 96, 0.12);
  --accent-glow:    rgba(233, 69, 96, 0.25);

  /* ── 功能色 ── */
  --purple:         #6C5CE7;
  --purple-soft:    rgba(108, 92, 231, 0.12);
  --yellow:         #FDCB6E;
  --yellow-soft:    rgba(253, 203, 110, 0.12);
  --success:        #4CAF50;
  --success-soft:   rgba(76, 175, 80, 0.12);
  --danger:         #FF5252;
  --danger-soft:    rgba(255, 82, 82, 0.12);
  --warning:        #FF9800;
  --warning-soft:   rgba(255, 152, 0, 0.12);
  --teal:           #00b894;
  --teal-soft:      rgba(0, 184, 148, 0.12);
  --blue:           #3498db;
  --blue-soft:      rgba(52, 152, 219, 0.12);

  /* ── 深色灰度（深色主题专用） ── */
  --gray-50:    #f0f0f5;
  --gray-100:   #d8d8e8;
  --gray-200:   #b0b0c0;
  --gray-300:   #8888a0;
  --gray-400:   #6b6b80;
  --gray-500:   #4e4e65;
  --gray-600:   #3a3a50;
  --gray-700:   #2a2a40;
  --gray-800:   #1e1e35;
  --gray-900:   #141428;

  /* ── 边框与分割 ── */
  --border:         #2a2a4a;
  --border-focus:   var(--accent);
  --border-soft:    #1e1e3e;
  --divider:        rgba(255, 255, 255, 0.06);

  /* ── 圆角 ── */
  --radius-xs:    4px;
  --radius-sm:    6px;
  --radius-md:    10px;
  --radius-lg:    16px;
  --radius-xl:    24px;
  --radius-full:  9999px;

  /* ── 阴影 ── */
  --shadow-sm:   0 1px 3px rgba(0, 0, 0, 0.3);
  --shadow-md:   0 4px 12px rgba(0, 0, 0, 0.4);
  --shadow-lg:   0 8px 24px rgba(0, 0, 0, 0.5);
  --shadow-glow: 0 0 20px var(--accent-glow);

  /* ── 间距阶梯（4px 倍数） ── */
  --space-0:   0;
  --space-1:   4px;
  --space-2:   8px;
  --space-3:   12px;
  --space-4:   16px;
  --space-5:   20px;
  --space-6:   24px;
  --space-8:   32px;
  --space-10:  40px;
  --space-12:  48px;
  --space-16:  64px;

  /* ── 字号体系（12 档） ── */
  --text-2xs:  9px;    /* 极小角标、隐私标记 */
  --text-xs:   10px;   /* badge、小标签 */
  --text-sm:   12px;   /* 辅助文字、时间戳、placeholder */
  --text-base: 14px;   /* 正文默认 */
  --text-md:   15px;   /* 密集列表正文 */
  --text-lg:   16px;   /* 列表标题、设置项名称 */
  --text-xl:   18px;   /* 小节标题、卡片标题 */
  --text-2xl:  20px;   /* 区块标题 */
  --text-3xl:  24px;   /* 页面标题 */
  --text-4xl:  32px;   /* 头像旁昵称、大数字 */
  --text-5xl:  40px;   /* 统计大屏数字 */
  --text-6xl:  48px;   /* 登录页品牌标题 */

  /* ── 字重 ── */
  --font-normal:   400;
  --font-medium:   500;
  --font-semibold: 600;
  --font-bold:     700;

  /* ── 行高 ── */
  --leading-tight:  1.25;
  --leading-normal: 1.5;
  --leading-relaxed: 1.75;

  /* ── 动效 ── */
  --ease-default:    cubic-bezier(0.4, 0, 0.2, 1);
  --ease-in:         cubic-bezier(0.4, 0, 1, 1);
  --ease-out:        cubic-bezier(0, 0, 0.2, 1);
  --ease-bounce:     cubic-bezier(0.34, 1.56, 0.64, 1);
  --duration-fast:   150ms;
  --duration-normal: 250ms;
  --duration-slow:   400ms;
}
```

---

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
