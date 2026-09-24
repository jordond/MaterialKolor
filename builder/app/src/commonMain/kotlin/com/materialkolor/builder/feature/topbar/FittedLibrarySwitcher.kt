package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass

// b-231

/** The top bar as a whole, for tests that check how its controls sit. */
internal const val TOP_BAR_TAG: String = "top-bar"

/** The library switcher in the top bar, in either form. */
internal const val LIBRARY_SWITCHER_TAG: String = "top-bar-library-switcher"

/**
 * The library switcher in the width the top bar's actions leave it.
 *
 * Where the window is wide enough to ask for the segmented row, the row is measured first, off
 * screen and out of the accessibility tree, and shown only when it fits. Everywhere else the
 * dropdown shows, and a dropdown wider than the room it gets is narrowed to fit.
 *
 * @param[modifier] Applied to the room the switcher gets, which it fills.
 * @param[switcherModifier] Applied to the switcher itself, in whichever form it shows.
 */
@Composable
internal fun FittedLibrarySwitcher(
    document: ThemeDocument,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    switcherModifier: Modifier = Modifier,
) {
    val wide = LocalLayout.current.windowClass == WindowClass.Expanded
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val segmented = wide && fitsSegmented(document, loose)
        val form = if (segmented) SwitcherForm.Segmented else SwitcherForm.Dropdown
        val shown = subcompose(form) {
            LibrarySwitcher(
                document = document,
                onSwitch = onSwitch,
                modifier = switcherModifier,
                segmented = segmented,
            )
        }.map { measurable -> measurable.measure(loose) }
        val natural = shown.maxOfOrNull { placeable -> placeable.width } ?: 0
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else natural
        val height = maxOf(constraints.minHeight, shown.maxOfOrNull { placeable -> placeable.height } ?: 0)
        layout(width, height) {
            shown.forEach { placeable -> placeable.placeRelative(0, (height - placeable.height) / 2) }
        }
    }
}

/**
 * Whether the segmented row fits in [constraints] at its own width. The row measured here is never
 * placed and says nothing to assistive technology, so only the one that shows can be reached.
 */
private fun SubcomposeMeasureScope.fitsSegmented(
    document: ThemeDocument,
    constraints: Constraints,
): Boolean {
    val probe = subcompose(SwitcherForm.Probe) {
        LibrarySwitcher(
            document = document,
            onSwitch = { _, _ -> },
            modifier = Modifier.clearAndSetSemantics {},
            segmented = true,
        )
    }
    val natural = probe.maxOfOrNull { measurable ->
        measurable.measure(constraints.copy(maxWidth = Constraints.Infinity)).width
    } ?: 0
    return !constraints.hasBoundedWidth || natural <= constraints.maxWidth
}

/** The switcher's slots, the form that shows and the segmented row measured to see if it fits. */
private enum class SwitcherForm {
    Segmented,
    Dropdown,
    Probe,
}
