@file:OptIn(ExperimentalWasmJsInterop::class)

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
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.js.ExperimentalWasmJsInterop

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
 * A back whose move has not come after a short wait is given up on, so what waits for it still
 * lands. A move that comes after all took the entry pushed in its place, and it closes nothing. The
 * entry it lands on stands in for the overlay that is still open, until that overlay closes.
 *
 * Only overlays this page load opened can be closed by a back. An overlay entry the page lands on
 * without having opened it, after a reload on one or a forward into one, has nothing open behind it,
 * so the router goes back over it quietly.
 */
internal class WebRouter(
    private val history: HistoryPort = BrowserHistory,
) : Router {
    override val initial: Route = RoutePath.parse(history.path(), history.query())

    private val pops = MutableSharedFlow<Unit>(extraBufferCapacity = POP_BUFFER)
    private var depth = history.depth()

    private var opened = 0
    private var pendingBacks = 0
    private val waitingPushes = ArrayDeque<String>()
    private var homeWaiting = false

    private var lateBacks = 0
    private var moves = 0
    private var givenUp = 0

    override val overlayPops: Flow<Unit> = pops.asSharedFlow()

    init {
        history.onPop(::moved)
        goBackQuietly(depth)
        exposeToE2e()
    }

    override fun replaceHome() {
        if (pendingBacks > 0) {
            homeWaiting = true
            waitForMove()
        } else {
            history.replaceUrl(HOME)
        }
    }

    override fun pushOverlay(id: String) {
        if (pendingBacks > 0) {
            waitingPushes.addLast(id)
            waitForMove()
        } else if (history.push(id, depth + 1)) {
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
        moves++
        val popped = depth - newDepth
        depth = newDepth
        if (popped > 0) {
            val ours = minOf(popped, pendingBacks)
            pendingBacks -= ours
            val late = minOf(popped - ours, lateBacks)
            lateBacks -= late
            val closed = minOf(popped - ours - late, opened)
            opened -= closed
            repeat(closed) { pops.tryEmit(Unit) }
        }
        goBackQuietly(depth - pendingBacks - opened)
        if (pendingBacks == 0) catchUp()
    }

    private fun goBackQuietly(steps: Int) {
        if (steps <= 0) return
        pendingBacks += steps
        if (!history.back(steps)) pendingBacks -= steps
    }

    private fun catchUp() {
        if (homeWaiting) {
            homeWaiting = false
            history.replaceUrl(HOME)
        }
        while (waitingPushes.isNotEmpty()) pushOverlay(waitingPushes.removeFirst())
    }

    private fun waitForMove() {
        val movesBefore = moves
        history.later(MOVE_WAIT_MS) { if (moves == movesBefore && pendingBacks > 0) giveUp() }
    }

    private fun giveUp() {
        val now = history.depth()
        if (now < depth) {
            moved(now)
            return
        }
        lateBacks += pendingBacks
        pendingBacks = 0
        val given = ++givenUp
        history.later(LATE_MOVE_MS) { if (givenUp == given) lateBacks = 0 }
        catchUp()
    }
}

/**
 * The History API as the router uses it, so a test can hold a move back or drop it. [later] runs
 * a block after a delay in milliseconds.
 */
internal interface HistoryPort {
    fun path(): String

    fun query(): String

    fun depth(): Int

    fun push(
        id: String,
        depth: Int,
    ): Boolean

    fun back(steps: Int): Boolean

    fun replaceUrl(url: String): Boolean

    fun onPop(listener: (depth: Int) -> Unit)

    fun later(
        delayMs: Int,
        block: () -> Unit,
    )
}

internal object BrowserHistory : HistoryPort {
    override fun path(): String = locationPath()

    override fun query(): String = locationQuery()

    override fun depth(): Int = historyOverlayDepth()

    override fun push(
        id: String,
        depth: Int,
    ): Boolean = historyPushOverlay(id, depth)

    override fun back(steps: Int): Boolean = historyBack(steps)

    override fun replaceUrl(url: String): Boolean = historyReplaceUrl(url)

    override fun onPop(listener: (depth: Int) -> Unit) {
        onHistoryPop(listener)
    }

    override fun later(
        delayMs: Int,
        block: () -> Unit,
    ) {
        window.setTimeout({
            block()
            null
        }, delayMs)
    }
}

private const val HOME = "/"
private const val POP_BUFFER = 16

/**
 * How long a push or home waits for a back's move before it gives the back up.
 */
private const val MOVE_WAIT_MS = 250

/**
 * How long a back given up on may still land and close nothing.
 */
private const val LATE_MOVE_MS = 1_000
