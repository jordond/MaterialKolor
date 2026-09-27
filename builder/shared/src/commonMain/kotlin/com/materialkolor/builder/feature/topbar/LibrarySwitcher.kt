package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.topbar_expressive
import com.materialkolor.builder.generated.resources.topbar_library
import com.materialkolor.builder.generated.resources.topbar_library_custom
import com.materialkolor.builder.generated.resources.topbar_library_fluent
import com.materialkolor.builder.generated.resources.topbar_library_m3
import com.materialkolor.builder.generated.resources.topbar_library_m3_expressive
import com.materialkolor.builder.generated.resources.topbar_library_unstyled
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSelect
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import org.jetbrains.compose.resources.stringResource

/**
 * The five choices the switcher offers. M3 Expressive is Material 3 with the Expressive flag on, a
 * choice of its own here while the document keeps it as a flag.
 *
 * @property[library] The library the choice exports for.
 * @property[expressive] Whether the choice carries Material 3's expressive shapes and type.
 */
internal enum class LibraryChoice(
    val library: Library,
    val expressive: Boolean = false,
) {
    M3(Library.Material3),
    M3Expressive(Library.Material3, expressive = true),
    Unstyled(Library.Unstyled),
    Fluent(Library.Fluent),
    Custom(Library.Custom),
    ;

    /**
     * The edit that moves a document to this choice and touches nothing else. A pick from the
     * switcher goes through [libraryPick], which carries the style along onto and off M3 Expressive.
     */
    val change: DocumentChange
        get() = DocumentChange.SetLibrary(library, expressive)

    companion object {
        /**
         * The choice [document] is on.
         */
        fun of(document: ThemeDocument): LibraryChoice = of(document.library, document.expressive)

        /**
         * The choice for [library], M3 Expressive for Material 3 with [expressive] on. Only Material 3
         * reads the flag.
         */
        fun of(
            library: Library,
            expressive: Boolean = false,
        ): LibraryChoice {
            val flag = expressive && library == Library.Material3
            return entries.first { choice -> choice.library == library && choice.expressive == flag }
        }
    }
}

/**
 * The library switcher. A segmented row when [segmented] holds, which by default it does on wide
 * windows, and a dropdown otherwise.
 *
 * [onSwitch] gets the new choice and where the reveal should grow from, the press that picked it
 * or the middle of the switcher after a keyboard pick. Picking the current choice does nothing.
 *
 * The arrow keys on the segmented row only move focus and Space or Enter picks, so walking past a
 * library does not re-theme the previews at every step.
 */
@Composable
internal fun LibrarySwitcher(
    document: ThemeDocument,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    segmented: Boolean = LocalLayout.current.windowClass == WindowClass.Expanded,
) {
    LibrarySwitcher(
        selected = LibraryChoice.of(document),
        onSwitch = onSwitch,
        modifier = modifier,
        segmented = segmented,
    )
}

/**
 * The library switcher on [selected], for callers that hold the choice rather than the document.
 */
@Composable
internal fun LibrarySwitcher(
    selected: LibraryChoice,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    segmented: Boolean = LocalLayout.current.windowClass == WindowClass.Expanded,
) {
    val origin = remember { RevealOrigin() }
    val label = stringResource(Res.string.topbar_library)
    val names = LibraryChoice.entries.associateWith { choice -> libraryName(choice) }
    val onSelect = { choice: LibraryChoice ->
        if (choice != selected) onSwitch(choice, origin.take())
    }
    val tracked = modifier.trackRevealOrigin(origin)
    if (segmented) {
        BuilderSegmented(
            options = LibraryChoice.entries,
            selected = selected,
            onSelect = onSelect,
            label = label,
            modifier = tracked,
            selectOnFocus = false,
            // Each name keeps its own width, so M3 Expressive does not widen the four short ones.
            equalWidths = false,
            optionLabel = { choice -> names.getValue(choice) },
        )
    } else {
        BuilderSelect(
            label = label,
            options = LibraryChoice.entries,
            selected = selected,
            onSelect = onSelect,
            modifier = tracked,
            optionLabel = { choice -> names.getValue(choice) },
        )
    }
}

/**
 * The switch under the export sheet's Material 3 card that moves between M3 and M3 Expressive,
 * which re-themes the previews like a library switch does.
 *
 * @param[onCheckedChange] Gets the new state and where the reveal grows from, the press that
 *   flipped it or the middle of the switch after a key.
 * @param[caption] A quieter line under the label, or null for none.
 */
@Composable
internal fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (on: Boolean, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val origin = remember { RevealOrigin() }
    BuilderSwitch(
        checked = checked,
        onCheckedChange = { on -> onCheckedChange(on, origin.take()) },
        label = stringResource(Res.string.topbar_expressive),
        // Its own width, even in a row with no end such as the phone's scrolling chips, where the
        // switch's label would otherwise get no room.
        modifier = modifier.width(IntrinsicSize.Max).trackRevealOrigin(origin),
        caption = caption,
    )
}

/**
 * What the switcher calls [choice].
 */
@Composable
internal fun libraryName(choice: LibraryChoice): String =
    stringResource(
        when (choice) {
            LibraryChoice.M3 -> Res.string.topbar_library_m3
            LibraryChoice.M3Expressive -> Res.string.topbar_library_m3_expressive
            LibraryChoice.Unstyled -> Res.string.topbar_library_unstyled
            LibraryChoice.Fluent -> Res.string.topbar_library_fluent
            LibraryChoice.Custom -> Res.string.topbar_library_custom
        },
    )

/**
 * What the undo and redo buttons call [library], naming Material 3 with Expressive on as M3
 * Expressive.
 */
@Composable
internal fun libraryName(
    library: Library,
    expressive: Boolean,
): String = libraryName(LibraryChoice.of(library, expressive))

/**
 * Where the last press on the switcher landed. Plain fields, since only a pick reads them and
 * nothing draws from them. The phone's chip row keeps one too.
 */
internal class RevealOrigin {
    var bounds: Rect = Rect.Zero
    var press: Offset? = null

    /**
     * The press in root coordinates, or the switcher's middle when a key made the pick.
     */
    fun take(): Offset {
        val local = press
        press = null
        return if (local == null) bounds.center else bounds.topLeft + local
    }
}

/**
 * Keeps [origin] up to date. It watches presses on the way down and consumes none of them.
 */
internal fun Modifier.trackRevealOrigin(origin: RevealOrigin): Modifier =
    onGloballyPositioned { coordinates -> origin.bounds = coordinates.boundsInRoot() }
        .pointerInput(origin) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                origin.press = down.position
            }
        }
