package com.materialkolor.builder.version

import io.github.xxfast.kstore.KStore
import io.github.xxfast.kstore.storage.storeOf

actual fun createVersionCacheStore(): KStore<CachedVersions> {
    return storeOf(key = VERSION_CACHE_KEY, default = null)
}

private const val VERSION_CACHE_KEY = "materialkolor_versions_cache"
