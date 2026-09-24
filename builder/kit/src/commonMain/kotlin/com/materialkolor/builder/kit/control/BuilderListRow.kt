package com.materialkolor.builder.kit.control

import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.FluentListRow
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.ListRowStyle
import com.materialkolor.builder.kit.skin.headless.UnstyledActionStyles
import com.materialkolor.builder.kit.skin.headless.actionSurface
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.skin.material.MaterialListRow

/**
 * One row of a list, such as a saved project.
 *
 * With [onClick] the row is a button named by its text. With [selected] as well it also reports
 * whether it is the current one, and a selected row carries a check so the choice never rests on
 * colour alone. Without [onClick] the row only groups its text for a screen reader.
 *
 * @param[headline] The main line.
 * @param[modifier] Applied to the row.
 * @param[supporting] A quieter second line.
 * @param[icon] A glyph at the start.
 * @param[onClick] Called when the row is pressed, or null for a row that only shows something.
 * @param[selected] Whether this is the current row, or null for a list with no current row.
 * @param[enabled] Whether a pressable row can be pressed.
 * @param[trailing] Something at the end, such as a badge or an icon button of its own.
 */
@Composable
public fun BuilderListRow(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: IconId? = null,
    onClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val row = ListRowContent(headline, supporting, icon, onClick, selected, enabled, trailing)
    when (LocalSkin.current.library) {
        Library.Material3 -> MaterialListRow(row, modifier)
        Library.Unstyled -> HeadlessListRow(row, UnstyledActionStyles.listRow, modifier)
        Library.Fluent -> FluentListRow(row, modifier)
        Library.Custom -> HeadlessListRow(row, CustomActionStyles.listRow, modifier)
    }
}

/** What a list row shows and does, handed whole to each skin. */
internal class ListRowContent(
    val headline: String,
    val supporting: String?,
    val icon: IconId?,
    val onClick: (() -> Unit)?,
    val selected: Boolean?,
    val enabled: Boolean,
    val trailing: (@Composable () -> Unit)?,
)

/**
 * The input and the semantics a list row takes, the same in every skin.
 *
 * A row with a current state is selectable, any other pressable row is clickable, and a row that
 * does nothing merges its text into one node.
 */
internal fun Modifier.listRowInput(
    row: ListRowContent,
    interactionSource: MutableInteractionSource,
    indication: Indication?,
): Modifier {
    val onClick = row.onClick ?: return semantics(mergeDescendants = true) {}
    return if (row.selected != null) {
        selectable(
            selected = row.selected,
            interactionSource = interactionSource,
            indication = indication,
            enabled = row.enabled,
            role = Role.Button,
            onClick = onClick,
        )
    } else {
        clickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = row.enabled,
            role = Role.Button,
            onClick = onClick,
        )
    }
}

/**
 * On the web, a pressable row's current state and its disabled note in its name (D37). A row with
 * neither keeps the name its text gives it, trailing slot included.
 */
@Composable
internal fun Modifier.listRowState(row: ListRowContent): Modifier {
    if (row.onClick == null || (row.selected == null && row.enabled)) return this
    val name = listOfNotNull(row.headline, row.supporting).joinToString(", ")
    return foldState(name, row.selected?.let { current -> ControlState.Selected(current) }, row.enabled)
}

/** A list row drawn from [style]. */
@Composable
internal fun HeadlessListRow(
    row: ListRowContent,
    style: ListRowStyle,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = if (row.selected == true) style.selected else style.idle
    val pressableFeedback = if (row.onClick == null) {
        Modifier
    } else {
        Modifier
            .controlTouchTarget(LocalLayout.current.primaryTouchTarget)
            .controlPress(interactionSource)
            .alpha(enabledAlpha(row.enabled))
            .controlRing(interactionSource, style.shape)
    }
    Row(
        modifier = modifier
            .listRowInput(row, interactionSource, indication = null)
            .listRowState(row)
            .then(pressableFeedback)
            .actionSurface(colors, style.shape, style.borderWidth)
            .heightIn(min = style.minHeight)
            .padding(horizontal = style.horizontalPadding, vertical = style.verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(style.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.icon != null) BuilderIcon(row.icon, contentDescription = null, tint = colors.content)
        Column(Modifier.weight(1f)) {
            BuilderText(row.headline, style = BuilderTextStyle.Label, color = colors.content)
            if (row.supporting != null) {
                BuilderText(row.supporting, style = BuilderTextStyle.Body, color = style.supporting)
            }
        }
        if (row.selected == true) BuilderIcon(IconId.Check, contentDescription = null, tint = colors.content)
        row.trailing?.invoke()
    }
}
