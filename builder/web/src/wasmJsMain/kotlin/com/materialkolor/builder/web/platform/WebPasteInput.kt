package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// stub
// B-301 listens for paste events on the document.
internal object WebPasteInput : PasteInput {
    override val pastes: Flow<Paste> = emptyFlow()
}
