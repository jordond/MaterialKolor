package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.audit.ContrastBadge
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.widget_badge_aa
import com.materialkolor.builder.kit.generated.resources.widget_badge_aa_large
import com.materialkolor.builder.kit.generated.resources.widget_badge_aaa
import com.materialkolor.builder.kit.generated.resources.widget_badge_fail
import com.materialkolor.builder.kit.generated.resources.widget_contrast_ratio
import com.materialkolor.builder.kit.generated.resources.widget_copy
import com.materialkolor.builder.kit.generated.resources.widget_pinned
import com.materialkolor.builder.kit.generated.resources.widget_tone
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor
import kotlin.math.roundToInt

/** How tall the colored part of a swatch is. */
private val SwatchColorHeight: Dp = 72.dp

/** The outline of a widget with keyboard focus, thick enough to read on any fill. */
internal val WidgetFocusWidth: Dp = 2.dp

/** Tags the pin badge on a pinned swatch, so a test can find it. */
internal const val SwatchPinTag: String = "swatch-pin"

private const val AAA_TEXT = 7.0
private const val AA_TEXT = 4.5
private const val AA_LARGE = 3.0

/**
 * One role of a scheme, filled with its color and inked with its on-pair.
 *
 * It shows the role name, the hex, the tone the role resolved to and its contrast against [onColor]
 * as a ratio and a WCAG badge. A role with no on-pair, such as outline or an on role, has no ratio to
 * rate, so it passes a null [contrast] and the contrast line stays hidden. It reads out as a button
 * named like "primary, #6750A4, tone 40". A [pinned] role wears a pin beside its name and reads out
 * pinned as its state. The web hears only the name, so there the ratio, the badge and the pinned
 * state travel in it, as in "primary, #6750A4, tone 40, 6.4:1, AA, pinned" (D37, F-22).
 *
 * The copy button is a visible button, not a hover trick. It shows on a touch screen, while the
 * swatch or the button has keyboard focus, and while a mouse is over the swatch. Keyboard focus rings
 * the swatch on the panel around it, where the focus color holds 3 to 1.
 *
 * @param[name] The role name, as in "primary".
 * @param[color] The role's color.
 * @param[onColor] The color that sits on it, which inks the name and the hex.
 * @param[tone] The HCT tone [color] resolved to, read from the scheme at runtime.
 * @param[contrast] The WCAG ratio between [color] and [onColor], or null when the role has no on-pair.
 * @param[onCopy] Called when the copy button is pressed. The caller does the copying.
 * @param[onClick] Called when the swatch itself is pressed.
 * @param[modifier] Applied to the swatch.
 * @param[pinned] Whether the role is pinned to a color of its own.
 */
@Composable
public fun SwatchTile(
    name: String,
    color: Color,
    onColor: Color,
    tone: Double,
    contrast: Double?,
    onCopy: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pinned: Boolean = false,
) {
    val tokens = LocalBuilderTokens.current
    val layout = LocalLayout.current
    val hex = color.hex()
    val toneText = stringResource(Res.string.widget_tone, tone.roundToInt())
    val copyLabel = stringResource(Res.string.widget_copy)
    val pinnedWord = stringResource(Res.string.widget_pinned)
    val badge = contrast?.let(::textBadge)
    val ratioText = contrast?.let { ratio -> stringResource(Res.string.widget_contrast_ratio, oneDecimal(ratio)) }
    val badgeText = badge?.label()
    val folds = LocalFoldsStateIntoName.current
    val semanticsName = if (folds) {
        listOfNotNull(name, hex, toneText, ratioText, badgeText, pinnedWord.takeIf { pinned })
    } else {
        listOf(name, hex, toneText)
    }.joinToString(", ")
    val shape = RoundedCornerShape(tokens.radius.medium)
    val hoverSource = remember { MutableInteractionSource() }
    val tileSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    var focusWithin by remember { mutableStateOf(false) }
    val showCopy = layout.coarsePointer || focusWithin || hovered

    Box(
        modifier = modifier
            .hoverable(hoverSource)
            .onFocusChanged { state -> focusWithin = state.hasFocus },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .controlRing(tileSource, shape)
                .widgetHairline(shape, hovered)
                .clip(shape)
                .background(tokens.panel)
                .clickable(
                    interactionSource = tileSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics {
                    contentDescription = semanticsName
                    if (pinned) stateDescription = pinnedWord
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SwatchColorHeight)
                    .background(color)
                    .padding(tokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BuilderText(
                        name,
                        modifier = Modifier.weight(1f, fill = false),
                        style = BuilderTextStyle.Label,
                        color = onColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pinned) {
                        BuilderIcon(
                            id = IconId.Pin,
                            contentDescription = null,
                            modifier = Modifier.testTag(SwatchPinTag),
                            tint = onColor,
                        )
                    }
                }
                BuilderText(hex, style = BuilderTextStyle.Value, color = onColor)
            }
            Column(
                modifier = Modifier
                    .heightIn(min = layout.primaryTouchTarget)
                    .padding(
                        start = tokens.spacing.medium,
                        top = tokens.spacing.small,
                        bottom = tokens.spacing.small,
                        end = layout.primaryTouchTarget + tokens.spacing.small,
                    ),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
            ) {
                BuilderText(toneText, style = BuilderTextStyle.Value, emphasis = Emphasis.Secondary)
                if (badge != null && ratioText != null && badgeText != null) ContrastLine(ratioText, badge, badgeText)
            }
        }
        if (showCopy) {
            // Placed by a box of its own, since Material's tooltip wraps the modifier it is given.
            Box(Modifier.align(Alignment.BottomEnd).padding(tokens.spacing.extraSmall)) {
                BuilderTooltip(text = copyLabel) {
                    BuilderIconButton(onClick = onCopy, icon = IconId.Copy, contentDescription = copyLabel)
                }
            }
        }
    }
}

