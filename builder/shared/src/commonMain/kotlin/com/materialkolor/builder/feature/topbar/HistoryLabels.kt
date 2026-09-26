package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.domain.edit.ChangeKind
import com.materialkolor.builder.domain.edit.ChangeLabel
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
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

/**
 * What the undo button says, naming the change it would take back ("Undo library change to
 * M3 Expressive"). [document] is the one on screen, which is where that change landed.
 */
@Composable
internal fun undoText(
    history: HistoryState,
    document: ThemeDocument,
): String {
    val label = history.undoLabel ?: return stringResource(Res.string.workspace_undo)
    val landing = label.detail?.let { detail -> undoneLanding(detail, document) }
    return stringResource(Res.string.topbar_undo_change, changeText(label, landing))
}

/**
 * What the redo button says, naming the change it would bring back. [document] is the one on
 * screen, which is where that change started.
 */
@Composable
internal fun redoText(
    history: HistoryState,
    document: ThemeDocument,
): String {
    val label = history.redoLabel ?: return stringResource(Res.string.workspace_redo)
    val landing = label.detail?.let { detail -> redoneLanding(detail, document) }
    return stringResource(Res.string.topbar_redo_change, changeText(label, landing))
}

/**
 * What the History list calls the step [entry], such as "Library change to Fluent". The value named
 * is the one the step landed on, read from its own result and never from the document on screen, so
 * a step reads the same wherever the history is. The first letter goes upper case to start the
 * line, which is right for English and may not be for every language a translation brings.
 */
@Composable
internal fun stepText(entry: HistoryEntry): String {
    val landing = entry.label.detail?.let { detail -> undoneLanding(detail, entry.after) }
    return changeText(entry.label, landing).replaceFirstChar { it.titlecase() }
}

/**
 * The change [label] describes, with the value it landed on when there is a name for it. A
 * library reads as the switcher names it, [library] when that is known, with M3 Expressive for
 * Material 3 with the Expressive chip on. A seed keeps its hex and a rename its name. Every other
 * detail is a raw key or names where the change went, a role or a slot, so it is left out.
 */
@Composable
private fun changeText(
    label: ChangeLabel,
    library: LibraryLanding?,
): String {
    val (kind, shown) = kindText(label.kind)
    val text = stringResource(kind)
    val detail = when (shown) {
        Detail.Hidden -> null
        Detail.AsIs -> label.detail
        Detail.LibraryName -> library?.let { landing -> libraryName(landing.library, landing.expressive) }
    }
    return if (detail == null) text else stringResource(Res.string.topbar_change_to, text, detail)
}

/**
 * How a change's detail shows after its name.
 */
private enum class Detail {
    /**
     * Left out.
     */
    Hidden,

    /**
     * Shown as it is, a hex or a name someone typed.
     */
    AsIs,

    /**
     * Shown as the library switcher and the Expressive chip name it.
     */
    LibraryName,
}

/**
 * The library and Expressive flag a step lands on, named the way the switcher names them.
 */
private data class LibraryLanding(
    val library: Library,
    val expressive: Boolean,
)

/**
 * The library and flag an undo takes back. The step landed on [document], so its flag is the one
 * the step set. Null when [document] is on another library than [detail] names.
 */
private fun undoneLanding(
    detail: String,
    document: ThemeDocument,
): LibraryLanding? =
    LibraryLanding(document.library, document.expressive).takeIf { landing -> landing.library.name == detail }

/**
 * The library and flag a redo brings back. The step starts from [document] and never lands where
 * it started, so a redo onto Material 3 from Material 3 flips the expressive flag. A step onto
 * Material 3 from another library may come from before Expressive was a flag, when it could land
 * on either, so it names nothing. Every other library lands with the flag off.
 */
private fun redoneLanding(
    detail: String,
    document: ThemeDocument,
): LibraryLanding? {
    val library = Library.entries.firstOrNull { library -> library.name == detail } ?: return null
    return when {
        library != Library.Material3 -> LibraryLanding(library, expressive = false)
        document.library == Library.Material3 -> LibraryLanding(library, expressive = !document.expressive)
        else -> null
    }
}

/**
 * The string for [kind], and how its detail shows.
 */
private fun kindText(kind: ChangeKind): Pair<StringResource, Detail> =
    when (kind) {
        ChangeKind.Seed -> Res.string.topbar_change_seed to Detail.AsIs
        ChangeKind.Preset -> Res.string.topbar_change_preset to Detail.Hidden
        ChangeKind.KeyColor -> Res.string.topbar_change_key_color to Detail.Hidden
        ChangeKind.ResetKeyColors -> Res.string.topbar_change_reset_key_colors to Detail.Hidden
        ChangeKind.Style -> Res.string.topbar_change_style to Detail.Hidden
        ChangeKind.CmfSeed -> Res.string.topbar_change_cmf_seed to Detail.AsIs
        ChangeKind.Contrast -> Res.string.topbar_change_contrast to Detail.Hidden
        ChangeKind.Spec -> Res.string.topbar_change_spec to Detail.Hidden
        ChangeKind.Platform -> Res.string.topbar_change_platform to Detail.Hidden
        ChangeKind.Amoled -> Res.string.topbar_change_amoled to Detail.Hidden
        ChangeKind.AddAccent -> Res.string.topbar_change_add_accent to Detail.Hidden
        ChangeKind.UpdateAccent -> Res.string.topbar_change_update_accent to Detail.Hidden
        ChangeKind.RemoveAccent -> Res.string.topbar_change_remove_accent to Detail.Hidden
        ChangeKind.Pin -> Res.string.topbar_change_pin to Detail.Hidden
        ChangeKind.ClearPins -> Res.string.topbar_change_clear_pins to Detail.Hidden
        ChangeKind.Library -> Res.string.topbar_change_library to Detail.LibraryName
        ChangeKind.MotionScheme -> Res.string.topbar_change_motion_scheme to Detail.Hidden
        ChangeKind.ThemeName -> Res.string.topbar_change_theme_name to Detail.AsIs
        ChangeKind.CustomTone -> Res.string.topbar_change_custom_tone to Detail.Hidden
        ChangeKind.Replace -> Res.string.topbar_change_replace to Detail.Hidden
    }
