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
 * Each collector adds its own `paste` listener and removes it when it stops. Pasted files win over
 * pasted text, since copying a file usually puts its name on the clipboard as text too. Files that
 * are not images come through as well, so `decode` turns them down and the user hears why.
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
