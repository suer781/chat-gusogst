# ── 预防性声明保护：核心规则 ─────────────────────────────────────────────

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.gusogst.chat.model.** { *; }

# ── Chaquopy Python runtime ──────────────────────────────────────────────
# Chaquopy bundles native .so libraries and uses JNI reflection.
-keep class com.chaquo.python.** { *; }
-keep class com.chaquo.python.android.** { *; }

# Hermes Bridge
-keep interface com.gusogst.chat.agent.StreamCallback { *; }
-keep class com.gusogst.chat.agent.HermesBridge$BridgeStreamCallback { *; }

# Native libraries
-keepclasseswithmembers class * {
    native <methods>;
}

-keepdirectories lib/arm64-v8a
