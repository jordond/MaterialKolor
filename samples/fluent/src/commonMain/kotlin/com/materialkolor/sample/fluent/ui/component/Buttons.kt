package com.materialkolor.sample.fluent.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.AccentButton
import io.github.composefluent.component.Button
import io.github.composefluent.component.Icon
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Text

/**
 * A Fluent [Button] with a text label, or an [AccentButton] when [accent] is true.
 *
 * @param[label] What the button says.
 * @param[onClick] What pressing it does.
 * @param[testTag] The test tag, on the node that owns the click.
 * @param[modifier] The modifier for the button.
 * @param[enabled] Whether it can be pressed.
 * @param[accent] Whether it is the accent button of its group.
 * @param[icon] An icon before the label.
 */
@Composable
internal fun LabeledButton(
    label: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val buttonModifier = modifier
        .buttonSemantics(tag = testTag, label = label, enabled = enabled, iconOnly = false, action = onClick)
        .focusStroke(
            visible = focused,
            color = FluentTheme.colors.stroke.focus.outer,
            shape = FluentTheme.shapes.control,
        )
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(text = label)
    }

    if (accent) {
        AccentButton(
            onClick = onClick,
            modifier = buttonModifier,
            disabled = !enabled,
            interaction = interaction,
            content = content,
        )
    } else {
        Button(
            onClick = onClick,
            modifier = buttonModifier,
            disabled = !enabled,
            interaction = interaction,
            content = content,
        )
    }
}

/**
 * A square Fluent [SubtleButton] showing only [icon].
 *
 * @param[icon] The icon.
 * @param[description] What the button does, for screen readers.
 * @param[onClick] What pressing it does.
 * @param[testTag] The test tag, on the node that owns the click.
 * @param[modifier] The modifier for the button.
 */
@Composable
internal fun IconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    SubtleButton(
        onClick = onClick,
        modifier = modifier
            .buttonSemantics(tag = testTag, label = description, enabled = true, iconOnly = true, action = onClick)
            .focusStroke(
                visible = focused,
                color = FluentTheme.colors.stroke.focus.outer,
                shape = FluentTheme.shapes.control,
            ),
        interaction = interaction,
        iconOnly = true,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
    }
}
