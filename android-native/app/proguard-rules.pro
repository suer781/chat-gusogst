# ── 预防性声明保护：明确所有需要保留的内容 ─────────────────────────────────

# ── Retrofit & OkHttp ───────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ── Gson & Serialization ─────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.gusogst.chat.model.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ── Kotlin Coroutines ───────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.channels.Channel {
    volatile boolean isClosedForReceive;
}
-keepclassmembernames class kotlinx.coroutines.channels.AbstractChannel {
    volatile int isClosedForReceive;
}

# ── AndroidX & Jetpack ──────────────────────────────────────────────────
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-dontwarn androidx.**

# ── Chaquopy Python runtime ──────────────────────────────────────────────
# Chaquopy bundles native .so libraries and uses JNI reflection.
# Keep all Chaquopy classes and their native methods.
-keep class com.chaquo.python.** { *; }
-keep class com.chaquo.python.android.** { *; }
-dontwarn com.chaquo.python.**

# ── Hermes Bridge ─────────────────────────────────────────────────────────
# Keep the Java StreamCallback interface — Chaquopy uses reflection to
# discover its methods for Python → Java callback dispatch.
-keep interface com.gusogst.chat.agent.StreamCallback { *; }
-keep class com.gusogst.chat.agent.HermesBridge$BridgeStreamCallback { *; }
-keep class com.gusogst.chat.agent.** { *; }

# ── Native libraries ─────────────────────────────────────────────────────
# Prevent stripping of .so files bundled by Chaquopy (Python stdlib, pip
# packages with C extensions like pydantic-core, ruamel.yaml).
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep extracted native libs in the app's native lib dir
-keepdirectories lib/arm64-v8a

# ── 预防性保护：通用保留规则 ─────────────────────────────────────────────
-keep class * extends android.app.Activity
-keep class * extends android.app.Application
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider
-keep class * extends android.preference.Preference
-keep class * extends androidx.fragment.app.Fragment

# Keep view binding generated classes
-keep class **_ViewBinding { *; }

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep custom views used in XML layouts
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
