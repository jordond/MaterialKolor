package com.materialkolor.builder.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private var cache: String? = null

actual fun updatePlatformQueryParams(queryParams: String) {
    cache = queryParams
}

actual fun readPlatformQueryParams(): String? {
    return cache?.takeIf { it.isNotBlank() }
}

actual val baseUrl: String = "https://materialkolor.com"

actual fun observePlatformQueryParams(): Flow<String> = emptyFlow()
