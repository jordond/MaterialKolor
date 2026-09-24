package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import com.materialkolor.builder.core.session.Timeline
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.feature.about.GITHUB_URL
import com.materialkolor.builder.feature.command.LocalAppleKeys
import com.materialkolor.builder.feature.command.LocalShortcutFocus
import com.materialkolor.builder.feature.command.Shortcut
import com.materialkolor.builder.feature.history.TimelineList
import com.materialkolor.builder.feature.poster.switcherPulse
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.skinOf
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.history_title
import com.materialkolor.builder.generated.resources.history_tooltip
import com.materialkolor.builder.generated.resources.topbar_about
import com.materialkolor.builder.generated.resources.topbar_appearance_dark
import com.materialkolor.builder.generated.resources.topbar_appearance_light
import com.materialkolor.builder.generated.resources.topbar_appearance_system
import com.materialkolor.builder.generated.resources.topbar_commands
import com.materialkolor.builder.generated.resources.topbar_commands_tooltip
import com.materialkolor.builder.generated.resources.topbar_export
import com.materialkolor.builder.generated.resources.topbar_github
import com.materialkolor.builder.generated.resources.topbar_help
import com.materialkolor.builder.generated.resources.topbar_more
import com.materialkolor.builder.generated.resources.topbar_share
import com.materialkolor.builder.generated.resources.topbar_shortcuts
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderPopover
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.shell.TopBarRegion
import com.materialkolor.builder.kit.skin.LocalSkin
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The top bar, the library switcher on the start edge and the project's actions on the end edge.
 *
 * On a wide window it holds the switcher, the command palette, undo, redo, History, Share, Export
 * code and the overflow menu. The actions always get their full width, and the switcher takes what
 * is left, segmented where the window is wide and the row fits, a dropdown otherwise. A Medium
 * window keeps the dropdown's name whole, moving History, then the command palette, then redo, then
 * undo into the overflow until it fits. Phones show the mark and the project's name with Share,
 * Export and the overflow, which holds History, the command palette, undo and redo, and the
 * libraries in a row of chips under the bar.
 *
 * The History list opens in a popover under the History button, or under the overflow button once
 * History has moved there (D57). It stays open while someone jumps between steps and hands focus
 * back to whatever opened it, the page itself when the H key did.
 *
 * A library switch goes through the reveal from the switcher as one undo entry. What has to outlive
 * a skin switch, the open menu and which control has focus, is held here, outside the skin's own
 * top bar region, and [focus] comes from the workspace, which hands Share and Export focus back when
 * their panels close. The Expressive suggestion is the model's, raised with the switch it follows.
 * It shows once the skin has caught up with the document, so it first draws in the new skin.
 */
