package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.icon.IconId

/**
 * Material's filter chip, a check in place of its icon while it is on.
 */
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
    val shape = FilterChipDefaults.shape
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
                .materialFeedback(interactionSource, shape),
            enabled = enabled,
            shape = shape,
            border = FilterChipDefaults.filterChipBorder(enabled, selected),
            leadingIcon = glyph?.let { id ->
                { BuilderIcon(id, contentDescription = null, tint = LocalContentColor.current) }
            },
            interactionSource = interactionSource,
        )
    }
}
