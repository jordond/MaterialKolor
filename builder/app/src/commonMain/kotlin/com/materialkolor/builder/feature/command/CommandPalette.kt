package com.materialkolor.builder.feature.command

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.persist.FineTuneRow
import com.materialkolor.builder.feature.canvas.RampTarget
import com.materialkolor.builder.feature.poster.readoutName
import com.materialkolor.builder.feature.poster.styleName
import com.materialkolor.builder.feature.share.ShareController
import com.materialkolor.builder.feature.share.sharedNoticeText
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.accents_label
import com.materialkolor.builder.generated.resources.contrast_label
import com.materialkolor.builder.generated.resources.extras_targets_label
import com.materialkolor.builder.generated.resources.keycolors_label
import com.materialkolor.builder.generated.resources.palette_category_poster
import com.materialkolor.builder.generated.resources.palette_category_roles
import com.materialkolor.builder.generated.resources.palette_close
import com.materialkolor.builder.generated.resources.palette_go_to
import com.materialkolor.builder.generated.resources.palette_nothing
import com.materialkolor.builder.generated.resources.palette_open_shared
import com.materialkolor.builder.generated.resources.palette_query
import com.materialkolor.builder.generated.resources.palette_row_keys
import com.materialkolor.builder.generated.resources.palette_set_seed
import com.materialkolor.builder.generated.resources.palette_show_on_ramp
import com.materialkolor.builder.generated.resources.palette_title
import com.materialkolor.builder.generated.resources.palette_words_accents
import com.materialkolor.builder.generated.resources.palette_words_appearance
import com.materialkolor.builder.generated.resources.palette_words_copy_seed
import com.materialkolor.builder.generated.resources.palette_words_device
import com.materialkolor.builder.generated.resources.palette_words_export
import com.materialkolor.builder.generated.resources.palette_words_fullscreen
import com.materialkolor.builder.generated.resources.palette_words_image
import com.materialkolor.builder.generated.resources.palette_words_inspect
import com.materialkolor.builder.generated.resources.palette_words_keys
import com.materialkolor.builder.generated.resources.palette_words_motion
import com.materialkolor.builder.generated.resources.palette_words_pins
import com.materialkolor.builder.generated.resources.palette_words_projects
import com.materialkolor.builder.generated.resources.palette_words_share
import com.materialkolor.builder.generated.resources.palette_words_shuffle
import com.materialkolor.builder.generated.resources.palette_words_undo
import com.materialkolor.builder.generated.resources.palette_words_vision
import com.materialkolor.builder.generated.resources.pins_label
import com.materialkolor.builder.generated.resources.poster_seed
import com.materialkolor.builder.generated.resources.style_label
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderListRow
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.extensions.collectAsState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.materialkolor.builder.domain.model.Style as PaletteStyle

/**
 * The command palette (F-33), every command of the registry behind one search field, open while
 * `state.panel` is `Panel.Palette`.
 *
 * Typing filters the rows as it goes, by label, group and a few other words for each. Text the
 * palette understands leads the list, a color to set the seed to, a share link or code to open, or
 * a style's name. A role's name offers to show it on its ramp, and a poster section's name offers to
 * go to it (AR-10). With nothing typed the commands come in the registry's order, the ones run
 * lately first. A command that cannot run keeps its row, which says why instead of its keys.
 *
 * Enter runs the top row that can run from inside the key press, so a copy or a share still counts
 * as the user's own (R-B-302). Down moves from the field into the rows and Up from the first row
 * back. Esc throws away a typed search, and a second Esc closes. A row that opens a panel takes the
 * palette's place, and any other closes the palette before it runs.
 *
 * @param[returnFocusTo] Where focus goes once it closes, the Commands button that opened it, or the
 * page's focus holder when Cmd or Ctrl+K did (AR-09).
 */
