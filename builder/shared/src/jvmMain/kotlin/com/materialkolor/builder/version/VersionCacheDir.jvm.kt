package com.materialkolor.builder.version

internal actual fun getVersionCacheDirectory(): String =
    System.getProperty("user.home") + "/.cache/materialkolor-builder"
