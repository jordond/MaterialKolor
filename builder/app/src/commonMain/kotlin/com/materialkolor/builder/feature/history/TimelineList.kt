package com.materialkolor.builder.feature.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.core.session.Timeline
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.history.HistoryEntry
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.command.Shortcut
import com.materialkolor.builder.feature.command.rememberPanelShortcuts
import com.materialkolor.builder.feature.poster.rememberThemeResolver
import com.materialkolor.builder.feature.projects.ProjectAge
import com.materialkolor.builder.feature.projects.ageText
import com.materialkolor.builder.feature.topbar.stepText
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.history_current
import com.materialkolor.builder.generated.resources.history_empty
import com.materialkolor.builder.generated.resources.history_future_hint
import com.materialkolor.builder.generated.resources.history_now_at
import com.materialkolor.builder.generated.resources.history_start
import com.materialkolor.builder.generated.resources.history_undone
import com.materialkolor.builder.generated.resources.history_undone_age
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.control.BuilderBadge
import com.materialkolor.builder.kit.control.BuilderListRow
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChip
import com.materialkolor.builder.kit.widget.SchemeChipSkeleton
import com.materialkolor.dynamiccolor.DynamicScheme
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

// b-509

/**
 * The History list, every step of the session newest first with Start at the bottom (D55, D57).
 *
 * Each row shows a swatch of what the step left, what the step did and how long ago. The row the
 * theme is at carries the row's check and a Current badge. The rows above it were undone, so their
 * swatches fade and their second line says so in words too (AR-03). A click, Enter or Space on a row
 * jumps there at once with no animation (MO-07), the list stays open so someone can hop between
 * steps while the preview changes behind it, and each jump is read out through the announcer.
 *
 * The page never hears a key pressed in here, so the list takes Undo and Redo itself and moves
 * focus to the row the history lands on, and H closes it. A jump across a library switch moves the
 * workspace into the new skin and drops focus, and nothing on the page claims it while a panel is
 * open, so the list hands focus back to the current row, in the frame its rows move, when it held
 * focus before the jump.
 *
 * Swatches read a light scheme from the resolver's scheme cache and never resolve a whole theme,
 * so the eight themes kept for undo stay put. Each paints a skeleton first, and a long history
 * fills in a few rows a frame, top first. A drag held down from before the list opened keeps
 * moving the newest step, and its swatch waits for the step to hold still before it reads again.
 *
 * @param[timeline] The steps and where the history is among them.
 * @param[dispatcher] Where jumps, undo, redo and closing go.
 * @param[currentRow] Takes focus on the row the theme is at. The popover opens with focus there.
 * @param[modifier] Applied to the list.
 */
