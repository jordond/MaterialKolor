package com.materialkolor.builder.preview.unstyled

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.Text
import com.composeunstyled.collectIsFocusVisibleAsState
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalReducedMotion

// Compose Unstyled has no spacing or type scale, so the gallery and the Trips app keep their own
// here, the way an app on the library would.
internal val Gap = 8.dp
internal val SectionGap = 16.dp
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

private const val PanelMillis = 200

/**
 * How a panel opens and closes, at once when motion is frozen for a screenshot or the user asked
 * for less of it.
 */
@Composable
internal fun <T> panelMotion(): FiniteAnimationSpec<T> =
    if (LocalMotionFrozen.current || LocalReducedMotion.current) snap() else tween(PanelMillis)

/**
 * Where a floating part sits against the box it belongs to.
 */
internal enum class Overhang {
    /**
     * Under the box, lined up with its start edge. Align it to the bottom start.
     */
    BelowStart,

    /**
     * Under the box, lined up with its end edge. Align it to the bottom end.
     */
    BelowEnd,

    /**
     * Past the box's end edge, centered on it. Align it to the center end.
     */
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
                Overhang.BelowStart -> placeable.placeRelative(0, space)
                Overhang.BelowEnd -> placeable.placeRelative(-placeable.width, space)
                Overhang.After -> placeable.placeRelative(space, -placeable.height / 2)
            }
        }
    }

/**
 * Where in its box a part floating [this] way is aligned.
 */
internal val Overhang.alignment: Alignment
    get() = when (this) {
        Overhang.BelowStart -> Alignment.BottomStart
        Overhang.BelowEnd -> Alignment.BottomEnd
        Overhang.After -> Alignment.CenterEnd
    }

/**
 * A short label on the inverse surface, shown in place by the control it names.
 */
@Composable
internal fun UnstyledTooltip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .previewRoles(UnstyledComponent.Tooltip)
            .clip(ControlShape)
            .background(UnstyledToken.InverseSurface.color)
            .padding(horizontal = Gap, vertical = 4.dp),
        style = SmallStyle,
        color = UnstyledToken.InverseOnSurface.color,
        maxLines = 1,
    )
}

/**
 * Whether a control's tooltip shows, and the key handler that hides it.
 *
 * @property[shown] True while a pointer rests on the control or the keyboard brings focus to it,
 * until Esc hides it.
 * @property[onEscape] Hides the tooltip on Esc until the pointer and focus have both left (WCAG
 * 1.4.13). Put it on the control or on a box around it.
 */
internal class TooltipVisibility(
    val shown: Boolean,
    val onEscape: Modifier,
)

/**
 * The [TooltipVisibility] of the control that reports to [interactions].
 */
@Composable
internal fun tooltipVisibility(interactions: InteractionSource): TooltipVisibility {
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusVisibleAsState()
    val wanted = hovered || focused
    // Like hover, this belongs to one copy of a split and not to the app's state.
    var hidden by remember { mutableStateOf(false) }
    LaunchedEffect(wanted) { if (!wanted) hidden = false }
    val shown = wanted && !hidden
    return TooltipVisibility(
        shown = shown,
        onEscape = Modifier.onKeyEvent { event ->
            val escape = shown && event.type == KeyEventType.KeyDown && event.key == Key.Escape
            if (escape) hidden = true
            escape
        },
    )
}

/**
 * Lets assistive tech open or close the panel a button shows with [onToggle], whichever way
 * [expanded] stands. Pair it with the kit's `foldedExpandedName`, which names the button and says
 * whether the panel is open, on the web too.
 */
internal fun Modifier.expandActions(
    expanded: Boolean,
    onToggle: () -> Unit,
): Modifier =
    semantics {
        if (expanded) {
            collapse {
                onToggle()
                true
            }
        } else {
            expand {
                onToggle()
                true
            }
        }
    }
