package com.materialkolor.builder.feature.poster

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager

// b-306c

/**
 * Moves the focus to [target] ahead of a button that is about to remove itself, while the keyboard
 * is in use, so the user carries on from somewhere close by. A button that goes while it holds the
 * focus clears it, so this has to run before the change that removes it. A pointer leaves the focus
 * alone.
 */
internal fun InputModeManager.handFocusTo(target: FocusRequester) {
    if (inputMode == InputMode.Keyboard) target.requestFocus()
}
