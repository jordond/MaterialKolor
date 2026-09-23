package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import com.materialkolor.builder.web.interop.listenForPastes
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Pastes on the document while nothing editable has focus.
 *
 * Each collector adds its own `paste` listener and removes it when it stops. Pasted images win over
 * pasted text, since copying an image file usually puts its name on the clipboard as text too.
 */
internal object WebPasteInput : PasteInput {
    override val pastes: Flow<Paste> =
        callbackFlow {
            val stop = listenForPastes(
                onText = { text -> trySend(Paste.Text(text)) },
                onFiles = { files -> trySend(Paste.Files(files.map(::BrowserImage))) },
            )
            awaitClose(stop)
        }
}
