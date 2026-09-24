package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.feature.about.GITHUB_URL
import com.materialkolor.builder.feature.poster.switcherPulse
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.feature.workspace.skinOf
import com.materialkolor.builder.generated.resources.Res
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
 * On a wide window it holds the segmented switcher, the command palette, undo, redo, Share, Export
 * code and the overflow menu. Narrower windows get the switcher as a dropdown, and phones move the
 * command palette, undo and redo into the overflow.
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
    val compact = LocalLayout.current.windowClass == WindowClass.Compact
    val items = overflowItems(state, dispatcher, compact, LocalUriHandler.current)

    TopBarRegion(modifier) {
        LibrarySwitcher(
            document = state.document,
            modifier = Modifier.topBarFocus(focus, TopBarControl.Library).switcherPulse(state, dispatcher), // b-314
            onSwitch = { choice, origin ->
                dispatcher.dispatch(WorkspaceAction.EditWithReveal(choice.change, origin))
            },
        )
        Spacer(Modifier.weight(1f))
        if (!compact) {
            TopBarIconButton(
                control = TopBarControl.Commands,
                focus = focus,
                icon = IconId.Command,
                description = stringResource(Res.string.topbar_commands),
                tooltip = stringResource(Res.string.topbar_commands_tooltip),
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Palette)) },
            )
            TopBarIconButton(
                control = TopBarControl.Undo,
                focus = focus,
                icon = IconId.Undo,
                description = undoText(state.history, state.document),
                enabled = state.history.canUndo,
                onClick = { dispatcher.dispatch(WorkspaceAction.Undo) },
            )
            TopBarIconButton(
                control = TopBarControl.Redo,
                focus = focus,
                icon = IconId.Redo,
                description = redoText(state.history, state.document),
                enabled = state.history.canRedo,
                onClick = { dispatcher.dispatch(WorkspaceAction.Redo) },
            )
        }
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

/**
 * The overflow menu, the chrome's appearance, Help, the shortcuts, About and GitHub. On a phone the
 * command palette, undo and redo come first.
 */
@Composable
private fun overflowItems(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    compact: Boolean,
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
    val phoneItems = if (compact) {
        listOf(
            BuilderMenuItem(
                label = stringResource(Res.string.topbar_commands),
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Palette)) },
                icon = IconId.Command,
            ),
            BuilderMenuItem(
                label = undoText(state.history, state.document),
                onClick = { dispatcher.dispatch(WorkspaceAction.Undo) },
                icon = IconId.Undo,
                enabled = state.history.canUndo,
            ),
            BuilderMenuItem(
                label = redoText(state.history, state.document),
                onClick = { dispatcher.dispatch(WorkspaceAction.Redo) },
                icon = IconId.Redo,
                enabled = state.history.canRedo,
            ),
        )
    } else {
        emptyList()
    }
    return phoneItems +
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
