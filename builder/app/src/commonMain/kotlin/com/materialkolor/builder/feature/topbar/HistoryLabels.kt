package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.topbar_change_add_accent
import com.materialkolor.builder.generated.resources.topbar_change_amoled
import com.materialkolor.builder.generated.resources.topbar_change_clear_pins
import com.materialkolor.builder.generated.resources.topbar_change_cmf_seed
import com.materialkolor.builder.generated.resources.topbar_change_contrast
import com.materialkolor.builder.generated.resources.topbar_change_custom_tone
import com.materialkolor.builder.generated.resources.topbar_change_key_color
import com.materialkolor.builder.generated.resources.topbar_change_library
import com.materialkolor.builder.generated.resources.topbar_change_motion_scheme
import com.materialkolor.builder.generated.resources.topbar_change_pin
import com.materialkolor.builder.generated.resources.topbar_change_platform
import com.materialkolor.builder.generated.resources.topbar_change_preset
import com.materialkolor.builder.generated.resources.topbar_change_remove_accent
import com.materialkolor.builder.generated.resources.topbar_change_replace
import com.materialkolor.builder.generated.resources.topbar_change_reset_key_colors
import com.materialkolor.builder.generated.resources.topbar_change_seed
import com.materialkolor.builder.generated.resources.topbar_change_spec
import com.materialkolor.builder.generated.resources.topbar_change_style
import com.materialkolor.builder.generated.resources.topbar_change_theme_name
import com.materialkolor.builder.generated.resources.topbar_change_to
import com.materialkolor.builder.generated.resources.topbar_change_update_accent
import com.materialkolor.builder.generated.resources.topbar_redo_change
import com.materialkolor.builder.generated.resources.topbar_undo_change
import com.materialkolor.builder.generated.resources.workspace_redo
import com.materialkolor.builder.generated.resources.workspace_undo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What the undo button says, naming the change it would take back ("Undo style change to Vibrant"). */
@Composable
internal fun undoText(history: HistoryState): String {
    val label = history.undoLabel ?: return stringResource(Res.string.workspace_undo)
    return stringResource(Res.string.topbar_undo_change, changeText(label))
}

/** What the redo button says, naming the change it would bring back. */
@Composable
internal fun redoText(history: HistoryState): String {
    val label = history.redoLabel ?: return stringResource(Res.string.workspace_redo)
    return stringResource(Res.string.topbar_redo_change, changeText(label))
}

/**
 * The change [label] describes, with the value it landed on when there is one. A detail that
 * names what was changed rather than where it went, a role or a slot, is left out.
 */
@Composable
private fun changeText(label: ChangeLabel): String {
    val (kind, namesValue) = kindText(label.kind)
    val text = stringResource(kind)
    val detail = label.detail
    return if (namesValue && detail != null) stringResource(Res.string.topbar_change_to, text, detail) else text
}

/** The string for [kind], and whether its detail is the value the change landed on. */
private fun kindText(kind: ChangeKind): Pair<StringResource, Boolean> =
    when (kind) {
        ChangeKind.Seed -> Res.string.topbar_change_seed to true
        ChangeKind.Preset -> Res.string.topbar_change_preset to true
        ChangeKind.KeyColor -> Res.string.topbar_change_key_color to false
        ChangeKind.ResetKeyColors -> Res.string.topbar_change_reset_key_colors to false
        ChangeKind.Style -> Res.string.topbar_change_style to true
        ChangeKind.CmfSeed -> Res.string.topbar_change_cmf_seed to true
        ChangeKind.Contrast -> Res.string.topbar_change_contrast to false
        ChangeKind.Spec -> Res.string.topbar_change_spec to true
        ChangeKind.Platform -> Res.string.topbar_change_platform to true
        ChangeKind.Amoled -> Res.string.topbar_change_amoled to false
        ChangeKind.AddAccent -> Res.string.topbar_change_add_accent to false
        ChangeKind.UpdateAccent -> Res.string.topbar_change_update_accent to false
        ChangeKind.RemoveAccent -> Res.string.topbar_change_remove_accent to false
        ChangeKind.Pin -> Res.string.topbar_change_pin to false
        ChangeKind.ClearPins -> Res.string.topbar_change_clear_pins to false
        ChangeKind.Library -> Res.string.topbar_change_library to true
        ChangeKind.MotionScheme -> Res.string.topbar_change_motion_scheme to true
        ChangeKind.ThemeName -> Res.string.topbar_change_theme_name to true
        ChangeKind.CustomTone -> Res.string.topbar_change_custom_tone to false
        ChangeKind.Replace -> Res.string.topbar_change_replace to false
    }