@Composable
internal fun TopBarContent(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    focus: TopBarFocus = rememberTopBarFocus(),
) {
    var menuOpen by remember { mutableStateOf(false) }
    val currentRow = remember { FocusRequester() } // b-509
    val windowClass = LocalLayout.current.windowClass
    // b-406
    val fit = remember { MediumBarFit() }
    val overflowed = when (windowClass) {
        WindowClass.Compact -> MediumOverflowOrder.toSet()
        WindowClass.Medium -> fit.overflowed
        WindowClass.Expanded -> emptySet()
    }
    val report = LocalSwitcherForm.current
    if (report != null) SideEffect { report.overflowed = overflowed }
    // The switcher changes form with the window class, so focus it held follows it to the new one.
    LaunchedEffect(windowClass) { focus.restoreAfterRefit(TopBarControl.Library) }
    val items = overflowItems(state, dispatcher, overflowed, LocalUriHandler.current)
    val selected = LibraryChoice.of(state.document)
    val switcherModifier = Modifier
        .testTag(LIBRARY_SWITCHER_TAG)
        .topBarFocus(focus, TopBarControl.Library)
        .switcherPulse(state, dispatcher) // b-314
        .reportSwitcherOrigin(report) // b-503a
    val onSwitch = { choice: LibraryChoice, origin: Offset ->
        dispatcher.dispatch(WorkspaceAction.EditWithReveal(choice.change, origin))
    }
    val actions: @Composable () -> Unit = {
        TopBarIconButton(
            control = TopBarControl.Share,
            focus = focus,
            icon = IconId.Share,
            description = stringResource(Res.string.topbar_share),
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Share)) },
        )
        BuilderButton(
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Export)) },
            label = stringResource(Res.string.topbar_export),
            modifier = Modifier.topBarFocus(focus, TopBarControl.Export),
            emphasis = Emphasis.Primary,
            icon = IconId.Export,
        )
        // b-509
        // The list hangs from More once History has moved into it, with More kept in one place either way.
        HistoryPopover(
            expanded = state.panel == Panel.History && TopBarControl.History in overflowed,
            timeline = state.timeline,
            dispatcher = dispatcher,
            currentRow = currentRow,
        ) {
            BuilderMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                items = items,
            ) {
                TopBarIconButton(
                    control = TopBarControl.More,
                    focus = focus,
                    icon = IconId.More,
                    description = stringResource(Res.string.topbar_more),
                    onClick = { menuOpen = true },
                )
            }
        }
    }

    if (windowClass == WindowClass.Compact) {
        // b-406
        Column(modifier) {
            CompactTopBar(state.projectName, Modifier.testTag(TOP_BAR_TAG)) { actions() }
            LibraryChipRow(selected = selected, onSwitch = onSwitch, switcherModifier = switcherModifier)
        }
    } else {
        TopBarRegion(modifier.testTag(TOP_BAR_TAG)) {
            // b-231
            // The actions take their full width first, since a row measures its weighted child last, and
            // the switcher gets what is left, so More options is never squeezed.
            if (windowClass == WindowClass.Medium) {
                LibraryDropdown(
                    selected = selected,
                    onSwitch = onSwitch,
                    fit = fit,
                    modifier = Modifier.weight(1f),
                    switcherModifier = switcherModifier,
                )
            } else {
                FittedLibrarySwitcher(
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    switcherModifier = switcherModifier,
                    onSwitch = onSwitch,
                    onRefit = { focus.restoreAfterRefit(TopBarControl.Library) },
                )
            }
            if (TopBarControl.Commands !in overflowed) {
                TopBarIconButton(
                    control = TopBarControl.Commands,
                    focus = focus,
                    icon = IconId.Command,
                    description = stringResource(Res.string.topbar_commands),
                    // b-315
                    tooltip = stringResource(
                        Res.string.topbar_commands_tooltip,
                        Shortcut.Palette.text(LocalAppleKeys.current),
                    ),
                    onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Palette)) },
                )
            }
            if (TopBarControl.Undo !in overflowed) {
                TopBarIconButton(
                    control = TopBarControl.Undo,
                    focus = focus,
                    icon = IconId.Undo,
                    description = undoText(state.history, state.document),
                    enabled = state.history.canUndo,
                    onClick = { dispatcher.dispatch(WorkspaceAction.Undo) },
                )
            }
            if (TopBarControl.Redo !in overflowed) {
                TopBarIconButton(
                    control = TopBarControl.Redo,
                    focus = focus,
                    icon = IconId.Redo,
                    description = redoText(state.history, state.document),
                    enabled = state.history.canRedo,
                    onClick = { dispatcher.dispatch(WorkspaceAction.Redo) },
                )
            }
            // b-509
            if (TopBarControl.History !in overflowed) {
                HistoryPopover(
                    expanded = state.panel == Panel.History,
                    timeline = state.timeline,
                    dispatcher = dispatcher,
                    currentRow = currentRow,
                ) {
                    TopBarIconButton(
                        control = TopBarControl.History,
                        focus = focus,
                        icon = IconId.History,
                        description = stringResource(Res.string.history_title),
                        tooltip = stringResource(
                            Res.string.history_tooltip,
                            Shortcut.History.text(LocalAppleKeys.current),
                        ),
                        onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.History)) },
                    )
                }
            }
            actions()
        }
    }

    // A library switch can still reach this a composition before the kit moves it into the new
    // skin, so the suggestion waits for the skin the document asks for.
    val inDocumentSkin = LocalSkin.current == skinOf(state.document)
    ExpressiveSuggestion(
        visible = state.expressiveSuggestion && inDocumentSkin,
        returnFocusTo = focus.requester(TopBarControl.Library),
        onKeepMine = { dispatcher.dispatch(WorkspaceAction.DismissExpressiveSuggestion) },
        onApply = {
            dispatcher.dispatch(WorkspaceAction.DismissExpressiveSuggestion)
            dispatcher.dispatch(WorkspaceAction.EditWithReveal(expressiveStyleChange(state.document), origin = null))
        },
    )
}