@Composable
internal fun CommandPalette(
    visible: Boolean,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
    model: CommandPaletteModel = metroViewModel(),
    share: ShareController = metroViewModel(),
) {
    // Outlives the palette, so a shared theme still opens once it has closed.
    val scope = rememberCoroutineScope()
    val runner = remember(dispatcher) { PaletteRunner(dispatcher) }
    // Once the palette has opened, its commands stay composed out here rather than in the dialog,
    // so what one starts and finishes later, a download or the Saved toast, carries on after the
    // palette has closed.
    val opened = remember { mutableStateOf(false) }
    val commands = if (visible || opened.value) actionRegistry(state, runner) else emptyList()
    if (visible && !opened.value) SideEffect { opened.value = true }
    val width = if (LocalLayout.current.windowClass == WindowClass.Expanded) ExpandedWidth else MediumWidth
    // The dialog keeps this much clear around its panel, so the panel itself comes out at the width.
    val margin = LocalBuilderTokens.current.spacing.large
    val close = { dispatcher.dispatch(WorkspaceAction.ClosePanel) }
    BuilderDialog(
        visible = visible,
        onDismissRequest = close,
        title = stringResource(Res.string.palette_title),
        modifier = modifier.width(width + margin * 2),
        returnFocusTo = returnFocusTo,
        actions = { BuilderButton(onClick = close, label = stringResource(Res.string.palette_close)) },
    ) {
        PaletteBody(
            state = state,
            commands = commands,
            runner = runner,
            model = model,
            onOpenShared = { code -> scope.openShared(share, code, dispatcher) },
        )
    }
}

@Composable
private fun ColumnScope.PaletteBody(
    state: WorkspaceModel.State,
    commands: List<Command>,
    runner: PaletteRunner,
    model: CommandPaletteModel,
    onOpenShared: (code: String) -> Unit,
) {
    DisposableEffect(model) { onDispose { model.clear() } }
    val palette by model.collectAsState()
    val entries = paletteEntries(state, commands, runner)
    val styleNames = PaletteStyle.entries.associateWith { style -> stringResource(styleName(style)) }
    val understood = understoodEntries(palette.query, entries, runner, onOpenShared)
    val rows = paletteRows(palette.query, palette.recents, entries, styleNames, understood)
    val requesters = remember(rows.size) { List(rows.size) { FocusRequester() } }
    val field = remember { FocusRequester() }
    val runnable = rows.indices.filter { index -> rows[index].state == CommandState.Enabled }

    fun run(entry: PaletteEntry) {
        if (entry.state != CommandState.Enabled) return
        model.ran(entry.id)
        runner.run(entry.run)
    }

    // A frame may not have drawn the latest search yet, so Enter finds its rows afresh.
    fun submit() {
        val latest = model.state.value
        val current = if (latest.query == palette.query) {
            rows
        } else {
            paletteRows(latest.query, latest.recents, entries, styleNames, understood)
        }
        current.firstOrNull { entry -> entry.state == CommandState.Enabled }?.let(::run)
    }

    BuilderTextField(
        value = palette.committed,
        onCommit = model::commit,
        label = stringResource(Res.string.palette_query),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(field)
            .onPreviewKeyEvent { event ->
                val first = runnable.firstOrNull()
                if (!event.pressed(Key.DirectionDown) || first == null) return@onPreviewKeyEvent false
                requesters[first].requestFocus()
                true
            }
            // Esc on a search the field already committed, which the field lets through, empties it.
            .onKeyEvent { event ->
                val typed = model.state.value.query
                if (!event.pressed(Key.Escape) || typed.isEmpty()) return@onKeyEvent false
                model.clear()
                true
            },
        onDraftChange = model::type,
        onSubmit = { submit() },
    )
    BuilderScrollArea(Modifier.weight(1f, fill = false).height(ListHeight), tabStop = false) {
        if (rows.isEmpty()) {
            BuilderText(stringResource(Res.string.palette_nothing), emphasis = Emphasis.Secondary)
        }
        rows.forEachIndexed { index, entry ->
            key(entry.id) {
                BuilderListRow(
                    headline = entry.label,
                    modifier = Modifier
                        .focusRequester(requesters[index])
                        .onKeyEvent { event ->
                            when {
                                event.pressed(Key.DirectionDown) -> {
                                    runnable.firstOrNull { other -> other > index }?.let { next ->
                                        requesters[next].requestFocus()
                                    }
                                    true
                                }
                                event.pressed(Key.DirectionUp) -> {
                                    val previous = runnable.lastOrNull { other -> other < index }
                                    if (previous == null) field.requestFocus() else requesters[previous].requestFocus()
                                    true
                                }
                                else -> {
                                    false
                                }
                            }
                        },
                    supporting = supportingLine(entry),
                    onClick = { run(entry) },
                    selected = entry.selected,
                    enabled = entry.state == CommandState.Enabled,
                )
            }
        }
    }
}

