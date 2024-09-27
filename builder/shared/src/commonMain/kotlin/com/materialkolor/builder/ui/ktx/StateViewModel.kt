package com.materialkolor.builder.ui.ktx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import dev.stateholder.StateHolder
import dev.stateholder.StateOwner
import dev.stateholder.StateProvider
import dev.stateholder.stateContainer
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@Suppress("MemberVisibilityCanBePrivate")
abstract class StateViewModel<State>(
    protected val stateHolder: StateHolder<State>,
) : ViewModel(), StateOwner<State> {

    constructor(stateProvider: StateProvider<State>) : this(stateContainer(stateProvider))

    constructor(initialState: State) : this(stateContainer(initialState))

    override val state: StateFlow<State> = stateHolder.state

    fun <T> Flow<T>.collectToState(
        scope: CoroutineScope = viewModelScope,
        block: suspend (state: State, value: T) -> State,
    ): Job {
        return stateHolder.addSource(this, scope, block)
    }

    fun <T> StateOwner<T>.collectToState(
        scope: CoroutineScope = viewModelScope,
        block: suspend (state: State, value: T) -> State,
    ): Job = state.collectToState(scope, block)

    protected fun updateState(block: (State) -> State) {
        stateHolder.update(block)
    }
}

interface EventHolder<Event> {
    val events: StateFlow<PersistentList<Event>>

    fun handle(event: Event)
}

abstract class UiStateViewModel<State, Event>(
    stateHolder: StateHolder<State>,
) : StateViewModel<State>(stateHolder),
    EventHolder<Event> {
    constructor(stateProvider: StateProvider<State>) : this(stateContainer(stateProvider))

    constructor(initialState: State) : this(stateContainer(initialState))

    protected val eventContainer = stateContainer<PersistentList<Event>>(persistentListOf())
    override val events: StateFlow<PersistentList<Event>> = eventContainer.state

    override fun handle(event: Event) {
        Logger.d { "Handling event $event" }
        eventContainer.update { it.remove(event) }
    }

    protected fun emit(event: Event) {
        Logger.d { "Emitting event $event" }
        eventContainer.update { it.add(event) }
    }

    override fun onCleared() {
        Logger.d { "Disposing model ${this::class.simpleName}" }
        super.onCleared()
    }
}

@Composable
fun <Event> HandleEvents(
    holder: EventHolder<Event>,
    onEvent: suspend (Event) -> Unit,
) {
    val events by holder.events.collectAsState()
    HandleEvents(events, remember { holder::handle }, onEvent)
}

@Composable
fun <Event> HandleEvents(
    events: PersistentList<Event>,
    handle: ((Event) -> Unit)? = null,
    onEvent: suspend (Event) -> Unit,
) {
    LaunchedEffect(events) {
        events.forEach { event ->
            onEvent(event)

            if (handle != null) {
                handle(event)
            }
        }
    }
}
