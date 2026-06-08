plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Chaquopy disabled for initial build - hermes_bridge.py is a stub
    // id("com.chaquo.python")
}

android {
    namespace = "com.gusogst.chat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gusogst.chat"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // 预防性声明：明确测试配置
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // Build Python only for arm64-v8a (95%+ of modern Android devices)
        // to keep APK size under control.  Add armeabi-v7a / x86_64 for
        // emulators during development if needed.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        debug {
            // 预防性声明：调试模式配置
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
        release {
            // 预防性声明：发布模式明确配置
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // 预防性声明：明确 Java 编译配置
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        encoding = "UTF-8"
    }

    kotlinOptions {
        // 预防性声明：明确 Kotlin 编译配置
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        // 预防性声明：明确启用的功能
        buildConfig = true
        viewBinding = false // 暂时禁用以避免问题
        aidl = false
        renderScript = false
        shaders = false
    }

    packaging {
        resources {
            // 预防性声明：明确排除冲突资源
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

/*
// ── Chaquopy: embed Python (Hermes Agent) into the Android process ──────
chaquopy {
    defaultConfig {
        // Use Python from PATH for build-time tasks
        buildPython("/opt/hermes/.venv/bin/python")

        // No external pip packages needed - hermes_bridge.py is a stub
        // Full Hermes Agent requires additional source modules
    }
}
*/

dependencies {
    // AndroidX Core Libraries (Latest Stable)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.1.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.3.0")
    implementation("androidx.recyclerview:recyclerview:1.5.0")

    // Lifecycle & Architecture Components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.0")

    // Networking (OkHttp & Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.12.0")
    implementation("com.squareup.retrofit2:converter-gson:2.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.13.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.13.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.13.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}
