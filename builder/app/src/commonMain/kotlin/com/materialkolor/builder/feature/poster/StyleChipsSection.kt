package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInputModeManager
import com.materialkolor.builder.LocalThemeResolver
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.picker.pickButtonFocus
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.picker_pick_cmf
import com.materialkolor.builder.generated.resources.style_chip
import com.materialkolor.builder.generated.resources.style_chips
import com.materialkolor.builder.generated.resources.style_cmf_derive
import com.materialkolor.builder.generated.resources.style_cmf_derived
import com.materialkolor.builder.generated.resources.style_cmf_field
import com.materialkolor.builder.generated.resources.style_description_cmf
import com.materialkolor.builder.generated.resources.style_description_content
import com.materialkolor.builder.generated.resources.style_description_expressive
import com.materialkolor.builder.generated.resources.style_description_fidelity
import com.materialkolor.builder.generated.resources.style_description_fruit_salad
import com.materialkolor.builder.generated.resources.style_description_monochrome
import com.materialkolor.builder.generated.resources.style_description_neutral
import com.materialkolor.builder.generated.resources.style_description_rainbow
import com.materialkolor.builder.generated.resources.style_description_tonal_spot
import com.materialkolor.builder.generated.resources.style_description_vibrant
import com.materialkolor.builder.generated.resources.style_label
import com.materialkolor.builder.generated.resources.style_line
import com.materialkolor.builder.generated.resources.style_name_cmf
import com.materialkolor.builder.generated.resources.style_name_content
import com.materialkolor.builder.generated.resources.style_name_expressive
import com.materialkolor.builder.generated.resources.style_name_fidelity
import com.materialkolor.builder.generated.resources.style_name_fruit_salad
import com.materialkolor.builder.generated.resources.style_name_monochrome
import com.materialkolor.builder.generated.resources.style_name_neutral
import com.materialkolor.builder.generated.resources.style_name_rainbow
import com.materialkolor.builder.generated.resources.style_name_tonal_spot
import com.materialkolor.builder.generated.resources.style_name_vibrant
import com.materialkolor.builder.generated.resources.style_spec_classic
import com.materialkolor.builder.generated.resources.style_spec_cmf
import com.materialkolor.builder.generated.resources.style_spec_revised
import com.materialkolor.builder.generated.resources.style_tooltip_cmf
import com.materialkolor.builder.generated.resources.style_tooltip_content
import com.materialkolor.builder.generated.resources.style_tooltip_expressive
import com.materialkolor.builder.generated.resources.style_tooltip_fidelity
import com.materialkolor.builder.generated.resources.style_tooltip_fruit_salad
import com.materialkolor.builder.generated.resources.style_tooltip_monochrome
import com.materialkolor.builder.generated.resources.style_tooltip_neutral
import com.materialkolor.builder.generated.resources.style_tooltip_rainbow
import com.materialkolor.builder.generated.resources.style_tooltip_tonal_spot
import com.materialkolor.builder.generated.resources.style_tooltip_vibrant
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderChoiceGroup
import com.materialkolor.builder.kit.control.BuilderHexField
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChip
import com.materialkolor.dynamiccolor.DynamicScheme
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * How a style chip asks for its scheme, the resolver's `scheme` in the app. A test can count the
 * calls through it.
 */
internal typealias StyleSchemeLookup = (inputs: SchemeInputs, isDark: Boolean) -> DynamicScheme

/**
 * The palette style chips, each a small picture of what that style makes of the seed (F-11).
 *
 * Each chip asks the shared resolver for its own scheme only once it is drawn, so the chips share
 * the cache the open theme sits in and the current style's chip costs nothing. A click or Enter
 * picks a style behind a reveal from the chip, as one undo entry. The arrow keys walk the chips
 * without picking one, and hover and focus only bring up the chip's tooltip, so the rest of the
 * app keeps the style it has. Picking Cmf brings up its tertiary seed.
 */
@Composable
internal fun StyleChipsSection(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val resolver = rememberThemeResolver()
    val lookup: StyleSchemeLookup = remember(resolver) { { inputs, isDark -> resolver.scheme(inputs, isDark) } }
    StyleChips(context, dispatcher, lookup, modifier)
}

