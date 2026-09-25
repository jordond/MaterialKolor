package com.materialkolor.builder.kit.skin.headless

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.kit.headless.CheckboxStyle
import com.materialkolor.builder.kit.headless.DisclosureStyle
import com.materialkolor.builder.kit.headless.SliderStyle
import com.materialkolor.builder.kit.headless.SwitchStyle
import com.materialkolor.builder.kit.headless.TabsStyle
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * The Unstyled skin's inputs, plain and square with hairline edges, the way Compose Unstyled's own
 * demos draw them.
 */
internal object UnstyledInputStyles {
    val switch: SwitchStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            SwitchStyle(
                trackWidth = 36.dp,
                trackHeight = 20.dp,
                thumbSize = 14.dp,
                trackShape = RoundedCornerShape(4.dp),
                thumbShape = RoundedCornerShape(2.dp),
                outlineWidth = 1.dp,
                trackOn = tokens.accent,
                trackOff = tokens.panel,
                outlineOff = tokens.borderStrong,
                thumbOn = tokens.onAccent,
                thumbOff = tokens.borderStrong,
                labelGap = tokens.spacing.medium,
                focus = tokens.focus,
                focusShape = RoundedCornerShape(tokens.radius.small),
            )
        }

    val checkbox: CheckboxStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            CheckboxStyle(
                boxSize = 18.dp,
                checkSize = 14.dp,
                boxShape = RoundedCornerShape(2.dp),
                outlineWidth = 1.dp,
                outline = tokens.borderStrong,
                checkedFill = tokens.accent,
                checkInk = tokens.onAccent,
                labelGap = tokens.spacing.small,
                focus = tokens.focus,
                focusShape = RoundedCornerShape(tokens.radius.small),
            )
        }

    val slider: SliderStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            SliderStyle(
                trackHeight = 4.dp,
                trackShape = RoundedCornerShape(2.dp),
                activeTrack = tokens.accent,
                inactiveTrack = tokens.border,
                thumbSize = 16.dp,
                thumbShape = RoundedCornerShape(4.dp),
                thumb = tokens.accent,
                thumbOutline = tokens.panel,
                thumbOutlineWidth = 2.dp,
                stopSize = 4.dp,
                stop = tokens.textMuted,
                focus = tokens.focus,
            )
        }

    val tabs: TabsStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            TabsStyle(
                container = Color.Transparent,
                containerShape = RoundedCornerShape(0.dp),
                containerPadding = 0.dp,
                tabShape = RoundedCornerShape(0.dp),
                tabPadding = PaddingValues(horizontal = tokens.spacing.medium, vertical = tokens.spacing.small),
                gap = tokens.spacing.extraSmall,
                selectedContainer = Color.Transparent,
                selectedInk = tokens.textStrong,
                ink = tokens.textMuted,
                indicator = tokens.accent,
                indicatorHeight = 2.dp,
                indicatorWidth = null,
                focus = tokens.focus,
            )
        }

    val disclosure: DisclosureStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            DisclosureStyle(
                container = tokens.panel,
                shape = RoundedCornerShape(4.dp),
                outline = tokens.border,
                outlineWidth = 1.dp,
                headerPadding = PaddingValues(horizontal = tokens.spacing.medium, vertical = tokens.spacing.small),
                contentPadding = PaddingValues(
                    start = tokens.spacing.medium,
                    end = tokens.spacing.medium,
                    bottom = tokens.spacing.medium,
                ),
                focus = tokens.focus,
            )
        }

    val field: FieldStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            FieldStyle(
                shape = RoundedCornerShape(4.dp),
                container = tokens.panel,
                outline = tokens.borderStrong,
                outlineWidth = 1.dp,
                active = tokens.accent,
                error = tokens.danger,
                activeWidth = 2.dp,
                activeAsUnderline = false,
                padding = PaddingValues(horizontal = tokens.spacing.medium, vertical = tokens.spacing.small),
                gap = tokens.spacing.extraSmall,
                cursor = tokens.accent,
            )
        }

    val hero: FieldStyle
        @Composable @ReadOnlyComposable
        get() = heroFieldStyle(LocalBuilderTokens.current, underline = 2.dp, shape = RoundedCornerShape(0.dp))
}

/**
 * The Custom skin's inputs, the builder's own look. Pills, a heavier outline and the accent filling
 * whatever is on, all over the same headless layer as Unstyled.
 */
