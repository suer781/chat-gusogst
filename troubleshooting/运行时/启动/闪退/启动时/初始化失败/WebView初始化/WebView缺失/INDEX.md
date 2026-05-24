# WebView 缺失

> 最后更新：2026-05-24

---

## 症状

```
java.lang.ClassNotFoundException: android.webkit.WebViewFactory
```

---

## 原因

系统 WebView 被卸载或定制 ROM 没有预装。

---

## 修复

```bash
# 检查
adb shell pm list packages | grep webview

# 启用
adb shell pm enable com.android.webview

# 或安装 Chrome
adb shell pm list packages | grep chrome
```

---

## 相关

- [版本过低/](../版本过低/INDEX.md)
- [被禁用/](../被禁用/INDEX.md)
- [../../../../../Capacitor初始化/](../../../../../Capacitor初始化/INDEX.md)
