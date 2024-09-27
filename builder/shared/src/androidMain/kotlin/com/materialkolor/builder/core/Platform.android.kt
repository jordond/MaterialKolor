package com.materialkolor.builder.core

import android.content.Intent
import com.mohamedrejeb.calf.core.PlatformContext

/**
 * Whether the platform supports exporting the current theme to code.
 */
actual val exportSupported: Boolean = false

actual val platformContext: PlatformContext
    get() = DI.context

actual fun shareUrl(url: String) {
    val activity = DI.activity
    val title = "Material Kolor Theme"
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, title)
        putExtra(Intent.EXTRA_TEXT, url)
    }

    val chooser = Intent.createChooser(shareIntent, title)
    activity.startActivity(chooser)
}
