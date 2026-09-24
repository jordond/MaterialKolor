package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.materialkolor.builder.generated.resources.topbar_library
import com.materialkolor.builder.generated.resources.topbar_library_custom
import com.materialkolor.builder.generated.resources.topbar_library_expressive
import com.materialkolor.builder.generated.resources.topbar_library_fluent
import com.materialkolor.builder.generated.resources.topbar_library_m3
import com.materialkolor.builder.generated.resources.topbar_library_unstyled
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSelect
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import org.jetbrains.compose.resources.stringResource

/**
 * The five libraries the switcher offers. M3 and Expressive share a library and differ only by
 * the document's expressive flag.
 */
internal enum class LibraryChoice(
    val library: Library,
    val expressive: Boolean,
) {
    M3(Library.Material3, expressive = false),
    Expressive(Library.Material3, expressive = true),
    Unstyled(Library.Unstyled, expressive = false),
    Fluent(Library.Fluent, expressive = false),
    Custom(Library.Custom, expressive = false),
    ;

    /** The one edit that moves a document to this choice, library and flag together. */
    val change: DocumentChange
        get() = DocumentChange.SetLibrary(library, expressive)

    companion object {
        /** The choice [document] is on. */
        fun of(document: ThemeDocument): LibraryChoice =
            when (document.library) {
                Library.Material3 -> if (document.expressive) Expressive else M3
                Library.Unstyled -> Unstyled
                Library.Fluent -> Fluent
                Library.Custom -> Custom
            }
    }
}

/**
 * Whether a switch to Expressive should suggest the Expressive style on the 2025 spec, which holds
 * when the style has no 2025 form or the spec is 2021 (F-03).
 */
internal fun suggestsExpressiveStyle(document: ThemeDocument): Boolean =
    EffectiveSpec.of(style = document.style, requested = document.spec) != SpecVersion.Spec2025

/**
 * The library switcher. A segmented row on wide windows and a dropdown on narrower ones.
 *
 * [onSwitch] gets the new choice and where the reveal should grow from, the press that picked it
 * or the middle of the switcher after a keyboard pick. Picking the current choice does nothing.
 *
 * With [selectOnFocus] off, the arrow keys on the segmented row only move focus and Space or Enter
 * picks, so walking past a library does not re-skin the app at every step.
 */
@Composable
internal fun LibrarySwitcher(
    document: ThemeDocument,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    // b-309
    selectOnFocus: Boolean = true,
) {
    val origin = remember { RevealOrigin() }
    val selected = LibraryChoice.of(document)
    val label = stringResource(Res.string.topbar_library)
    val names = LibraryChoice.entries.associateWith { choice -> libraryName(choice) }
    val onSelect = { choice: LibraryChoice ->
        if (choice != selected) onSwitch(choice, origin.take())
    }
    val tracked = modifier.trackRevealOrigin(origin)
    when (LocalLayout.current.windowClass) {
        WindowClass.Expanded -> {
            BuilderSegmented(
                options = LibraryChoice.entries,
                selected = selected,
                onSelect = onSelect,
                label = label,
                modifier = tracked,
                selectOnFocus = selectOnFocus, // b-309
                optionLabel = { choice -> names.getValue(choice) },
            )
        }
        WindowClass.Medium, WindowClass.Compact -> {
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
}

/** What the switcher calls [choice]. The undo and redo buttons name a library switch the same way. */
@Composable
internal fun libraryName(choice: LibraryChoice): String =
    stringResource(
        when (choice) {
            LibraryChoice.M3 -> Res.string.topbar_library_m3
            LibraryChoice.Expressive -> Res.string.topbar_library_expressive
            LibraryChoice.Unstyled -> Res.string.topbar_library_unstyled
            LibraryChoice.Fluent -> Res.string.topbar_library_fluent
            LibraryChoice.Custom -> Res.string.topbar_library_custom
        },
    )

/**
 * Where the last press on the switcher landed. Plain fields, since only a pick reads them and
 * nothing draws from them.
 */
private class RevealOrigin {
    var bounds: Rect = Rect.Zero
    var press: Offset? = null

    /** The press in root coordinates, or the switcher's middle when a key made the pick. */
    fun take(): Offset {
        val local = press
        press = null
        return if (local == null) bounds.center else bounds.topLeft + local
    }
}

/** Keeps [origin] up to date. It watches presses on the way down and consumes none of them. */
private fun Modifier.trackRevealOrigin(origin: RevealOrigin): Modifier =
    onGloballyPositioned { coordinates -> origin.bounds = coordinates.boundsInRoot() }
        .pointerInput(origin) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                origin.press = down.position
            }
        }
