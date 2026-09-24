package com.materialkolor.builder.kit.shell

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.control.BottomSheetDetent
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.sheet_detent_full
import com.materialkolor.builder.kit.generated.resources.sheet_detent_half
import com.materialkolor.builder.kit.generated.resources.sheet_detent_peek
import com.materialkolor.builder.kit.generated.resources.shell_poster_label
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * The poster's own frame, the seed page with design D's corners, lifted when it floats.
 *
 * With a [contentWidth] the content is laid out at that width whatever the frame's, so an opening
 * panel is revealed by the frame rather than squeezed by it. Without one it fills the frame.
 */
@Composable
internal fun PosterPanel(
    modifier: Modifier,
    floating: Boolean,
    contentWidth: Dp?,
    content: @Composable () -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val style = posterOverlayStyle(tokens)
    val shape = RoundedCornerShape(tokens.radius.large)
    val label = stringResource(Res.string.shell_poster_label)
    val asText = LocalFoldsStateIntoName.current
    Box(
        modifier = modifier
            .then(if (floating) Modifier.shadow(style.shadow, shape) else Modifier)
            .clip(shape)
            .background(style.surface)
            .semantics {
                paneTitle = label
                // The web mirror drops the pane title, so there the panel is named by its text (D37).
                if (asText) roleLessName(label, asText = true)
            },
    ) {
        val sized = if (contentWidth == null) Modifier.fillMaxWidth() else Modifier.revealWidth(contentWidth)
        Box(sized.fillMaxHeight()) { content() }
    }
}

/**
 * The poster's overlay dress, the same in every skin since the poster is content. It is the Custom
 * dress with its soft shadow and large corners, standing on the page rather than a raised surface.
 * Read it inside [PosterSurface], so its colours are the poster's.
 */
internal fun posterOverlayStyle(tokens: BuilderTokens): OverlayStyle =
    customOverlayStyle(tokens.copy(panelRaised = tokens.panel))

/** The rail opens with the panel arrival motion and closes with the exit, and snaps under reduced motion (MO-05). */
@Composable
internal fun posterSpec(opening: Boolean): AnimationSpec<Float> {
    val motion = LocalBuilderMotion.current
    return when {
        LocalReducedMotion.current -> snap()
        opening -> motion.panelEnter()
        else -> motion.panelExit()
    }
}

/**
 * Sizes to [width] at layout time, so the rail animating open or shut only lays out again and
 * never recomposes the poster.
 */
internal fun Modifier.layoutWidth(width: () -> Dp): Modifier =
    layout { measurable, constraints ->
        val px = width().roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)
        val placeable = measurable.measure(constraints.copy(minWidth = px, maxWidth = px))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/**
 * Lays the content out at [width] even while the space it is given is narrower, its start edge on
 * the start edge. Whatever sticks out past the end is for the parent to clip.
 */
private fun Modifier.revealWidth(width: Dp): Modifier =
    layout { measurable, constraints ->
        val px = width.roundToPx()
        val placeable = measurable.measure(constraints.copy(minWidth = px, maxWidth = px))
        val shown = px.coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(shown, placeable.height) { placeable.placeRelative(0, 0) }
    }

/** The kit's names for the detents, remembered on the three names like the public sheet's. */
@Composable
internal fun posterDetentNames(): (BottomSheetDetent) -> String {
    val peek = stringResource(Res.string.sheet_detent_peek)
    val half = stringResource(Res.string.sheet_detent_half)
    val full = stringResource(Res.string.sheet_detent_full)
    return remember(peek, half, full) {
        { detent: BottomSheetDetent ->
            when (detent) {
                BottomSheetDetent.Peek -> peek
                BottomSheetDetent.Half -> half
                BottomSheetDetent.Full -> full
            }
        }
    }
}
