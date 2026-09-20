package com.materialkolor.builder.version

import com.materialkolor.builder.MainApp

internal actual fun getVersionCacheDirectory(): String = MainApp.context().cacheDir.absolutePath
