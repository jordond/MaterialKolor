package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.feature.image.ImageMenuButton
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.picker.pickButtonFocus
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_all_locked
import com.materialkolor.builder.generated.resources.poster_lock_hue
import com.materialkolor.builder.generated.resources.poster_lock_seed
import com.materialkolor.builder.generated.resources.poster_lock_style
import com.materialkolor.builder.generated.resources.poster_pick
import com.materialkolor.builder.generated.resources.poster_shuffle
import com.materialkolor.builder.generated.resources.poster_space
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
 * draw, so Shuffle turns off and a line says why. The Space keycap inside it is a hint for a
 * keyboard, so a touch screen leaves it out. The three actions share one row and the locks another.
 *
 * @param[shuffle] Whether Shuffle leads the row. The sheet's seed row already holds it.
 * @param[locks] Whether the locks follow the actions. The sheet shows them further down.
 */
@Composable
internal fun SeedActions(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    shuffle: Boolean = true,
    locks: Boolean = true, // b-510
) {
    val spacing = LocalBuilderTokens.current.spacing
    val preferences = context.preferences
    val nothingToShuffle = preferences.shufflesNothing()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        // b-510
        // The three share one row at 400 dp in every skin, with a tight gap Material's roomy buttons need.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (shuffle) {
                // b-510
                val space = stringResource(Res.string.poster_space)
                BuilderButton(
                    onClick = { dispatcher.dispatch(WorkspaceAction.Shuffle(origin = null)) },
                    label = stringResource(Res.string.poster_shuffle),
                    emphasis = Emphasis.Primary,
                    enabled = !nothingToShuffle,
                    hint = if (LocalLayout.current.coarsePointer) null else space,
                )
            }
            // b-307
            val pick = remember { FocusRequester() }
            BuilderButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPicker(PickerTarget.Seed, returnFocusTo = pick)) },
                label = stringResource(Res.string.poster_pick),
                modifier = pickButtonFocus(pick),
                icon = IconId.Eyedropper,
            )
            ImageMenuButton(context, dispatcher) // b-311b
        }
        if (locks) ShuffleLocks(context, dispatcher) // b-510
    }
}

// b-510

/**
 * The three shuffle locks on one row, and why Shuffle is off once the seed and the style are both
 * locked. The label says lock, so the locks leave their glyph out and fit the row.
 */
@Composable
internal fun ShuffleLocks(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val preferences = context.preferences
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            ShuffleLock.entries.forEach { lock ->
                BuilderToggleButton(
                    checked = preferences.isLocked(lock),
                    onCheckedChange = { on -> dispatcher.dispatch(WorkspaceAction.SetLock(lock, on)) },
                    label = stringResource(lockLabel(lock)),
                )
            }
        }
        if (preferences.shufflesNothing()) {
            BuilderText(text = stringResource(Res.string.poster_all_locked), emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * Shuffle as an icon, for the rail and the sheet's seed row. With nothing left to shuffle it turns
 * off and its tooltip says why.
 */
@Composable
internal fun ShuffleIconButton(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    emphasis: Emphasis = Emphasis.Subtle,
) {
    val nothingToShuffle = context.preferences.shufflesNothing()
    val name = stringResource(Res.string.poster_shuffle)
    PosterIconButton(
        icon = IconId.Shuffle,
        description = name,
        onClick = { dispatcher.dispatch(WorkspaceAction.Shuffle(origin = null)) },
        modifier = modifier,
        emphasis = emphasis,
        enabled = !nothingToShuffle,
        tooltip = if (nothingToShuffle) stringResource(Res.string.poster_all_locked) else name,
    )
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
