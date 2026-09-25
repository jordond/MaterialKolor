package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.ButtonKeycap
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.HeadlessBadge
import com.materialkolor.builder.kit.control.HeadlessCard
import com.materialkolor.builder.kit.control.HeadlessChoiceChips
import com.materialkolor.builder.kit.control.HeadlessDivider
import com.materialkolor.builder.kit.control.HeadlessFilterChip
import com.materialkolor.builder.kit.control.HeadlessListRow
import com.materialkolor.builder.kit.control.HeadlessProgress
import com.materialkolor.builder.kit.control.ListRowContent
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.iconButtonSemantics
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.headless.ActionColors
import com.materialkolor.builder.kit.skin.headless.ActionMetrics
import com.materialkolor.builder.kit.skin.headless.ActionStyles
import com.materialkolor.builder.kit.skin.headless.actionStyles
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.github.composefluent.Colors
import io.github.composefluent.FluentTheme
import io.github.composefluent.LocalContentColor
import io.github.composefluent.background.BackgroundSizing
import io.github.composefluent.background.Layer
import io.github.composefluent.component.ButtonColor
import io.github.composefluent.component.ButtonDefaults
import io.github.composefluent.component.ToggleButton
import io.github.composefluent.scheme.VisualStateScheme
import io.github.composefluent.scheme.collectVisualState

/*
 * The Fluent actions. The buttons, the icon button and the toggle button are Fluent's own, with the
 * builder's press scale, focus ring and touch target laid over them the way the Material ones have.
 * Chips, badges, cards, rows, dividers and the progress bar have no Fluent component that fits the
 * kit, so they stay the headless layer drawn in Fluent's own colours and metrics.
 */

/**
 * Grows the footprint to the layout's touch target, then shrinks on press and rings on focus inside
 * it. Fluent draws no focus visual of its own on a button, so the builder's ring is the only one.
 */
@Composable
private fun Modifier.fluentFeedback(interactionSource: MutableInteractionSource): Modifier =
    controlTouchTarget(LocalLayout.current.primaryTouchTarget)
        .controlPress(interactionSource)
        .controlRing(interactionSource, FluentTheme.shapes.control)

/** A glyph and a label in whatever ink the surrounding Fluent layer provides. */
@Composable
private fun FluentLabel(
    label: String,
    icon: IconId?,
) {
    val ink = LocalContentColor.current
    if (icon != null) BuilderIcon(icon, contentDescription = null, tint = ink)
    BuilderText(label, style = BuilderTextStyle.Label, color = ink, maxLines = 1)
}

/**
 * Fluent's colours for each emphasis. Fluent has no destructive button, so danger is an accent
 * button in the critical ink. On the poster every one draws in the poster's ink, with primary and
 * danger filled in it.
 */
@Composable
internal fun fluentButtonColors(emphasis: Emphasis): VisualStateScheme<ButtonColor> {
    val fluent = fluentOwnButtonColors(emphasis)
    val poster = LocalFluentPosterInk.current ?: return fluent
    return poster.buttons(fluent, accent = emphasis == Emphasis.Primary || emphasis == Emphasis.Danger)
}

@Composable
private fun fluentOwnButtonColors(emphasis: Emphasis): VisualStateScheme<ButtonColor> =
    when (emphasis) {
        Emphasis.Primary -> {
            ButtonDefaults.accentButtonColors()
        }
        Emphasis.Secondary -> {
            ButtonDefaults.buttonColors()
        }
        Emphasis.Subtle -> {
            ButtonDefaults.subtleButtonColors()
        }
        Emphasis.Danger -> {
            val colors = FluentTheme.colors
            val critical = colors.system.critical
            val rest = ButtonColor(critical, colors.text.onAccent.primary, colors.borders.accentControl)
            ButtonDefaults.accentButtonColors(
                default = rest,
                hovered = rest.copy(fillColor = critical.copy(alpha = HoveredFillAlpha)),
                pressed = ButtonColor(
                    fillColor = critical.copy(alpha = PressedFillAlpha),
                    contentColor = colors.text.onAccent.secondary,
                    borderBrush = SolidColor(colors.stroke.control.onAccentDefault),
                ),
            )
        }
    }

/** How much of its fill Fluent keeps on an accent button under the pointer. */
private const val HoveredFillAlpha = 0.9f

