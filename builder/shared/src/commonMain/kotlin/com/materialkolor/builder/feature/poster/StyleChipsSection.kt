package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.LocalThemeResolver
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.Reason
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.picker.pickButtonFocus
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.picker_pick_cmf
import com.materialkolor.builder.generated.resources.style_chip
import com.materialkolor.builder.generated.resources.style_chip_tooltip
import com.materialkolor.builder.generated.resources.style_chips
import com.materialkolor.builder.generated.resources.style_cmf_derive
import com.materialkolor.builder.generated.resources.style_cmf_derived
import com.materialkolor.builder.generated.resources.style_cmf_field
import com.materialkolor.builder.generated.resources.style_label
import com.materialkolor.builder.generated.resources.style_line
import com.materialkolor.builder.generated.resources.style_spec_forced
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderChoiceGroup
import com.materialkolor.builder.kit.control.BuilderHexField
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChip
import com.materialkolor.builder.kit.widget.SchemeChipFootprint
import com.materialkolor.builder.kit.widget.SchemeChipName
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
 * What the chips wait on before each chip as they catch up, the next frame in the app.
 */
internal typealias ChipPause = suspend () -> Unit

/**
 * The palette style chips, each a small picture of what that style makes of the seed.
 *
 * Each chip asks the shared resolver for its own scheme, so the chips share the cache the open
 * theme sits in and the current style's chip costs nothing. They all draw at once the first time.
 * After that a change to the scheme brings them up to date one chip a frame, so a drag that moves
 * the seed or the contrast every frame never generates ten schemes inside one. A click or Enter
 * picks a style behind a reveal from the chip, as one undo entry. The arrow keys walk the chips
 * without picking one, and hover and focus only bring up the chip's tooltip, so the rest of the app
 * keeps the style it has. Picking Cmf brings up its tertiary seed.
 */
@Composable
internal fun StyleChipsSection(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    scrolling: Boolean = false,
    details: Boolean = true,
) {
    val resolver = rememberThemeResolver()
    val lookup: StyleSchemeLookup = remember(resolver) { { inputs, isDark -> resolver.scheme(inputs, isDark) } }
    StyleChips(context, dispatcher, lookup, modifier, scrolling = scrolling, details = details)
}

/**
 * The resolver the app root provides, or one of the poster's own where there is none.
 */
@Composable
internal fun rememberThemeResolver(): ThemeResolver = LocalThemeResolver.current ?: remember { ThemeResolver() }

/**
 * [StyleChipsSection] with the scheme lookup passed in.
 *
 * @param[pause] What the chips wait on before each chip as they catch up.
 * @param[scrolling] Lay the chips out as one row that scrolls sideways, as the phone sheet's peek
 * shows them, rather than a grid of five a row.
 * @param[details] Whether what the chosen style does follows the chips. The sheet shows it further
 * down, see [StyleDetails].
 */
@Composable
internal fun StyleChips(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    lookup: StyleSchemeLookup,
    modifier: Modifier = Modifier,
    pause: ChipPause = NextFrame,
    scrolling: Boolean = false,
    details: Boolean = true,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val selected = context.document.style
    val isDark = context.visibleModes == PreviewMode.Dark
    val shelf = rememberChipShelf(context.result.document, isDark, lookup, pause)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        StyleHeader(selected, plain = scrolling, reason = context.capabilities[Control.Style].explanation)
        StyleChipRow(selected, context.document, shelf, scrolling) { style, origin ->
            dispatcher.dispatch(WorkspaceAction.EditWithReveal(DocumentChange.SetStyle(style), origin))
        }
        if (details) StyleDetails(context, dispatcher)
    }
}

/**
 * What the chosen style does with the seed with the Keep on shuffle toggle at the end of that line,
 * and the second seed while the style is Cmf.
 *
 * @param[info] Whether the line carries the Style info button, for the phone sheet, whose peek
 * labels the chips without one to leave room for the contrast levels. Its explanation says why the
 * target treats the style differently, if it does, as the Style label's does on the docked poster.
 */
@Composable
internal fun StyleDetails(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    info: Boolean = false,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val selected = context.document.style
    var open by rememberSaveable { mutableStateOf(false) }
    // Only the 400 poster has room for the line and the toggle side by side. The 320 poster
    // and the phone sheet put the toggle under the line.
    val stacked = LocalLayout.current.posterMode != PosterMode.Docked400
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderText(
                text = stringResource(
                    Res.string.style_line,
                    stringResource(styleName(selected)),
                    stringResource(styleDescription(selected)),
                ),
                modifier = Modifier.weight(1f),
            )
            if (info) InfoButton(topic = InfoTopic.Style, expanded = open, onClick = { open = !open })
            if (!stacked) KeepStyleToggle(context, dispatcher)
        }
        if (stacked) KeepStyleToggle(context, dispatcher)
        if (info && open) {
            InfoNote(InfoTopic.Style)
            // Why the target treats the style differently opens with the explanation.
            context.capabilities[Control.Style].explanation?.let { reason -> ReasonLine(reason) }
        }
        if (selected == Style.Cmf) {
            CmfSeedField(context, dispatcher)
        }
    }
}

/**
 * The Style label with its info button and, while the chosen style runs in one spec whatever the
 * theme asks for, a note on the right naming that spec. The info button's explanation carries
 * [reason], why the target treats the style differently. A [plain] label has no info button, and
 * the sheet's [StyleDetails] carries both instead.
 */
