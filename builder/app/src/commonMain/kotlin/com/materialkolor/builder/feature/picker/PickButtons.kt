package com.materialkolor.builder.feature.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * The Pick buttons on screen, each known by the focus requester it sends with `OpenPicker`.
 *
 * The picker hands focus back only to a button that still stands (AR-09), the rule `PanelTrigger`
 * keeps for the poster. A Pick button that left meanwhile, with the poster folded to its rail or
 * its accent removed, gets no request.
 */
internal object PickButtons {
    private val onScreen = mutableStateMapOf<FocusRequester, Int>()

    /** [opener] while its button is on screen, otherwise null. */
    fun returnFocusFor(opener: FocusRequester?): FocusRequester? =
        opener?.takeIf { requester -> (onScreen[requester] ?: 0) > 0 }

    fun add(requester: FocusRequester) {
        onScreen[requester] = (onScreen[requester] ?: 0) + 1
    }

    fun remove(requester: FocusRequester) {
        val left = (onScreen[requester] ?: 0) - 1
        if (left > 0) onScreen[requester] = left else onScreen.remove(requester)
    }
}

/**
 * The modifier for a Pick button that sends [requester] with `OpenPicker`, which counts the button
 * as on screen while it is composed.
 */
@Composable
internal fun pickButtonFocus(requester: FocusRequester): Modifier {
    DisposableEffect(requester) {
        PickButtons.add(requester)
        onDispose { PickButtons.remove(requester) }
    }
    return Modifier.focusRequester(requester)
}
