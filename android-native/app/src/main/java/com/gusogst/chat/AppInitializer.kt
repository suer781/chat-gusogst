package com.gusogst.chat

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController

/**
 * App initialization - mirrors Web ui/init.ts
 * Sets status bar color, style, safe area, splash screen
 */
object AppInitializer {

    fun init(activity: Activity) {
        initStatusBar(activity)
        initSplashScreen(activity)
    }

    private fun initStatusBar(activity: Activity) {
        activity.window.apply {
            statusBarColor = android.graphics.Color.parseColor("#0f0f23")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }

    private fun initSplashScreen(activity: Activity) {
        // Android 12+ has built-in splash screen
        // Splash theme defined in styles.xml
    }
}
