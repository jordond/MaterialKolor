package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.feature.image.ImageMenuButton
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.picker.pickButtonFocus
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_all_locked
import com.materialkolor.builder.generated.resources.poster_pick
import com.materialkolor.builder.generated.resources.poster_shuffle
import com.materialkolor.builder.generated.resources.poster_shuffle_off
import com.materialkolor.builder.generated.resources.poster_space
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * Shuffle, Pick and Image on one row, Shuffle taking what the other two leave.
 *
 * Shuffle only asks. The workspace draws the next seed with the locks applied and lands it as one
 * undo entry behind a crossfade. With the seed and the style both locked there is nothing left to
 * draw, so Shuffle turns off and its tooltip and what it reads out say why. The Space keycap inside
 * it is a hint for a keyboard, so a touch screen leaves it out.
 *
 * The locks live elsewhere, the style's by the style and the rest in Fine-tune. On the 320 poster
 * Pick and Image draw as their glyphs alone, so the three still share one row, and read out the
 * same.
 *
 * @param[shuffle] Whether Shuffle leads the row. The sheet's seed row already holds it.
 */
@Composable
internal fun SeedActions(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    shuffle: Boolean = true,
) {
    val spacing = LocalBuilderTokens.current.spacing
    // b-524 Material's roomy buttons leave Image a row of its own at 320, so there the two go
    // glyph only.
    val narrow = LocalLayout.current.posterMode == PosterMode.Docked320
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        // A tight gap, so Material's roomy buttons share one row at 400 dp. A row too narrow for all
        // three wraps, and Shuffle then fills a row of its own.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            if (shuffle) ShuffleButton(context, dispatcher, Modifier.weight(1f))
            val pick = remember { FocusRequester() }
            val openPicker = {
                dispatcher.dispatch(
                    WorkspaceAction.OpenPicker(PickerTarget.Seed, returnFocusTo = pick),
                )
            }
            if (narrow) {
                PosterIconButton(
                    icon = IconId.Eyedropper,
                    description = stringResource(Res.string.poster_pick),
                    onClick = openPicker,
                    emphasis = Emphasis.Secondary,
                    buttonModifier = pickButtonFocus(pick),
                )
            } else {
                BuilderButton(
                    onClick = openPicker,
                    label = stringResource(Res.string.poster_pick),
                    modifier = pickButtonFocus(pick),
                    icon = IconId.Eyedropper,
                )
            }
            ImageMenuButton(context, dispatcher, glyphOnly = narrow)
        }
    }
}

/**
 * Shuffle as the row's lead button, with the Space keycap for a keyboard. With nothing left to
 * shuffle it turns off, its tooltip says why and it reads out as Shuffle and the reason.
 */
@Composable
private fun ShuffleButton(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val nothingToShuffle = context.preferences.shufflesNothing()
    val space = stringResource(Res.string.poster_space)
    val button: @Composable (Modifier) -> Unit = { buttonModifier ->
        BuilderButton(
            onClick = { dispatcher.dispatch(WorkspaceAction.Shuffle(origin = null)) },
            label = stringResource(Res.string.poster_shuffle),
            modifier = buttonModifier,
            emphasis = Emphasis.Primary,
            enabled = !nothingToShuffle,
            hint = if (LocalLayout.current.coarsePointer) null else space,
        )
    }
    if (!nothingToShuffle) {
        button(modifier)
        return
    }
    val reason = stringResource(Res.string.poster_all_locked)
    val spoken = stringResource(Res.string.poster_shuffle_off, reason)
    BuilderTooltip(text = reason, modifier = modifier) {
        button(Modifier.fillMaxWidth().semantics { contentDescription = spoken })
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

/**
 * Whether these preferences hold [lock] on.
 */
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
