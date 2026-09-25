package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.topbar_library
import com.materialkolor.builder.kit.control.BuilderChoiceChips
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

// b-406

/**
 * The phone's library chip row under the top bar, for tests that check where it sits.
 */
internal const val LIBRARY_CHIP_ROW_TAG: String = "top-bar-library-chips"

/**
 * The library switcher on a phone, a row of chips under the top bar, one per library.
 *
 * The row scrolls sideways when the names run past the edge, and the chip with focus scrolls into
 * view. The arrow keys only move focus and Space or Enter picks, as on the segmented row, so walking
 * past a library does not re-skin the app at every step. It tells [LocalSwitcherForm] the libraries
 * show as their own controls, the way the registry finds them (P6).
 *
 * @param[selected] The library the document is on.
 * @param[onSwitch] Gets the new choice and where the reveal grows from.
 * @param[switcherModifier] Applied to the chips' group, where focus and the pulse go.
 */
@Composable
internal fun LibraryChipRow(
    selected: LibraryChoice,
    onSwitch: (choice: LibraryChoice, origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    switcherModifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val names = LibraryChoice.entries.associateWith { choice -> libraryName(choice) }
    val report = LocalSwitcherForm.current
    if (report != null) SideEffect { report.segmented = true }
    // A tap's reveal grows from the tap, and a key's from the middle of the row.
    val origin = remember { RevealOrigin() }
    Box(
        modifier = modifier
            .testTag(LIBRARY_CHIP_ROW_TAG)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        BuilderChoiceChips(
            options = LibraryChoice.entries,
            selected = selected,
            onSelect = { choice -> if (choice != selected) onSwitch(choice, origin.take()) },
            label = stringResource(Res.string.topbar_library),
            modifier = Modifier
                .padding(horizontal = spacing.medium, vertical = spacing.extraSmall)
                .then(switcherModifier.trackRevealOrigin(origin)),
            selectOnFocus = false,
            optionLabel = { choice -> names.getValue(choice) },
        )
    }
}
