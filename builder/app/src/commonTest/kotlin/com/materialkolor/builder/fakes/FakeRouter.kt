package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.domain.link.Route
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A [Router] that remembers what it was asked to do and lets a test press back.
 */
internal class FakeRouter(
    override val initial: Route = Route.Home,
) : Router {
    private val pops = Channel<Unit>(Channel.UNLIMITED)

    /** Every call made so far, oldest first. */
    val calls: MutableList<RouterCall> = mutableListOf()

    override val overlayPops: Flow<Unit> = pops.receiveAsFlow()

    override fun replaceHome() {
        calls += RouterCall.ReplaceHome
    }

    override fun pushOverlay(id: String) {
        calls += RouterCall.PushOverlay(id)
    }

    override fun popOverlay() {
        calls += RouterCall.PopOverlay
    }

    /** Press back with an overlay open. It waits for a collector if there is none yet. */
    fun back() {
        pops.trySend(Unit)
    }
}

/**
 * One call a [FakeRouter] saw.
 */
internal sealed interface RouterCall {
    data object ReplaceHome : RouterCall

    data class PushOverlay(
        val id: String,
    ) : RouterCall

    data object PopOverlay : RouterCall
}
