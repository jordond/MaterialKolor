package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.explainer_chroma_capped
import com.materialkolor.builder.generated.resources.explainer_chroma_gamut
import com.materialkolor.builder.generated.resources.explainer_chroma_kept
import com.materialkolor.builder.generated.resources.explainer_chroma_lifted
import com.materialkolor.builder.generated.resources.explainer_chroma_none
import com.materialkolor.builder.generated.resources.explainer_hue_turned
import com.materialkolor.builder.generated.resources.explainer_keep_chroma_detail
import com.materialkolor.builder.generated.resources.explainer_override_detail
import com.materialkolor.builder.generated.resources.explainer_take_bolder
import com.materialkolor.builder.generated.resources.explainer_take_calmer
import com.materialkolor.builder.generated.resources.explainer_take_deeper
import com.materialkolor.builder.generated.resources.explainer_take_lighter
import com.materialkolor.builder.generated.resources.explainer_take_turned
import com.materialkolor.builder.generated.resources.explainer_tone_dark
import com.materialkolor.builder.generated.resources.explainer_tone_light
import com.materialkolor.builder.generated.resources.explainer_why
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The card under the seed actions that says how primary reads next to the seed, the seed and primary
 * side by side, the line and a Why link to the explainer on one row (F-16). It stays away while primary sits within 2 of the seed in hue,
 * chroma and tone, pins included.
 *
 * Why opens the explainer, which [ExplainerHost] shows over the workspace so it opens over the rail
 * too, and it hands focus back to Why once it closes.
 *
 * @param[why] Where the explainer hands focus back, or null where nothing hosts it.
 */
@Composable
internal fun PrimaryExplainerLine(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    why: PanelTrigger? = null, // b-221f
) {
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val result = context.result
    val seed = result.document.seed
    val primary = result.roles[Role.Primary, false].argb
    val take = remember(result) { ExplainerText.take(HctReadout.of(seed), HctReadout.of(primary)) }
    if (take == null) return
    // b-510
    val card = RoundedCornerShape(tokens.radius.medium)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tokens.panelRaised, card)
            .padding(start = spacing.medium, end = spacing.small),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TakeSwatch(seed.toColor(), ringed = true)
        BuilderIcon(IconId.ChevronRight, contentDescription = null, emphasis = Emphasis.Secondary)
        TakeSwatch(primary.toColor(), ringed = false)
        BuilderText(
            text = stringResource(take.line),
            modifier = Modifier.weight(1f).padding(vertical = spacing.small),
            style = BuilderTextStyle.Label,
        )
        BuilderButton(
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Explainer)) },
            label = stringResource(Res.string.explainer_why),
            modifier = triggerFocus(why),
            emphasis = Emphasis.Subtle,
        )
    }
}

// b-510

/**
 * One of the card's two swatches, the seed and then primary. The seed sits on its own colour, so
 * it gets a ring in the poster's ink to show at all.
 */
@Composable
private fun TakeSwatch(
    color: Color,
    ringed: Boolean,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small)
    Box(
        Modifier
            .size(tokens.iconSize)
            .background(color, shape)
            .then(if (ringed) Modifier.border(tokens.outlineWidth, tokens.borderStrong, shape) else Modifier),
    )
}

/**
 * The poster line for this take.
 */
internal val PrimaryTake.line: StringResource
    get() = when (this) {
        PrimaryTake.Calmer -> Res.string.explainer_take_calmer
        PrimaryTake.Bolder -> Res.string.explainer_take_bolder
        PrimaryTake.Deeper -> Res.string.explainer_take_deeper
        PrimaryTake.Lighter -> Res.string.explainer_take_lighter
        PrimaryTake.Turned -> Res.string.explainer_take_turned
    }

/**
 * The words of this sentence.
 */
internal val ExplainerKey.resource: StringResource
    get() = when (this) {
        ExplainerKey.ChromaCapped -> Res.string.explainer_chroma_capped
        ExplainerKey.ChromaLifted -> Res.string.explainer_chroma_lifted
        ExplainerKey.ChromaKept -> Res.string.explainer_chroma_kept
        ExplainerKey.ChromaNone -> Res.string.explainer_chroma_none
        ExplainerKey.ChromaGamut -> Res.string.explainer_chroma_gamut
        ExplainerKey.HueTurned -> Res.string.explainer_hue_turned
        ExplainerKey.ToneLight -> Res.string.explainer_tone_light
        ExplainerKey.ToneDark -> Res.string.explainer_tone_dark
        ExplainerKey.KeepChroma -> Res.string.explainer_keep_chroma_detail
        ExplainerKey.PrimaryOverride -> Res.string.explainer_override_detail
    }
