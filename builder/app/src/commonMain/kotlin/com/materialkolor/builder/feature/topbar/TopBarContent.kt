package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderPopover
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.shell.TopBarControlMaxHeight
import com.materialkolor.builder.kit.shell.TopBarRegion
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The top bar, the library switcher on the start edge and the project's actions on the end edge.
 *
 * On a wide window it holds the switcher, the command palette, undo, redo, History, Share, Export
 * code and the overflow menu. The actions always get their full width, and the switcher takes what
 * is left, segmented where the window is wide and the row fits, a dropdown otherwise. A wide window
 * shows Share with its label and the command palette with its key in a keycap, and turns both into
 * glyphs when that makes the room the segmented row needs. A Medium window keeps the dropdown's name
 * whole, moving History, then the command palette, then redo, then undo into the overflow until it
 * fits. Phones show the project's swatch and name with Share, Export as a glyph and the overflow,
 * which holds History, the command palette, undo and redo, and the libraries in a row of chips under
 * the bar.
 *
 * The History list opens in a popover under the History button, or under the overflow button once
 * History has moved there (D57). It stays open while someone jumps between steps and hands focus
 * back to whatever opened it, the page itself when the H key did. A jump can switch the skin, whose
 * top bar draws its buttons somewhere new, so both buttons move there with the list still open.
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
    // b-512
    // A wide bar shows Share's label and the palette's keycap while the segmented switcher still fits.
    val wideFit = remember { WideBarFit() }
    val wideForms = windowClass == WindowClass.Expanded && !wideFit.compact
    LaunchedEffect(wideForms) {
        focus.restoreAfterRefit(TopBarControl.Share)
        focus.restoreAfterRefit(TopBarControl.Commands)
    }
    val items = overflowItems(state, dispatcher, overflowed, LocalUriHandler.current)
    val selected = LibraryChoice.of(state.document)
    val switcherModifier = Modifier
        .testTag(LIBRARY_SWITCHER_TAG)
        // b-512
        // A dropdown's floating label stays inside the bar.
        .heightIn(max = TopBarControlMaxHeight)
        .topBarFocus(focus, TopBarControl.Library)
        .switcherPulse(state, dispatcher) // b-314
        .reportSwitcherOrigin(report) // b-503a
    val onSwitch = { choice: LibraryChoice, origin: Offset ->
        dispatcher.dispatch(WorkspaceAction.EditWithReveal(choice.change, origin))
    }
    // b-509
    // The list hangs from More once History has moved into it, with More kept in one place either way.
    val moreButton = rememberMovable {
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
    // b-509
    val historyButton = rememberMovable {
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
    val actions: @Composable () -> Unit = {
        val share = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Share)) }
        // b-512
        if (wideForms) {
            BuilderButton(
                onClick = share,
                label = stringResource(Res.string.topbar_share),
                modifier = Modifier.topBarFocus(focus, TopBarControl.Share),
                emphasis = Emphasis.Secondary,
            )
        } else {
            TopBarIconButton(
                control = TopBarControl.Share,
                focus = focus,
                icon = IconId.Share,
                description = stringResource(Res.string.topbar_share),
                onClick = share,
            )
        }
        val export = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Export)) }
        // b-512
        // A phone's bar has room for the glyph alone, still named Export code.
        if (windowClass == WindowClass.Compact) {
            BuilderIconButton(
                onClick = export,
                icon = IconId.Export,
                contentDescription = stringResource(Res.string.topbar_export),
                modifier = Modifier.topBarFocus(focus, TopBarControl.Export),
                emphasis = Emphasis.Primary,
            )
        } else {
            BuilderButton(
                onClick = export,
                label = stringResource(Res.string.topbar_export),
                modifier = Modifier.topBarFocus(focus, TopBarControl.Export),
                emphasis = Emphasis.Primary,
                icon = IconId.Export,
            )
        }
        moreButton() // b-509
    }

    if (windowClass == WindowClass.Compact) {
        // b-406
        Column(modifier) {
            // b-512
            CompactTopBar(state.projectName, state.document.seed, Modifier.testTag(TOP_BAR_TAG)) { actions() }
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
                // b-512
                val shownCompact = wideFit.compact
                FittedLibrarySwitcher(
                    selected = selected,
                    modifier = Modifier.weight(1f),
                    switcherModifier = switcherModifier,
                    onSwitch = onSwitch,
                    onRefit = { focus.restoreAfterRefit(TopBarControl.Library) },
                    onFit = { needed, room -> wideFit.refit(needed, room, shownCompact) },
                )
            }
            if (TopBarControl.Commands !in overflowed) {
                CommandsButton(
                    focus = focus,
                    keycap = wideForms, // b-512
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
            if (TopBarControl.History !in overflowed) historyButton() // b-509
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

// b-512

/**
 * The command palette's button, a search glyph, and with [keycap] the palette's key beside it in a
 * keycap. It reads as Command palette either way, with the key in its tooltip.
 */
@Composable
private fun CommandsButton(
    focus: TopBarFocus,
    keycap: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(Res.string.topbar_commands)
    val keys = Shortcut.Palette.text(LocalAppleKeys.current)
    // b-315
    val tooltip = stringResource(Res.string.topbar_commands_tooltip, keys)
    if (!keycap) {
        TopBarIconButton(
            control = TopBarControl.Commands,
            focus = focus,
            icon = IconId.Search,
            description = description,
            tooltip = tooltip,
            onClick = onClick,
        )
        return
    }
    val tokens = LocalBuilderTokens.current
    val ink = tokens.textMuted
    BuilderTooltip(text = tooltip) {
        BuilderPressable(
            onClick = onClick,
            label = description,
            modifier = Modifier.topBarFocus(focus, TopBarControl.Commands),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = tokens.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderIcon(IconId.Search, contentDescription = null, tint = ink)
                BuilderText(
                    text = keys,
                    modifier = Modifier
                        .border(tokens.outlineWidth, tokens.border, RoundedCornerShape(tokens.radius.small))
                        .padding(horizontal = tokens.spacing.extraSmall + tokens.spacing.extraSmall / 2),
                    style = BuilderTextStyle.Value,
                    color = ink,
                    maxLines = 1,
                )
            }
        }
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

// b-509

/**
 * [content] as movable content, so wherever the skin's top bar draws it next it moves there with all
 * it holds rather than starting over. An open History list stays open that way, and on the desktop,
 * where its popover is a window of its own, no window closes while a skin switch measures the page,
 * which crashes the scene. It always draws the latest [content].
 */
@Composable
private fun rememberMovable(content: @Composable () -> Unit): @Composable () -> Unit {
    val latest by rememberUpdatedState(content)
    return remember { movableContentOf { latest() } }
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
            icon = option.icon, // b-512
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

// b-512

/** The glyph an appearance row wears, so its text starts where the other rows' text does. */
private val Appearance.icon: IconId
    get() = when (this) {
        Appearance.System -> IconId.Desktop
        Appearance.Light -> IconId.Sun
        Appearance.Dark -> IconId.Moon
    }
