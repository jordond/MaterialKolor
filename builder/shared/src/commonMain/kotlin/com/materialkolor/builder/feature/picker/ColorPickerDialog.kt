package com.materialkolor.builder.feature.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.picker_cancel
import com.materialkolor.builder.generated.resources.picker_done
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderSheet
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.SheetPresentation
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.HctPicker
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The color picker, the band in the live color over the HCT picker, then Cancel and Done with Done
 * filled in the picked color. A dialog on wider windows, and on a phone a full screen sheet with the
 * band as its top and the buttons as its footer.
 *
 * Every color the picker, the eyedropper or Was reach goes to the caller, whatever the gesture, and
 * the caller files them as one session. Esc and a click on the veil cancel, except while the
 * browser's own eyedropper is up, since the Esc that closes it belongs to it. A cancelled eyedropper
 * changes nothing.
 *
 * The picker body gets a bounded height and scrolls itself when even its smallest plane does not
 * fit, so the buttons never leave the screen.
 *
 * @param[value] The color the picker shows.
 * @param[was] The color the target had when the picker opened, which Was shows.
 * @param[wasFromSeed] Whether [was] came from the seed, the target storing no color of its own.
 * @param[onPick] Called with each new color, and whether it came off the screen.
 * @param[onGoBack] Called when Was is pressed, to go back to [was] and keep the picker open.
 * @param[pickScreenColor] Picks a color off the screen, null on a cancel, or null itself where
 * there is no eyedropper. The button starts it inside its click, as the browser asks.
 * @param[returnFocusTo] The Pick button that opened the picker.
 */
@Composable
internal fun ColorPickerDialog(
    visible: Boolean,
    title: String,
    value: Argb,
    was: Argb,
    wasFromSeed: Boolean,
    onPick: (argb: Argb, fromScreen: Boolean) -> Unit,
    onGoBack: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    pickScreenColor: (suspend () -> Argb?)? = null,
    returnFocusTo: FocusRequester? = null,
) {
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }
    val onPickScreen = pickScreenColor?.let { pick ->
        {
            // The browser opens its eyedropper only inside the click, so the call is the first suspension.
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                picking = true
                try {
                    pick()?.let { argb -> onPick(argb, true) }
                    // The Esc that closed the eyedropper reaches the page too. Let it pass first.
                    withFrameNanos { }
                } finally {
                    picking = false
                }
            }
            Unit
        }
    }
    val compact = LocalLayout.current.windowClass == WindowClass.Compact
    val hero: @Composable () -> Unit = {
        PickerHero(
            title = title,
            color = value,
            was = was,
            wasFromSeed = wasFromSeed,
            onGoBack = onGoBack,
            onPickScreen = onPickScreen,
            compact = compact,
        )
    }
    val onDismissRequest = { if (!picking) onCancel() }
    val cancelLabel = stringResource(Res.string.picker_cancel)
    val doneLabel = stringResource(Res.string.picker_done)
    val fill = value.toColor()
    if (compact) {
        BuilderSheet(
            visible = visible,
            onDismissRequest = onDismissRequest,
            title = title,
            presentation = SheetPresentation.FullScreen,
            modifier = modifier,
            returnFocusTo = returnFocusTo,
            hero = hero,
            footer = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.medium),
                ) {
                    BuilderButton(onClick = onCancel, label = cancelLabel, modifier = Modifier.weight(1f))
                    BuilderButton(
                        onClick = onDone,
                        label = doneLabel,
                        modifier = Modifier.weight(1f),
                        emphasis = Emphasis.Primary,
                        fill = fill,
                    )
                }
            },
        ) { HctPicker(value = value, onChange = { argb, _ -> onPick(argb, false) }) }
        return
    }
    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = title,
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        maxWidth = DialogMaxWidth,
        actions = {
            BuilderButton(onClick = onCancel, label = cancelLabel)
            BuilderButton(onClick = onDone, label = doneLabel, emphasis = Emphasis.Primary, fill = fill)
        },
        hero = hero,
    ) { HctPicker(value = value, onChange = { argb, _ -> onPick(argb, false) }) }
}

/**
 * The widest the dialog grows, room for the plane beside the tiles.
 */
private val DialogMaxWidth = 860.dp
