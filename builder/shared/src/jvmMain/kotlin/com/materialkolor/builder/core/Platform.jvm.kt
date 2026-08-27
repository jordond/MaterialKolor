package com.materialkolor.builder.core

import com.mohamedrejeb.calf.core.PlatformContext

/**
 * Whether the platform supports exporting the current theme to code.
 */
actual val exportSupported: Boolean = true

actual val platformContext: PlatformContext
    get() = PlatformContext.INSTANCE

actual val isMobile: Boolean = false

actual val shareToClipboard: Boolean = true

actual fun shareUrl(url: String) {
    copyTextToClipboard(url)
}
