package com.materialkolor.sample.shared.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Holds the [SampleState] a sample renders and runs every [SampleAction] through [reduce].
 *
 * @param[initial] The state to start from.
 */
@Stable
public class SampleStore(
    initial: SampleState = SampleState.Initial,
) {
    /** The current state. Reading it in composition recomposes when it changes. */
    public var state: SampleState by mutableStateOf(initial)
        private set

    /**
     * Replaces [state] with the result of [action].
     */
    public fun dispatch(action: SampleAction) {
        state = state.reduce(action)
    }
}

/**
 * A [SampleStore] that survives recomposition.
 *
 * @param[initial] The state to start from. Only the first composition reads it.
 */
@Composable
public fun rememberSampleStore(initial: SampleState = SampleState.Initial): SampleStore =
    remember { SampleStore(initial) }
