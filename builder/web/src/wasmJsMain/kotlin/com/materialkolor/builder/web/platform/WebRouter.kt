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
 * waits for it, since pushing first would put the new entry under the one being popped, and so does
 * [replaceHome]. The browser may fold several backs into one move, so a move settles as many of the
 * router's own backs as it covers and reports the rest as the user's.
 *
 * Only overlays this page load opened can be closed by a back. An overlay entry the page lands on
 * without having opened it, after a reload on one or a forward into one, has nothing open behind it,
 * so the router goes back over it quietly.
 */
internal class WebRouter : Router {
    override val initial: Route = RoutePath.parse(locationPath(), locationQuery())

    private val pops = MutableSharedFlow<Unit>(extraBufferCapacity = POP_BUFFER)
    private var depth = historyOverlayDepth()

    // Overlays this page load opened and has not closed yet, the entries from depth 1 up.
    private var opened = 0
    private var pendingBacks = 0
    private val waitingPushes = ArrayDeque<String>()
    private var homeWaiting = false

    override val overlayPops: Flow<Unit> = pops.asSharedFlow()

    init {
        onHistoryPop(::moved)
        goBackQuietly(depth)
        exposeToE2e()
    }

    // While a back is on its way the current entry is about to change, so home waits for it too.
    override fun replaceHome() {
        if (pendingBacks > 0) homeWaiting = true else historyReplaceUrl(HOME)
    }

    override fun pushOverlay(id: String) {
        if (pendingBacks > 0) {
            waitingPushes.addLast(id)
        } else if (historyPushOverlay(id, depth + 1)) {
            depth++
            opened++
        }
    }

    override fun popOverlay() {
        if (waitingPushes.removeLastOrNull() != null) return
        if (opened <= 0) return
        opened--
        goBackQuietly(1)
    }

    private fun moved(newDepth: Int) {
        val popped = depth - newDepth
        depth = newDepth
        if (popped > 0) {
            val ours = minOf(popped, pendingBacks)
            pendingBacks -= ours
            val closed = minOf(popped - ours, opened)
            opened -= closed
            repeat(closed) { pops.tryEmit(Unit) }
        }
        goBackQuietly(depth - pendingBacks - opened)
        if (pendingBacks == 0) catchUp()
    }

    // A back the browser refused never reports a move, so it is not left counted as pending.
    private fun goBackQuietly(steps: Int) {
        if (steps <= 0) return
        pendingBacks += steps
        if (!historyBack(steps)) pendingBacks -= steps
    }

    private fun catchUp() {
        if (homeWaiting) {
            homeWaiting = false
            historyReplaceUrl(HOME)
        }
        while (waitingPushes.isNotEmpty()) pushOverlay(waitingPushes.removeFirst())
    }
}

private const val HOME = "/"
private const val POP_BUFFER = 16
