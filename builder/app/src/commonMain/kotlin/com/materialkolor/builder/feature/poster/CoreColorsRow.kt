package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.workspace.FineTuneSection
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.keycolors_summary_both
import com.materialkolor.builder.generated.resources.keycolors_summary_none
import com.materialkolor.builder.generated.resources.keycolors_summary_pins
import com.materialkolor.builder.generated.resources.keycolors_summary_set
import com.materialkolor.builder.generated.resources.keycolors_title
import com.materialkolor.builder.kit.control.BuilderDisclosure
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The fine tune row "Core colors and pins", the key colors and the roles pinned to colors of their
 * own. The project remembers whether it is open, and its summary counts what is set.
 */
@Composable
internal fun CoreColorsRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    BuilderDisclosure(
        // b-521 shim, B-524 replaces
        expanded = context.fineTune in setOf(FineTuneSection.KeyColors, FineTuneSection.Pins),
        onExpandedChange = { open ->
            dispatcher.dispatch(
                if (open) WorkspaceAction.OpenFineTune(FineTuneSection.KeyColors) else WorkspaceAction.CloseFineTune,
            )
        },
        title = stringResource(Res.string.keycolors_title),
        modifier = modifier,
        flush = true,
        summary = coreColorsSummary(context.document),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
            val picks = remember { KeyColorPicks() }
            KeyColorRows(context, dispatcher, picks = picks)
            PinnedRoles(context, dispatcher, picks = picks)
        }
    }
}

/**
 * How many key colors are set by hand and how many roles are pinned, or that the seed has it all.
 */
@Composable
private fun coreColorsSummary(document: ThemeDocument): String {
    val set = KeyColor.entries.count { slot -> document.keyColors[slot] != null }
    val pinned = document.pins.size
    val keyColors = pluralStringResource(Res.plurals.keycolors_summary_set, set, set)
    val pins = pluralStringResource(Res.plurals.keycolors_summary_pins, pinned, pinned)
    return when {
        set > 0 && pinned > 0 -> stringResource(Res.string.keycolors_summary_both, keyColors, pins)
        set > 0 -> keyColors
        pinned > 0 -> pins
        else -> stringResource(Res.string.keycolors_summary_none)
    }
}
