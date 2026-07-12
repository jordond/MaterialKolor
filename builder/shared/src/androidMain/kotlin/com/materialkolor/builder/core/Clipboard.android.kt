package com.materialkolor.builder.core

import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService

internal actual fun copyTextToClipboard(text: String) {
    val clipboard = DI.context.getSystemService<ClipboardManager>()
        ?: error("ClipboardManager not found")

    val clip = ClipData.newPlainText(text, text)
    clipboard.setPrimaryClip(clip)
}