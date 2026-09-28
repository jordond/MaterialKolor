package com.materialkolor.builder.kit.skin.headless

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.takesTonalFill
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.readableInk

/**
 * The fill, ink and outline of one state of an action.
 *
 * @property[container] The fill, transparent for a control that only has an outline or nothing.
 * @property[content] The label and icon ink.
 * @property[border] The outline, transparent for none.
 */
@Immutable
internal data class ActionColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

/**
 * A primary button filled with [fill], a colour of the user's own rather than the skin's. Its label
 * and glyphs take [readableInk] and its outline the same ink at [FillHairlineAlpha], so a fill close
 * to the panel behind it still shows where the button ends.
 */
internal fun filledActionColors(fill: Color): ActionColors {
    val ink = readableInk(fill)
    return ActionColors(container = fill, content = ink, border = ink.copy(alpha = FillHairlineAlpha))
}

/**
 * How strongly a filled button's outline takes its ink.
 */
internal const val FillHairlineAlpha: Float = 0.28f

/**
 * How a button and an icon button look, one set of colours per [Emphasis].
 */
@Immutable
internal data class ButtonStyle(
    val shape: Shape,
    val iconShape: Shape,
    val height: Dp,
    val horizontalPadding: Dp,
    val gap: Dp,
    val borderWidth: Dp,
    val primary: ActionColors,
    val secondary: ActionColors,
    val subtle: ActionColors,
    val danger: ActionColors,
    val tonal: ActionColors,
) {
    fun colors(emphasis: Emphasis): ActionColors =
        when (emphasis) {
            Emphasis.Primary -> primary
            Emphasis.Secondary -> secondary
            Emphasis.Subtle -> subtle
            Emphasis.Danger -> danger
        }

    /**
     * The colours of [emphasis], or the [tonal] fill for a secondary or subtle button that asks for it.
     */
    fun colors(
        emphasis: Emphasis,
        tonal: Boolean,
    ): ActionColors = if (takesTonalFill(emphasis, tonal)) this.tonal else colors(emphasis)
}

/**
 * Anything with an on and an off look, a toggle button, a chip or one option of a segmented control.
 */
@Immutable
internal data class SelectableStyle(
    val shape: Shape,
    val height: Dp,
    val horizontalPadding: Dp,
    val gap: Dp,
    val borderWidth: Dp,
    val off: ActionColors,
    val on: ActionColors,
) {
    fun colors(selected: Boolean): ActionColors = if (selected) on else off
}

/**
 * The frame around a segmented control and the look of each option inside it.
 *
 * @property[endRingOffset] How far the focus ring stands off the rounded end of the first and the
 * last option. Where an end option is as round as the frame's end, a ring at the usual offset
 * follows the frame's own outline and cannot be told from it, so the frame's inset takes it just
 * outside. Its top and bottom keep the usual offset, in line with the rings of
 * the options between.
 */
@Immutable
internal data class SegmentedStyle(
    val shape: Shape,
    val inset: Dp,
    val borderWidth: Dp,
    val colors: ActionColors,
    val option: SelectableStyle,
    val endRingOffset: Dp = FocusRingOffset,
)

/**
 * A badge, one set of colours per [BadgeStatus].
 */
@Immutable
internal data class BadgeStyle(
    val shape: Shape,
    val height: Dp,
    val horizontalPadding: Dp,
    val gap: Dp,
    val borderWidth: Dp,
    val neutral: ActionColors,
    val info: ActionColors,
    val success: ActionColors,
    val warning: ActionColors,
    val danger: ActionColors,
) {
    fun colors(status: BadgeStatus): ActionColors =
        when (status) {
            BadgeStatus.Neutral -> neutral
            BadgeStatus.Info -> info
            BadgeStatus.Success -> success
            BadgeStatus.Warning -> warning
            BadgeStatus.Danger -> danger
        }
}

@Immutable
internal data class CardStyle(
    val shape: Shape,
    val borderWidth: Dp,
    val padding: Dp,
    val gap: Dp,
    val colors: ActionColors,
)

@Immutable
internal data class DividerStyle(
    val color: Color,
    val thickness: Dp,
)

