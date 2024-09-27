package com.materialkolor.builder.core

import com.mohamedrejeb.calf.core.PlatformContext
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * Whether the platform supports exporting the current theme to code.
 */
actual val exportSupported: Boolean = false

actual val platformContext: PlatformContext
    get() = PlatformContext.INSTANCE

actual fun shareUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    val activityViewController = UIActivityViewController(
        activityItems = listOf(nsUrl),
        applicationActivities = null,
    )

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
    rootViewController?.presentViewController(activityViewController, animated = true, completion = null)
}
