package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.command.LocalAppleKeys
import com.materialkolor.builder.feature.command.Shortcut
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.topbar_commands
import com.materialkolor.builder.generated.resources.topbar_commands_tooltip
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * An icon button under a tooltip that says what it does, the description unless [tooltip] says more.
 */
@Composable
internal fun TopBarIconButton(
    control: TopBarControl,
    focus: TopBarFocus,
    icon: IconId,
    description: String,
    onClick: () -> Unit,
    tooltip: String = description,
    enabled: Boolean = true,
) {
    BuilderTooltip(text = tooltip) {
        BuilderIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = description,
            modifier = Modifier.topBarFocus(focus, control),
            enabled = enabled,
        )
    }
}

/**
 * The command palette's button, a search glyph, and with [keycap] the palette's key beside it in a
 * keycap. It reads as Command palette either way, with the key in its tooltip.
 */
@Composable
internal fun CommandsButton(
    focus: TopBarFocus,
    keycap: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(Res.string.topbar_commands)
    val keys = Shortcut.Palette.text(LocalAppleKeys.current)
    val tooltip = stringResource(Res.string.topbar_commands_tooltip, keys)
    if (!keycap) {
        TopBarIconButton(
            control = TopBarControl.Commands,
            focus = focus,
            icon = IconId.Search,
            description = description,
            tooltip = tooltip,
            onClick = onClick,
        )
        return
    }
    val tokens = LocalBuilderTokens.current
    val ink = tokens.textMuted
    BuilderTooltip(text = tooltip) {
        BuilderPressable(
            onClick = onClick,
            label = description,
            modifier = Modifier.topBarFocus(focus, TopBarControl.Commands),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = tokens.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderIcon(IconId.Search, contentDescription = null, tint = ink)
                BuilderText(
                    text = keys,
                    modifier = Modifier
                        .border(tokens.outlineWidth, tokens.border, RoundedCornerShape(tokens.radius.small))
                        .padding(horizontal = tokens.spacing.extraSmall + tokens.spacing.extraSmall / 2),
                    style = BuilderTextStyle.Value,
                    color = ink,
                    maxLines = 1,
                )
            }
        }
    }
}
