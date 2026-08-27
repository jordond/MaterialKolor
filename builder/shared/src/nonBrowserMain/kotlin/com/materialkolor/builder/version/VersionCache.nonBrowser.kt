package com.materialkolor.builder.version

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.file.storeOf
import kotlinx.io.files.Path

actual fun createVersionCacheStore(): KStore<CachedVersions> {
    val cacheDir = getVersionCacheDirectory()
    val path = Path("$cacheDir/materialkolor_versions_cache.json")
    return storeOf(file = path, default = null)
}

internal expect fun getVersionCacheDirectory(): String
