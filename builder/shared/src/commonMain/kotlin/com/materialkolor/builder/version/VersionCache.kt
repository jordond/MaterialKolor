package com.materialkolor.builder.version

import co.touchlab.kermit.Logger
import io.github.xxfast.kstore.KStore

interface VersionCache {
    suspend fun get(): CachedVersions?
    suspend fun set(versions: CachedVersions)
    suspend fun clear()
}

class DefaultVersionCache(
    private val store: KStore<CachedVersions>,
) : VersionCache {

    override suspend fun get(): CachedVersions? {
        return try {
            val cached = store.get()
            if (cached != null && cached.isExpired()) {
                Logger.d { "Version cache expired, clearing" }
                store.delete()
                null
            } else {
                cached
            }
        } catch (e: Exception) {
            Logger.w(e) { "Failed to read version cache" }
            null
        }
    }

    override suspend fun set(versions: CachedVersions) {
        try {
            store.set(versions)
            Logger.d { "Cached versions: stable=${versions.stable}, prerelease=${versions.prerelease}" }
        } catch (e: Exception) {
            Logger.w(e) { "Failed to write version cache" }
        }
    }

    override suspend fun clear() {
        try {
            store.delete()
        } catch (e: Exception) {
            Logger.w(e) { "Failed to clear version cache" }
        }
    }
}

expect fun createVersionCacheStore(): KStore<CachedVersions>
