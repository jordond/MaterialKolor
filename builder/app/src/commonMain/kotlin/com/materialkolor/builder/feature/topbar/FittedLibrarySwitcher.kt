package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.BuilderType
import com.materialkolor.builder.kit.token.LocalBuilderType

// b-231

/** The top bar as a whole, for tests that check how its controls sit. */
internal const val TOP_BAR_TAG: String = "top-bar"

/** The library switcher in the top bar, in either form. */
internal const val LIBRARY_SWITCHER_TAG: String = "top-bar-library-switcher"

/**
 * Told each time the switcher measures its segmented row to see if it fits, or null, which it
 * always is outside tests. Tests provide it to prove an edit that leaves the row alone skips it.
 */
internal val LocalSwitcherFitProbe: ProvidableCompositionLocal<(() -> Unit)?> =
    staticCompositionLocalOf { null }

// b-315d

/**
 * The form the library switcher shows, which the command registry reads to say where each library
 * sits (P6), the segmented row or a row of the dropdown.
 */
@Stable
internal class SwitcherFormState {
    /** True for the segmented row, false for the dropdown, or null before the switcher has shown. */
    var segmented: Boolean? by mutableStateOf(null)

    // b-406

    /** The top bar buttons the bar has moved into its overflow menu, all three on a phone. */
    var overflowed: Set<TopBarControl> by mutableStateOf(emptySet())

    // b-503a

    /**
     * The middle of the switcher where it was last placed, in root coordinates, or null before it has
     * been. A library key reveals the new skin from here (flow 5.3). A plain field, since only a
     * shortcut reads it and nothing draws from it.
     */
    var origin: Offset? = null
}

// b-503a

/** Keeps [report]'s origin on the middle of the switcher this goes on. Nothing when [report] is null. */
internal fun Modifier.reportSwitcherOrigin(report: SwitcherFormState?): Modifier =
    if (report == null) {
        this
    } else {
        onGloballyPositioned { coordinates -> report.origin = coordinates.boundsInRoot().center }
    }

/** Where the switcher reports its form, or null where nothing reads it. */
internal val LocalSwitcherForm: ProvidableCompositionLocal<SwitcherFormState?> =
    staticCompositionLocalOf { null }

/**
 * The library switcher in the width the top bar's actions leave it.
 *
 * Where the window is wide enough to ask for the segmented row, the row is measured first, off
 * screen and out of the accessibility tree, and shown only when it fits. Everywhere else the
 * dropdown shows, and a dropdown wider than the room it gets is narrowed to fit. The row is only
 * measured again when something that sets its width changes, so an edit that keeps the library
 * costs the switcher nothing. The form it shows goes to [LocalSwitcherForm] for the command registry.
 *
 * @param[selected] The library the document is on.
 * @param[modifier] Applied to the room the switcher gets, which it fills.
 * @param[switcherModifier] Applied to the switcher itself, in whichever form it shows.
 * @param[onRefit] Called once the switcher has changed form, after the old form has gone, so focus
 *   it held can move to the new one.
 */
@Composable
internal fun FittedLibrarySwitcher(
    selected: LibraryChoice,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    switcherModifier: Modifier = Modifier,
    onRefit: () -> Unit = {},
) {
    val wide = LocalLayout.current.windowClass == WindowClass.Expanded
    val skin = LocalSkin.current
    val type = LocalBuilderType.current
    val labels = LibraryChoice.entries.map { choice -> libraryName(choice) }
    val probe = LocalSwitcherFitProbe.current
    val report = LocalSwitcherForm.current // b-315d
    val fit = remember { SwitcherFit() }
    SubcomposeLayout(modifier) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val key = FitKey(loose, selected, skin, type, labels, density, fontScale)
        val segmented = wide &&
            fit.fits(key) {
                probe?.invoke()
                fitsSegmented(selected, loose)
            }
        val form = if (segmented) SwitcherForm.Segmented else SwitcherForm.Dropdown
        val refit = fit.shown != null && fit.shown != form
        fit.shown = form
        val shown = subcompose(form) {
            // A form's slot composes afresh each time the switcher changes to it.
            val changedForm = remember { refit }
            if (changedForm) LaunchedEffect(Unit) { onRefit() }
            if (report != null) SideEffect { report.segmented = segmented } // b-315d
            LibrarySwitcher(
                selected = selected,
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
    selected: LibraryChoice,
    constraints: Constraints,
): Boolean {
    val probe = subcompose(SwitcherForm.Probe) {
        LibrarySwitcher(
            selected = selected,
            onSwitch = { _, _ -> },
            modifier = Modifier.clearAndSetSemantics {},
            segmented = true,
        )
    }
    val natural = probe.maxOfOrNull { measurable ->
        // b-509
        // A row that shares its width out evenly, as Fluent's does, needs its widest option's room for
        // every option. Its intrinsic width counts that, and a measure with no end to the room does not.
        val shared = measurable.maxIntrinsicWidth(constraints.maxHeight)
        maxOf(shared, measurable.measure(constraints.copy(maxWidth = Constraints.Infinity)).width)
    } ?: 0
    return !constraints.hasBoundedWidth || natural <= constraints.maxWidth
}

/**
 * Everything the segmented row's width hangs on, the room it gets, the choice it marks, the skin
 * and type it draws in and the names it shows.
 */
private data class FitKey(
    val constraints: Constraints,
    val selected: LibraryChoice,
    val skin: Skin,
    val type: BuilderType,
    val labels: List<String>,
    val density: Float,
    val fontScale: Float,
)

/**
 * What the switcher last measured and showed. Plain fields, since only its own measure reads them
 * and nothing draws from them.
 */
private class SwitcherFit {
    private var key: FitKey? = null
    private var fits: Boolean = false

    /** The form that showed last, or null before the first measure. */
    var shown: SwitcherForm? = null

    /** Whether the row fits for [key], from [measure] when anything in [key] changed since. */
    fun fits(
        key: FitKey,
        measure: () -> Boolean,
    ): Boolean {
        if (key != this.key) {
            fits = measure()
            this.key = key
        }
        return fits
    }
}

/** The switcher's slots, the form that shows and the segmented row measured to see if it fits. */
private enum class SwitcherForm {
    Segmented,
    Dropdown,
    Probe,
}
