package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.platform.UriHandler
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.feature.about.GITHUB_URL
import com.materialkolor.builder.feature.poster.shufflesNothing
import com.materialkolor.builder.feature.poster.styleName
import com.materialkolor.builder.feature.poster.styleTooltip
import com.materialkolor.builder.feature.topbar.LibraryChoice
import com.materialkolor.builder.feature.topbar.TopBarControl
import com.materialkolor.builder.feature.topbar.redoText
import com.materialkolor.builder.feature.topbar.undoText
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.ShuffleLock
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.command_add_image
import com.materialkolor.builder.generated.resources.command_choice
import com.materialkolor.builder.generated.resources.command_copy_seed
import com.materialkolor.builder.generated.resources.command_nothing_to_redo
import com.materialkolor.builder.generated.resources.command_nothing_to_undo
import com.materialkolor.builder.generated.resources.command_single_keys
import com.materialkolor.builder.generated.resources.command_use_library
import com.materialkolor.builder.generated.resources.command_use_style
import com.materialkolor.builder.generated.resources.extras_amoled
import com.materialkolor.builder.generated.resources.extras_motion_expressive
import com.materialkolor.builder.generated.resources.extras_motion_label
import com.materialkolor.builder.generated.resources.extras_motion_standard
import com.materialkolor.builder.generated.resources.finetune_keep_hue_spoken
import com.materialkolor.builder.generated.resources.finetune_keep_seed_spoken
import com.materialkolor.builder.generated.resources.finetune_title
import com.materialkolor.builder.generated.resources.history_title
import com.materialkolor.builder.generated.resources.poster_all_locked
import com.materialkolor.builder.generated.resources.poster_copied_hex
import com.materialkolor.builder.generated.resources.poster_copy_hex
import com.materialkolor.builder.generated.resources.poster_image
import com.materialkolor.builder.generated.resources.poster_lock_hue
import com.materialkolor.builder.generated.resources.poster_lock_seed
import com.materialkolor.builder.generated.resources.poster_lock_style
import com.materialkolor.builder.generated.resources.poster_shuffle
import com.materialkolor.builder.generated.resources.style_chip
import com.materialkolor.builder.generated.resources.style_keep_spoken
import com.materialkolor.builder.generated.resources.topbar_about
import com.materialkolor.builder.generated.resources.topbar_commands
import com.materialkolor.builder.generated.resources.topbar_github
import com.materialkolor.builder.generated.resources.topbar_help
import com.materialkolor.builder.generated.resources.topbar_library
import com.materialkolor.builder.generated.resources.topbar_library_custom
import com.materialkolor.builder.generated.resources.topbar_library_fluent
import com.materialkolor.builder.generated.resources.topbar_library_m3
import com.materialkolor.builder.generated.resources.topbar_library_m3_expressive
import com.materialkolor.builder.generated.resources.topbar_library_unstyled
import com.materialkolor.builder.generated.resources.topbar_more
import com.materialkolor.builder.generated.resources.topbar_shortcuts
import com.materialkolor.builder.kit.layout.WindowClass
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The registry's sections, one per part of the builder, each adding its commands in the order the
// palette lists them. The theme's sections are here, the preview's in PreviewCommands.kt, and the
// project's, the export's and the appearance's in ProjectCommands.kt. None restarts on its own,
// since a section that did would add its commands a second time to the list the registry already
// handed out. A state one reads rebuilds the whole registry instead.

@Composable
@NonRestartableComposable
internal fun generalCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    uriHandler: UriHandler,
    shortcuts: ShortcutsModel,
) {
    val more = stringResource(Res.string.topbar_more)
    val palette = stringResource(Res.string.topbar_commands)
    val inOverflow = list.windowClass == WindowClass.Compact || TopBarControl.Commands in list.overflowed
    val paletteSite = if (inOverflow) {
        ControlSite.MenuItem(Region.TopBar, more, palette)
    } else {
        ControlSite.Direct(Region.TopBar, palette)
    }
    list.add("palette", CommandCategory.General, palette, paletteSite, shortcut = Shortcut.Palette) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Palette))
    }
    val sheet = stringResource(Res.string.topbar_shortcuts)
    val sheetSite = ControlSite.MenuItem(Region.TopBar, more, sheet)
    list.add("cheatSheet", CommandCategory.General, sheet, sheetSite, shortcut = Shortcut.CheatSheet) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.CheatSheet))
    }
    val singleKeys = stringResource(Res.string.command_single_keys)
    val on = state.preferences.singleKeyShortcuts
    val singleKeysSite = ControlSite.InPanel(Panel.CheatSheet, singleKeys)
    list.add("singleKeys", CommandCategory.General, singleKeys, singleKeysSite, selected = on) {
        shortcuts.setSingleKeys(!on)
    }
    val help = stringResource(Res.string.topbar_help)
    list.add("help", CommandCategory.General, help, ControlSite.MenuItem(Region.TopBar, more, help)) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Help))
    }
    val about = stringResource(Res.string.topbar_about)
    list.add("about", CommandCategory.General, about, ControlSite.MenuItem(Region.TopBar, more, about)) {
        dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.About))
    }
    val github = stringResource(Res.string.topbar_github)
    list.add("github", CommandCategory.General, github, ControlSite.MenuItem(Region.TopBar, more, github)) {
        uriHandler.openUri(GITHUB_URL)
    }
}

