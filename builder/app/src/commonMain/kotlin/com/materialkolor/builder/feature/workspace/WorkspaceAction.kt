package com.materialkolor.builder.feature.workspace

import androidx.compose.ui.geometry.Offset
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.FineTuneRow
import com.materialkolor.builder.domain.persist.MotionOverride
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.PreviewTab
import com.materialkolor.builder.feature.canvas.VisionSimulation
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.kit.control.ToastDuration

/**
 * Everything the workspace can be asked to do.
 *
 * Every feature dispatches one of these, so the list is complete up front and a later feature only
 * handles an action rather than adding one. A reveal action carries the point its circle grows
 * from, in window coordinates, or null to crossfade in place instead.
 */
internal sealed interface WorkspaceAction {
    /** Make [change] to the document straight away, the path every drag and keystroke takes. */
    data class Edit(
        val change: DocumentChange,
        val phase: EditPhase,
    ) : WorkspaceAction

    /** Make [change] as one discrete edit behind a reveal, a library switch or a style chip. */
    data class EditWithReveal(
        val change: DocumentChange,
        val origin: Offset?,
    ) : WorkspaceAction

    /** Step back once. */
    data object Undo : WorkspaceAction

    /** Step forward once. */
    data object Redo : WorkspaceAction

    /** Draw a new seed, and a new style when the style is not locked. */
    data class Shuffle(
        val origin: Offset?,
    ) : WorkspaceAction

    /** Turn one of the shuffle locks on or off. */
    data class SetLock(
        val lock: ShuffleLock,
        val on: Boolean,
    ) : WorkspaceAction

    /** Open the color picker on [target]. */
    data class OpenPicker(
        val target: PickerTarget,
    ) : WorkspaceAction

    /** Ask for an image to take a seed from. */
    data object OpenImagePicker : WorkspaceAction

    /** Show another tab of the preview. */
    data class SetPreviewTab(
        val tab: PreviewTab,
    ) : WorkspaceAction

    // b-217aa

    /**
     * Show the preview light, dark or split, behind a reveal out of [origin] if there is one. It
     * never touches the chrome's appearance.
     */
    data class SetPreviewMode(
        val mode: PreviewMode,
        val origin: Offset?,
    ) : WorkspaceAction

    /** Move the divider of the split preview, from 0 at the start edge to 1 at the end. */
    data class SetSplitFraction(
        val fraction: Float,
    ) : WorkspaceAction

    /** Frame the preview at another device width. */
    data class SetDeviceWidth(
        val width: DeviceWidth,
    ) : WorkspaceAction

    /** Simulate a color vision deficiency over the canvas, or stop. */
    data class SetVision(
        val vision: VisionSimulation,
    ) : WorkspaceAction

    /** Turn the inspect overlay on or off. */
    data class SetInspect(
        val on: Boolean,
    ) : WorkspaceAction

    /** Hide the poster and the top bar, or bring them back. */
    data object ToggleFullscreen : WorkspaceAction

    /** Collapse the poster to its rail, or open it again. */
    data class SetPosterCollapsed(
        val collapsed: Boolean,
    ) : WorkspaceAction

    /** Open or close one of the fine tune rows. */
    data class SetFineTuneRowOpen(
        val row: FineTuneRow,
        val open: Boolean,
    ) : WorkspaceAction

    /** Open [panel] over the workspace. */
    data class OpenPanel(
        val panel: Panel,
    ) : WorkspaceAction

    /** Close whichever panel is open. */
    data object ClosePanel : WorkspaceAction

    /** Change the export options of [target] with [update]. */
    data class SetExportPref(
        val target: ExportTarget,
        val update: (ExportPrefs) -> ExportPrefs,
    ) : WorkspaceAction

    /** Put [text] on the clipboard and say so, naming what it was with [label]. */
    data class CopyText(
        val text: String,
        val label: String,
    ) : WorkspaceAction

    /**
     * Show [message] in a toast for [duration]. A toast with an action names it [actionLabel] and
     * runs [onAction] when it is pressed, and it needs both. Give it [ToastDuration.Long], the time
     * the kit allows for an action such as Undo.
     */
    data class ShowToast(
        val message: String,
        val actionLabel: String? = null,
        val duration: ToastDuration = ToastDuration.Short,
        val onAction: (() -> Unit)? = null,
    ) : WorkspaceAction

    /** Draw the chrome light, dark or as the system does. It never touches the preview mode. */
    data class SetAppearance(
        val appearance: Appearance,
    ) : WorkspaceAction

    /** Let the chrome's motion follow the system, or force it one way. */
    data class SetMotionOverride(
        val motion: MotionOverride,
    ) : WorkspaceAction

    /** Close the hint [id] for good. */
    data class DismissHint(
        val id: String,
    ) : WorkspaceAction

    /** Put the Expressive suggestion away. It changes nothing on its own. */
    data object DismissExpressiveSuggestion : WorkspaceAction
}

/**
 * The panels and popovers that open over the workspace. At most one is open at a time.
 */
internal enum class Panel {
    Export,
    Projects,
    Palette,
    About,
    CheatSheet,
    Picker,
    Explainer,
    Share,
}

/**
 * What a shuffle can be told to leave alone.
 */
internal enum class ShuffleLock {
    /** Keep the seed's hue and vary only its chroma and tone. */
    Hue,

    /** Keep the palette style. */
    Style,

    /** Keep the seed. */
    Seed,
}
