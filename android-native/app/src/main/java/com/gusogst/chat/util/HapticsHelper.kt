package com.gusogst.chat.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticsHelper(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    var enabled = true
    fun light() { if (enabled) vibrate(10) }
    fun medium() { if (enabled) vibrate(25) }
    fun heavy() { if (enabled) vibrate(50) }
    fun success() { if (enabled) { vibrate(20); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ vibrate(20) }, 80) } }
    fun error() { if (enabled) { vibrate(40); android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ vibrate(40) }, 120) } }
    private fun vibrate(ms: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") vibrator.vibrate(ms)
    }
}
