package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A [PasteInput] a test pastes into.
 */
internal class FakePasteInput : PasteInput {
    private val pasted = Channel<Paste>(Channel.UNLIMITED)

    override val pastes: Flow<Paste> = pasted.receiveAsFlow()

    /** Paste [paste] with nothing editable focused. It waits for a collector if there is none yet. */
    fun paste(paste: Paste) {
        pasted.trySend(paste)
    }
}
