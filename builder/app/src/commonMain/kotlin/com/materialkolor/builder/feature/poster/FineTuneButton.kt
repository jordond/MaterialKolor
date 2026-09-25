package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.extras_summary_colors
import com.materialkolor.builder.generated.resources.finetune_button
import com.materialkolor.builder.generated.resources.finetune_summary
import com.materialkolor.builder.generated.resources.finetune_summary_extras
import com.materialkolor.builder.generated.resources.finetune_summary_from_seed
import com.materialkolor.builder.generated.resources.finetune_summary_spec
import com.materialkolor.builder.generated.resources.finetune_title
import com.materialkolor.builder.generated.resources.keycolors_summary_both
import com.materialkolor.builder.generated.resources.keycolors_summary_pins
import com.materialkolor.builder.generated.resources.keycolors_summary_set
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The button at the foot of the poster that opens the Fine-tune sheet, with one line under its
 * title that sums up what the sheet holds, the key colors and pins set, the spec and the extra
 * colors.
 *
 * It is drawn as an outline in the poster's ink with no fill, the way board E draws it. Material's
 * is a pill and the other skins keep the corner their controls have. It reads out as its title and
 * that line. The sheet it opens hands focus back to it through [trigger] once it closes.
 *
 * @param[trigger] The poster's Fine-tune trigger, or null where nothing hands focus back.
 */
@Composable
internal fun FineTuneButton(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    trigger: PanelTrigger? = null,
) {
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val title = stringResource(Res.string.finetune_title)
    val summary = fineTuneSummary(context.document)
    val name = stringResource(Res.string.finetune_button, title, summary)
    // b-527
    val shape = if (LocalSkin.current.library == Library.Material3) {
        RoundedCornerShape(percent = PILL_PERCENT)
    } else {
        RoundedCornerShape(tokens.radius.small)
    }
    BuilderPressable(
        onClick = { dispatcher.dispatch(WorkspaceAction.OpenFineTune()) },
        label = name,
        modifier = modifier
            .fillMaxWidth()
            .then(triggerFocus(trigger)),
        shape = shape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(FineTuneOutline, tokens.textStrong, shape)
                .padding(horizontal = spacing.large, vertical = spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderIcon(IconId.Sliders, contentDescription = null)
            Column(Modifier.weight(1f)) {
                BuilderText(text = title, style = BuilderTextStyle.Label, maxLines = 1)
                BuilderText(text = summary, maxLines = 2)
            }
            // The sheet rises from here, so the chevron points up.
            BuilderIcon(IconId.ChevronDown, contentDescription = null, modifier = Modifier.rotate(HALF_TURN))
        }
    }
}

/**
 * How thick the button's outline is drawn, board E's line, a little heavier than a field's.
 */
private val FineTuneOutline = 1.5.dp

/**
 * The corner of a pill, half its height.
 */
private const val PILL_PERCENT = 50

/**
 * The line under the button's title. The key colors and pins set, or that the colors come from the
 * seed, then the spec the scheme is really built with, then the extra colors when there are any.
 */
@Composable
internal fun fineTuneSummary(document: ThemeDocument): String {
    val set = KeyColor.entries.count { slot -> document.keyColors[slot] != null }
    val pinned = document.pins.size
    val keyColors = pluralStringResource(Res.plurals.keycolors_summary_set, set, set)
    val pins = pluralStringResource(Res.plurals.keycolors_summary_pins, pinned, pinned)
    val colors = when {
        set > 0 && pinned > 0 -> stringResource(Res.string.keycolors_summary_both, keyColors, pins)
        set > 0 -> keyColors
        pinned > 0 -> pins
        else -> stringResource(Res.string.finetune_summary_from_seed)
    }
    val spec = stringResource(
        Res.string.finetune_summary_spec,
        stringResource(specName(EffectiveSpec.of(document.style, document.spec))),
    )
    val count = document.accents.size
    if (count == 0) return stringResource(Res.string.finetune_summary, colors, spec)
    val extras = pluralStringResource(Res.plurals.extras_summary_colors, count, count)
    return stringResource(Res.string.finetune_summary_extras, colors, spec, extras)
}

private const val HALF_TURN = 180f