/** The resolver the app root provides, or one of the poster's own where there is none. */
@Composable
internal fun rememberThemeResolver(): ThemeResolver = LocalThemeResolver.current ?: remember { ThemeResolver() }

/** [StyleChipsSection] with the scheme lookup passed in. */
@Composable
internal fun StyleChips(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    lookup: StyleSchemeLookup,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val selected = context.document.style
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        InfoLabel(label = stringResource(Res.string.style_label), topic = InfoTopic.Style)
        StyleChipRow(context, lookup) { style, origin ->
            dispatcher.dispatch(WorkspaceAction.EditWithReveal(DocumentChange.SetStyle(style), origin))
        }
        BuilderText(
            text = stringResource(
                Res.string.style_line,
                stringResource(styleName(selected)),
                stringResource(styleDescription(selected)),
            ),
        )
        context.capabilities[Control.Style].explanation?.let { reason -> ReasonLine(reason) }
        if (selected == Style.Cmf) {
            CmfSeedField(context, dispatcher)
        }
    }
}

/**
 * The ten chips as one radio group with a single tab stop. Tab lands on the chosen chip, and the
 * arrows, Home and End move the focus around the group without picking, wrapping at the ends.
 */
@Composable
private fun StyleChipRow(
    context: PosterContext,
    lookup: StyleSchemeLookup,
    onChoose: (style: Style, origin: Offset?) -> Unit,
) {
    val selected = context.document.style
    BuilderChoiceGroup(
        options = Style.entries,
        selected = selected,
        // The keys only move the focus here, so a pick that ever comes this way has no chip to reveal from.
        onSelect = { style -> if (style != selected) onChoose(style, null) },
        label = stringResource(Res.string.style_chips),
        selectOnFocus = false,
    ) { style, isSelected, optionModifier ->
        StyleChip(
            context = context,
            style = style,
            lookup = lookup,
            selected = isSelected,
            onChoose = { origin -> if (style != selected) onChoose(style, origin) },
            modifier = optionModifier,
        )
    }
}

/**
 * One chip, drawn from the scheme [style] makes of the document's seed in the mode the preview
 * shows, with the specs the style runs in under it.
 */
@Composable
private fun StyleChip(
    context: PosterContext,
    style: Style,
    lookup: StyleSchemeLookup,
    selected: Boolean,
    onChoose: (origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val isDark = context.visibleModes == PreviewMode.Dark
    val inputs = SchemeInputs.from(context.result.document.copy(style = style))
    val colors = remember(inputs, isDark) { ChipColors.of(lookup(inputs, isDark)) }
    val bounds = remember { ChipBounds() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        SchemeChip(
            primary = colors.primary,
            secondaryContainer = colors.secondaryContainer,
            tertiaryContainer = colors.tertiaryContainer,
            selected = selected,
            onClick = { onChoose(bounds.rect.center) },
            label = stringResource(
                Res.string.style_chip,
                stringResource(styleName(style)),
                stringResource(styleTooltip(style)),
            ),
            modifier = modifier.onGloballyPositioned { coordinates -> bounds.rect = coordinates.boundsInRoot() },
        )
        BuilderBadge(label = stringResource(specSupport(style)))
    }
}

/** The three colors a chip is drawn in, read from its scheme once. */
private class ChipColors(
    val primary: Color,
    val secondaryContainer: Color,
    val tertiaryContainer: Color,
) {
    companion object {
        fun of(scheme: DynamicScheme): ChipColors =
            ChipColors(Color(scheme.primary), Color(scheme.secondaryContainer), Color(scheme.tertiaryContainer))
    }
}

/** Where a chip sits in the root, which a pick reveals from. Only a pick reads it. */
private class ChipBounds {
    var rect: Rect = Rect.Zero
}

/**
 * The second seed Cmf reads for its tertiary palette. With none set the field shows the tertiary
 * key color Cmf derived, and once one is set a button hands it back, and a keyboard user's focus
 * with it to the field. A target that ignores it says why and takes no input.
 */
@Composable
private fun CmfSeedField(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val stored = context.document.cmfTertiarySeed
    val derived = remember(context.result) { context.result.ramps[KeyColor.Tertiary, false].keyColor }
    val state = context.capabilities[Control.CmfSecondSeed]
    val messages = rememberHexMessages()
    val input = LocalInputModeManager.current
    val field = remember { FocusRequester() }
    val pick = remember { FocusRequester() } // b-307
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        // b-307
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderHexField(
                value = stored ?: derived,
                onCommit = { argb, _ ->
                    dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetCmfSeed(argb), EditPhase.Discrete))
                },
                label = stringResource(Res.string.style_cmf_field),
                errorMessage = messages::errorOf,
                noteMessage = messages::noteOf,
                modifier = Modifier.weight(1f).focusRequester(field),
                enabled = state.usable,
            )
            BuilderIconButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPicker(PickerTarget.CmfSeed, pick)) },
                icon = IconId.Eyedropper,
                contentDescription = stringResource(Res.string.picker_pick_cmf),
                modifier = pickButtonFocus(pick),
                enabled = state.usable,
            )
        }
        if (stored == null) {
            BuilderText(text = stringResource(Res.string.style_cmf_derived), emphasis = Emphasis.Secondary)
        } else {
            BuilderButton(
                onClick = {
                    input.handFocusTo(field)
                    dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetCmfSeed(null), EditPhase.Discrete))
                },
                label = stringResource(Res.string.style_cmf_derive),
                emphasis = Emphasis.Subtle,
                enabled = state.usable,
            )
        }
        state.explanation?.let { reason -> ReasonLine(reason) }
    }
}

