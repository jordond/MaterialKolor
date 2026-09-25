package com.materialkolor.builder.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.feature.poster.InfoTopic
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.about_close
import com.materialkolor.builder.generated.resources.about_help_pages
import com.materialkolor.builder.generated.resources.about_help_title
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * How much of the window a dialog under About may take before its body scrolls.
 */
internal const val ABOUT_HEIGHT_FRACTION = 0.6f

/**
 * Help, open while `state.panel` is [Panel.Help] (F-36). It gathers every info button's question
 * and answer from the poster in one list, so they can be read without hunting for the buttons.
 *
 * It offers the help pages only once [HELP_PAGES_URL] names them.
 *
 * @param[returnFocusTo] The overflow button that opened it, which gets focus back once it closes
 * (AR-09).
 */
@Composable
internal fun HelpHost(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val layout = LocalLayout.current
    // Taken out here, since a dialog on the desktop provides its own and would skip a caller's.
    val uriHandler = LocalUriHandler.current
    val close = { dispatcher.dispatch(WorkspaceAction.ClosePanel) }
    BuilderDialog(
        visible = state.panel == Panel.Help,
        onDismissRequest = close,
        title = stringResource(Res.string.about_help_title),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        actions = {
            HELP_PAGES_URL?.let { url ->
                BuilderButton(
                    onClick = { uriHandler.openUri(url) },
                    label = stringResource(Res.string.about_help_pages),
                    icon = IconId.ExternalLink,
                )
            }
            BuilderButton(onClick = close, label = stringResource(Res.string.about_close), emphasis = Emphasis.Primary)
        },
    ) {
        BuilderScrollArea(Modifier.heightIn(max = layout.heightDp * ABOUT_HEIGHT_FRACTION)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
                InfoTopic.entries.forEach { topic ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                        BuilderText(
                            text = stringResource(topic.question),
                            modifier = Modifier.semantics { heading() },
                            style = BuilderTextStyle.SectionLabel,
                        )
                        BuilderText(text = stringResource(topic.explanation), emphasis = Emphasis.Secondary)
                    }
                }
            }
        }
    }
}
