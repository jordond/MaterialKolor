package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SpecVersion
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
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * The four libraries the switcher offers. Expressive is not one of them but a flag on Material 3,
 * which the Expressive switch beside the switcher turns on and off.
 */
internal enum class LibraryChoice(
    val library: Library,
) {
    M3(Library.Material3),
    Unstyled(Library.Unstyled),
    Fluent(Library.Fluent),
    Custom(Library.Custom),
    ;

    /**
     * The one edit that moves a document to this choice, with the Expressive flag off. M3 lands on
     * plain Material 3, and the Expressive switch takes it from there.
     */
    val change: DocumentChange
        get() = DocumentChange.SetLibrary(library, expressive = false)

    companion object {
        /**
         * The choice [document] is on.
         */
        fun of(document: ThemeDocument): LibraryChoice = of(document.library)

        /**
         * The choice for [library].
         */
        fun of(library: Library): LibraryChoice = entries.first { choice -> choice.library == library }
    }
}

/**
 * Whether [document] is on Material 3 with the Expressive switch on.
 */
internal val ThemeDocument.onExpressive: Boolean
    get() = library == Library.Material3 && expressive

/**
 * The one edit the Expressive switch makes, Material 3 with the flag set to [on].
 */
internal fun expressiveChange(on: Boolean): DocumentChange = DocumentChange.SetLibrary(Library.Material3, on)

/**
 * Whether a switch to Expressive should suggest the Expressive style on the 2025 spec, which holds
 * when the style has no 2025 form or the spec is 2021.
 */
internal fun suggestsExpressiveStyle(document: ThemeDocument): Boolean =
    EffectiveSpec.of(style = document.style, requested = document.spec) != SpecVersion.Spec2025

/**
 * The library switcher. A segmented row when [segmented] holds, which by default it does on wide
 * windows, and a dropdown otherwise. While Material 3 is picked the Expressive switch sits after it.
 *
 * [onSwitch] gets the new choice and where the reveal should grow from, the press that picked it
 * or the middle of the switcher after a keyboard pick. Picking the current choice does nothing.
 * [onExpressiveChange] gets the switch's new state and where its reveal grows from the same way.
 *
 * The arrow keys on the segmented row only move focus and Space or Enter picks, so walking past a
 * library does not re-theme the previews at every step.
 */
@Composable
internal fun LibrarySwitcher(
    document: ThemeDocument,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    onExpressiveChange: (on: Boolean, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    segmented: Boolean = LocalLayout.current.windowClass == WindowClass.Expanded,
) {
    LibrarySwitcher(
        selected = LibraryChoice.of(document),
        expressive = document.expressive,
        onSwitch = onSwitch,
        onExpressiveChange = onExpressiveChange,
        modifier = modifier,
        segmented = segmented,
    )
}

/**
 * The library switcher on [selected], for callers that hold the choice rather than the document.
 *
 * @param[expressive] Whether the Expressive switch is on.
 * @param[switcherModifier] Applied to the library control itself, in whichever form it shows.
 * @param[expressiveModifier] Applied to the Expressive switch.
 * @param[expressiveShown] Whether the Expressive switch shows, by default only on Material 3. The
 *   off screen measures pass true, so the switcher keeps its room whichever library is picked.
 */
@Composable
internal fun LibrarySwitcher(
    selected: LibraryChoice,
    expressive: Boolean,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    onExpressiveChange: (on: Boolean, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    switcherModifier: Modifier = Modifier,
    expressiveModifier: Modifier = Modifier,
    segmented: Boolean = LocalLayout.current.windowClass == WindowClass.Expanded,
    expressiveShown: Boolean = selected == LibraryChoice.M3,
) {
    val origin = remember { RevealOrigin() }
    val label = stringResource(Res.string.topbar_library)
    val names = LibraryChoice.entries.associateWith { choice -> libraryName(choice) }
    val onSelect = { choice: LibraryChoice ->
        if (choice != selected) onSwitch(choice, origin.take())
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The switch keeps its whole width and the library control takes what is left, so a Medium
        // bar short of room narrows the dropdown rather than cutting the switch's name.
        val tracked = Modifier.weight(1f, fill = false).then(switcherModifier.trackRevealOrigin(origin))
        if (segmented) {
            BuilderSegmented(
                options = LibraryChoice.entries,
                selected = selected,
                onSelect = onSelect,
                label = label,
                modifier = tracked,
                selectOnFocus = false,
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
        if (expressiveShown) {
            ExpressiveSwitch(
                checked = expressive,
                onCheckedChange = onExpressiveChange,
                modifier = expressiveModifier,
            )
        }
    }
}

/**
 * The switch that turns Material 3's Expressive flavor on and off, which re-themes the previews like
 * a library switch does.
 *
 * @param[onCheckedChange] Gets the new state and where the reveal grows from, the press that
 *   flipped it or the middle of the switch after a key.
 */
@Composable
internal fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (on: Boolean, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val origin = remember { RevealOrigin() }
    BuilderSwitch(
        checked = checked,
        onCheckedChange = { on -> onCheckedChange(on, origin.take()) },
        label = stringResource(Res.string.topbar_expressive),
        // Its own width, even in a row with no end such as the phone's scrolling chips, where the
        // switch's label would otherwise get no room.
        modifier = modifier.width(IntrinsicSize.Max).trackRevealOrigin(origin),
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
            LibraryChoice.Unstyled -> Res.string.topbar_library_unstyled
            LibraryChoice.Fluent -> Res.string.topbar_library_fluent
            LibraryChoice.Custom -> Res.string.topbar_library_custom
        },
    )

/**
 * What the undo and redo buttons call [library], naming Material 3 with the Expressive switch on
 * as M3 Expressive.
 */
@Composable
internal fun libraryName(
    library: Library,
    expressive: Boolean,
): String =
    if (library == Library.Material3 && expressive) {
        stringResource(Res.string.topbar_library_m3_expressive)
    } else {
        libraryName(LibraryChoice.of(library))
    }

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
