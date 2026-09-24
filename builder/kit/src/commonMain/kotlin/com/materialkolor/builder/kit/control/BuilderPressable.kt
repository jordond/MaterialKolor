package com.materialkolor.builder.kit.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.skin.headless.enabledAlpha
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A press target around something an app draws for itself, such as a picture that opens a larger
 * copy of itself.
 *
 * It draws nothing of its own but the press and the focus ring every skin's buttons show, so what it
 * holds is the whole control. It reads as a button named [label], and on the web a disabled one says
 * so in its name, "Open the eyedropper, disabled" (D37). What it holds is only drawn and reads as
 * nothing, since [label] already says what pressing it does. A card whose own text should be read
 * out is a [BuilderCard] with a click instead.
 *
 * It takes at least the layout's primary touch target, as the other kit controls do, with [content]
 * in the middle of it.
 *
 * @param[onClick] Called when it is pressed.
 * @param[label] What pressing it does, read out as its name.
 * @param[modifier] Applied to the pressable.
 * @param[enabled] Whether it can be pressed. A disabled one draws faint.
 * @param[shape] The outline [content] is clipped to and the focus ring follows.
 * @param[content] What it shows.
 */
@Composable
public fun BuilderPressable(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(LocalBuilderTokens.current.radius.small),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val name = stateName(label, state = null, enabled)
    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ).semantics { contentDescription = name }
            .clearAndSetSemantics { }
            .controlTouchTarget(LocalLayout.current.primaryTouchTarget)
            .controlPress(interactionSource)
            .alpha(enabledAlpha(enabled))
            .controlRing(interactionSource, shape)
            .clip(shape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
