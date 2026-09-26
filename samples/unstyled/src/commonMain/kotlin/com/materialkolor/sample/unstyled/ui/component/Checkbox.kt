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
import com.materialkolor.sample.unstyled.theme.GradientTokens
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.brush
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.shape
import com.materialkolor.unstyled.MaterialKolorTokens

private const val CHECK_ENTER_SCALE = 0.4f

@Composable
internal fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val round = ShapeTokens.pill.shape

    UnstyledCheckbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        interactionSource = interactionSource,
        indication = LocalIndication.current,
        modifier = modifier
            .size(40.dp)
            .controlFocusRing(interactionSource, round, offset = 0.dp)
            .clip(round),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .sunken(round)
                    .border(width = 1.5.dp, color = MaterialKolorTokens.outline.color, shape = round),
            )

            CheckedIndicator(
                modifier = Modifier.size(24.dp),
                enter = fadeIn() + scaleIn(initialScale = CHECK_ENTER_SCALE),
                exit = fadeOut() + scaleOut(targetScale = CHECK_ENTER_SCALE),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(round)
                        .background(GradientTokens.accent.brush),
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
