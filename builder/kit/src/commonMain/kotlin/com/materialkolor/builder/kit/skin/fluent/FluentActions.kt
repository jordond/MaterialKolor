package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.HeadlessBadge
import com.materialkolor.builder.kit.control.HeadlessButton
import com.materialkolor.builder.kit.control.HeadlessCard
import com.materialkolor.builder.kit.control.HeadlessChoiceChips
import com.materialkolor.builder.kit.control.HeadlessDivider
import com.materialkolor.builder.kit.control.HeadlessFilterChip
import com.materialkolor.builder.kit.control.HeadlessIconButton
import com.materialkolor.builder.kit.control.HeadlessListRow
import com.materialkolor.builder.kit.control.HeadlessProgress
import com.materialkolor.builder.kit.control.HeadlessSegmented
import com.materialkolor.builder.kit.control.HeadlessToggleButton
import com.materialkolor.builder.kit.control.ListRowContent
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.headless.ActionMetrics
import com.materialkolor.builder.kit.skin.headless.ActionStyles
import com.materialkolor.builder.kit.skin.headless.actionStyles
import com.materialkolor.builder.kit.skin.headless.rememberActionStyles
import com.materialkolor.builder.kit.token.BuilderTokens

// fluent-placeholder

/*
 * The Fluent actions, for now the headless layer in a Windows like style set. Fluent is not a kit
 * dependency yet, so B-403 swaps these for the real components where Fluent has them.
 */

/** The Fluent style set, four dp corners, 32 dp controls and a filled, outlined secondary button. */
private val FluentActionStyles: ActionStyles
    @Composable get() = rememberActionStyles(::fluentActionStyles)

private fun fluentActionStyles(tokens: BuilderTokens): ActionStyles {
    val corner = RoundedCornerShape(4.dp)
    return actionStyles(
        tokens = tokens,
        metrics = ActionMetrics(
            controlShape = corner,
            iconButtonShape = corner,
            chipShape = corner,
            cardShape = RoundedCornerShape(tokens.radius.small),
            rowShape = corner,
            badgeShape = RoundedCornerShape(percent = 50),
            barShape = RoundedCornerShape(percent = 50),
            controlHeight = 32.dp,
            chipHeight = 28.dp,
            badgeHeight = 20.dp,
            barHeight = 3.dp,
            horizontalPadding = tokens.spacing.medium,
            borderWidth = 1.dp,
            filledSecondary = true,
            borderedSecondary = true,
        ),
    )
}

@Composable
internal fun FluentButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier,
    emphasis: Emphasis,
    icon: IconId?,
    enabled: Boolean,
) {
    HeadlessButton(onClick, label, FluentActionStyles.button, modifier, emphasis, icon, enabled)
}

@Composable
internal fun FluentIconButton(
    onClick: () -> Unit,
    icon: IconId,
    contentDescription: String,
    modifier: Modifier,
    emphasis: Emphasis,
    enabled: Boolean,
) {
    HeadlessIconButton(onClick, icon, contentDescription, FluentActionStyles.button, modifier, emphasis, enabled)
}

@Composable
internal fun FluentToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    icon: IconId?,
    enabled: Boolean,
) {
    HeadlessToggleButton(checked, onCheckedChange, label, FluentActionStyles.toggleButton, modifier, icon, enabled)
}

@Composable
internal fun <T> FluentSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    optionIcon: (T) -> IconId?,
    optionLabel: (T) -> String,
) {
    HeadlessSegmented(
        options = options,
        selected = selected,
        onSelect = onSelect,
        label = label,
        style = FluentActionStyles.segmented,
        modifier = modifier,
        enabled = enabled,
        optionIcon = optionIcon,
        optionLabel = optionLabel,
    )
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