/** How much of its fill Fluent keeps on an accent button while it is pressed. */
private const val PressedFillAlpha = 0.8f

/** Fluent's control height. */
private val FluentControlHeight = 32.dp

/** The room Fluent's button keeps either side of its label. */
private val FluentButtonPadding = 12.dp

/**
 * The Fluent style set for the actions Fluent has no component for, four dp corners, 32 dp controls
 * and Fluent's own fills. Fluent's inks and fills are translucent, so each is laid over the panel it
 * sits on, the way the tokens are.
 */
private val FluentActionStyles: ActionStyles
    @Composable get() {
        val tokens = LocalBuilderTokens.current
        val colors = FluentTheme.colors
        val poster = LocalFluentPosterInk.current
        return remember(tokens, colors, poster) { fluentActionStyles(tokens, colors, poster) }
    }

private fun fluentActionStyles(
    tokens: BuilderTokens,
    colors: Colors,
    poster: FluentPosterInk?,
): ActionStyles {
    val corner = RoundedCornerShape(4.dp)
    val styles = actionStyles(
        tokens = tokens,
        metrics = ActionMetrics(
            controlShape = corner,
            iconButtonShape = corner,
            chipShape = corner,
            cardShape = RoundedCornerShape(FluentCardRadius),
            rowShape = corner,
            badgeShape = RoundedCornerShape(percent = 50),
            barShape = RoundedCornerShape(percent = 50),
            controlHeight = FluentControlHeight,
            chipHeight = 28.dp,
            badgeHeight = 20.dp,
            barHeight = 3.dp,
            horizontalPadding = tokens.spacing.medium,
            borderWidth = 1.dp,
            filledSecondary = true,
            borderedSecondary = true,
        ),
    )
    val panel = tokens.panel

    fun Color.onPanel(): Color = compositeOver(panel)
    val control = ActionColors(colors.control.default.onPanel(), tokens.textStrong, tokens.borderStrong)
    return styles.copy(
        chip = styles.chip.copy(off = control),
        card = styles.card.copy(
            colors = ActionColors(
                container = colors.background.card.default
                    .onPanel(),
                content = tokens.textStrong,
                border = poster?.outline ?: colors.stroke.card.default
                    .onPanel(),
            ),
        ),
        badge = styles.badge.copy(
            neutral = ActionColors(colors.controlSolid.default, tokens.textStrong, tokens.borderStrong),
        ),
        divider = styles.divider.copy(
            color = poster?.outline ?: colors.stroke.divider.default
                .onPanel(),
        ),
        progress = styles.progress.copy(
            track = colors.controlStrong.default.onPanel(),
            indicator = poster?.ink ?: colors.fillAccent.default,
        ),
        listRow = styles.listRow.copy(
            selected = styles.listRow.selected.copy(container = colors.subtleFill.secondary.onPanel()),
        ),
    )
}

/** How round Fluent draws a card, its overlay corner. */
private val FluentCardRadius = 8.dp

/**
 * Fluent's `Button`, `AccentButton` and `SubtleButton`, drawn from the same Fluent layer and colours.
 *
 * Fluent's own button takes the press on a row inside its layer, so a name folded onto the modifier
 * and a test tag would both land on a node that is not the button. This puts the press on the layer
 * itself, which keeps the whole button one node the way every other skin's is.
 */
@Composable
internal fun FluentButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier,
    emphasis: Emphasis,
    icon: IconId?,
    enabled: Boolean,
    hint: String? = null, // b-510
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = fluentButtonColors(emphasis).schemeFor(interactionSource.collectVisualState(disabled = !enabled))
    FluentButtonLayer(
        colors = colors,
        emphasis = emphasis,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).foldState(label, null, enabled)
            .fluentFeedback(interactionSource)
            .defaultMinSize(minHeight = FluentControlHeight),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FluentButtonPadding),
            horizontalArrangement = Arrangement.spacedBy(ButtonDefaults.iconSpacing, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FluentLabel(label, icon)
            if (hint != null) ButtonKeycap(hint, LocalContentColor.current) // b-510
        }
    }
}

/**
 * Fluent's icon only button, a square of the same Fluent layer and colours as [FluentButton], with
 * the press on the layer for the same reason. The glyph carries the name, with the open or closed
 * state of a panel it shows folded in.
 */
