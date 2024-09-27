@file:Suppress("UnusedReceiverParameter")

package com.materialkolor.builder.core

import android.app.Activity
import android.content.Context
import com.materialkolor.builder.MainApp

val DI.context: Context
    get() = MainApp.context()

val DI.activity: Activity
    get() = MainApp.activity()