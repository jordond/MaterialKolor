package com.materialkolor.builder.feature.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.picker_cancel
import com.materialkolor.builder.generated.resources.picker_done
import com.materialkolor.builder.generated.resources.picker_eyedropper
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.widget.HctPicker
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The color picker in a dialog, the HCT tracks with Pick from screen under them where the browser
 * has an eyedropper, then Cancel and Done (F-06, F-07).
 *
 * Every color the tracks or the eyedropper reach goes to [onPick], whatever the gesture, and the
 * caller files them as one session. Esc and a click on the veil cancel, except while the browser's
 * own eyedropper is up, since the Esc that closes it belongs to it. A cancelled eyedropper changes
 * nothing.
 *
 * @param[value] The color the picker shows.
 * @param[onPick] Called with each new color, and whether it came off the screen.
 * @param[pickScreenColor] Picks a color off the screen, null on a cancel, or null itself where
 * there is no eyedropper. The button starts it inside its click, as the browser asks.
 * @param[returnFocusTo] The Pick button that opened the picker.
 */
@Composable
internal fun ColorPickerDialog(
    visible: Boolean,
    title: String,
    value: Argb,
    onPick: (argb: Argb, fromScreen: Boolean) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    pickScreenColor: (suspend () -> Argb?)? = null,
    returnFocusTo: FocusRequester? = null,
) {
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }
    BuilderDialog(
        visible = visible,
        onDismissRequest = { if (!picking) onCancel() },
        title = title,
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        actions = {
            BuilderButton(onClick = onCancel, label = stringResource(Res.string.picker_cancel))
            BuilderButton(onClick = onDone, label = stringResource(Res.string.picker_done), emphasis = Emphasis.Primary)
        },
    ) {
        HctPicker(value = value, onChange = { argb, _ -> onPick(argb, false) })
        if (pickScreenColor != null) {
            BuilderButton(
                onClick = {
                    // The browser opens its eyedropper only inside the click, so the call is the first suspension.
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        picking = true
                        try {
                            pickScreenColor()?.let { argb -> onPick(argb, true) }
                            // The Esc that closed the eyedropper reaches the page too. Let it pass first.
                            withFrameNanos { }
                        } finally {
                            picking = false
                        }
                    }
                },
                label = stringResource(Res.string.picker_eyedropper),
                icon = IconId.Eyedropper,
            )
        }
    }
}
