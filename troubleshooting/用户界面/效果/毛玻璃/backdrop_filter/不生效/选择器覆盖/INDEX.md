# 选择器覆盖导致 backdrop-filter 不生效

> 最后更新：2026-05-24

---

## 症状

`data-glass="off"` 时毛玻璃仍然显示。

---

## 原因

通用样式 `.glass-card` 的 `backdrop-filter` 优先级高于 `data-glass` 选择器。

---

## 修复

```css
.glass-card[data-glass="off"] {
  backdrop-filter: none !important;
}
```

---

## 相关

- [CSS未声明/](../CSS未声明/INDEX.md)
- [内联样式/](../内联样式/INDEX.md)
- [../../开关/](../../开关/INDEX.md)
