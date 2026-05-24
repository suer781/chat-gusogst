# 导航栏指示器偏移

> **状态：** 已修复

---

## 症状

底部导航栏的活动指示器（图标下方的小横条）始终偏右，在切换远距离标签时尤其明显（如从"聊天"切换到"设置"）。

---

## 原因

`activity_main.xml` 将 `navIndicator` 放在了 `bottomNav` 的 **水平 LinearLayout 内部**，作为第 5 个子元素。4 个标签用 `layout_weight="1"` 平分宽度，指示器固定宽度 `30dp` 且无 weight，**跟在 4 个标签右侧**：

```
┌──────────┬──────────┬──────────┬──────────┬──────┐
│  聊天     │  人设     │  供应商   │  设置     │ ████ │  ← 指示器在这里
└──────────┴──────────┴──────────┴──────────┴──────┘
```

`MainActivity.kt` 用 `translationX` 左移覆盖：

```kotlin
val targetX = itemWidth * index + (itemWidth - indicatorWidth) / 2f - navIndicator.left
```

**这个公式在系统边栏（System bars）应用 padding 后失效。** `translationX` 是相对于 View 的**原始布局位置**，padding 后的偏移导致 `navIndicator.left` 与真实位置不一致，指示器始终偏右。

这不是像素级问题，而是**设计级问题**——指示器本就不该作为水平布局的子元素。

---

## 修复

### `activity_main.xml`

将 `navIndicator` 从 `bottomNav` 内移出，作为外层 `FrameLayout` 的直接子元素，用 `android:layout_gravity="bottom"` 贴底：

**修复前：**
```
FrameLayout
  ├── View (环境光)
  └── LinearLayout (垂直)
       ├── LinearLayout (标题栏)
       ├── FrameLayout (内容区)
       └── LinearLayout (底部导航, 水平)
            ├── LinearLayout (聊天)
            ├── LinearLayout (人设)
            ├── LinearLayout (供应商)
            ├── LinearLayout (设置)
            └── View (navIndicator)      ← 这里
```

**修复后：**
```
FrameLayout
  ├── View (环境光)
  ├── LinearLayout (垂直)
  │    ├── LinearLayout (标题栏)
  │    ├── FrameLayout (内容区)
  │    └── LinearLayout (底部导航, 无指示器)
  └── View (navIndicator)                ← 现在在这里 (覆盖层)
```

### `MainActivity.kt`

用 `View.setX()` 代替 `translationX`，**直接设置绝对坐标**：

```kotlin
val targetX = navItem.x + (navItem.width / 2f) - (navIndicator.width / 2f)
navIndicator.x = targetX
```

| 对比项 | 旧方案 (`translationX`) | 新方案 (`View.setX()`) |
|--------|------------------------|------------------------|
| 参考系 | 相对原始布局位置 | 父容器内绝对坐标 |
| 受 padding 影响？ | 会——偏离 | 不会——始终读取最新位置 |
| 需要 `- left` 修正？ | 需要 | 不需要 |
| 缓存问题 | 布局变化后偏移 | 稳定 |

---

## 验证

1. 在模拟器/真机上切换 4 个标签，指示器应对齐标签中心
2. 在有手势导航栏的设备上重复（底部 padding 不应影响指示器位置）
3. 横竖屏旋转后检查
4. 在有刘海/挖孔屏的设备上检查

---

## 如果问题还在

1. **检查宽度为 0 的保护逻辑**。`moveIndicator` 会在标签未布局时重试，如果一直失败，加一个 `postDelayed` 兜底：

   ```kotlin
   if (navItem.width == 0) {
       navItem.postDelayed({ moveIndicator(index, animate) }, 100)
       return
   }
   ```

2. **确认 `android:clipChildren="false"`**。目前设置在 `bottomNav` 上，如果指示器在边缘被截断需要检查。

3. **确认 `layout_gravity` 正确**。指示器应只有 `android:layout_gravity="bottom"`，**不要有** `start` / `end` / `left` / `right`。

---

## 相关文件

| 文件 | 功能 |
|------|------|
| `android-native/app/src/main/res/layout/activity_main.xml` | 指示器声明位置（FrameLayout 覆盖层） |
| `android-native/app/src/main/java/com/gusogst/chat/ui/MainActivity.kt` | `moveIndicator()` 用 `View.setX()` 定位 |
| `android-native/app/src/main/java/com/gusogst/chat/ui/MainActivity.kt` | `initNav()` 布局变化监听 |
| `android-native/app/src/main/java/com/gusogst/chat/ui/MainActivity.kt` | `setupWindowInsets()` 边栏 padding |

---

## 后续

- 如果新增第 5 个标签，指示器宽度可能需要调整
- 平板/横屏场景可以考虑用 Material Design 的 `BottomNavigationView`
