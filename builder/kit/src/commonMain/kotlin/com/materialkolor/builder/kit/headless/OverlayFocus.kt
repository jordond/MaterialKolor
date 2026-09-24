package com.materialkolor.builder.kit.headless

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged

/**
 * Where focus lands as an overlay opens, so it is always inside an open modal or popover.
 *
 * It goes to the first thing in the panel that takes it. When nothing does, a menu of disabled rows
 * or a dialog of text alone, the panel takes it itself, so Tab and Esc reach the overlay rather than
 * the page under it. When something inside has already taken focus, a field that asks for it as it
 * opens, focus stays where it is.
 */
internal class OverlayFocus {
    private val panel = FocusRequester()
    private val inside = FocusRequester()
    private var panelTakesFocus by mutableStateOf(false)

    /** Whether focus rests on the panel or on anything inside it. */
    private var hasFocus = false

    /** Goes on the panel, around what it holds. */
    val modifier: Modifier = Modifier
        .focusRequester(panel)
        .onFocusChanged { state -> hasFocus = state.hasFocus }
        .focusProperties { canFocus = panelTakesFocus }
        .focusTarget()
        .focusRequester(inside)

    /**
     * Focuses [first], or else the first thing in the panel in reading order, or else the panel.
     * It does nothing while focus is already inside.
     */
    fun enter(first: FocusRequester? = null) {
        if (hasFocus) return
        if (first?.requestFocus() == true || inside.requestFocus()) return
        panelTakesFocus = true
        panel.requestFocus()
    }
}