@Composable
internal fun FluentIconButton(
    onClick: () -> Unit,
    icon: IconId,
    contentDescription: String,
    modifier: Modifier,
    emphasis: Emphasis,
    enabled: Boolean,
    expanded: Boolean?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = fluentButtonColors(emphasis).schemeFor(interactionSource.collectVisualState(disabled = !enabled))
    val spoken = iconButtonSemantics(contentDescription, enabled, expanded)
    FluentButtonLayer(
        colors = colors,
        emphasis = emphasis,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).then(spoken.state)
            .fluentFeedback(interactionSource)
            .size(FluentControlHeight),
    ) {
        Box(contentAlignment = Alignment.Center) {
            BuilderIcon(icon, contentDescription = spoken.name, tint = LocalContentColor.current)
        }
    }
}

/**
 * Fluent's button fill and outline round [content], in [colors] for its visual state.
 */
@Composable
private fun FluentButtonLayer(
    colors: ButtonColor,
    emphasis: Emphasis,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val effects = LocalBuilderMotion.current.effects<Color>()
    val fill by animateColorAsState(colors.fillColor, effects, label = "fill")
    val ink by animateColorAsState(colors.contentColor, effects, label = "ink")
    Layer(
        modifier = modifier,
        shape = FluentTheme.shapes.control,
        color = fill,
        contentColor = ink,
        border = BorderStroke(LocalBuilderTokens.current.outlineWidth, colors.borderBrush),
        // Fluent's Button stops the fill inside the outline unless accentButton is set, and only Secondary has one.
        backgroundSizing = if (emphasis == Emphasis.Secondary) {
            BackgroundSizing.InnerBorderEdge
        } else {
            BackgroundSizing.OuterBorderEdge
        },
        content = content,
    )
}

/**
 * Fluent's own `ToggleButton`.
 *
 * Fluent marks it selected rather than on, so the on or off state goes on beside it for whatever
 * reads a checkbox's state. It shows a check in place of its icon while it is on, so on and off
 * never differ by fill alone (AR-03).
 */
@Composable
internal fun FluentToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    icon: IconId?,
    enabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    ToggleButton(
        checked = checked,
        onCheckedChanged = onCheckedChange,
        modifier = modifier
            .foldState(label, ControlState.Checked(checked), enabled)
            .semantics { toggleableState = ToggleableState(checked) }
            .fluentFeedback(interactionSource),
        disabled = !enabled,
        // Fluent's own pair, an accent button while on and a standard one while off.
        colors = fluentButtonColors(if (checked) Emphasis.Primary else Emphasis.Secondary),
        interaction = interactionSource,
    ) {
        FluentLabel(label, if (checked) IconId.Check else icon)
    }
}

@Composable
internal fun FluentFilterChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    icon: IconId?,
    enabled: Boolean,
) {
    HeadlessFilterChip(selected, onSelectedChange, label, FluentActionStyles.chip, modifier, icon, enabled)
}

@Composable
internal fun <T> FluentChoiceChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    optionIcon: (T) -> IconId?,
    selectOnFocus: Boolean,
    optionLabel: (T) -> String,
) {
    HeadlessChoiceChips(
        options = options,
        selected = selected,
        onSelect = onSelect,
        label = label,
        style = FluentActionStyles.chip,
        modifier = modifier,
        enabled = enabled,
        optionIcon = optionIcon,
        selectOnFocus = selectOnFocus,
        optionLabel = optionLabel,
    )
}

@Composable
internal fun FluentBadge(
    label: String,
    modifier: Modifier,
    status: BadgeStatus,
    icon: IconId?,
) {
    HeadlessBadge(label, FluentActionStyles.badge, modifier, status, icon)
}

@Composable
internal fun FluentCard(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    HeadlessCard(FluentActionStyles.card, modifier, onClick, enabled, content)
}

@Composable
internal fun FluentDivider(
    modifier: Modifier,
    orientation: Orientation,
) {
    HeadlessDivider(FluentActionStyles.divider, modifier, orientation)
}

@Composable
internal fun FluentProgress(
    label: String,
    modifier: Modifier,
    progress: Float?,
) {
    HeadlessProgress(label, FluentActionStyles.progress, modifier, progress)
}

@Composable
internal fun FluentListRow(
    row: ListRowContent,
    modifier: Modifier,
) {
    HeadlessListRow(row, FluentActionStyles.listRow, modifier)
}
