package com.materialkolor.builder.preview.fluent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalMotionFrozen

/**
 * Shows or hides a panel with the skin's panel motion, growing along the width when [horizontal]
 * and along the height otherwise. Under frozen motion it shows or hides at once.
 *
 * The gallery's expanders open this way.
 */
@Composable
internal fun PanelMotion(
    visible: Boolean,
    horizontal: Boolean = false,
    content: @Composable () -> Unit,
) {
    val frozen = LocalMotionFrozen.current
    val motion = LocalBuilderMotion.current
    val enter = when {
        frozen -> EnterTransition.None
        horizontal -> expandHorizontally(motion.panelEnter()) + fadeIn(motion.panelEnter())
        else -> expandVertically(motion.panelEnter()) + fadeIn(motion.panelEnter())
    }
    val exit = when {
        frozen -> ExitTransition.None
        horizontal -> shrinkHorizontally(motion.panelExit()) + fadeOut(motion.panelExit())
        else -> shrinkVertically(motion.panelExit()) + fadeOut(motion.panelExit())
    }
    AnimatedVisibility(visible = visible, enter = enter, exit = exit) { content() }
}
