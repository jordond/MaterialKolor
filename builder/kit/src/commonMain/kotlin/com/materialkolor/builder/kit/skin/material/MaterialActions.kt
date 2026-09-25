package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.control.BadgeStatus
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ButtonKeycap
import com.materialkolor.builder.kit.control.ButtonSize
import com.materialkolor.builder.kit.control.CardDisabledNote
import com.materialkolor.builder.kit.control.CompactButtonGap
import com.materialkolor.builder.kit.control.CompactButtonHeight
import com.materialkolor.builder.kit.control.CompactButtonIcon
import com.materialkolor.builder.kit.control.CompactButtonPadding
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.ListRowContent
import com.materialkolor.builder.kit.control.ListRowHeadline
import com.materialkolor.builder.kit.control.ListRowSupporting
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.iconButtonSemantics
import com.materialkolor.builder.kit.control.listRowInput
import com.materialkolor.builder.kit.control.listRowState
import com.materialkolor.builder.kit.control.trailingLabel
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/*
 * The Material3 actions. Each one is the library's own component, with the builder's press scale,
 * focus ring and touch target laid over it, and the builder's type and glyphs inside it.
 */

/**
 * Turns Material's own touch target rule off, so [materialFeedback] grows the footprint instead.
 * Material grows it inside the component, which would put the press scale and the focus ring
 * around the grown footprint rather than the control.
 */
@Composable
internal fun MaterialTarget(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
        content = content,
    )
}

/**
 * Grows the footprint to the layout's touch target, then shrinks on press and rings on focus inside
 * it, so both hug the control whatever the footprint.
 */
@Composable
internal fun Modifier.materialFeedback(
    interactionSource: MutableInteractionSource,
    shape: Shape,
): Modifier =
    controlTouchTarget(LocalLayout.current.primaryTouchTarget)
        .controlPress(interactionSource)
        .controlRing(interactionSource, shape)

/**
 * A glyph and a label in whatever ink the surrounding Material component provides.
 */
