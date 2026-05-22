# 分支策略

## 核心规则

**`main` 和 `AndroidUI` 永不合并。**

## 分支说明

| 分支 | 路线 | 技术栈 | 说明 |
|------|------|--------|------|
| `main` | Web 版本 | Capacitor + HTML/CSS/JS | 主开发分支，CI 自动构建 APK |
| `AndroidUI` | 纯原生 Android | Kotlin/Jetpack Compose | 独立演进的原生版本 |

## 为什么永不合并？

- 两条分支的技术栈完全不同（Web vs Native）
- 强行合并会产生大量冲突，且合并后的代码无法正常工作
- 两者共享的是**设计理念和功能规划**，不是代码

## 如果需要同步功能？

- 功能设计层面可以互相参考
- 具体实现必须各自独立编写
- 禁止 `git merge`、`git rebase`、`git cherry-pick` 跨分支操作
