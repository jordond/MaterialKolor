package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.explainer_close
import com.materialkolor.builder.generated.resources.explainer_container_light
import com.materialkolor.builder.generated.resources.explainer_hct
import com.materialkolor.builder.generated.resources.explainer_keep_chroma
import com.materialkolor.builder.generated.resources.explainer_match
import com.materialkolor.builder.generated.resources.explainer_match_badge
import com.materialkolor.builder.generated.resources.explainer_match_dark
import com.materialkolor.builder.generated.resources.explainer_match_pins_dark
import com.materialkolor.builder.generated.resources.explainer_match_pins_light
import com.materialkolor.builder.generated.resources.explainer_match_ratio
import com.materialkolor.builder.generated.resources.explainer_match_warning
import com.materialkolor.builder.generated.resources.explainer_primary_dark
import com.materialkolor.builder.generated.resources.explainer_primary_light
import com.materialkolor.builder.generated.resources.explainer_seed
import com.materialkolor.builder.generated.resources.explainer_title
import com.materialkolor.builder.generated.resources.explainer_use_override
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCheckbox
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderDivider
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * The primary explainer (F-16), open while `state.panel` is [Panel.Explainer]. It sits over the
 * workspace rather than in the poster, so it opens over the rail too.
 *
 * @param[returnFocusTo] The Why button that opened it, which gets focus back once it closes
 * (AR-09), or null while no Why is on screen.
 */
@Composable
internal fun ExplainerHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    returnFocusTo: FocusRequester? = null,
) {
    ExplainerDialog(rememberPosterContext(state), dispatcher, returnFocusTo)
}

/**
 * The explainer as a dialog, one history entry that Back closes. It lists the seed and primary in
 * HCT, says in sentences worked out from the scheme why they differ, and offers three ways out that
 * are one undo entry each.
 *
 * @param[context] What the poster reads, whose open panel says whether the dialog shows.
 * @param[returnFocusTo] What gets focus back once it closes.
 */
@Composable
internal fun ExplainerDialog(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    returnFocusTo: FocusRequester? = null,
) {
    BuilderDialog(
        visible = context.openPanel == Panel.Explainer,
        onDismissRequest = { dispatcher.dispatch(WorkspaceAction.ClosePanel) },
        title = stringResource(Res.string.explainer_title),
        returnFocusTo = returnFocusTo,
        actions = {
            BuilderButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.ClosePanel) },
                label = stringResource(Res.string.explainer_close),
            )
        },
    ) {
        ExplainerBody(context, dispatcher)
    }
}

/**
 * The colors, the reasons and the three ways out.
 */
@Composable
private fun ExplainerBody(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val result = context.result
    val styleName = stringResource(styleName(result.document.style))
    val sentences = remember(result) {
        val light = ExplainerText.sentences(PrimaryFacts.of(result, isDark = false))
        val dark = ExplainerText.sentences(PrimaryFacts.of(result, isDark = true))
        (light + dark).distinct()
    }
    // b-503b
    // The body scrolls in the height the title and Close leave, so Match exactly at its foot stays in
    // reach in a short window. Its buttons take the focus, so the area needs no Tab stop of its own.
    BuilderScrollArea(tabStop = false) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                HctLine(stringResource(Res.string.explainer_seed), context.document.seed)
                HctLine(stringResource(Res.string.explainer_primary_light), result.roles[Role.Primary, false].argb)
                HctLine(stringResource(Res.string.explainer_primary_dark), result.roles[Role.Primary, true].argb)
                HctLine(
                    stringResource(Res.string.explainer_container_light),
                    result.roles[Role.PrimaryContainer, false].argb,
                )
            }
            sentences.forEach { sentence -> BuilderText(text = sentence.text(styleName)) }
            BuilderDivider()
            ExplainerActions(context, dispatcher)
        }
    }
}

/**
 * One listed color, a swatch with its hex and HCT values.
 */
