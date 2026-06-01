package com.gusogst.chat

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat
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

    @Suppress("DEPRECATION")
    private fun initStatusBar(activity: Activity) {
        activity.window.statusBarColor = ContextCompat.getColor(activity, R.color.bg_primary_dark)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun initSplashScreen(activity: Activity) {
        // Android 12+ has built-in splash screen
        // Splash theme defined in styles.xml
    }
}