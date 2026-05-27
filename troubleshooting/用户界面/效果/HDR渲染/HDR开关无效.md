# HDR 开关无效

## 症状

在「基本设置 → HDR 渲染」中打开开关后，header 和底部导航栏没有任何视觉变化。

## 原因

`HdrHelper` 工具类虽已实现（含 `applyHeaderGlow`/`applyNavGlow`/`applyCardGlow`/`applyBubbleGlow` 等方法），但在 `MainActivity.kt` 的 `settings.observe` 回调中完全没有调用它。

```kotlin
// 修改前：只处理了 theme 和 glass
viewModel.settings.observe(this) { s ->
    applyTheme(s.theme)
    applyGlassEffect(findViewById(R.id.header), s.glassEnabled)
    // s.hdrEnabled 被忽略了！
}
```

UI 可以正确保存 `hdrEnabled` 状态到 `UISettings`，但 MainActivity 从不读取它，导致 HDR 效果永远不生效。

## 修复方案

### 1. 添加 HdrHelper 的 import

```kotlin
import com.gusogst.chat.util.HdrHelper
```

### 2. 在 settings.observe 中添加 HDR 调用

```kotlin
viewModel.settings.observe(this) { s ->
    applyTheme(s.theme)
    applyGlassEffect(findViewById(R.id.header), s.glassEnabled)
    applyHdrEffect(s.hdrEnabled, s.theme)  // 新增
}
```

### 3. 新增 applyHdrEffect 方法

```kotlin
/**
 * Apply HDR glow to header and nav bar based on toggle + theme.
 * Uses HdrHelper which mirrors the Web hdr_v3.css glow effect.
 */
private fun applyHdrEffect(enabled: Boolean, theme: String) {
    val isDark = theme in listOf("dark", "pureBlack", "system")
    val header = findViewById<View>(R.id.header)
    HdrHelper.applyHeaderGlow(header, enabled, isDark)
    HdrHelper.applyNavGlow(bottomNav, enabled, isDark)
}
```

## 验证步骤

1. 打开设置 → 基本设置 → HDR 渲染开关
2. 观察 header 区域是否出现微弱的紫色发光底色（`DARK.headerBg = argb(20, 180, 120, 200)`）
3. 导航栏底部也应出现相同色调的微弱 glow
4. 关闭开关后，header 和导航栏恢复透明底色
5. 切换到浅色主题，重复验证 LIGHT 色系是否正确

## 相关文件

- `android-native/app/src/main/java/com/gusogst/chat/ui/MainActivity.kt`
- `android-native/app/src/main/java/com/gusogst/chat/util/HdrHelper.kt`
- `android-native/app/src/main/java/com/gusogst/chat/model/Models.kt`（UISettings.hdrEnabled 字段）
- `android-native/app/src/main/java/com/gusogst/chat/ui/settings/BasicSettingsFragment.kt`（HDR 开关 UI）

## 后续注意事项

- HdrHelper 目前仅对 header 和 nav 生效。气泡（applyBubbleGlow）和卡片（applyCardGlow）尚未接入，如需全量 HDR 效果需在 MessageAdapter 和 Fragment 中额外调用。
- `applyGlassEffect` 和 `applyHdrEffect` 互不冲突——glass 控制毛玻璃模糊，HDR 控制发光底色。二者可以同时开启。
- 主题切换时 `applyHdrEffect` 的 `isDark` 判断会影响使用哪套 HDR 色值（DARK vs LIGHT）。