/** The ratio against the on-pair and the badge it earns, with an icon so the badge never rests on color. */
@Composable
private fun ContrastLine(
    ratioText: String,
    badge: ContrastBadge,
    badgeText: String,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(ratioText, style = BuilderTextStyle.Value)
        BuilderIcon(
            id = badge.icon,
            contentDescription = null,
            tint = badge.tint(tokens),
            size = tokens.iconSize,
        )
        BuilderText(badgeText, style = BuilderTextStyle.Label)
    }
}

/** The badge a text pair at [ratio] earns under WCAG 2.2. */
internal fun textBadge(ratio: Double): ContrastBadge =
    when {
        ratio >= AAA_TEXT -> ContrastBadge.Aaa
        ratio >= AA_TEXT -> ContrastBadge.Aa
        ratio >= AA_LARGE -> ContrastBadge.AaLarge
        else -> ContrastBadge.Fail
    }

private val ContrastBadge.icon: IconId
    get() = when (this) {
        ContrastBadge.Aaa, ContrastBadge.Aa -> IconId.Check
        ContrastBadge.AaLarge -> IconId.Warning
        ContrastBadge.Fail -> IconId.Error
    }

private fun ContrastBadge.tint(tokens: BuilderTokens): Color =
    when (this) {
        ContrastBadge.Aaa, ContrastBadge.Aa -> tokens.success
        ContrastBadge.AaLarge -> tokens.warning
        ContrastBadge.Fail -> tokens.danger
    }

@Composable
private fun ContrastBadge.label(): String =
    when (this) {
        ContrastBadge.Aaa -> stringResource(Res.string.widget_badge_aaa)
        ContrastBadge.Aa -> stringResource(Res.string.widget_badge_aa)
        ContrastBadge.AaLarge -> stringResource(Res.string.widget_badge_aa_large)
        ContrastBadge.Fail -> stringResource(Res.string.widget_badge_fail)
    }

/**
 * [value] floored to one decimal, "4.4" for 4.49, so the ratio shown never reads higher than the
 * ratio its badge was rated on.
 */
internal fun oneDecimal(value: Double): String {
    val tenths = floor(value * 10).toInt()
    return "${tenths / 10}.${tenths % 10}"
}

/** The color as `#RRGGBB`, the way the rest of the builder writes it. */
internal fun Color.hex(): String = Argb(toArgb()).toHex()

/**
 * The outline a pressable widget draws in place of a skin's indication. The hairline turns strong
 * under a mouse and gives way to a line in the focus color under keyboard focus. A widget ringed
 * outside its edge by [controlRing] keeps [widgetHairline] instead.
 */
@Composable
internal fun Modifier.widgetOutline(
    interactionSource: InteractionSource,
    shape: Shape,
    hovered: Boolean,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    if (!focused) return widgetHairline(shape, hovered)
    return border(WidgetFocusWidth, LocalBuilderTokens.current.focus, shape)
}

/** The hairline around a pressable widget, strong while a mouse is over it. */
@Composable
internal fun Modifier.widgetHairline(
    shape: Shape,
    hovered: Boolean,
): Modifier {
    val tokens = LocalBuilderTokens.current
    val line = if (hovered) tokens.borderStrong else tokens.border
    return border(tokens.outlineWidth, line, shape)
}
