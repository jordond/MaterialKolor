package com.materialkolor.builder.core

import android.content.Intent
import android.net.Uri
import com.materialkolor.builder.MainApp

internal actual fun launchUrl(url: String) {
    val context = MainApp.context()
    val safeUrl =
        if (!url.startsWith("http://") && !url.startsWith("https://")) "http://$url"
        else url

    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(browserIntent)
}