/** An icon button under a tooltip that says what it does, the description unless [tooltip] says more. */
@Composable
private fun TopBarIconButton(
    control: TopBarControl,
    focus: TopBarFocus,
    icon: IconId,
    description: String,
    onClick: () -> Unit,
    tooltip: String = description,
    enabled: Boolean = true,
) {
    BuilderTooltip(text = tooltip) {
        BuilderIconButton(
            onClick = onClick,
            icon = icon,
            contentDescription = description,
            modifier = Modifier.topBarFocus(focus, control),
            enabled = enabled,
        )
    }
}

// b-509

/**
 * The History list in a popover under [anchor], open while [expanded] holds. Focus starts on the
 * step the theme is at, through [currentRow], and goes back to the anchor once the list closes, or
 * to the page when the H key opened it (AR-09).
 */
@Composable
private fun HistoryPopover(
    expanded: Boolean,
    timeline: Timeline?,
    dispatcher: Dispatcher<WorkspaceAction>,
    currentRow: FocusRequester,
    anchor: @Composable () -> Unit,
) {
    BuilderPopover(
        expanded = expanded,
        onDismissRequest = { dispatcher.dispatch(WorkspaceAction.ClosePanel) },
        initialFocus = currentRow,
        returnFocusTo = LocalShortcutFocus.current?.returnFocusFor(Panel.History, otherwise = null),
        anchor = anchor,
    ) {
        if (timeline != null) TimelineList(timeline, dispatcher, currentRow)
    }
}

/**
 * The overflow menu, the chrome's appearance, Help, the shortcuts, About and GitHub. Whichever of
 * History, the command palette, undo and redo the bar has moved in come first, all four on a phone.
 */
@Composable
private fun overflowItems(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    overflowed: Set<TopBarControl>, // b-406
    uriHandler: UriHandler,
): List<BuilderMenuItem> {
    val appearance = state.preferences.appearance
    val appearanceItems = Appearance.entries.map { option ->
        BuilderMenuItem(
            label = stringResource(appearanceLabel(option)),
            onClick = { dispatcher.dispatch(WorkspaceAction.SetAppearance(option)) },
            selected = option == appearance,
        )
    }
    val movedItems = buildList {
        if (TopBarControl.Commands in overflowed) {
            add(
                BuilderMenuItem(
                    label = stringResource(Res.string.topbar_commands),
                    onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Palette)) },
                    icon = IconId.Command,
                ),
            )
        }
        if (TopBarControl.Undo in overflowed) {
            add(
                BuilderMenuItem(
                    label = undoText(state.history, state.document),
                    onClick = { dispatcher.dispatch(WorkspaceAction.Undo) },
                    icon = IconId.Undo,
                    enabled = state.history.canUndo,
                ),
            )
        }
        if (TopBarControl.Redo in overflowed) {
            add(
                BuilderMenuItem(
                    label = redoText(state.history, state.document),
                    onClick = { dispatcher.dispatch(WorkspaceAction.Redo) },
                    icon = IconId.Redo,
                    enabled = state.history.canRedo,
                ),
            )
        }
        // b-509
        if (TopBarControl.History in overflowed) {
            add(
                BuilderMenuItem(
                    label = stringResource(Res.string.history_title),
                    onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.History)) },
                    icon = IconId.History,
                ),
            )
        }
    }
    return movedItems +
        appearanceItems +
        listOf(
            BuilderMenuItem(
                label = stringResource(Res.string.topbar_help),
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Help)) }, // b-314
                icon = IconId.Help,
            ),
            BuilderMenuItem(
                label = stringResource(Res.string.topbar_shortcuts),
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.CheatSheet)) },
                icon = IconId.Keyboard,
            ),
            BuilderMenuItem(
                label = stringResource(Res.string.topbar_about),
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.About)) },
                icon = IconId.Info,
            ),
            BuilderMenuItem(
                label = stringResource(Res.string.topbar_github),
                onClick = { uriHandler.openUri(GITHUB_URL) },
                icon = IconId.ExternalLink,
            ),
        )
}

private fun appearanceLabel(appearance: Appearance): StringResource =
    when (appearance) {
        Appearance.System -> Res.string.topbar_appearance_system
        Appearance.Light -> Res.string.topbar_appearance_light
        Appearance.Dark -> Res.string.topbar_appearance_dark
    }
