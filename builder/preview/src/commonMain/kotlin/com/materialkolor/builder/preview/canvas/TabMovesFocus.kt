package com.materialkolor.builder.preview.canvas

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Lets Tab leave a multi-line text field the way it leaves every other control in the canvas.
 *
 * A multi-line field types Tab as a character, so someone who reaches one from the keyboard can
 * only get out again backwards, with Shift+Tab. Here Tab moves [focusManager] on to the next control
 * and Shift+Tab back to the one before, and the key is used up both on the way down and on the way
 * up. It has to listen before the field does, since the field takes Tab first. And it acts on the
 * key going down, since that is the only half the web's backing input hands on to Compose. Tab with
 * Ctrl, Alt or Meta held goes on to the field.
 *
 * Every multi-line field in the sample apps and galleries uses this. A single-line field already
 * lets Tab through to the focus system.
 */
internal fun Modifier.tabMovesFocus(focusManager: FocusManager): Modifier =
    onPreviewKeyEvent { event ->
        if (event.key != Key.Tab || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
            return@onPreviewKeyEvent false
        }
        when (event.type) {
            KeyEventType.KeyDown -> {
                focusManager.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                true
            }
            KeyEventType.KeyUp -> {
                true
            }
            else -> {
                false
            }
        }
    }
