package com.materialkolor.sample.shared.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
public class SampleStore(
    initial: SampleState = SampleState.Initial,
) {
    public var state: SampleState by mutableStateOf(initial)
        private set

    public fun dispatch(action: SampleAction) {
        state = state.reduce(action)
    }
}

/**
 * @param[initial] Only the first composition reads it.
 */
@Composable
public fun rememberSampleStore(initial: SampleState = SampleState.Initial): SampleStore =
    remember { SampleStore(initial) }
