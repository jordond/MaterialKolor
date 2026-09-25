package com.materialkolor.builder.feature.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.edit.PinMode
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.poster.keyColorName
import com.materialkolor.builder.feature.poster.readoutName
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.picker_title_accent
import com.materialkolor.builder.generated.resources.picker_title_cmf
import com.materialkolor.builder.generated.resources.picker_title_key_color
import com.materialkolor.builder.generated.resources.picker_title_pin_dark
import com.materialkolor.builder.generated.resources.picker_title_pin_light
import com.materialkolor.builder.generated.resources.picker_title_seed
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * The color picker, open while `state.panel` is `Panel.Picker` on `state.pickerTarget` (F-06).
 *
 * It holds the picker's session and sends its edits through [dispatcher], so the workspace keeps
 * the document, the history and the capabilities in step. Whatever closes the picker other than
 * Done puts the value back, see [PickerSession].
 *
 * A switch to another project does not close it. The picker stays open and starts a fresh session
 * on the new project's value, and the old value is not put back since that document is gone. No
 * path in the UI switches projects with the picker open today, as every switch starts from
 * Projects or Share and opening either one closes the picker first.
 *
 * @param[returnFocusTo] The Pick button that opened the picker, which gets focus back once it closes
 * while it is still on screen (AR-09).
 */
@Composable
internal fun PickerHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null, // b-307
    model: PickerModel = metroViewModel(),
) {
    val session = remember { PickerSession() }
    val open = state.pickerTarget.takeIf { state.panel == Panel.Picker }
    SideEffect {
        session.sync(open, state.document, state.projectGeneration, state.capabilities).forEach(dispatcher::dispatch)
    }
    // The dialog keeps the last target's title and color while it fades out.
    val kept = remember { KeptTarget() }
    if (open != null) kept.target = open
    val target = kept.target ?: return
    val environment = model.environment
    ColorPickerDialog(
        visible = open != null,
        title = titleOf(target, state.document),
        value = target.shownIn(state.document, LocalThemeResult.current),
        onPick = { argb, fromScreen -> session.pick(argb, fromScreen)?.let(dispatcher::dispatch) },
        onDone = { session.done().forEach(dispatcher::dispatch) },
        onCancel = { session.cancel().forEach(dispatcher::dispatch) },
        modifier = modifier,
        pickScreenColor = if (environment.eyeDropperAvailable) environment::pickScreenColor else null,
        returnFocusTo = PickButtons.returnFocusFor(returnFocusTo),
    )
}

/**
 * The target the picker showed last, held past its close.
 */
private class KeptTarget {
    var target: PickerTarget? = null
}

/**
 * What the picker on [target] is called, the role as the readout names it or the accent by name.
 */
@Composable
private fun titleOf(
    target: PickerTarget,
    document: ThemeDocument,
): String =
    when (target) {
        PickerTarget.Seed -> {
            stringResource(Res.string.picker_title_seed)
        }
        is PickerTarget.KeyColorOverride -> {
            stringResource(Res.string.picker_title_key_color, stringResource(keyColorName(target.slot)))
        }
        is PickerTarget.Pin -> {
            val name = ColorRef.OfRole(target.role).readoutName(document)
            val light = target.mode == PinMode.Light
            stringResource(if (light) Res.string.picker_title_pin_light else Res.string.picker_title_pin_dark, name)
        }
        is PickerTarget.Accent -> {
            val accent = document.accents.getOrNull(target.index)
            stringResource(Res.string.picker_title_accent, accent?.name.orEmpty())
        }
        PickerTarget.CmfSeed -> {
            stringResource(Res.string.picker_title_cmf)
        }
    }
