# 四角锚点系统

> 最后更新：2026-05-24

---

## 核心变量

```css
:root {
  --safe-top: env(safe-area-inset-top, 48px);
  --safe-bottom: env(safe-area-inset-bottom, 20px);
  --safe-left: env(safe-area-inset-left, 0px);
  --safe-right: env(safe-area-inset-right, 0px);
  --header-height: 48px;
  --header-total: calc(var(--header-height) + var(--safe-top));
  --nav-height: 56px;
  --nav-total: calc(var(--nav-height) + var(--safe-bottom));
}
```

---

## 分类

| 子类 | 入口 |
|------|------|
| 变量 | [变量/](变量/INDEX.md) |
| 引用 | [引用/](引用/INDEX.md) |
| 计算 | [计算/](计算/INDEX.md) |
