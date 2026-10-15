package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.engine.color.HctReadout
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
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * One plain line under the seed that says how primary reads next to it, with a Why button after it
 * that opens the explainer. It stays away while primary sits within 2 of the seed in hue, chroma and
 * tone, pins included.
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
    why: PanelTrigger? = null,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val result = context.result
    val take = remember(result) {
        val seed = result.document.seed
        val primary = result.roles[Role.Primary, false].argb
        ExplainerText.take(HctReadout.of(seed), HctReadout.of(primary))
    }
    if (take == null) return
    // The line and Why read as one sentence, so Why follows the words rather than the edge.
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(
            text = stringResource(take.line),
            modifier = Modifier.weight(1f, fill = false),
        )
        BuilderButton(
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Explainer)) },
            label = stringResource(Res.string.explainer_why),
            modifier = triggerFocus(why),
            emphasis = Emphasis.Subtle,
        )
    }
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
