# HDR 开关无效

## 症状

在「基本设置 → HDR 渲染」中打开开关后，header 和底部导航栏没有任何视觉变化。即使 HDR 开启，界面看起来和关闭时一模一样。

## 原因（三个叠加 Bug）

### Bug 1：isDark 对 system 主题永远返回 true（主因）

```kotlin
// 修改前
val isDark = theme in listOf("dark", "pureBlack", "system")
```

当用户选择「跟随系统」主题且设备处于浅色模式时，`isDark` 被错误地设为 `true`，使用 `DARK` 色系（如 `reflectionHighlight = argb(60, 255, 255, 255)` 的白色半透明反射层渲染在白色背景上完全不可见）。只有设备本身是暗色模式的用户才能看到效果。

### Bug 2：HDR 辉光仅应用到 header，未覆盖全屏

`s.observe` 回调只调用了 `applyGlassWithHdr(header, …)`，`android.R.id.content` 根背景没有得到 HDR 辉光层。主内容区、卡片、气泡的 HDR 渲染由 `MessageAdapter` 独立控制，但切换 HDR 时 adapter 的 `isDark` 也不刷新。

### Bug 3：MessageAdapter.isDark 没有触发重绘

```kotlin
var isDark = true  // 普通 var，修改后现有 ViewHolder 不更新
```

`ChatFragment` 给 `adapter.isDark` 赋值后，`notifyDataSetChanged()` 不会被调用，已有气泡依然使用旧的 `isDark` 值。

## 修复方案

### 1. isDark 检测修正

```kotlin
/** 正确判断当前是否暗色主题（"system" 按实际系统模式） */
private fun isDarkTheme(theme: String): Boolean = when (theme) {
    "dark", "pureBlack" -> true
    "light", "pureWhite" -> false
    else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
```

`ChatFragment` 中也做同样的修正：

```kotlin
adapter.isDark = when (s.theme) {
    "dark", "pureBlack" -> true
    "light", "pureWhite" -> false
    else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}
```

### 2. HDR 覆盖全屏

在 settings observer 中对 `android.R.id.content` 也应用 `HdrHelper.applyGlassWithHdr()`：

```kotlin
HdrHelper.applyGlassWithHdr(
    findViewById(R.id.header),
    s.hdrEnabled, s.glassEnabled, isDark
)
HdrHelper.applyGlassWithHdr(
    findViewById(android.R.id.content),
    s.hdrEnabled, s.glassEnabled, isDark
)
HdrHelper.applyNavGlow(bottomNav, s.hdrEnabled, isDark)
HdrHelper.applyIndicatorGlow(navIndicator, s.hdrEnabled, isDark)
```

### 3. MessageAdapter.isDark 加 setter 触发重绘

```kotlin
var isDark = true
    set(value) {
        field = value
        notifyDataSetChanged()
    }
```

## 验证步骤

1. 切换到「浅色模式」主题，打开 HDR 开关
2. header 区域出现微弱的暗色辉光（LIGHT 色系）
3. 切到「暗色模式」，HDR 反射层在深色背景上清晰可见
4. 关闭 HDR，header 和内容区恢复原始背景
5. 选「跟随系统」，分别在系统浅色/暗色下验证 HDR 效果都正确

## 相关文件

- `android-native/app/src/main/java/com/gusogst/chat/ui/MainActivity.kt`
- `android-native/app/src/main/java/com/gusogst/chat/ui/chat/ChatFragment.kt`
- `android-native/app/src/main/java/com/gusogst/chat/ui/chat/MessageAdapter.kt`
- `android-native/app/src/main/java/com/gusogst/chat/util/HdrHelper.kt`
- `android-native/app/src/main/java/com/gusogst/chat/model/Models.kt`（UISettings.hdrEnabled 字段）
- `android-native/app/src/main/java/com/gusogst/chat/ui/settings/BasicSettingsFragment.kt`（HDR 开关 UI）

## 后续注意事项

- `isDark`/`hdrEnabled`/`glassEnabled` 三者都需要触发 `notifyDataSetChanged()`，以确保 ListAdapter 中已绑定的 ViewHolder 重新 bind
- 未来如果 `glassEnabled` 也需要「跟随系统」主题判断，同样要注意 `isDark` 检测的正确性
- `applyGlassWithHdr` 内部逻辑：`enabled`（HDR）控制反射辉光层，`glassEnabled` 控制底层的透光渐变，两个参数独立