@Composable
@NonRestartableComposable
internal fun historyCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val undo = undoText(state.history, state.document)
    val redo = redoText(state.history, state.document)
    val nothingToUndo = stringResource(Res.string.command_nothing_to_undo)
    val nothingToRedo = stringResource(Res.string.command_nothing_to_redo)
    list.add(
        id = "undo",
        category = CommandCategory.History,
        label = undo,
        site = topBarSite(list, undo, TopBarControl.Undo),
        shortcut = Shortcut.Undo,
        disabledBecause = nothingToUndo.takeUnless { state.history.canUndo },
    ) { dispatcher.dispatch(WorkspaceAction.Undo) }
    list.add(
        id = "redo",
        category = CommandCategory.History,
        label = redo,
        site = topBarSite(list, redo, TopBarControl.Redo),
        shortcut = Shortcut.Redo,
        disabledBecause = nothingToRedo.takeUnless { state.history.canRedo },
    ) { dispatcher.dispatch(WorkspaceAction.Redo) }
    // The list always opens, with only Start in it before the first change.
    val history = stringResource(Res.string.history_title)
    list.add(
        id = "history",
        category = CommandCategory.History,
        label = history,
        site = topBarSite(list, history, TopBarControl.History),
        shortcut = Shortcut.History,
    ) { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.History)) }
}

/**
 * A top bar button that phones, and a Medium bar short of room, move into the overflow menu.
 */
@Composable
private fun topBarSite(
    list: CommandList,
    name: String,
    control: TopBarControl,
): ControlSite =
    if (list.windowClass == WindowClass.Compact || control in list.overflowed) {
        ControlSite.MenuItem(Region.TopBar, stringResource(Res.string.topbar_more), name)
    } else {
        ControlSite.Direct(Region.TopBar, name)
    }

@Composable
@NonRestartableComposable
internal fun seedCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val seedEntry = state.capabilities[Control.SeedEntryPoints]
    val shuffle = stringResource(Res.string.poster_shuffle)
    list.add(
        id = "shuffle",
        category = CommandCategory.Seed,
        label = shuffle,
        site = ControlSite.Direct(Region.Poster, shuffle),
        control = seedEntry,
        shortcut = Shortcut.Shuffle,
        disabledBecause = stringResource(Res.string.poster_all_locked).takeIf { state.preferences.shufflesNothing() },
    ) { dispatcher.dispatch(WorkspaceAction.Shuffle(origin = null)) }
    ShuffleLock.entries.forEach { lock ->
        val name = stringResource(lockName(lock))
        val on = when (lock) {
            ShuffleLock.Hue -> state.preferences.hueLock
            ShuffleLock.Style -> state.preferences.styleLock
            ShuffleLock.Seed -> state.preferences.seedLock
        }
        val shortcut = when (lock) {
            ShuffleLock.Hue -> Shortcut.HueLock
            ShuffleLock.Style -> Shortcut.StyleLock
            ShuffleLock.Seed -> null
        }
        val site = lockSite(lock) // b-524
        list.add("lock.${lock.name}", CommandCategory.Seed, name, site, shortcut = shortcut, selected = on) {
            dispatcher.dispatch(WorkspaceAction.SetLock(lock, !on))
        }
    }
    val hexLabel = stringResource(Res.string.poster_copied_hex)
    val copySite = ControlSite.Direct(Region.Poster, stringResource(Res.string.poster_copy_hex))
    val copy = stringResource(Res.string.command_copy_seed)
    list.add("copySeed", CommandCategory.Seed, copy, copySite, shortcut = Shortcut.CopySeed) {
        dispatcher.dispatch(WorkspaceAction.CopyText(state.document.seed.toHex(), hexLabel))
    }
    val imageSite = ControlSite.Direct(Region.Poster, stringResource(Res.string.poster_image))
    val image = stringResource(Res.string.command_add_image)
    list.add("addImage", CommandCategory.Seed, image, imageSite, seedEntry, shortcut = Shortcut.AddImage) {
        dispatcher.dispatch(WorkspaceAction.OpenImagePicker)
    }
}

/**
 * Where [lock]'s control sits, the Style lock by the style and the other two in Fine-tune, each
 * named the way it reads out.
 */
