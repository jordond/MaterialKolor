package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.headless.radioGroupOption
import com.materialkolor.builder.kit.headless.rememberRadioGroupFocus
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * Material's filter chips as one single choice group.
 *
 * Material has no single choice chip row, so the row is a selectable group and each chip reads out
 * as a radio button in place of the filter chip's checkbox. The radio group's roving focus and arrow
 * keys are laid over it, since Material's chips move neither.
 */
@Composable
internal fun <T> MaterialChoiceChips(
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
    val selectedIndex = options.indexOf(selected)
    val focus = rememberRadioGroupFocus(options.size, selectedIndex)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val spacing = LocalBuilderTokens.current.spacing.small
    val asText = LocalFoldsStateIntoName.current
    MaterialTarget {
        FlowRow(
            modifier = modifier
                .selectableGroup()
                .semantics { roleLessName(label, asText) },
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, value ->
                key(index) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isSelected = index == selectedIndex
                    val glyph = if (isSelected) IconId.Check else optionIcon(value)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(value) },
                        label = {
                            BuilderText(
                                optionLabel(value),
                                style = BuilderTextStyle.Label,
                                color = LocalContentColor.current,
                                maxLines = 1,
                            )
                        },
                        modifier = Modifier
                            .semantics { role = Role.RadioButton }
                            .radioGroupOption(focus, index, selectedIndex, rtl, selectOnFocus) { target ->
                                onSelect(options[target])
                            }.foldState(
                                name = optionLabel(value),
                                state = ControlState.Selected(isSelected),
                                enabled = enabled,
                                role = FoldedRole.Radio,
                            ).materialFeedback(interactionSource, FilterChipDefaults.shape),
                        enabled = enabled,
                        leadingIcon = glyph?.let { id ->
                            { BuilderIcon(id, contentDescription = null, tint = LocalContentColor.current) }
                        },
                        interactionSource = interactionSource,
                    )
                }
            }
        }
    }
}
