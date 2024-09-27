package com.materialkolor.builder.core

import com.mohamedrejeb.calf.core.PlatformContext
import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.io.readByteArray
import kotlinx.coroutines.flow.Flow

expect val baseUrl: String

/**
 * Whether the platform supports exporting the current theme to code.
 *
 * TODO: Maybe on non-supported platforms we can provide a URL to the web version.
 */
expect val exportSupported: Boolean

expect val platformContext: PlatformContext

suspend fun KmpFile.readBytes(): ByteArray = readByteArray(platformContext)

expect fun updatePlatformQueryParams(queryParams: String)

expect fun readPlatformQueryParams(): String?

expect fun observePlatformQueryParams(): Flow<String>

expect val isMobile: Boolean

expect val shareToClipboard: Boolean

expect fun shareUrl(url: String)
