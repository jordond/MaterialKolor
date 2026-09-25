package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import com.materialkolor.builder.generated.resources.style_chip_tooltip
import com.materialkolor.builder.generated.resources.style_spec_forced
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.widget.SchemeChipName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
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
import com.materialkolor.builder.generated.resources.style_chips
import com.materialkolor.builder.generated.resources.style_cmf_derive
import com.materialkolor.builder.generated.resources.style_cmf_derived
import com.materialkolor.builder.generated.resources.style_cmf_field
import com.materialkolor.builder.generated.resources.style_label
import com.materialkolor.builder.generated.resources.style_line
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

// pf-1

/** What the chips wait on before each chip as they catch up, the next frame in the app. */
internal typealias ChipPause = suspend () -> Unit

/**
 * The palette style chips, each a small picture of what that style makes of the seed (F-11).
 *
 * Each chip asks the shared resolver for its own scheme, so the chips share the cache the open theme
 * sits in and the current style's chip costs nothing. They all draw at once the first time. After
 * that a change to the scheme brings them up to date one chip a frame, so a drag that moves the
 * seed or the contrast every frame never generates ten schemes inside one (PB-05). A
 * click or Enter picks a style behind a reveal from the chip, as one undo entry. The arrow keys walk
 * the chips without picking one, and hover and focus only bring up the chip's tooltip, so the rest
 * of the app keeps the style it has. Picking Cmf brings up its tertiary seed.
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

/**
 * [StyleChipsSection] with the scheme lookup passed in.
 *
 * @param[pause] What the chips wait on before each chip as they catch up.
 */
