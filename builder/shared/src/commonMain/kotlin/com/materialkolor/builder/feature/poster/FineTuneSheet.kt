package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.feature.workspace.FineTuneSection
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.finetune_close
import com.materialkolor.builder.generated.resources.finetune_keep_hue
import com.materialkolor.builder.generated.resources.finetune_keep_hue_spoken
import com.materialkolor.builder.generated.resources.finetune_keep_seed
import com.materialkolor.builder.generated.resources.finetune_keep_seed_spoken
import com.materialkolor.builder.generated.resources.finetune_locks_label
import com.materialkolor.builder.generated.resources.finetune_title
import com.materialkolor.builder.kit.control.BuilderInsetSheet
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSheet
import com.materialkolor.builder.kit.control.BuilderToggleButton
import com.materialkolor.builder.kit.control.SheetPresentation
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The Fine-tune sheet, which rises inside the poster from its foot and leaves the header and the
 * hex in view above it. It is open while `context.fineTune` names a section, and opens scrolled to
 * that section.
 *
 * Stand it in the poster's `BuilderInsetSheetHost` inside an `InversePosterSurface`, so it keeps
 * still while the poster scrolls and every control in it draws in the poster's inverse. Esc, the
 * close button and a click on the veil close it, and focus goes back to [returnFocusTo].
 *
 * @param[returnFocusTo] The Fine-tune button, or null while none is on screen.
 */
@Composable
internal fun FineTuneSheet(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        // The header and the hex stay in view, unless the poster is too short to leave the sheet
        // half of it.
        val tallest = maxOf(maxHeight - HeroClearance, maxHeight * MIN_SHARE)
        BuilderInsetSheet(
            open = context.fineTune != null,
            onDismiss = { dispatcher.dispatch(WorkspaceAction.CloseFineTune) },
            title = stringResource(Res.string.finetune_title),
            returnFocusTo = returnFocusTo,
            maxHeight = tallest,
            closeLabel = stringResource(Res.string.finetune_close),
        ) {
            FineTuneContent(context, dispatcher)
        }
    }
}

/**
 * The Fine-tune sheet on a phone, over the whole screen in the workspace's own colors, the way
 * export opens there. It stands over the workspace rather than in the poster, so it sits outside
 * the poster's colors.
 *
 * It only opens while the poster rests in the phone sheet. Anywhere else the poster holds its own
 * [FineTuneSheet].
 *
 * @param[returnFocusTo] The Fine-tune button, or null while none is on screen.
 */
@Composable
internal fun FineTuneHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    returnFocusTo: FocusRequester? = null,
) {
    val phone = LocalLayout.current.posterMode == PosterMode.Sheet
    val context = rememberPosterContext(state)
    BuilderSheet(
        visible = phone && context.fineTune != null,
        onDismissRequest = { dispatcher.dispatch(WorkspaceAction.CloseFineTune) },
        title = stringResource(Res.string.finetune_title),
        presentation = SheetPresentation.FullScreen,
        closeLabel = stringResource(Res.string.finetune_close),
        returnFocusTo = returnFocusTo,
    ) {
        BuilderScrollArea(Modifier.weight(1f), tabStop = false, scrollbarInGutter = true) {
            FineTuneContent(context, dispatcher)
        }
    }
}

/**
 * What the sheet holds, in order. The locks Shuffle keeps, the key colors, the pinned roles, then
 * what the output takes, the spec and platform, the extra colors, the target's own options and the
 * Custom tones. Each part keeps its own eyebrow and info button.
 *
 * It scrolls the section `context.fineTune` names to the top once it is laid out, and again each
 * time the named section changes while it is open.
 */
@Composable
internal fun FineTuneContent(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val places = remember { FineTunePlaces() }
    val section = context.fineTune
    LaunchedEffect(section) {
        if (section == null) return@LaunchedEffect
        // Wait for a frame, so the sections have their places before the sheet scrolls to one.
        withFrameNanos { }
        places.bringToTop(section)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
        FineTuneLocks(context, dispatcher, places.modifier(FineTuneSection.Locks))
        val picks = remember { KeyColorPicks() }
        KeyColorRows(context, dispatcher, places.modifier(FineTuneSection.KeyColors), picks = picks)
        PinnedRoles(context, dispatcher, places.modifier(FineTuneSection.Pins), picks = picks)
        SpecPlatformControl(context, dispatcher, places.modifier(FineTuneSection.Spec))
        AccentsEditor(context, dispatcher, places.modifier(FineTuneSection.Accents))
        TargetOptions(context, dispatcher, places.modifier(FineTuneSection.TargetOptions))
        CustomToneTable(context, dispatcher)
    }
}

/**
 * The locks Shuffle honours that live in the sheet, the hue and the seed. The style's own lock sits
 * by the style on the poster. Each reads out whole, as in "Keep the hue when shuffling", since its
 * short label leans on the eyebrow above it.
 */
@Composable
private fun FineTuneLocks(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Eyebrow(stringResource(Res.string.finetune_locks_label))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            SheetLocks.forEach { (lock, words) ->
                val kept = context.preferences.isLocked(lock)
                BuilderToggleButton(
                    checked = kept,
                    onCheckedChange = { on -> dispatcher.dispatch(WorkspaceAction.SetLock(lock, on)) },
                    label = stringResource(words.label),
                    modifier = Modifier.foldedToggleName(stringResource(words.spoken), kept),
                    icon = if (kept) IconId.Lock else null,
                )
            }
        }
    }
}

/**
 * Where each section of the sheet starts, so the sheet can open scrolled to one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
private class FineTunePlaces {
    private val requesters = FineTuneSection.entries.associateWith { BringIntoViewRequester() }

    /**
     * The modifier that marks where [section] starts.
     */
    fun modifier(section: FineTuneSection): Modifier = Modifier.bringIntoViewRequester(requesters.getValue(section))

    /**
     * Scrolls the sheet until [section] starts at its top, or as near as the sheet scrolls. A
     * section the target leaves out has no place, and the sheet stays where it is.
     *
     * It asks for a box as tall as [section] and far more, which no sheet fits. The scroll then
     * lines the box's top up with the sheet's top rather than bringing its bottom into view.
     */
    suspend fun bringToTop(section: FineTuneSection) {
        requesters.getValue(section).bringIntoView(Rect(0f, 0f, 1f, TALLER_THAN_ANY_SHEET))
    }
}

/**
 * What a lock in the sheet shows and what it reads out.
 */
private class LockWords(
    val label: StringResource,
    val spoken: StringResource,
)

/**
 * The locks the sheet holds, in the order it shows them.
 */
private val SheetLocks: List<Pair<ShuffleLock, LockWords>> = listOf(
    ShuffleLock.Hue to LockWords(Res.string.finetune_keep_hue, Res.string.finetune_keep_hue_spoken),
    ShuffleLock.Seed to LockWords(Res.string.finetune_keep_seed, Res.string.finetune_keep_seed_spoken),
)

/**
 * How much of the poster's top the sheet leaves in view, the header and the hex, as board E draws
 * it.
 */
private val HeroClearance: Dp = 236.dp

/**
 * The least of the poster's height the sheet may take when the poster is too short for the
 * clearance above.
 */
private const val MIN_SHARE = 0.5f

private const val TALLER_THAN_ANY_SHEET = 100_000f
