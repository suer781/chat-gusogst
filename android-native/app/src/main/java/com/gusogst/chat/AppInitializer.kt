package com.gusogst.chat

import android.app.Application
import android.util.Log
import com.gusogst.chat.agent.HermesBridge

class AppInitializer : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("AppInitializer", "Application initialized")
        HermesBridge.init(this)
    }
}