/**
 * How an indeterminate bar moves.
 *
 * No motion duration fits a loop. The longest one, the reveal, is under a third of a trip, so the
 * pace lives here with the rest of the bar's look.
 *
 * @property[periodMillis] How long the sweep takes to cross the track once.
 * @property[fraction] How much of the track the sweep covers.
 */
@Immutable
internal data class ProgressSweep(
    val periodMillis: Int,
    val fraction: Float,
)

/**
 * The sweep every skin runs, Material3 included, so a bar keeps the same pace in both.
 */
internal val ActionSweep: ProgressSweep = ProgressSweep(periodMillis = 1400, fraction = 0.4f)

@Immutable
internal data class ProgressStyle(
    val shape: Shape,
    val height: Dp,
    val track: Color,
    val indicator: Color,
    val sweep: ProgressSweep,
)

@Immutable
internal data class ListRowStyle(
    val shape: Shape,
    val minHeight: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val gap: Dp,
    val borderWidth: Dp,
    val idle: ActionColors,
    val selected: ActionColors,
    val supporting: Color,
)

/**
 * Everything the actions and display controls read from a skin that has no components of its own.
 */
@Immutable
internal data class ActionStyles(
    val button: ButtonStyle,
    val toggleButton: SelectableStyle,
    val chip: SelectableStyle,
    val segmented: SegmentedStyle,
    val badge: BadgeStyle,
    val card: CardStyle,
    val divider: DividerStyle,
    val progress: ProgressStyle,
    val listRow: ListRowStyle,
)

/**
 * The shapes and sizes that tell one headless look from another. The colours come from the skin's
 * tokens the same way for every look.
 *
 * @property[filledSecondary] Whether a secondary button sits on a raised fill.
 * @property[borderedSecondary] Whether a secondary button has an outline.
 */
@Immutable
internal data class ActionMetrics(
    val controlShape: Shape,
    val iconButtonShape: Shape,
    val chipShape: Shape,
    val cardShape: Shape,
    val rowShape: Shape,
    val badgeShape: Shape,
    val barShape: Shape,
    val controlHeight: Dp,
    val chipHeight: Dp,
    val badgeHeight: Dp,
    val barHeight: Dp,
    val horizontalPadding: Dp,
    val borderWidth: Dp,
    val filledSecondary: Boolean,
    val borderedSecondary: Boolean,
)

/**
 * The Custom skin's actions, pill shaped and a little roomier, with raised fills instead of outlines.
 */
internal val CustomActionStyles: ActionStyles
    @Composable get() = rememberActionStyles(::customActionStyles)

/**
 * The styles [build] cuts from the surrounding skin's tokens, rebuilt only when the tokens change.
 */
@Composable
internal fun rememberActionStyles(build: (BuilderTokens) -> ActionStyles): ActionStyles {
    val tokens = LocalBuilderTokens.current
    return remember(tokens, build) { build(tokens) }
}

private fun customActionStyles(tokens: BuilderTokens): ActionStyles {
    val pill = RoundedCornerShape(percent = 50)
    val styles = actionStyles(
        tokens = tokens,
        metrics = ActionMetrics(
            controlShape = pill,
            iconButtonShape = CircleShape,
            chipShape = pill,
            cardShape = RoundedCornerShape(tokens.radius.medium),
            rowShape = RoundedCornerShape(tokens.radius.medium),
            badgeShape = pill,
            barShape = pill,
            controlHeight = 40.dp,
            chipHeight = 32.dp,
            badgeHeight = 24.dp,
            barHeight = 6.dp,
            horizontalPadding = tokens.spacing.large,
            borderWidth = 1.dp,
            filledSecondary = true,
            borderedSecondary = false,
        ),
    )
    // The pill options end as round as the pill frame, so the end rings stand outside the frame.
    return styles.copy(segmented = styles.segmented.copy(endRingOffset = styles.segmented.inset))
}

/**
 * One headless look, coloured from [tokens].
 *
 * Primary actions and anything switched on use the accent. Status badges use the status ink as a
 * fill with the panel as their ink, which reads because the status inks are cut to read on a panel.
 */
