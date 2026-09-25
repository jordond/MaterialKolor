package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.FineTuneRow
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.extras_summary
import com.materialkolor.builder.generated.resources.extras_summary_colors
import com.materialkolor.builder.generated.resources.extras_summary_no_colors
import com.materialkolor.builder.generated.resources.extras_title
import com.materialkolor.builder.kit.control.BuilderDisclosure
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The fine tune row "Spec, platform, extra colors and target options". The project remembers
 * whether it is open, and its summary names the spec the scheme is built with and counts the extra
 * colors.
 *
 * Each part asks the target how it shows up. A hidden one takes no room, a disabled one stays on
 * screen and says why, and a working one carries its note when the target has one.
 */
@Composable
internal fun SpecExtrasRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    BuilderDisclosure(
        expanded = FineTuneRow.SpecExtras in context.openFineTuneRows,
        onExpandedChange = { open ->
            dispatcher.dispatch(WorkspaceAction.SetFineTuneRowOpen(FineTuneRow.SpecExtras, open))
        },
        title = stringResource(Res.string.extras_title),
        modifier = modifier,
        flush = true,
        summary = specExtrasSummary(context.document),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
            SpecPlatformControl(context, dispatcher)
            AccentsEditor(context, dispatcher)
            TargetOptions(context, dispatcher)
            CustomToneTable(context, dispatcher)
        }
    }
}

/**
 * The spec the scheme is really built with, and how many extra colors the theme has.
 */
@Composable
private fun specExtrasSummary(document: ThemeDocument): String {
    val spec = stringResource(specName(EffectiveSpec.of(document.style, document.spec)))
    val count = document.accents.size
    val colors = if (count == 0) {
        stringResource(Res.string.extras_summary_no_colors)
    } else {
        pluralStringResource(Res.plurals.extras_summary_colors, count, count)
    }
    return stringResource(Res.string.extras_summary, spec, colors)
}