@Composable
internal fun StyleChips(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    lookup: StyleSchemeLookup,
    modifier: Modifier = Modifier,
    pause: ChipPause = NextFrame, // pf-1
) {
    val spacing = LocalBuilderTokens.current.spacing
    val selected = context.document.style
    // pf-1
    val isDark = context.visibleModes == PreviewMode.Dark
    val shelf = rememberChipShelf(context.result.document, isDark, lookup, pause)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        StyleHeader(selected) // b-510
        StyleChipRow(selected, context.document, shelf) { style, origin ->
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

// b-510

/**
 * The Style label with its info button and, while the chosen style runs in one spec whatever the
 * theme asks for, a note on the right naming that spec.
 */
@Composable
private fun StyleHeader(selected: Style) {
    val spacing = LocalBuilderTokens.current.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        InfoLabel(
            label = stringResource(Res.string.style_label),
            topic = InfoTopic.Style,
            modifier = Modifier.weight(1f),
        )
        forcedSpec(selected)?.let { spec ->
            val note = stringResource(
                Res.string.style_spec_forced,
                stringResource(specName(spec)),
                stringResource(styleDisplayName(selected)),
            )
            // The label row is as tall as its info button, so the note sits level with the label.
            Box(Modifier.heightIn(min = LocalLayout.current.minTouchTarget), contentAlignment = Alignment.Center) {
                BuilderBadge(label = note, icon = IconId.Lock)
            }
        }
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
    onChoose: (style: Style, origin: Offset?) -> Unit,
) {
    val tags = remember(document.style, document.spec) {
        Style.entries.associateWith { style -> specTag(style, document) }
    }
    BuilderChoiceGroup(
        options = Style.entries,
        selected = selected,
        // The keys only move the focus here, so a pick that ever comes this way has no chip to reveal from.
        onSelect = { style -> if (style != selected) onChoose(style, null) },
        label = stringResource(Res.string.style_chips),
        selectOnFocus = false,
        columns = ChipColumns, // b-510
    ) { style, isSelected, optionModifier ->
        StyleChip(
            style = style,
            shelf = shelf,
            selected = isSelected,
            tag = tags[style],
            onChoose = { origin -> if (style != selected) onChoose(style, origin) },
            modifier = optionModifier,
        )
    }
}

/** How many chips each row of the grid holds, two rows for the ten styles as the design has it. */
private const val ChipColumns = 5

/**
 * One chip, drawn from the scheme [style] makes of the document's seed in the mode the preview
 * shows, as [shelf] has it now, with the style's name under it. A chip that would move the theme to
 * another spec names that spec, [tag], under its name, and its tooltip lists every spec it runs in.
 */
@Composable
private fun StyleChip(
    style: Style,
    shelf: ChipShelf,
    selected: Boolean,
    tag: SpecVersion?,
    onChoose: (origin: Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = shelf[style] // pf-1
    val bounds = remember { ChipBounds() }
    val name = stringResource(styleName(style))
    val shown = stringResource(styleDisplayName(style))
    val hint = stringResource(styleTooltip(style))
    // b-510
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
        SchemeChipName(name = shown, modifier = Modifier.fillMaxWidth())
        if (tag != null) {
            BuilderBadge(
                label = stringResource(specName(tag)),
                modifier = Modifier.padding(top = LocalBuilderTokens.current.spacing.extraSmall),
            )
        }
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

// pf-1

/**
 * The chip colours of [document] in the mode [isDark] picks, drawn all at once the first time and
 * brought up to date one chip per [pause] after that.
 */
@Composable
private fun rememberChipShelf(
    document: ThemeDocument,
    isDark: Boolean,
    lookup: StyleSchemeLookup,
    pause: ChipPause,
): ChipShelf {
    val shelf = remember { ChipShelf(document, isDark, lookup) }
    val currentLookup by rememberUpdatedState(lookup)
    val currentPause by rememberUpdatedState(pause)
    LaunchedEffect(shelf, document, isDark) { shelf.catchUp(document, isDark, currentLookup, currentPause) }
    return shelf
}

/**
 * The colours each style chip shows, kept apart from the document so a change to it never draws a
 * chip inside the frame that brings it (PB-05).
 *
 * Each chip remembers what it was drawn from. [catchUp] draws again every chip the document has moved
 * on from, the one left waiting longest first, and pauses before each. A drag that moves the scheme
 * every frame starts a new catch up every frame, so the chips take turns, each a few frames behind at
 * most, and all of them are current again about ten frames after the drag stops.
 *
 * The pause is a frame rather than a `yield`, since on the web Compose runs a yielded effect again
 * inside the same frame.
 */
@Stable
private class ChipShelf(
    document: ThemeDocument,
    isDark: Boolean,
    lookup: StyleSchemeLookup,
) {
    /** What each chip was last drawn from. Only the catch up reads it. */
    private val drawnFrom = mutableMapOf<Style, ChipKey>()

    /** When each chip was last drawn, counted in draws, so the longest waiting goes first. */
    private val drawnAt = mutableMapOf<Style, Int>()
    private var draws = 0

    private val shown: Map<Style, MutableState<ChipColors>> =
        Style.entries.associateWith { style ->
            mutableStateOf(draw(style, ChipKey.of(document, style, isDark), lookup))
        }

    /** The colours [style]'s chip shows now. */
    operator fun get(style: Style): ChipColors = shown.getValue(style).value

    /** Draws again every chip [document] in the mode [isDark] picks has moved on from, pausing before each. */
    suspend fun catchUp(
        document: ThemeDocument,
        isDark: Boolean,
        lookup: StyleSchemeLookup,
        pause: ChipPause,
    ) {
        val behind = Style.entries
            .map { style -> style to ChipKey.of(document, style, isDark) }
            .filter { (style, key) -> drawnFrom[style] != key }
            .sortedBy { (style, _) -> drawnAt.getValue(style) }
        for ((style, key) in behind) {
            pause()
            shown.getValue(style).value = draw(style, key, lookup)
        }
    }

    private fun draw(
        style: Style,
        key: ChipKey,
        lookup: StyleSchemeLookup,
    ): ChipColors {
        drawnFrom[style] = key
        drawnAt[style] = draws++
        return ChipColors.of(lookup(key.inputs, key.isDark))
    }
}

/** Waits for the next frame, so a catch up draws one chip a frame. */
private val NextFrame: ChipPause = { withFrameNanos {} }

/**
 * What one chip is drawn from.
 *
 * @property[inputs] The scheme inputs its style makes of the document.
 * @property[isDark] The mode the preview shows.
 */
private data class ChipKey(
    val inputs: SchemeInputs,
    val isDark: Boolean,
) {
    companion object {
        fun of(
            document: ThemeDocument,
            style: Style,
            isDark: Boolean,
        ): ChipKey = ChipKey(SchemeInputs.from(document.copy(style = style)), isDark)
    }
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
