# 作用域错误

> 最后更新：2026-05-24

---

## 症状

```
error: cannot access SomeClass
  class file for com.example.SomeClass not found
  (it is provided by dependency with scope 'compileOnly')
```

---

## 原因

依赖用了 `compileOnly` 作用域，只在编译时可见，运行时找不到。

---

## 修复

```groovy
// 改为 implementation
implementation 'com.example:library:1.0'
// 或
api 'com.example:library:1.0'
```

---

## 作用域对比

| 作用域 | 编译时 | 运行时 | 传递 |
|--------|--------|--------|------|
| implementation | ✅ | ✅ | ❌ |
| api | ✅ | ✅ | ✅ |
| compileOnly | ✅ | ❌ | ❌ |
| runtimeOnly | ❌ | ✅ | ❌ |

---

## 相关

- [版本冲突/](../版本冲突/INDEX.md)
- [未引入依赖/](../未引入依赖/INDEX.md)
