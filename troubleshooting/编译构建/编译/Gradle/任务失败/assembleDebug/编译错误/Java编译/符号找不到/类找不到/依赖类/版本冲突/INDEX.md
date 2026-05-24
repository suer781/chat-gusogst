# 版本冲突导致类找不到

> 最后更新：2026-05-24

---

## 症状

```
error: cannot access SomeClass
class file for com.example.SomeClass not found
```

---

## 原因

两个依赖库引入了同一个类的不同版本，编译器无法选择。

---

## 排查

```bash
# 查看依赖树
./gradlew app:dependencies --configuration debugRuntimeClasspath | grep '冲突的包'

# 查找哪个依赖引入了
./gradlew app:dependencyInsight --dependency com.example:lib --configuration debugRuntimeClasspath
```

---

## 修复

```groovy
// 方式一：exclude
implementation('some:library:1.0') {
    exclude group: 'com.conflicting', module: 'module'
}

// 方式二：force
configurations.all {
    resolutionStrategy {
        force 'com.conflicting:module:2.0'
    }
}
```

---

## 相关

- [未引入依赖/](../未引入依赖/INDEX.md)
- [作用域错误/](../作用域错误/INDEX.md)
- [../../../../../../依赖/](../../../../../../依赖/INDEX.md)