@Composable
private fun lockSite(lock: ShuffleLock): ControlSite {
    // b-524
    val fineTune = stringResource(Res.string.finetune_title)
    return when (lock) {
        ShuffleLock.Hue -> ControlSite.Direct(
            Region.Poster,
            stringResource(Res.string.finetune_keep_hue_spoken),
            fineTune,
        )
        ShuffleLock.Style -> ControlSite.Direct(Region.Poster, stringResource(Res.string.style_keep_spoken))
        ShuffleLock.Seed -> ControlSite.Direct(
            Region.Poster,
            stringResource(Res.string.finetune_keep_seed_spoken),
            fineTune,
        )
    }
}

private fun lockName(lock: ShuffleLock): StringResource =
    when (lock) {
        ShuffleLock.Hue -> Res.string.poster_lock_hue
        ShuffleLock.Style -> Res.string.poster_lock_style
        ShuffleLock.Seed -> Res.string.poster_lock_seed
    }

@Composable
@NonRestartableComposable
internal fun libraryAndStyleCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val current = LibraryChoice.of(state.document)
    val switcher = stringResource(Res.string.topbar_library)
    LibraryChoice.entries.forEachIndexed { index, choice ->
        val name = stringResource(libraryName(choice))
        // Wherever the switcher shows them, as it measured itself, which on a wide window depends on
        // the room the top bar's actions leave it.
        val site = if (list.librarySegmented) {
            ControlSite.Direct(Region.TopBar, name)
        } else {
            ControlSite.MenuItem(Region.TopBar, switcher, name)
        }
        list.add(
            id = "library.${choice.name}",
            category = CommandCategory.Library,
            label = stringResource(Res.string.command_use_library, name),
            site = site,
            shortcut = LIBRARY_KEYS[index],
            selected = choice == current,
        ) {
            // The new previews reveal from the switcher, as a press on it would.
            if (choice != current) {
                dispatcher.dispatch(WorkspaceAction.PickLibrary(choice, origin = list.switcherOrigin()))
            }
        }
    }
    val styleState = state.capabilities[Control.Style]
    Style.entries.forEach { style ->
        val name = stringResource(styleName(style))
        val selected = style == state.document.style
        val chip = stringResource(Res.string.style_chip, name, stringResource(styleTooltip(style)))
        list.add(
            id = "style.${style.name}",
            category = CommandCategory.Style,
            label = stringResource(Res.string.command_use_style, name),
            site = ControlSite.Direct(Region.Poster, chip),
            control = styleState,
            selected = selected,
        ) {
            if (!selected) {
                dispatcher.dispatch(WorkspaceAction.EditWithReveal(DocumentChange.SetStyle(style), origin = null))
            }
        }
    }
}

private val LIBRARY_KEYS = listOf(
    Shortcut.Library1,
    Shortcut.Library2,
    Shortcut.Library3,
    Shortcut.Library4,
    Shortcut.Library5,
)

private fun libraryName(choice: LibraryChoice): StringResource =
    when (choice) {
        LibraryChoice.M3 -> Res.string.topbar_library_m3
        LibraryChoice.M3Expressive -> Res.string.topbar_library_m3_expressive
        LibraryChoice.Unstyled -> Res.string.topbar_library_unstyled
        LibraryChoice.Fluent -> Res.string.topbar_library_fluent
        LibraryChoice.Custom -> Res.string.topbar_library_custom
    }

@Composable
@NonRestartableComposable
internal fun targetCommands(
    list: CommandList,
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val extras = stringResource(Res.string.finetune_title) // b-524
    val amoled = stringResource(Res.string.extras_amoled)
    val on = state.document.amoled
    list.add(
        id = "amoled",
        category = CommandCategory.Target,
        label = amoled,
        site = ControlSite.Direct(Region.Poster, amoled, opener = extras),
        control = state.capabilities[Control.AmoledDark],
        selected = on,
    ) { dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetAmoled(!on), EditPhase.Discrete)) }
    val motion = stringResource(Res.string.extras_motion_label)
    MotionSchemeChoice.entries.forEach { choice ->
        val name = stringResource(motionSchemeName(choice))
        val selected = choice == state.document.motionScheme
        list.add(
            id = "motionScheme.${choice.name}",
            category = CommandCategory.Target,
            label = stringResource(Res.string.command_choice, motion, name),
            site = ControlSite.Direct(Region.Poster, name, opener = extras),
            control = state.capabilities[Control.MotionScheme],
            selected = selected,
        ) {
            if (!selected) {
                dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetMotionScheme(choice), EditPhase.Discrete))
            }
        }
    }
}

private fun motionSchemeName(choice: MotionSchemeChoice): StringResource =
    when (choice) {
        MotionSchemeChoice.Standard -> Res.string.extras_motion_standard
        MotionSchemeChoice.Expressive -> Res.string.extras_motion_expressive
    }
