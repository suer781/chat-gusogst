# WebView 版本过低

> 最后更新：2026-05-24

---

## 症状

```
java.lang.NoSuchMethodError: No virtual method ...
```

---

## 原因

WebView 版本太低，不支持某些 API。

---

## 版本要求

| 特性 | 最低 Chromium |
|------|---------------|
| backdrop-filter | 76+ |
| color(display-p3) | 110+ |
| oklch() | 111+ |
| env(safe-area-inset-*) | 69+ |

---

## 修复

更新系统 WebView（应用商店）或更新 Chrome。

---

## 相关

- [WebView缺失/](../WebView缺失/INDEX.md)
- [被禁用/](../被禁用/INDEX.md)
