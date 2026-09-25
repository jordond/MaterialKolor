package com.materialkolor.builder.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.projects_empty
import com.materialkolor.builder.generated.resources.projects_new
import com.materialkolor.builder.generated.resources.projects_new_copy
import com.materialkolor.builder.generated.resources.projects_search
import com.materialkolor.builder.generated.resources.projects_search_empty
import com.materialkolor.builder.generated.resources.projects_title
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSidePanel
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource

/**
 * The projects drawer (F-30), a side panel over the poster at Medium and Expanded and the whole
 * screen at Compact.
 *
 * It lists every saved project newest first, with New and Copy this one above them and a search once
 * there are more than [SEARCH_THRESHOLD] that narrows the list as it is typed. Without storage a banner says nothing will be kept and
 * offers the share link instead.
 *
 * @param[visible] Whether the drawer is open.
 * @param[state] The projects and the open one.
 * @param[now] The time now, in milliseconds since the epoch, for each project's age.
 * @param[onAction] Called with what the drawer asks for.
 * @param[onGetLink] Called when the storage banner asks for a share link.
 * @param[onDismissRequest] Called when the drawer asks to close.
 * @param[returnFocusTo] The button that opened the drawer, which gets focus back once it closes.
 */
@Composable
internal fun ProjectsDrawer(
    visible: Boolean,
    state: ProjectsModel.State,
    now: Long,
    onAction: (ProjectsAction) -> Unit,
    onGetLink: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null, // b-221f
) {
    val spacing = LocalBuilderTokens.current.spacing
    var renaming by remember { mutableStateOf<String?>(null) }
    // The field shows what was last committed. The query follows every keystroke, and feeding it
    // back in as the value would reset the caret and any composition on each one.
    var searched by remember { mutableStateOf(state.query) }
    BuilderSidePanel(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.projects_title),
        modifier = modifier,
        returnFocusTo = returnFocusTo, // b-221f
    ) {
        // b-511
        // The panel keeps its own room from its edges, so the buttons and the rows line up with the title.
        Column(
            modifier = Modifier.padding(bottom = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            if (!state.storageAvailable) StorageUnavailableBanner(onGetLink)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                BuilderButton(
                    onClick = { onAction(ProjectsAction.New(copyCurrent = false)) },
                    label = stringResource(Res.string.projects_new),
                    emphasis = Emphasis.Primary,
                    icon = IconId.Plus,
                )
                BuilderButton(
                    onClick = { onAction(ProjectsAction.New(copyCurrent = true)) },
                    label = stringResource(Res.string.projects_new_copy),
                    icon = IconId.Copy,
                )
            }
            if (state.searchable) {
                BuilderTextField(
                    value = searched,
                    onCommit = { query ->
                        searched = query
                        onAction(ProjectsAction.Search(query))
                    },
                    label = stringResource(Res.string.projects_search),
                    modifier = Modifier.fillMaxWidth(),
                    onDraftChange = { query -> onAction(ProjectsAction.Search(query)) },
                )
            }
        }
        BuilderScrollArea(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.selectableGroup().padding(vertical = spacing.small), // b-511
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                val shown = state.shown
                if (shown.isEmpty()) {
                    val empty = when {
                        state.projects.isEmpty() -> Res.string.projects_empty
                        else -> Res.string.projects_search_empty
                    }
                    BuilderText(
                        text = stringResource(empty),
                        emphasis = Emphasis.Subtle,
                    )
                }
                shown.forEach { meta ->
                    ProjectRow(
                        meta = meta,
                        open = meta.id == state.openId,
                        now = now,
                        renaming = renaming == meta.id,
                        onRenamingChange = { editing ->
                            // A row that lets go only closes its own field, never one opened since.
                            renaming = if (editing) meta.id else renaming.takeUnless { id -> id == meta.id }
                        },
                        onAction = onAction,
                    )
                }
            }
        }
    }
}
