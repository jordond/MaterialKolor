package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_all_locked
import com.materialkolor.builder.generated.resources.poster_image
import com.materialkolor.builder.generated.resources.poster_lock_hue
import com.materialkolor.builder.generated.resources.poster_lock_seed
import com.materialkolor.builder.generated.resources.poster_lock_style
import com.materialkolor.builder.generated.resources.poster_pick
import com.materialkolor.builder.generated.resources.poster_shuffle
import com.materialkolor.builder.generated.resources.poster_space
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderToggleButton
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Shuffle, Pick and Image, with the shuffle locks under them (F-10).
 *
 * Shuffle only asks. The workspace draws the next seed with the locks applied and lands it as one
 * undo entry behind a crossfade. With the seed and the style both locked there is nothing left to
 * draw, so Shuffle turns off and a line says why. The Space key beside it is a hint for a keyboard,
 * so a touch screen leaves it out.
 */
@Composable
internal fun SeedActions(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val preferences = context.preferences
    val nothingToShuffle = preferences.shufflesNothing()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderButton(
                    onClick = { dispatcher.dispatch(WorkspaceAction.Shuffle(origin = null)) },
                    label = stringResource(Res.string.poster_shuffle),
                    emphasis = Emphasis.Primary,
                    icon = IconId.Shuffle,
                    enabled = !nothingToShuffle,
                )
                if (!LocalLayout.current.coarsePointer) {
                    BuilderBadge(label = stringResource(Res.string.poster_space), icon = IconId.Keyboard)
                }
            }
            BuilderButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPicker(PickerTarget.Seed)) },
                label = stringResource(Res.string.poster_pick),
                icon = IconId.Eyedropper,
            )
            BuilderButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenImagePicker) },
                label = stringResource(Res.string.poster_image),
                icon = IconId.Image,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            ShuffleLock.entries.forEach { lock ->
                BuilderToggleButton(
                    checked = preferences.isLocked(lock),
                    onCheckedChange = { on -> dispatcher.dispatch(WorkspaceAction.SetLock(lock, on)) },
                    label = stringResource(lockLabel(lock)),
                    icon = IconId.Lock,
                )
            }
        }
        if (nothingToShuffle) {
            BuilderText(text = stringResource(Res.string.poster_all_locked), emphasis = Emphasis.Secondary)
        }
    }
}

/** Whether these preferences hold [lock] on. */
internal fun Preferences.isLocked(lock: ShuffleLock): Boolean =
    when (lock) {
        ShuffleLock.Hue -> hueLock
        ShuffleLock.Style -> styleLock
        ShuffleLock.Seed -> seedLock
    }

/**
 * Whether the locks leave a shuffle nothing to move. The hue lock only shapes a new seed, so it
 * never matters once the seed itself is locked.
 */
internal fun Preferences.shufflesNothing(): Boolean = seedLock && styleLock

private fun lockLabel(lock: ShuffleLock): StringResource =
    when (lock) {
        ShuffleLock.Hue -> Res.string.poster_lock_hue
        ShuffleLock.Style -> Res.string.poster_lock_style
        ShuffleLock.Seed -> Res.string.poster_lock_seed
    }
