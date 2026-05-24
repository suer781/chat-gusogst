# WebView 被禁用

> 最后更新：2026-05-24

---

## 症状

```
java.lang.IllegalStateException: WebView has been disabled
```

---

## 修复

```bash
adb shell pm enable com.android.webview
# 或
adb shell pm enable com.android.chrome
```

---

## 相关

- [WebView缺失/](../WebView缺失/INDEX.md)
- [版本过低/](../版本过低/INDEX.md)
