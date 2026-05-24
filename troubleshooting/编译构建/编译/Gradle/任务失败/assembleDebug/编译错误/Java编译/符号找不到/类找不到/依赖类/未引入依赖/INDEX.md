# 未引入依赖

> 最后更新：2026-05-24

---

## 症状

```
error: package com.example does not exist
```

---

## 修复

```groovy
// android/app/build.gradle
dependencies {
    implementation 'com.example:library:1.0'
}
```

然后 `./gradlew clean && ./gradlew assembleDebug`

---

## 相关

- [版本冲突/](../版本冲突/INDEX.md)
- [作用域错误/](../作用域错误/INDEX.md)
