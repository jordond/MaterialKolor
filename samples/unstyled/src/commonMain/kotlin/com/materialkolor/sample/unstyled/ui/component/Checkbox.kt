package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composeunstyled.CheckedIndicator
import com.composeunstyled.UnstyledCheckbox
import com.composeunstyled.UnstyledIcon
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

private const val CHECK_ENTER_SCALE = 0.6f

/**
 * A checkbox. The box is 20dp and the round area around it takes the pointer, shows the state layer and wears the
 * focus ring.
 *
 * @param[checked] Whether it is on.
 * @param[onCheckedChange] Called with the new value when it is toggled.
 * @param[accessibilityLabel] What it is called, for a checkbox that sits next to its label rather than holding it.
 * @param[modifier] Applied to the toggleable node.
 */
@Composable
internal fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accessibilityLabel: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    UnstyledCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier
            .size(36.dp)
            .controlFocusRing(interactionSource, Shapes.Round, offset = 0.dp)
            .clip(Shapes.Round),
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        accessibilityLabel = accessibilityLabel,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(Shapes.Small)
                    .border(width = 2.dp, color = MaterialKolorTokens.outline.color, shape = Shapes.Small),
            ) {
                CheckedIndicator(
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn() + scaleIn(initialScale = CHECK_ENTER_SCALE),
                    exit = fadeOut() + scaleOut(targetScale = CHECK_ENTER_SCALE),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialKolorTokens.primary.color),
                        contentAlignment = Alignment.Center,
                    ) {
                        UnstyledIcon(
                            imageVector = Lucide.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialKolorTokens.onPrimary.color,
                        )
                    }
                }
            }
        }
    }
}
