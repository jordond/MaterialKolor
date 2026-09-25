package com.materialkolor.builder.kit.a11y

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Whether focus is being moved from the keyboard, the way a browser decides `:focus-visible` (D58).
 *
 * A key press turns it on and a pointer press turns it off. So a ring shows on whatever Tab or an
 * arrow reaches, and not on the button a click leaves focused. Focus a panel moves as it opens
 * follows the input that opened it, ringed after a key and bare after a click. A modifier key on its
 * own, or a key pressed with Cmd, Ctrl or Alt held, is a shortcut rather than a move, and leaves it
 * as it was.
 *
 * It starts on, so focus that moves before anyone touches the page still shows, and so does focus a
 * test moves without any input.
 */
@Stable
internal class FocusVisibility {
    /** Whether a focused control draws its ring now. */
    var isVisible: Boolean by mutableStateOf(true)
        private set

    /** Notes a key going down, which turns the rings on unless it is a modifier or part of a shortcut. */
    fun onKey(event: KeyEvent) {
        if (event.type != KeyEventType.KeyDown || event.key in ModifierKeys) return
        if (event.isMetaPressed || event.isCtrlPressed || event.isAltPressed) return
        isVisible = true
    }

    /** Notes a pointer going down anywhere, which turns the rings off. */
    fun onPointerPress() {
        isVisible = false
    }
}

/** The keys that only change what another key does. */
private val ModifierKeys: Set<Key> = setOf(
    Key.ShiftLeft,
    Key.ShiftRight,
    Key.CtrlLeft,
    Key.CtrlRight,
    Key.AltLeft,
    Key.AltRight,
    Key.MetaLeft,
    Key.MetaRight,
)

/**
 * The focus visibility the kit's rings read. The default stays on for good, so a control drawn
 * outside `BuilderTheme` rings on any focus as it did before.
 */
internal val LocalFocusVisibility: ProvidableCompositionLocal<FocusVisibility> =
    staticCompositionLocalOf { FocusVisibility() }

/**
 * Feeds [visibility] every key and pointer press under this node, before anything under it sees
 * them, and consumes none of them.
 */
internal fun Modifier.trackFocusVisibility(visibility: FocusVisibility): Modifier =
    onPreviewKeyEvent { event ->
        visibility.onKey(event)
        false
    }.pointerInput(visibility) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) visibility.onPointerPress()
            }
        }
    }

/**
 * Whether this source has focus and [LocalFocusVisibility] says to show it, as a focus ring does.
 * A control without focus never reads the visibility, so a flip of it recomposes only the one
 * control that has focus.
 */
@Composable
internal fun InteractionSource.collectIsFocusVisibleAsState(): State<Boolean> {
    val focused = collectIsFocusedAsState()
    val visibility = LocalFocusVisibility.current
    return remember(focused, visibility) { derivedStateOf { focused.value && visibility.isVisible } }
}