@Composable
private fun StyleHeader(
    selected: Style,
    plain: Boolean,
    reason: Reason?,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val label = stringResource(Res.string.style_label)
    // The spec note sits at the end of the label's line, and an open explanation goes under both.
    val note = forcedSpec(selected)?.let { spec ->
        stringResource(
            Res.string.style_spec_forced,
            stringResource(specName(spec)),
            stringResource(styleDisplayName(selected)),
        )
    }
    if (plain) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow(label)
            if (note != null) SpecNote(note, Modifier.weight(1f))
        }
    } else {
        ReasonInfoLabel(label = label, topic = InfoTopic.Style, reason = reason) {
            if (note != null) SpecNote(note, Modifier.weight(1f))
        }
    }
}

/**
 * The note on the right of the Style label naming the one spec the chosen style runs in.
 */
@Composable
private fun SpecNote(
    note: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.CenterEnd) {
        BuilderBadge(label = note, icon = IconId.Lock)
    }
}

/**
 * The ten chips as one radio group with a single tab stop, five equal cells a row. Tab lands on the
 * chosen chip, and the arrows, Home and End move the focus around the group without picking,
 * wrapping at the ends.
 *
 * It reads nothing of the document but the style and the spec, so a drag leaves it alone and only
 * a chip whose colours [shelf] brought up to date draws again.
 */
@Composable
private fun StyleChipRow(
    selected: Style,
    document: ThemeDocument,
    shelf: ChipShelf,
    scrolling: Boolean,
    onChoose: (style: Style, origin: Offset?) -> Unit,
) {
    val tags = remember(document.style, document.spec) {
        Style.entries.associateWith { style -> specTag(style, document) }
    }
    val spacing = LocalBuilderTokens.current.spacing
    // A row that scrolls gives each chip a cell of its own width, since it has no width to share.
    val cell = if (scrolling) Modifier.width(SchemeChipFootprint + spacing.small) else null
    // The 320 poster's grid reaches into the poster's side room, so each cell is wide enough
    // for the longest name, Monochrome, at the chip name's smallest size.
    val narrow = LocalLayout.current.posterMode == PosterMode.Docked320
    val grid = if (narrow) Modifier.sideBleed(spacing.medium) else Modifier
    BuilderChoiceGroup(
        options = Style.entries,
        selected = selected,
        // The keys only move the focus here, so a pick that ever comes this way has no chip to reveal from.
        onSelect = { style -> if (style != selected) onChoose(style, null) },
        label = stringResource(Res.string.style_chips),
        modifier = if (scrolling) Modifier.horizontalScroll(rememberScrollState()) else grid,
        selectOnFocus = false,
        columns = if (scrolling) 0 else ChipColumns,
    ) { style, isSelected, optionModifier ->
        StyleChip(
            style = style,
            shelf = shelf,
            selected = isSelected,
            tag = tags[style],
            onChoose = { origin -> if (style != selected) onChoose(style, origin) },
            modifier = optionModifier,
            cell = cell ?: Modifier,
            named = !scrolling,
        )
    }
}

/**
 * How many chips each row of the grid holds, two rows for the ten styles.
 */
private const val ChipColumns = 5

/**
 * Lays the content out [bleed] wider on each side than the room it is given, centred on that room,
 * so it reaches into the padding around it.
 */
private fun Modifier.sideBleed(bleed: Dp): Modifier =
    layout { measurable, constraints ->
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val extra = bleed.roundToPx()
        val wide = constraints.copy(
            minWidth = constraints.minWidth + extra * 2,
            maxWidth = constraints.maxWidth + extra * 2,
        )
        val placeable = measurable.measure(wide)
        layout(placeable.width - extra * 2, placeable.height) { placeable.place(-extra, 0) }
    }

/**
 * One chip, drawn from the scheme [style] makes of the document's seed in the mode the preview
 * shows, as [shelf] has it now, with the style's name under it. A chip that would move the theme to
 * another spec names that spec, [tag], under its name, and its tooltip lists every spec it runs in.
 *
 * @param[named] Whether the name and the spec show under the chip. The phone sheet's peek draws the
 * chips alone, the way the phone board does, so the contrast levels fit under them. The chip still
 * reads out its name, and its tooltip names the specs.
 */
@Composable
private fun StyleChip(
    style: Style,
    shelf: ChipShelf,
    selected: Boolean,
    tag: SpecVersion?,
    onChoose: (origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
    cell: Modifier = Modifier,
    named: Boolean = true,
) {
    val colors = shelf[style]
    val bounds = remember { ChipBounds() }
    val name = stringResource(styleName(style))
    val shown = stringResource(styleDisplayName(style))
    val hint = stringResource(styleTooltip(style))
    Column(cell, horizontalAlignment = Alignment.CenterHorizontally) {
        SchemeChip(
            primary = colors.primary,
            secondaryContainer = colors.secondaryContainer,
            tertiaryContainer = colors.tertiaryContainer,
            selected = selected,
            onClick = { onChoose(bounds.rect.center) },
            label = stringResource(Res.string.style_chip, name, hint),
            // A cell narrower than the chip's ring room lets the ring reach past it, and the circle
            // keeps its size.
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .then(modifier)
                .onGloballyPositioned { coordinates -> bounds.rect = coordinates.boundsInRoot() },
            tooltip = stringResource(Res.string.style_chip_tooltip, shown, hint, stringResource(specSupport(style))),
        )
        if (!named) return@Column
        SchemeChipName(name = shown, modifier = Modifier.fillMaxWidth())
        if (tag != null) {
            BuilderBadge(
                label = stringResource(specName(tag)),
                modifier = Modifier.padding(top = LocalBuilderTokens.current.spacing.extraSmall),
            )
        }
    }
}

/**
 * Where a chip sits in the root, which a pick reveals from. Only a pick reads it.
 */
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
    val pick = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
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
