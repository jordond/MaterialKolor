package com.materialkolor.builder.feature.poster

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.about_first_run_hint
import com.materialkolor.builder.generated.resources.about_first_run_hint_close
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

// b-314

/** The first visit hint, by the id its dismissal is kept under (F-35). */
internal const val FIRST_RUN_HINT = "first-run"

/** The library switcher's one pulse beside the hint, kept apart so it plays once per browser. */
internal const val SWITCHER_PULSE_HINT = "switcher-pulse"

/**
 * Whether the first visit hint shows (F-35). It waits for boot to open a project, which it only does
 * once the stored preferences are read, so a hint closed on an earlier visit never flashes. It never
 * comes back once it was closed or the first export is done.
 *
 * @param[projectName] The open project's name, empty until boot has opened one.
 */
internal fun showsFirstRunHint(
    preferences: Preferences,
    projectName: String,
): Boolean =
    projectName.isNotEmpty() &&
        !preferences.firstExportDone &&
        FIRST_RUN_HINT !in preferences.dismissedHints

/**
 * The one hint a first visit gets, a card on the poster in the poster's ink with a close button.
 * No tour follows it.
 *
 * Closing it from the keyboard hands focus on to the next control on the poster first, so focus
 * never drops out of the poster with the card.
 */
@Composable
internal fun FirstRunHint(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    if (!showsFirstRunHint(context.preferences, context.projectName)) return
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val focusManager = LocalFocusManager.current
    var closeFocused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(tokens.outlineWidth, tokens.borderStrong, RoundedCornerShape(tokens.radius.medium))
            .padding(start = spacing.large, top = spacing.small, end = spacing.small, bottom = spacing.small),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(text = stringResource(Res.string.about_first_run_hint), modifier = Modifier.weight(1f))
        PosterIconButton(
            icon = IconId.Close,
            description = stringResource(Res.string.about_first_run_hint_close),
            onClick = {
                // A click that left focus elsewhere keeps it there.
                if (closeFocused) focusManager.moveFocus(FocusDirection.Next)
                dispatcher.dispatch(WorkspaceAction.DismissHint(FIRST_RUN_HINT))
            },
            buttonModifier = Modifier.onFocusChanged { focusState -> closeFocused = focusState.isFocused },
        )
    }
}

/**
 * The library switcher's pulse beside the first visit hint (F-35). It plays once per browser while
 * the hint shows, then puts itself away.
 */
@Composable
internal fun Modifier.switcherPulse(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
): Modifier {
    val preferences = state.preferences
    val pulsing = showsFirstRunHint(preferences, state.projectName) &&
        SWITCHER_PULSE_HINT !in preferences.dismissedHints
    return pulseRing(active = pulsing) { dispatcher.dispatch(WorkspaceAction.DismissHint(SWITCHER_PULSE_HINT)) }
}

/**
 * One focus colored ring that grows out of the element and fades, on the skin's reveal timing, then
 * [onDone]. Under reduced motion or frozen motion it draws nothing and calls [onDone] straight away.
 *
 * @param[active] Whether the pulse is due. It plays each time this turns true.
 */
@Composable
internal fun Modifier.pulseRing(
    active: Boolean,
    onDone: () -> Unit,
): Modifier {
    val still = LocalReducedMotion.current || LocalMotionFrozen.current
    val motion = LocalBuilderMotion.current
    val tokens = LocalBuilderTokens.current
    val done by rememberUpdatedState(onDone)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active, still) {
        if (!active) return@LaunchedEffect
        if (!still) {
            progress.snapTo(0f)
            progress.animateTo(1f, motion.reveal())
        }
        done()
    }
    if (!active || still) return this
    return drawWithContent {
        drawContent()
        val grown = progress.value
        val reach = tokens.spacing.small.toPx() * grown
        val width = tokens.highlightWidth.toPx()
        val corner = minOf(size.height / 2, tokens.radius.medium.toPx()) + reach
        drawRoundRect(
            color = tokens.focus.copy(alpha = tokens.focus.alpha * (1f - grown)),
            topLeft = Offset(-reach, -reach),
            size = Size(size.width + reach * 2, size.height + reach * 2),
            cornerRadius = CornerRadius(corner),
            style = Stroke(width),
        )
    }
}
