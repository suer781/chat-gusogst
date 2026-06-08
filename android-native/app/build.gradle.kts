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

        // Build Python only for arm64-v8a (95%+ of modern Android devices)
        // to keep APK size under control.  Add armeabi-v7a / x86_64 for
        // emulators during development if needed.
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.5")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
