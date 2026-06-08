plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        encoding = "UTF-8"
    }

    kotlinOptions {
        // 预防性声明：明确 Kotlin 编译配置
        jvmTarget = "17"
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

// ── Chaquopy: embed Python (Hermes Agent) into the Android process ──────
chaquopy {
    defaultConfig {
        // Use the system Python for build-time tasks
        buildPython("python3")

        // Core Hermes Agent dependencies (pure-Python subset)
        pip {
            options("--index-url", "https://pypi.org/simple")
            options("--extra-index-url", "https://chaquo.com/pypi-13.1")

            // Core: openai SDK + httpx stack
            install("openai==1.2.0")
            install("pydantic==1.10.21")
            install("httpx[socks]<1")
            install("httpcore<2")
            install("h11")
            install("anyio<4")
            install("sniffio")
            install("idna")

            // Config / env / serialization
            install("python-dotenv")
            install("pyyaml")
            install("ruamel.yaml")
            install("requests")
            install("urllib3")
            install("charset-normalizer")

            // CLI / rich output
            install("jinja2")
            install("markupsafe")
            install("rich")
            install("pygments")

            // Utilities
            install("fire")

            // Extra pure-Python convenience deps
            install("distro")
            install("tqdm")
            install("packaging")
            install("attrs")
        }

        // Source directories: Python modules loaded into the APK
        // (Hermes Agent source not included for GitHub Actions builds)
    }

    // Pre-built Python packages (platform-native *.so libs) are
    // excluded from minification so they survive R8/ProGuard.
}

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
