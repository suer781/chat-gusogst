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
# Keep all Chaquopy classes and their native methods.
-keep class com.chaquo.python.** { *; }
-keep class com.chaquo.python.android.** { *; }

# ── Hermes Bridge ─────────────────────────────────────────────────────────
# Keep the Java StreamCallback interface — Chaquopy uses reflection to
# discover its methods for Python → Java callback dispatch.
-keep interface com.gusogst.chat.agent.StreamCallback { *; }
-keep class com.gusogst.chat.agent.HermesBridge$BridgeStreamCallback { *; }

# ── Native libraries ─────────────────────────────────────────────────────
# Prevent stripping of .so files bundled by Chaquopy (Python stdlib, pip
# packages with C extensions like pydantic-core, ruamel.yaml).
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep extracted native libs in the app's native lib dir
-keepdirectories lib/arm64-v8a
