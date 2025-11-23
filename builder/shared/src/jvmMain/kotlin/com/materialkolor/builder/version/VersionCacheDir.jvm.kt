package com.materialkolor.builder.version

internal actual fun getVersionCacheDirectory(): String {
    return System.getProperty("user.home") + "/.cache/materialkolor-builder"
}
