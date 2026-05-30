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
        // Use the Hermes venv Python for build-time tasks (pip install, .pyc
        // compilation).  The runtime Python that runs on-device is Chaquopy's
        // own cross-compiled build — this setting only affects the *build*
        // Python, not the embedded one.
        python {
            buildPython("/opt/hermes/.venv/bin/python")
        }

        // Core Hermes Agent dependencies (pure-Python subset that can be
        // cross-compiled for Android).  C-extension deps are excluded here
        // and handled via lazy fallback at runtime.
        pip {
            // Core: openai SDK + httpx stack
            install("openai==2.24.0")
            install("httpx[socks]==0.28.1")
            install("httpcore==1.0.9")
            install("h11==0.16.0")
            install("anyio==4.11.0")
            install("sniffio==1.3.1")
            install("certifi==2025.10.5")
            install("idna==3.10")

            // Config / env / serialization
            install("pydantic==2.12.5")
            install("pydantic-core==2.41.5")
            install("typing_extensions==4.15.0")
            install("annotated-types==0.7.0")
            install("python-dotenv==1.2.1")
            install("pyyaml==6.0.3")
            install("ruamel.yaml==0.18.17")
            install("requests==2.33.0")
            install("urllib3==2.5.0")
            install("charset-normalizer==3.4.4")

            // CLI / rich output
            install("jinja2==3.1.6")
            install("markupsafe==3.0.3")
            install("tenacity==9.1.4")
            install("rich==14.3.3")
            install("markdown-it-py==4.0.0")
            install("mdurl==0.1.2")
            install("pygments==2.19.2")

            // Utilities
            install("fire==0.7.1")
            install("termcolor==3.1.0")

            // Extra pure-Python convenience deps
            install("distro==1.9.0")
            install("tqdm==4.67.1")
            install("packaging==24.2")
            install("attrs==25.3.0")
        }

        // ── Source directories: Python modules loaded into the APK ──
        // Hermes Agent source — referenced from /opt/hermes (no copy).
        // Chaquopy traverses each dir, collects *.py files, and places them
        // under the Python path on-device so ``import agent`` etc. resolve.
        sourceSets {
            getByName("main") {
                srcDir("/opt/hermes")
                // Prevent deeply-nested test/docs/node_modules clutter
                exclude(
                    "**/tests/**",
                    "**/node_modules/**",
                    "**/__pycache__/**",
                    "**/*.pyc",
                    "**/docs/**",
                    "**/assets/**",
                    "**/website/**",
                    "**/nix/**",
                    "**/docker/**",
                    "**/packaging/**",
                    "**/scripts/**",
                    "**/optional-skills/**",
                    "**/skills/**",
                    "**/locales/**",
                    "**/plans/**",
                    "**/datagen-config-examples/**",
                    "**/tui_gateway/**",
                    "**/ui-tui/**",
                    "**/web/**",
                    "**/cron/**",
                    "**/acp_adapter/**",
                    "**/acp_registry/**"
                )
            }
        }
    }

    // ── Pre-built Python packages (platform-native *.so libs) are
    // excluded from minification so they survive R8/ProGuard.
    // Chaquopy's own ProGuard rules handle the Java/Kotlin side.
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
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