@Composable
internal fun TimelineList(
    timeline: Timeline,
    dispatcher: Dispatcher<WorkspaceAction>,
    currentRow: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val keys = rememberPanelShortcuts()
    val focus = remember { ListFocus() }
    val announcer = LocalAnnouncer.current
    SideEffect {
        keys.onShortcut = { shortcut ->
            when {
                shortcut == Shortcut.Undo -> {
                    focus.noteKey()
                    dispatcher.dispatch(WorkspaceAction.Undo)
                    true
                }
                shortcut == Shortcut.Redo -> {
                    focus.noteKey()
                    dispatcher.dispatch(WorkspaceAction.Redo)
                    true
                }
                shortcut == Shortcut.History -> {
                    dispatcher.dispatch(WorkspaceAction.ClosePanel)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }
    FocusFollowsTheHistory(timeline.cursor, focus, currentRow)
    val spacing = LocalBuilderTokens.current.spacing
    Column(
        modifier = modifier
            .then(keys.modifier)
            .onFocusChanged { state -> focus.inList = state.hasFocus }
            .selectableGroup()
            .widthIn(min = TimelineMinWidth),
    ) {
        val count = timeline.steps.size
        for (index in count downTo 0) {
            key(index) {
                val entry = timeline.steps.getOrNull(index - 1)
                val headline = if (entry == null) stringResource(Res.string.history_start) else stepText(entry)
                val nowAt = stringResource(Res.string.history_now_at, headline)
                val current = index == timeline.cursor
                val undone = index > timeline.cursor
                TimelineRow(
                    headline = headline,
                    supporting = entry?.let { step -> supportingText(step, timeline.now, undone) },
                    document = entry?.after ?: timeline.start,
                    origin = entry?.before ?: timeline.start, // b-509b
                    position = count - index,
                    current = current,
                    undone = undone,
                    onClick = {
                        focus.noteJump()
                        dispatcher.dispatch(WorkspaceAction.JumpTo(index))
                        announcer.announce(nowAt)
                    },
                    modifier = if (current) Modifier.focusRequester(currentRow) else Modifier,
                )
            }
        }
        val hint = when {
            count == 0 -> Res.string.history_empty
            timeline.cursor < count -> Res.string.history_future_hint
            else -> null
        }
        if (hint != null) {
            BuilderText(
                text = stringResource(hint),
                modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small),
                emphasis = Emphasis.Secondary,
            )
        }
    }
}

/** One step, or Start, as a row that jumps there. */
@Composable
private fun TimelineRow(
    headline: String,
    supporting: String?,
    document: ThemeDocument,
    origin: ThemeDocument,
    position: Int,
    current: Boolean,
    undone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badge = stringResource(Res.string.history_current)
    BuilderListRow(
        headline = headline,
        modifier = modifier,
        supporting = supporting,
        leading = { StepSwatch(document, origin, position, undone) },
        onClick = onClick,
        selected = current,
        trailing = if (current) {
            { BuilderBadge(label = badge) }
        } else {
            null
        },
    )
}

/**
 * How long ago [step] was made, "2 minutes ago", with "undone" after it once it was [undone]. A step
 * saved before steps kept a time has no age, and only says Undone when it was.
 */
@Composable
private fun supportingText(
    step: HistoryEntry,
    now: Long,
    undone: Boolean,
): String? {
    val age = step.at?.let { at -> ageText(ProjectAge.of(at, now)) }
    return when {
        !undone -> age
        age != null -> stringResource(Res.string.history_undone_age, age)
        else -> stringResource(Res.string.history_undone)
    }
}

/**
 * The scheme [document] shows as its own target sees it, a skeleton until its colors are read. The
 * row [position] places from the top waits a frame for each [SWATCHES_PER_FRAME] rows above it, so a
 * long history never reads every scheme inside one frame. A swatch that already shows keeps its
 * colors while [document] keeps changing from the same [origin], as a drag folding into its step
 * does, and reads again once it has held still for [HOLD_STILL_NANOS], so a drag never reads a
 * scheme on every frame. A row that shows a step from another [origin] now, as every row does once
 * the oldest step drops at capacity, starts over and never shows the last step's colors.
 */
@Composable
private fun StepSwatch(
    document: ThemeDocument,
    origin: ThemeDocument,
    position: Int,
    undone: Boolean,
) {
    val resolver = rememberThemeResolver()
    val probe = LocalSwatchReadProbe.current
    // b-509b
    val colors by key(origin) {
        produceState<SwatchColors?>(null, document, resolver) {
            // A new document for the row starts this over, so a step that moves on every frame is not read.
            if (value != null) awaitHoldStill()
            repeat(1 + position / SWATCHES_PER_FRAME) { withFrameNanos { } }
            probe?.invoke()
            value = SwatchColors.of(lightScheme(resolver, document))
        }
    }
    val faded = if (undone) Modifier.alpha(UNDONE_SWATCH_ALPHA) else Modifier
    val shown = colors
    if (shown == null) {
        SchemeChipSkeleton(faded)
    } else {
        SchemeChip(shown.primary, shown.secondaryContainer, shown.tertiaryContainer, faded)
    }
}

/** The light scheme of [document] for its own target, from the resolver's scheme cache alone. */
private fun lightScheme(
    resolver: ThemeResolver,
    document: ThemeDocument,
): DynamicScheme {
    val target = ExportTarget.of(document.library, document.expressive)
    return resolver.scheme(SchemeInputs.from(document.forTarget(target)), isDark = false)
}

/** The three colors a swatch draws, read from its scheme once. */
private class SwatchColors(
    val primary: Color,
    val secondaryContainer: Color,
    val tertiaryContainer: Color,
) {
    companion object {
        fun of(scheme: DynamicScheme): SwatchColors =
            SwatchColors(Color(scheme.primary), Color(scheme.secondaryContainer), Color(scheme.tertiaryContainer))
    }
}

/**
 * Whether the list holds focus, and whether it did when the last jump or key ran, which a skin
 * switch needs after it has dropped focus. Plain fields, since nothing draws from them.
 */
private class ListFocus {
    /** Whether focus is in the list right now. */
    var inList: Boolean = false

    /** Whether focus was in the list when the last jump or key ran. */
    var heldAtLastAction: Boolean = false
        private set

    /** Whether the row the history lands on next takes focus, after Undo or Redo pressed here. */
    var followCursor: Boolean = false

    fun noteJump() {
        heldAtLastAction = inList
        followCursor = false
    }

    fun noteKey() {
        heldAtLastAction = inList
        followCursor = inList
    }
}

/**
 * Puts focus on [currentRow] after Undo or Redo pressed in the list moved [cursor], and after a
 * library switch dropped it while the list held it.
 *
 * The switch lands with the skin, a frame or two after the jump, and redraws every row in the new
 * skin. The row that held focus goes with its old skin and leaves focus nowhere, where no key
 * reaches the list, not even Esc. So focus goes back in the same composition that swaps the rows,
 * before the frame ends, and a key pressed at any point of the switch still lands in the list.
 */
@Composable
private fun FocusFollowsTheHistory(
    cursor: Int,
    focus: ListFocus,
    currentRow: FocusRequester,
) {
    LaunchedEffect(cursor) {
        if (!focus.followCursor) return@LaunchedEffect
        focus.followCursor = false
        // A row this list does not show yet has no node to take focus, and that is fine.
        runCatching { currentRow.requestFocus() }
    }
    // b-509b
    val library = LocalSkin.current.library
    val lastLibrary = remember { LibraryHolder(library) }
    SideEffect {
        if (lastLibrary.library == library) return@SideEffect
        lastLibrary.library = library
        if (focus.heldAtLastAction && !focus.inList) runCatching { currentRow.requestFocus() }
    }
}

/** The library the list last drew in. Plain, since only the effect above reads it. */
private class LibraryHolder(
    var library: Library,
)

/** The narrowest the list gets, so a short history does not squeeze its rows. */
private val TimelineMinWidth: Dp = 280.dp

/** How faded an undone step's swatch is, the kit's own disabled alpha. */
private const val UNDONE_SWATCH_ALPHA = 0.38f

/** How many swatches read their scheme in one frame. */
private const val SWATCHES_PER_FRAME = 8

/**
 * Told each time a swatch reads its scheme, or null, which it always is outside tests. Tests
 * provide it to prove a drag running under the open list reads the newest step once, not per frame.
 */
internal val LocalSwatchReadProbe: ProvidableCompositionLocal<(() -> Unit)?> =
    staticCompositionLocalOf { null }

/** Waits out [HOLD_STILL_NANOS] of frames, the time a step holds still before its swatch reads it again. */
private suspend fun awaitHoldStill() {
    val start = withFrameNanos { frame -> frame }
    var now = start
    while (now - start < HOLD_STILL_NANOS) now = withFrameNanos { frame -> frame }
}

/** How long a swatch's step holds still before the swatch reads it again, in frame time. */
private const val HOLD_STILL_NANOS: Long = 150_000_000L
