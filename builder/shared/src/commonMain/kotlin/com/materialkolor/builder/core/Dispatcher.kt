package com.materialkolor.builder.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import kotlinx.datetime.Clock
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

private const val TAG = "Dispatcher"

interface Dispatcher<Action> {
    fun dispatch(action: Action)

    operator fun invoke(action: Action) {
        dispatch(action)
    }

    fun relay(action: Action): () -> Unit = { dispatch(action) }

    fun <T1> relayOf(action: KFunction1<T1, Action>): (T1) -> Unit = { t1 -> dispatch(action(t1)) }

    fun <T1, T2> relayOf(action: KFunction2<T1, T2, Action>): (T1, T2) -> Unit =
        { t1, t2 -> dispatch(action(t1, t2)) }

    fun <T1, T2, T3> relayOf(action: (T1, T2, T3) -> Action): (T1, T2, T3) -> Unit =
        { t1, t2, t3 -> dispatch(action(t1, t2, t3)) }

    fun <T1, T2, T3, T4> relayOf(action: (T1, T2, T3, T4) -> Action): (T1, T2, T3, T4) -> Unit =
        { t1, t2, t3, t4 -> dispatch(action(t1, t2, t3, t4)) }

    fun <T1, T2, T3, T4, T5> relayOf(action: (T1, T2, T3, T4, T5) -> Action): (T1, T2, T3, T4, T5) -> Unit =
        { t1, t2, t3, t4, t5 -> dispatch(action(t1, t2, t3, t4, t5)) }

    fun <T1, T2, T3, T4, T5, T6> relayOf(
        action: (T1, T2, T3, T4, T5, T6) -> Action,
    ): (T1, T2, T3, T4, T5, T6) -> Unit =
        { t1, t2, t3, t4, t5, t6 -> dispatch(action(t1, t2, t3, t4, t5, t6)) }

    @Composable
    fun rememberRelay(action: Action): () -> Unit = remember(action) { relay(action) }

    @Composable
    fun <T1> rememberRelayOf(action: KFunction1<T1, Action>): (T1) -> Unit =
        remember(action) { { t1 -> dispatch(action(t1)) } }

    @Composable
    fun <T1, T2> rememberRelayOf(action: KFunction2<T1, T2, Action>): (T1, T2) -> Unit =
        remember(action) { { t1, t2 -> dispatch(action(t1, t2)) } }

    @Composable
    fun <T1, T2, T3> rememberRelayOf(action: (T1, T2, T3) -> Action): (T1, T2, T3) -> Unit =
        remember(action) { { t1, t2, t3 -> dispatch(action(t1, t2, t3)) } }

    @Composable
    fun <T1, T2, T3, T4> rememberRelayOf(action: (T1, T2, T3, T4) -> Action): (T1, T2, T3, T4) -> Unit =
        remember(action) { { t1, t2, t3, t4 -> dispatch(action(t1, t2, t3, t4)) } }

    @Composable
    fun <T1, T2, T3, T4, T5> rememberRelayOf(
        action: (T1, T2, T3, T4, T5) -> Action,
    ): (T1, T2, T3, T4, T5) -> Unit =
        remember(action) { { t1, t2, t3, t4, t5 -> dispatch(action(t1, t2, t3, t4, t5)) } }

    @Composable
    fun <T1, T2, T3, T4, T5, T6> rememberRelayOf(
        action: (T1, T2, T3, T4, T5, T6) -> Action,
    ): (T1, T2, T3, T4, T5, T6) -> Unit =
        remember(action) { { t1, t2, t3, t4, t5, t6 -> dispatch(action(t1, t2, t3, t4, t5, t6)) } }
}

@Composable
fun <A> rememberDispatcher(block: (A) -> Unit): Dispatcher<A> = remember { Dispatcher(block) }

@Composable
fun <A> rememberDebounceDispatcher(
    debounce: Long = 100,
    exclude: List<A> = emptyList(),
    block: (A) -> Unit,
): Dispatcher<A> = remember { DebounceDispatcher(debounce, exclude, block) }

fun <A> Dispatcher(block: (A) -> Unit): Dispatcher<A> =
    object : Dispatcher<A> {
        override fun dispatch(action: A) {
            block(action)
            Logger.d(TAG) { "Dispatched action: $action" }
        }
    }

fun <A> Dispatcher(
    debounce: Long,
    block: (A) -> Unit,
): Dispatcher<A> = DebounceDispatcher(debounce, emptyList(), block)

@Suppress("FunctionName")
fun <A> DebounceDispatcher(
    debounce: Long = 100,
    exclude: List<A> = emptyList(),
    block: (A) -> Unit,
): Dispatcher<A> {
    return object : Dispatcher<A> {
        private val lookup: MutableMap<A, Long> = mutableMapOf()

        override fun dispatch(action: A) {
            if (exclude.contains(action)) {
                Logger.d(TAG) { "Dispatched excluded action immediately: $action" }
                return block(action)
            }

            val currentTime = Clock.System.now().toEpochMilliseconds()
            val lastTime = lookup[action]
            if (lastTime == null || currentTime - lastTime > debounce) {
                lookup[action] = currentTime
                block(action)
                Logger.d(TAG) { "Dispatched debounced action: $action" }
            } else {
                Logger.d(TAG) { "Action is debounced: $action" }
            }
        }
    }
}