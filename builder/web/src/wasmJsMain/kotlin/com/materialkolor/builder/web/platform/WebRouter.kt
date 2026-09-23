package com.materialkolor.builder.web.platform

import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.RoutePath
import com.materialkolor.builder.web.interop.historyBack
import com.materialkolor.builder.web.interop.historyOverlayDepth
import com.materialkolor.builder.web.interop.historyPushOverlay
import com.materialkolor.builder.web.interop.historyReplaceUrl
import com.materialkolor.builder.web.interop.locationPath
import com.materialkolor.builder.web.interop.locationQuery
import com.materialkolor.builder.web.interop.onHistoryPop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The address bar and the back button, through the History API.
 *
 * The address is read once, when the shell builds its services before the first composition. After
 * that the address only changes when [replaceHome] puts `/` back, so edits never add an entry. Each
 * overlay adds one entry on the same address, and the router counts how deep it is in overlay
 * entries so it can tell a back that closes an overlay from one it made itself.
 *
 * [popOverlay] goes back, which the browser finishes later. An overlay opened before that lands
 * waits for it, since pushing first would put the new entry under the one being popped.
 */
internal class WebRouter : Router {
    override val initial: Route = RoutePath.parse(locationPath(), locationQuery())

    private val pops = MutableSharedFlow<Unit>(extraBufferCapacity = POP_BUFFER)
    private var depth = historyOverlayDepth()
    private var pendingBacks = 0
    private val waitingPushes = ArrayDeque<String>()

    override val overlayPops: Flow<Unit> = pops.asSharedFlow()

    init {
        onHistoryPop(::moved)
        exposeToE2e()
    }

    override fun replaceHome() {
        historyReplaceUrl(HOME)
    }

    override fun pushOverlay(id: String) {
        if (pendingBacks > 0) {
            waitingPushes.addLast(id)
        } else if (historyPushOverlay(id, depth + 1)) {
            depth++
        }
    }

    override fun popOverlay() {
        if (waitingPushes.removeLastOrNull() != null) return
        if (depth - pendingBacks <= 0) return
        pendingBacks++
        historyBack()
    }

    private fun moved(newDepth: Int) {
        val popped = depth - newDepth
        depth = newDepth
        if (pendingBacks > 0) {
            pendingBacks--
            if (pendingBacks == 0) pushWaiting()
            return
        }
        repeat(popped.coerceAtLeast(0)) { pops.tryEmit(Unit) }
    }

    private fun pushWaiting() {
        while (waitingPushes.isNotEmpty()) pushOverlay(waitingPushes.removeFirst())
    }
}

private const val HOME = "/"
private const val POP_BUFFER = 16
