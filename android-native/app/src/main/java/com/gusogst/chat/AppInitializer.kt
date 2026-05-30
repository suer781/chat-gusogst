package com.gusogst.chat

import android.app.Activity
import android.os.Build
import android.view.WindowInsetsController
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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
        // Use color resource instead of hardcoded value
        activity.window.statusBarColor = activity.resources.getColor(R.color.status_bar_color, null)
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        controller.isAppearanceLightStatusBars = true
    }

    private fun initSplashScreen(activity: Activity) {
        // Android 12+ has built-in splash screen
        // Splash theme defined in styles.xml
    }
}
