package com.materialkolor.builder.preview.unstyled

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.collectIsFocusVisibleAsState
import com.composeunstyled.focusRing
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import androidx.compose.ui.semantics.Role as SemanticsRole

// Compose Unstyled has no spacing or type scale, so the dashboard keeps its own here, the way an
// app on the library would.
internal val Gap = 8.dp
internal val SectionGap = 16.dp
internal val PageGap = 24.dp
internal val CardShape = RoundedCornerShape(12.dp)
internal val ControlShape = RoundedCornerShape(8.dp)
internal val PillShape = RoundedCornerShape(percent = 50)
internal val IconSize = 18.dp
internal val TitleStyle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
internal val HeadingStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
internal val ValueStyle = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
internal val BodyStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
internal val LabelStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
internal val SmallStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)

private val ControlHeight = 36.dp
private val IconButtonSize = 40.dp
private val FocusRingWidth = 2.dp
private val TooltipGap = 6.dp
private const val PanelMillis = 200

/**
 * How a panel opens and closes, at once when motion is frozen for a screenshot or the user asked
 * for less of it.
 */
@Composable
internal fun <T> panelMotion(): FiniteAnimationSpec<T> =
    if (LocalMotionFrozen.current || LocalReducedMotion.current) snap() else tween(PanelMillis)

/** Where a floating part sits against the box it belongs to. */
internal enum class Overhang {
    /** Centered under the box. Align it to the bottom center. */
    Below,

    /** Under the box, lined up with its start edge. Align it to the bottom start. */
    BelowStart,

    /** Under the box, lined up with its end edge. Align it to the bottom end. */
    BelowEnd,

    /** Past the box's end edge, centered on it. Align it to the center end. */
    After,
}

/**
 * Lay the part out at no size of its own, [overhang] of the point it is aligned to, so it floats
 * over whatever the layout draws next. This is how the tooltips show in place, with no popup or
 * window. Keep it to parts nobody presses, since what floats past its box is not where its box
 * says it is. The box holding it has to draw above its neighbours, which a `zIndex` on
 * the box sees to.
 */
internal fun Modifier.overhang(
    overhang: Overhang,
    gap: Dp,
): Modifier =
    layout { measurable, _ ->
        val placeable = measurable.measure(Constraints())
        val space = gap.roundToPx()
        layout(0, 0) {
            when (overhang) {
                Overhang.Below -> placeable.placeRelative(-placeable.width / 2, space)
                Overhang.BelowStart -> placeable.placeRelative(0, space)
                Overhang.BelowEnd -> placeable.placeRelative(-placeable.width, space)
                Overhang.After -> placeable.placeRelative(space, -placeable.height / 2)
            }
        }
    }

/** A short label on the inverse surface, shown in place by the control it names. */
@Composable
internal fun DashboardTooltip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .previewRoles(UnstyledComponent.Tooltip)
            .clip(ControlShape)
            .background(DashboardToken.InverseSurface.color)
            .padding(horizontal = Gap, vertical = 4.dp),
        style = SmallStyle,
        color = DashboardToken.InverseOnSurface.color,
        maxLines = 1,
    )
}

/** Where in its box a part floating [this] way is aligned. */
internal val Overhang.alignment: Alignment
    get() = when (this) {
        Overhang.Below -> Alignment.BottomCenter
        Overhang.BelowStart -> Alignment.BottomStart
        Overhang.BelowEnd -> Alignment.BottomEnd
        Overhang.After -> Alignment.CenterEnd
    }

/**
 * An icon with no label of its own, so [label] shows as a tooltip while a pointer rests on it or
 * the keyboard brings focus to it.
 *
 * @param[icon] What the button shows.
 * @param[label] What it does, its content description and its tooltip.
 * @param[onClick] Called on a press.
 * @param[tooltip] Where the tooltip shows.
 * @param[modifier] Applied to the box around the button and its tooltip.
 * @param[toggled] Whether what the button opens is open.
 * @param[on] What the button sits on, which shows through it when not toggled.
 */
@Composable
internal fun DashboardIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tooltip: Overhang,
    modifier: Modifier = Modifier,
    toggled: Boolean = false,
    on: DashboardToken = DashboardToken.Surface,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusVisibleAsState()
    val roles = if (toggled) {
        Modifier.previewRoles(UnstyledComponent.ToggledIconButton)
    } else {
        Modifier.previewRoles(on.role, Role.OnSurfaceVariant)
    }
    Box(modifier) {
        UnstyledButton(
            onClick = onClick,
            modifier = Modifier
                .size(IconButtonSize)
                .then(roles)
                .focusRing(interactions, FocusRingWidth, DashboardToken.Primary.color, ControlShape)
                .clip(ControlShape)
                .background(if (toggled) DashboardToken.SecondaryContainer.color else Color.Transparent),
            interactionSource = interactions,
        ) {
            UnstyledIcon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(IconSize),
                tint = (if (toggled) DashboardToken.OnSecondaryContainer else DashboardToken.OnSurfaceVariant).color,
            )
        }
        if (hovered || focused) {
            DashboardTooltip(label, Modifier.align(tooltip.alignment).overhang(tooltip, TooltipGap))
        }
    }
}

/** How a labelled button is painted. */
internal enum class ButtonStyle(
    val component: UnstyledComponent,
) {
    /** The page's main action, on primary. */
    Filled(UnstyledComponent.FilledButton),

    /** A quieter action, outlined. */
    Outlined(UnstyledComponent.OutlinedButton),

    /** The way out of the alert, on error. */
    Alert(UnstyledComponent.AlertButton),
}

/**
 * A labelled button, with an optional leading icon.
 *
 * @param[label] What it says.
 * @param[style] How it is painted.
 * @param[onClick] Called on a press.
 * @param[modifier] Applied to the button.
 * @param[icon] Shown before the label, if given.
 * @param[role] What assistive tech calls it.
 */
@Composable
internal fun DashboardButton(
    label: String,
    style: ButtonStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    role: SemanticsRole = SemanticsRole.Button,
) {
    val interactions = remember { MutableInteractionSource() }
    val (container, content) = when (style) {
        ButtonStyle.Filled -> DashboardToken.Primary.color to DashboardToken.OnPrimary.color
        ButtonStyle.Outlined -> Color.Transparent to DashboardToken.OnSurface.color
        ButtonStyle.Alert -> DashboardToken.Error.color to DashboardToken.OnError.color
    }
    val outline = if (style == ButtonStyle.Outlined) {
        Modifier.border(1.dp, DashboardToken.Outline.color, ControlShape)
    } else {
        Modifier
    }
    UnstyledButton(
        onClick = onClick,
        modifier = modifier
            .height(ControlHeight)
            .previewRoles(style.component)
            .focusRing(interactions, FocusRingWidth, DashboardToken.Primary.color, ControlShape, offset = 2.dp)
            .clip(ControlShape)
            .background(container)
            .then(outline),
        contentPadding = PaddingValues(horizontal = 14.dp),
        role = role,
        interactionSource = interactions,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Gap), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                UnstyledIcon(icon, contentDescription = null, modifier = Modifier.size(IconSize), tint = content)
            }
            Text(label, style = LabelStyle, color = content, maxLines = 1)
        }
    }
}
