package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update

/**
 * A [PasteInput] a test pastes into.
 *
 * Every collector sees every paste, as every listener on the web does. A paste made before anyone
 * collects waits for the first collector.
 */
internal class FakePasteInput : PasteInput {
    private val collectors = MutableStateFlow<List<SendChannel<Paste>>>(emptyList())
    private val waiting = MutableStateFlow<List<Paste>>(emptyList())

    override val pastes: Flow<Paste> =
        callbackFlow {
            collectors.update { all -> all + channel }
            handOverWaiting()
            awaitClose { collectors.update { all -> all - channel } }
        }

    /**
     * Paste [paste] with nothing editable focused. It waits for a collector if there is none yet.
     */
    fun paste(paste: Paste) {
        val all = collectors.value
        if (all.isEmpty()) {
            waiting.update { pastes -> pastes + paste }
            // A collector that came in meanwhile has already looked, so it gets it here.
            handOverWaiting()
        } else {
            all.forEach { collector -> collector.trySend(paste) }
        }
    }

    private fun handOverWaiting() {
        val first = collectors.value.firstOrNull() ?: return
        waiting.getAndUpdate { emptyList() }.forEach { paste -> first.trySend(paste) }
    }
}