internal object CustomInputStyles {
    val switch: SwitchStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            SwitchStyle(
                trackWidth = 44.dp,
                trackHeight = 24.dp,
                thumbSize = 18.dp,
                trackShape = CircleShape,
                thumbShape = CircleShape,
                outlineWidth = 2.dp,
                trackOn = tokens.accent,
                trackOff = tokens.panelRaised,
                outlineOff = tokens.borderStrong,
                thumbOn = tokens.onAccent,
                thumbOff = tokens.borderStrong,
                labelGap = tokens.spacing.medium,
                focus = tokens.focus,
                focusShape = RoundedCornerShape(tokens.radius.medium),
            )
        }

    val checkbox: CheckboxStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            CheckboxStyle(
                boxSize = 20.dp,
                checkSize = 16.dp,
                boxShape = RoundedCornerShape(6.dp),
                outlineWidth = 2.dp,
                outline = tokens.borderStrong,
                checkedFill = tokens.accent,
                checkInk = tokens.onAccent,
                labelGap = tokens.spacing.small,
                focus = tokens.focus,
                focusShape = RoundedCornerShape(tokens.radius.medium),
            )
        }

    val slider: SliderStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            SliderStyle(
                trackHeight = 8.dp,
                trackShape = CircleShape,
                activeTrack = tokens.accent,
                inactiveTrack = tokens.panelRaised,
                thumbSize = 20.dp,
                thumbShape = CircleShape,
                thumb = tokens.accent,
                thumbOutline = tokens.onAccent,
                thumbOutlineWidth = 3.dp,
                stopSize = 4.dp,
                stop = tokens.textMuted,
                focus = tokens.focus,
            )
        }

    val tabs: TabsStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            TabsStyle(
                container = tokens.panelRaised,
                containerShape = CircleShape,
                containerPadding = tokens.spacing.extraSmall,
                tabShape = CircleShape,
                tabPadding = PaddingValues(horizontal = tokens.spacing.large, vertical = tokens.spacing.small),
                gap = tokens.spacing.extraSmall,
                selectedContainer = tokens.accent,
                selectedInk = tokens.onAccent,
                ink = tokens.textStrong,
                indicator = Color.Transparent,
                indicatorHeight = 0.dp,
                indicatorWidth = null,
                focus = tokens.focus,
            )
        }

    val disclosure: DisclosureStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            DisclosureStyle(
                container = tokens.panelRaised,
                shape = RoundedCornerShape(tokens.radius.medium),
                outline = Color.Transparent,
                outlineWidth = 0.dp,
                headerPadding = PaddingValues(horizontal = tokens.spacing.large, vertical = tokens.spacing.medium),
                contentPadding = PaddingValues(
                    start = tokens.spacing.large,
                    end = tokens.spacing.large,
                    bottom = tokens.spacing.large,
                ),
                focus = tokens.focus,
            )
        }

    val field: FieldStyle
        @Composable @ReadOnlyComposable
        get() = LocalBuilderTokens.current.let { tokens ->
            FieldStyle(
                shape = RoundedCornerShape(tokens.radius.small),
                container = tokens.panelRaised,
                outline = tokens.borderStrong,
                outlineWidth = 1.dp,
                active = tokens.accent,
                error = tokens.danger,
                activeWidth = 2.dp,
                activeAsUnderline = false,
                padding = PaddingValues(horizontal = tokens.spacing.medium, vertical = tokens.spacing.medium),
                gap = tokens.spacing.extraSmall,
                cursor = tokens.accent,
            )
        }

    val hero: FieldStyle
        @Composable @ReadOnlyComposable
        get() = heroFieldStyle(LocalBuilderTokens.current, underline = 3.dp, shape = RoundedCornerShape(2.dp))
}

/**
 * How a headless text field draws its box.
 *
 * @property[shape] The box's corners.
 * @property[container] The fill behind the text.
 * @property[outline] The resting edge, which has to read at 3:1 on the panel.
 * @property[outlineWidth] How thick the resting edge is.
 * @property[active] The edge while the field has focus.
 * @property[error] The edge while the draft cannot be committed.
 * @property[activeWidth] How thick the focused or error edge is.
 * @property[activeAsUnderline] Draw the focused or error edge as a line under the text instead of
 * around it.
 * @property[padding] Between the edge and the text.
 * @property[gap] Between the label, the box and the message under it.
 * @property[cursor] The caret.
 * @property[focusRing] The ring drawn round the box under keyboard focus, beside the focused edge,
 * for a skin whose edge runs along one side only. Null where the focused edge goes all the way round
 * and is the ring itself.
 */
@Immutable
internal class FieldStyle(
    val shape: Shape,
    val container: Color,
    val outline: Color,
    val outlineWidth: Dp,
    val active: Color,
    val error: Color,
    val activeWidth: Dp,
    val activeAsUnderline: Boolean,
    val padding: PaddingValues,
    val gap: Dp,
    val cursor: Color,
    val focusRing: Color? = null,
)

/**
 * The poster's seed headline, which reads as a heading until it has focus and then shows a line
 * under it in the skin's shape. The line marks one side only, so keyboard focus also rings the
 * headline in the focus colour, the way Fluent's field does. It reads the surrounding
 * tokens, so on the poster it is drawn in the seed and the ring takes the poster's ink.
 */
internal fun heroFieldStyle(
    tokens: BuilderTokens,
    underline: Dp,
    shape: Shape,
): FieldStyle =
    FieldStyle(
        shape = shape,
        container = Color.Transparent,
        outline = Color.Transparent,
        outlineWidth = 0.dp,
        active = tokens.textStrong,
        error = tokens.danger,
        activeWidth = underline,
        activeAsUnderline = true,
        padding = PaddingValues(vertical = tokens.spacing.extraSmall),
        gap = tokens.spacing.extraSmall,
        cursor = tokens.textStrong,
        focusRing = tokens.focus,
    )
