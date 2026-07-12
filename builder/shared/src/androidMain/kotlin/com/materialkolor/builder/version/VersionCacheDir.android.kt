package com.materialkolor.builder.version

import com.materialkolor.builder.MainApp

internal actual fun getVersionCacheDirectory(): String {
    return MainApp.context().cacheDir.absolutePath
}