@Composable
private fun RowScope.MaterialLabel(
    label: String,
    icon: IconId?,
    hint: String? = null,
    trailingIcon: IconId? = null,
    size: ButtonSize = ButtonSize.Regular,
) {
    val ink = LocalContentColor.current
    val compact = size == ButtonSize.Compact
    val iconSize = if (compact) CompactButtonIcon else LocalBuilderTokens.current.iconSize
    val spacing = if (compact) CompactButtonGap else ButtonDefaults.IconSpacing
    if (icon != null) {
        BuilderIcon(icon, contentDescription = null, tint = ink, size = iconSize)
        Spacer(Modifier.width(spacing))
    }
    BuilderText(
        text = label,
        modifier = trailingLabel(trailingIcon),
        style = BuilderTextStyle.Label,
        color = ink,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    if (hint != null) {
        Spacer(Modifier.width(HintSpacing))
        ButtonKeycap(hint, ink)
    }
    if (trailingIcon != null) {
        Spacer(Modifier.width(spacing))
        BuilderIcon(trailingIcon, contentDescription = null, tint = ink, size = iconSize)
    }
}

/**
 * Room round a button with a keycap, Material's small button start and a tight end the keycap fills.
 */
private val HintPadding: PaddingValues = PaddingValues(start = 16.dp, end = 4.dp)

/**
 * Room round a compact button's content.
 */
private val CompactPadding: PaddingValues = PaddingValues(horizontal = CompactButtonPadding)

/**
 * Room between a button's label and its keycap.
 */
private val HintSpacing: Dp = 6.dp

@Composable
internal fun MaterialButton(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier,
    emphasis: Emphasis,
    icon: IconId?,
    enabled: Boolean,
    hint: String? = null,
    trailingIcon: IconId? = null,
    size: ButtonSize = ButtonSize.Regular,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val compact = size == ButtonSize.Compact
    // The drawn pill shrinks inside the footprint materialFeedback grows, so the touch target stays.
    val decorated = modifier
        .foldState(label, null, enabled)
        .materialFeedback(interactionSource, ButtonDefaults.shape)
        .then(if (compact) Modifier.height(CompactButtonHeight) else Modifier)
    val content: @Composable RowScope.() -> Unit = { MaterialLabel(label, icon, hint, trailingIcon, size) }
    MaterialTarget {
        when (emphasis) {
            Emphasis.Primary -> {
                val padding = when {
                    hint != null -> HintPadding
                    compact -> CompactPadding
                    else -> ButtonDefaults.ContentPadding
                }
                Button(
                    onClick,
                    decorated,
                    enabled,
                    contentPadding = padding,
                    interactionSource = interactionSource,
                    content = content,
                )
            }
            Emphasis.Secondary -> {
                OutlinedButton(
                    onClick,
                    decorated,
                    enabled,
                    contentPadding = if (compact) CompactPadding else ButtonDefaults.ContentPadding,
                    interactionSource = interactionSource,
                    content = content,
                )
            }
            Emphasis.Subtle -> {
                TextButton(
                    onClick,
                    decorated,
                    enabled,
                    contentPadding = if (compact) CompactPadding else ButtonDefaults.TextButtonContentPadding,
                    interactionSource = interactionSource,
                    content = content,
                )
            }
            Emphasis.Danger -> {
                Button(
                    onClick = onClick,
                    modifier = decorated,
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    contentPadding = if (compact) CompactPadding else ButtonDefaults.ContentPadding,
                    interactionSource = interactionSource,
                    content = content,
                )
            }
        }
    }
}

@Composable
internal fun MaterialIconButton(
    onClick: () -> Unit,
    icon: IconId,
    contentDescription: String,
    modifier: Modifier,
    emphasis: Emphasis,
    enabled: Boolean,
    expanded: Boolean?,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val spoken = iconButtonSemantics(contentDescription, enabled, expanded)
    val decorated = modifier
        .materialFeedback(interactionSource, IconButtonDefaults.standardShape)
        .then(spoken.state)
    val content: @Composable () -> Unit = {
        BuilderIcon(icon, contentDescription = spoken.name, tint = LocalContentColor.current)
    }
    MaterialTarget {
        when (emphasis) {
            Emphasis.Primary -> {
                FilledIconButton(onClick, decorated, enabled, interactionSource = interactionSource, content = content)
            }
            Emphasis.Secondary -> {
                FilledTonalIconButton(
                    onClick = onClick,
                    modifier = decorated,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    content = content,
                )
            }
            Emphasis.Subtle -> {
                IconButton(onClick, decorated, enabled, interactionSource = interactionSource, content = content)
            }
            Emphasis.Danger -> {
                IconButton(
                    onClick = onClick,
                    modifier = decorated,
                    enabled = enabled,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    interactionSource = interactionSource,
                    content = content,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MaterialToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    icon: IconId?,
    enabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    MaterialTarget {
        ToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier
                .foldState(label, ControlState.Checked(checked), enabled)
                .materialFeedback(interactionSource, ButtonDefaults.shape),
            enabled = enabled,
            interactionSource = interactionSource,
        ) {
            MaterialLabel(label, icon)
        }
    }
}

@Composable
internal fun MaterialFilterChip(
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier,
    icon: IconId?,
    enabled: Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val glyph = if (selected) IconId.Check else icon
    MaterialTarget {
        FilterChip(
            selected = selected,
            onClick = { onSelectedChange(!selected) },
            label = {
                BuilderText(
                    label,
                    style = BuilderTextStyle.Label,
                    color = LocalContentColor.current,
                    maxLines = 1,
                )
            },
            modifier = modifier
                .semantics { toggleableState = ToggleableState(selected) }
                .foldState(label, ControlState.Selected(selected), enabled)
                .materialFeedback(interactionSource, FilterChipDefaults.shape),
            enabled = enabled,
            leadingIcon = glyph?.let { id ->
                { BuilderIcon(id, contentDescription = null, tint = LocalContentColor.current) }
            },
            interactionSource = interactionSource,
        )
    }
}

@Composable
internal fun MaterialBadge(
    label: String,
    modifier: Modifier,
    status: BadgeStatus,
    icon: IconId?,
) {
    val tokens = LocalBuilderTokens.current
    val scheme = MaterialTheme.colorScheme
    val (container, ink) = when (status) {
        BadgeStatus.Neutral -> scheme.secondaryContainer to scheme.onSecondaryContainer
        BadgeStatus.Info -> scheme.primary to scheme.onPrimary
        BadgeStatus.Success -> tokens.success to tokens.panel
        BadgeStatus.Warning -> tokens.warning to tokens.panel
        BadgeStatus.Danger -> scheme.error to scheme.onError
    }
    Badge(
        modifier = modifier.semantics(mergeDescendants = true) {},
        containerColor = container,
        contentColor = ink,
    ) {
        if (icon != null) {
            BuilderIcon(icon, contentDescription = null, tint = ink)
            Spacer(Modifier.width(tokens.spacing.extraSmall))
        }
        BuilderText(label, style = BuilderTextStyle.Value, color = ink, maxLines = 1)
    }
}

@Composable
internal fun MaterialCard(
    modifier: Modifier,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val padded: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            content = content,
        )
    }
    if (onClick == null) {
        Card(modifier.semantics { isTraversalGroup = true }, content = padded)
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        modifier = modifier
            .semantics { role = Role.Button }
            .controlPress(interactionSource)
            .controlRing(interactionSource, CardDefaults.shape),
        enabled = enabled,
        interactionSource = interactionSource,
    ) {
        padded()
        CardDisabledNote(enabled)
    }
}

@Composable
internal fun MaterialDivider(
    modifier: Modifier,
    orientation: Orientation,
) {
    when (orientation) {
        Orientation.Horizontal -> HorizontalDivider(modifier)
        Orientation.Vertical -> VerticalDivider(modifier)
    }
}

@Composable
internal fun MaterialListRow(
    row: ListRowContent,
    modifier: Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val current = row.selected == true
    val feedback = if (row.onClick == null) {
        Modifier
    } else {
        Modifier
            .controlPress(interactionSource)
            .alpha(enabledAlpha(row.enabled))
            .controlRing(interactionSource, RectangleShape)
    }
    ListItem(
        headlineContent = { ListRowHeadline(row.headline, LocalContentColor.current) },
        modifier = modifier
            .listRowInput(row, interactionSource, indication = ripple())
            .listRowState(row)
            .then(feedback),
        supportingContent = row.supporting?.let { text ->
            { ListRowSupporting(text, LocalContentColor.current) }
        },
        leadingContent = row.icon?.let { id ->
            { BuilderIcon(id, contentDescription = null, tint = LocalContentColor.current) }
        } ?: row.leading,
        trailingContent = if (current || row.trailing != null) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (current) BuilderIcon(IconId.Check, contentDescription = null, tint = LocalContentColor.current)
                    row.trailing?.invoke()
                }
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(
            containerColor = if (current) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        ),
    )
}