@Composable
private fun HctLine(
    label: String,
    argb: Argb,
) {
    val tokens = LocalBuilderTokens.current
    val hct = remember(argb) { HctReadout.of(argb).rounded() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(tokens.spacing.large).background(argb.toColor(), CircleShape))
        Column {
            BuilderText(text = label, style = BuilderTextStyle.Label)
            BuilderText(
                text = stringResource(Res.string.explainer_hct, argb.toHex(), hct.hue, hct.chroma, hct.tone),
                style = BuilderTextStyle.Value,
                emphasis = Emphasis.Secondary,
            )
        }
    }
}

/**
 * Keep chroma, Use as primary override and Match exactly, each with what it would do. Each lands
 * as one undo entry and closes the explainer.
 */
@Composable
private fun ExplainerActions(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val resolver = rememberThemeResolver()
    val result = context.result
    val seed = context.document.seed
    val keepChroma = remember(result) {
        val inputs = SchemeInputs.from(result.document.copy(style = Style.Fidelity))
        ExplainerText.keepChroma(result.document.seed, resolver.scheme(inputs, isDark = false))
    }
    val primaryOverride = remember(result, seed) {
        val overridden = result.document.copy(keyColors = result.document.keyColors.with(KeyColor.Primary, seed))
        ExplainerText.primaryOverride(resolver.scheme(SchemeInputs.from(overridden), isDark = false))
    }
    var pinDark by rememberSaveable { mutableStateOf(false) }
    val match = remember(result, seed, pinDark) { MatchExactly.of(result, seed, pinDark) }
    val apply = { action: WorkspaceAction ->
        dispatcher.dispatch(WorkspaceAction.ClosePanel)
        dispatcher.dispatch(action)
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        ActionBlock(
            detail = keepChroma.text(),
            label = stringResource(Res.string.explainer_keep_chroma),
            enabled = context.document.style != Style.Fidelity,
            onClick = {
                apply(WorkspaceAction.EditWithReveal(DocumentChange.SetStyle(Style.Fidelity), origin = null))
            },
        )
        ActionBlock(
            detail = primaryOverride.text(),
            label = stringResource(Res.string.explainer_use_override),
            enabled = context.document.keyColors.primary != seed,
            onClick = {
                val change = DocumentChange.SetKeyColor(KeyColor.Primary, seed)
                apply(WorkspaceAction.Edit(change, EditPhase.Discrete))
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            BuilderCheckbox(
                checked = pinDark,
                onCheckedChange = { checked -> pinDark = checked },
                label = stringResource(Res.string.explainer_match_dark),
            )
            BuilderText(
                text = stringResource(
                    Res.string.explainer_match_ratio,
                    ratioText(match.before),
                    ratioText(match.after),
                ),
            )
            if (match.warns) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    BuilderBadge(
                        label = stringResource(Res.string.explainer_match_badge),
                        status = BadgeStatus.Warning,
                        icon = IconId.Warning,
                    )
                    BuilderText(text = stringResource(Res.string.explainer_match_warning))
                }
            }
            BuilderText(
                text = stringResource(
                    Res.string.explainer_match_pins_light,
                    seed.toHex(),
                    match.onPrimaryLight.toHex(),
                ),
                emphasis = Emphasis.Secondary,
            )
            match.onPrimaryDark?.let { onDark ->
                BuilderText(
                    text = stringResource(Res.string.explainer_match_pins_dark, seed.toHex(), onDark.toHex()),
                    emphasis = Emphasis.Secondary,
                )
            }
            BuilderButton(
                onClick = {
                    val matched = match.applyTo(context.document)
                    apply(WorkspaceAction.Edit(DocumentChange.Replace(matched), EditPhase.Discrete))
                },
                label = stringResource(Res.string.explainer_match),
                emphasis = Emphasis.Primary,
            )
        }
    }
}

/**
 * One way out, what it would do and the button that does it.
 */
@Composable
private fun ActionBlock(
    detail: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderText(text = detail, emphasis = Emphasis.Secondary)
        BuilderButton(onClick = onClick, label = label, enabled = enabled)
    }
}

/**
 * The sentence in words, with [styleName] first where the sentence names the style.
 */
@Composable
private fun ExplainerSentence.text(styleName: String = ""): String {
    val arguments: List<Any> = if (key.namesStyle) listOf(styleName) + values else values
    return stringResource(key.resource, *arguments.toTypedArray())
}
