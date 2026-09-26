package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.style_keep
import com.materialkolor.builder.generated.resources.style_keep_spoken
import com.materialkolor.builder.kit.control.BuilderToggleButton
import com.materialkolor.builder.kit.icon.IconId
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * The Style lock as a toggle at the end of the style's line, which keeps the style when Shuffle
 * draws a new seed.
 *
 * It shows a short label and reads out the longer "Keep the style when shuffling", so the lock
 * makes sense to someone who hears it without the line before it.
 */
@Composable
internal fun KeepStyleToggle(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    // b-523
    val kept = context.preferences.isLocked(ShuffleLock.Style)
    BuilderToggleButton(
        checked = kept,
        onCheckedChange = { on -> dispatcher.dispatch(WorkspaceAction.SetLock(ShuffleLock.Style, on)) },
        label = stringResource(Res.string.style_keep),
        modifier = modifier,
        icon = IconId.Lock,
        // b-526 The kit carries the longer name, so the web reads it as it reads the kit's own toggles.
        contentDescription = stringResource(Res.string.style_keep_spoken),
    )
}
