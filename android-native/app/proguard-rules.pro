# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

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