/** What [style] is called, the way the library spells it. */
internal fun styleName(style: Style): StringResource =
    when (style) {
        Style.TonalSpot -> Res.string.style_name_tonal_spot
        Style.Neutral -> Res.string.style_name_neutral
        Style.Vibrant -> Res.string.style_name_vibrant
        Style.Expressive -> Res.string.style_name_expressive
        Style.Rainbow -> Res.string.style_name_rainbow
        Style.FruitSalad -> Res.string.style_name_fruit_salad
        Style.Monochrome -> Res.string.style_name_monochrome
        Style.Fidelity -> Res.string.style_name_fidelity
        Style.Content -> Res.string.style_name_content
        Style.Cmf -> Res.string.style_name_cmf
    }

/** The short hint [style]'s chip shows on hover and focus. */
internal fun styleTooltip(style: Style): StringResource =
    when (style) {
        Style.TonalSpot -> Res.string.style_tooltip_tonal_spot
        Style.Neutral -> Res.string.style_tooltip_neutral
        Style.Vibrant -> Res.string.style_tooltip_vibrant
        Style.Expressive -> Res.string.style_tooltip_expressive
        Style.Rainbow -> Res.string.style_tooltip_rainbow
        Style.FruitSalad -> Res.string.style_tooltip_fruit_salad
        Style.Monochrome -> Res.string.style_tooltip_monochrome
        Style.Fidelity -> Res.string.style_tooltip_fidelity
        Style.Content -> Res.string.style_tooltip_content
        Style.Cmf -> Res.string.style_tooltip_cmf
    }

/** The one line on what [style] does with the seed. */
internal fun styleDescription(style: Style): StringResource =
    when (style) {
        Style.TonalSpot -> Res.string.style_description_tonal_spot
        Style.Neutral -> Res.string.style_description_neutral
        Style.Vibrant -> Res.string.style_description_vibrant
        Style.Expressive -> Res.string.style_description_expressive
        Style.Rainbow -> Res.string.style_description_rainbow
        Style.FruitSalad -> Res.string.style_description_fruit_salad
        Style.Monochrome -> Res.string.style_description_monochrome
        Style.Fidelity -> Res.string.style_description_fidelity
        Style.Content -> Res.string.style_description_content
        Style.Cmf -> Res.string.style_description_cmf
    }

/** The specs [style] runs in, as its chip's badge says them. */
internal fun specSupport(style: Style): StringResource {
    val offered = EffectiveSpec.offered(style)
    return when {
        SpecVersion.Spec2026 in offered -> Res.string.style_spec_cmf
        SpecVersion.Spec2025 in offered -> Res.string.style_spec_revised
        else -> Res.string.style_spec_classic
    }
}