/** The line under a row, its group and its keys, or why it cannot run. */
@Composable
private fun supportingLine(entry: PaletteEntry): String =
    when (val state = entry.state) {
        is CommandState.Disabled -> {
            state.reason
        }
        CommandState.Enabled -> {
            val keys = entry.keys
            if (keys == null) entry.category else stringResource(Res.string.palette_row_keys, entry.category, keys)
        }
    }

/** What the palette understood [query] to be, first, then the rows the search finds in [entries]. */
private fun paletteRows(
    query: String,
    recents: List<String>,
    entries: List<PaletteEntry>,
    styleNames: Map<PaletteStyle, String>,
    understood: (PastedText) -> PaletteEntry?,
): List<PaletteEntry> {
    val first = classify(query, styleNames)?.let(understood)
    val found = searchPalette(entries, query, recents)
    return if (first == null) found else listOf(first) + found.filter { entry -> entry.id != first.id }
}

/**
 * Turns what the palette understood into its row. A color sets the seed, a share link or code opens
 * its theme, and a style's name is that style's own command, disabled with its reason where the
 * target has no styles.
 */
@Composable
private fun understoodEntries(
    query: String,
    entries: List<PaletteEntry>,
    runner: PaletteRunner,
    onOpenShared: (code: String) -> Unit,
): (PastedText) -> PaletteEntry? {
    val seedCategory = stringResource(CommandCategory.Seed.title)
    val shareCategory = stringResource(CommandCategory.Share.title)
    val openShared = stringResource(Res.string.palette_open_shared)
    // Only the search this frame drew needs the words, since a row Enter finds ahead of a frame runs
    // without being shown.
    val color = (classify(query) as? PastedText.Color)?.argb
    val setSeed = color?.let { argb -> stringResource(Res.string.palette_set_seed, argb.toHex()) }.orEmpty()
    return { understood ->
        when (understood) {
            is PastedText.Color -> {
                PaletteEntry(SET_SEED_ID, setSeed, seedCategory, null, CommandState.Enabled, null) {
                    val change = DocumentChange.SetSeed(understood.argb, SeedSource.Typed)
                    runner.dispatch(WorkspaceAction.EditWithReveal(change, origin = null))
                }
            }
            is PastedText.Share -> {
                PaletteEntry(OPEN_SHARED_ID, openShared, shareCategory, null, CommandState.Enabled, null) {
                    onOpenShared(understood.code)
                }
            }
            is PastedText.Style -> {
                entries.firstOrNull { entry -> entry.id == "style.${understood.style.name}" }
            }
        }
    }
}

/** Every one of [commands] as a row, then the poster's sections and the roles (AR-10). */
@Composable
private fun paletteEntries(
    state: WorkspaceModel.State,
    commands: List<Command>,
    runner: PaletteRunner,
): List<PaletteEntry> {
    val apple = LocalAppleKeys.current
    val categories = CommandCategory.entries.associateWith { category -> stringResource(category.title) }
    val words = paletteWords()
    val rows = commands.map { command ->
        PaletteEntry(
            id = command.id,
            label = command.label,
            category = categories.getValue(command.category),
            keys = command.shortcut?.text(apple),
            state = command.state,
            selected = command.selected,
            words = words(command.id),
            run = command.run,
        )
    }
    val rolesCategory = stringResource(Res.string.palette_category_roles)
    val roles = Role.entries.map { role ->
        val name = ColorRef.OfRole(role).readoutName(state.document)
        PaletteEntry(
            id = "role.${role.name}",
            label = stringResource(Res.string.palette_show_on_ramp, name),
            category = rolesCategory,
            keys = null,
            state = CommandState.Enabled,
            selected = null,
            onlyWhenAsked = true,
        ) { runner.dispatch(WorkspaceAction.ShowOnRamp(RampTarget.OfRole(role, isDark = false))) }
    }
    val posterCategory = stringResource(Res.string.palette_category_poster)
    val sections = PosterSection.entries.map { section ->
        PaletteEntry(
            id = "section.${section.name}",
            label = stringResource(Res.string.palette_go_to, stringResource(section.title)),
            category = posterCategory,
            keys = null,
            state = CommandState.Enabled,
            selected = null,
            words = section.words?.let { resource -> wordList(stringResource(resource)) }.orEmpty(),
            onlyWhenAsked = true,
        ) {
            runner.dispatch(WorkspaceAction.SetPosterCollapsed(false))
            if (section.inCoreColors) runner.dispatch(WorkspaceAction.SetFineTuneRowOpen(FineTuneRow.CoreColors, true))
        }
    }
    return rows + sections + roles
}