internal fun actionStyles(
    tokens: BuilderTokens,
    metrics: ActionMetrics,
): ActionStyles {
    val none = Color.Transparent
    val accent = ActionColors(tokens.accent, tokens.onAccent, none)
    val outlined = ActionColors(none, tokens.textStrong, tokens.borderStrong)
    val secondary = ActionColors(
        container = if (metrics.filledSecondary) tokens.panelRaised else none,
        content = tokens.textStrong,
        border = if (metrics.borderedSecondary) tokens.borderStrong else none,
    )
    val gap = tokens.spacing.small
    val selectable = SelectableStyle(
        shape = metrics.controlShape,
        height = metrics.controlHeight,
        horizontalPadding = metrics.horizontalPadding,
        gap = gap,
        borderWidth = metrics.borderWidth,
        off = secondary.copy(border = tokens.borderStrong),
        on = accent.copy(border = tokens.accent),
    )
    return ActionStyles(
        button = ButtonStyle(
            shape = metrics.controlShape,
            iconShape = metrics.iconButtonShape,
            height = metrics.controlHeight,
            horizontalPadding = metrics.horizontalPadding,
            gap = gap,
            borderWidth = metrics.borderWidth,
            primary = accent,
            secondary = secondary,
            subtle = ActionColors(none, tokens.textMuted, none),
            danger = ActionColors(none, tokens.danger, tokens.danger),
            tonal = ActionColors(tokens.panelRaised, tokens.textStrong, none),
        ),
        toggleButton = selectable,
        chip = selectable.copy(
            shape = metrics.chipShape,
            height = metrics.chipHeight,
            horizontalPadding = tokens.spacing.medium,
            off = outlined,
        ),
        segmented = SegmentedStyle(
            shape = metrics.controlShape,
            inset = tokens.spacing.extraSmall,
            borderWidth = metrics.borderWidth,
            colors = ActionColors(none, tokens.textStrong, tokens.borderStrong),
            option = selectable.copy(
                height = metrics.chipHeight,
                borderWidth = 0.dp,
                off = ActionColors(none, tokens.textStrong, none),
            ),
        ),
        badge = BadgeStyle(
            shape = metrics.badgeShape,
            height = metrics.badgeHeight,
            horizontalPadding = tokens.spacing.small,
            gap = tokens.spacing.extraSmall,
            borderWidth = metrics.borderWidth,
            neutral = ActionColors(tokens.panelRaised, tokens.textStrong, tokens.border),
            info = accent,
            success = ActionColors(tokens.success, tokens.panel, none),
            warning = ActionColors(tokens.warning, tokens.panel, none),
            danger = ActionColors(tokens.danger, tokens.panel, none),
        ),
        card = CardStyle(
            shape = metrics.cardShape,
            borderWidth = metrics.borderWidth,
            padding = tokens.spacing.large,
            gap = gap,
            colors = ActionColors(tokens.panel, tokens.textStrong, tokens.border),
        ),
        divider = DividerStyle(color = tokens.border, thickness = 1.dp),
        progress = ProgressStyle(
            shape = metrics.barShape,
            height = metrics.barHeight,
            track = tokens.border,
            indicator = tokens.accent,
            sweep = ActionSweep,
        ),
        listRow = ListRowStyle(
            shape = metrics.rowShape,
            minHeight = metrics.controlHeight,
            horizontalPadding = tokens.spacing.medium,
            verticalPadding = tokens.spacing.small,
            gap = tokens.spacing.medium,
            borderWidth = metrics.borderWidth,
            idle = ActionColors(none, tokens.textStrong, none),
            selected = ActionColors(tokens.panelRaised, tokens.textStrong, tokens.borderStrong),
            supporting = tokens.textMuted,
        ),
    )
}

/**
 * Fills and outlines the control in [colors]. A transparent fill or outline draws nothing.
 */
internal fun Modifier.actionSurface(
    colors: ActionColors,
    shape: Shape,
    borderWidth: Dp,
): Modifier {
    val filled = if (colors.container.alpha > 0f) background(colors.container, shape) else this
    return if (borderWidth > 0.dp &&
        colors.border.alpha > 0f
    ) {
        filled.border(borderWidth, colors.border, shape)
    } else {
        filled
    }
}
