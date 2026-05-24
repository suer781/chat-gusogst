# backdrop-filter 版本要求

> 最后更新：2026-05-24

---

## 最低版本

**Chrome 76+ / Chromium 76+**

---

## 降级方案

```css
.element {
  /* 降级：半透明背景 */
  background: rgba(255, 255, 255, 0.3);

  /* 进阶：毛玻璃 */
  @supports (backdrop-filter: blur(1px)) {
    backdrop-filter: blur(14px) saturate(1.6);
    background: rgba(255, 255, 255, 0.1);
  }
}
```

---

## 相关

- [广色域/](../广色域/INDEX.md)
- [safe_area/](../safe_area/INDEX.md)
- [../../../../CSS/](../../../../CSS/INDEX.md)