/** The other words each command is found by, looked up by its id. */
@Composable
private fun paletteWords(): (id: String) -> List<String> {
    val resolved = WORDS.values.distinct().associateWith { resource -> wordList(stringResource(resource)) }
    return { id -> WORDS[id.substringBefore('.')]?.let(resolved::getValue).orEmpty() }
}

/** The words of each command, by its id or the part of its id before the first dot. */
private val WORDS: Map<String, StringResource> = mapOf(
    "shuffle" to Res.string.palette_words_shuffle,
    "undo" to Res.string.palette_words_undo,
    "copySeed" to Res.string.palette_words_copy_seed,
    "addImage" to Res.string.palette_words_image,
    "inspect" to Res.string.palette_words_inspect,
    "fullscreen" to Res.string.palette_words_fullscreen,
    "export" to Res.string.palette_words_export,
    "share" to Res.string.palette_words_share,
    "copyLink" to Res.string.palette_words_share,
    "projects" to Res.string.palette_words_projects,
    "appearance" to Res.string.palette_words_appearance,
    "motion" to Res.string.palette_words_motion,
    "vision" to Res.string.palette_words_vision,
    "deviceWidth" to Res.string.palette_words_device,
    "cheatSheet" to Res.string.palette_words_keys,
    "singleKeys" to Res.string.palette_words_keys,
)

private fun wordList(text: String): List<String> =
    text.split(',').map { word -> word.trim() }.filter { word -> word.isNotEmpty() }

/**
 * The poster's sections a search can go to, by the names the poster shows. Key colors and pins
 * sit in the Core colors row, which opens with them.
 */
private enum class PosterSection(
    val title: StringResource,
    val words: StringResource? = null,
    val inCoreColors: Boolean = false,
) {
    Seed(Res.string.poster_seed),
    Style(Res.string.style_label),
    Contrast(Res.string.contrast_label),
    KeyColors(Res.string.keycolors_label, inCoreColors = true),
    Pins(Res.string.pins_label, Res.string.palette_words_pins, inCoreColors = true),
    Accents(Res.string.accents_label, Res.string.palette_words_accents),
    TargetOptions(Res.string.extras_targets_label),
}

/**
 * The dispatcher the palette's commands send through, which closes the palette as a command runs.
 *
 * The first action a running command sends closes the palette ahead of it, unless it opens a panel,
 * which takes the palette's place under the same history entry. A command that sends nothing, such
 * as a download or an export option, closes the palette once it is done.
 */
private class PaletteRunner(
    private val workspace: Dispatcher<WorkspaceAction>,
) : Dispatcher<WorkspaceAction> {
    private var open = false

    override fun dispatch(action: WorkspaceAction) {
        if (open) {
            open = false
            if (action !is WorkspaceAction.OpenPanel) workspace.dispatch(WorkspaceAction.ClosePanel)
        }
        workspace.dispatch(action)
    }

    /** Runs [command] from inside the press, the palette still open as it starts. */
    fun run(command: () -> Unit) {
        open = true
        try {
            command()
        } finally {
            if (open) {
                open = false
                workspace.dispatch(WorkspaceAction.ClosePanel)
            }
        }
    }
}

/** Opens the theme [code] carries, and toasts why when it cannot. */
private fun CoroutineScope.openShared(
    share: ShareController,
    code: String,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    launch {
        val notice = share.openShared(code) ?: return@launch
        dispatcher.dispatch(WorkspaceAction.ShowToast(sharedNoticeText(notice)))
    }
}

/** Whether this presses [key] on its own, with no modifier held. */
private fun KeyEvent.pressed(key: Key): Boolean {
    val modified = isShiftPressed || isCtrlPressed || isMetaPressed || isAltPressed
    return type == KeyEventType.KeyDown && this.key == key && !modified
}

private const val SET_SEED_ID = "palette.setSeed"
private const val OPEN_SHARED_ID = "palette.openShared"

/** The panel's width on a wide window and on a medium one. */
private val ExpandedWidth: Dp = 640.dp
private val MediumWidth: Dp = 560.dp

/** How tall the rows stand, so the palette keeps its size as a search narrows them. */
private val ListHeight: Dp = 400.dp
