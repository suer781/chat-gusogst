# :root 声明规范

> 最后更新：2026-05-24

---

## 核心规则

**所有 CSS 变量必须在 :root 显式声明默认值，不依赖隐式继承。**

---

## 正确示例

```css
:root {
  /* ✅ 有默认值 */
  --safe-top: env(safe-area-inset-top, 48px);
  --glass-opacity: 0.3;

  /* ❌ 错误：没有 fallback */
  /* --safe-top: env(safe-area-inset-top); */
}
```

---

## 原因

- Capacitor WebView 中 `env()` 可能返回空
- 没有 fallback 时变量值为空
- 所有引用该变量的地方都会失效

---

## 相关

- [未声明/](../未声明/INDEX.md)
- [重复声明/](../重复声明/INDEX.md)
- [../../赋值/](../../赋值/INDEX.md)
- [../../兜底/](../../兜底/INDEX.md)
