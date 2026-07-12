package com.materialkolor.builder

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(ActivityCallbackListener { currentActivity = it })
    }

    @SuppressLint("StaticFieldLeak")
    companion object {
        private var instance: MainApp? = null

        private var currentActivity: Activity? = null

        fun context(): Context {
            return instance?.applicationContext ?: error("MainApp is not initialized")
        }

        fun activity(): Activity {
            return currentActivity ?: error("Activity is not initialized")
        }
    }
}