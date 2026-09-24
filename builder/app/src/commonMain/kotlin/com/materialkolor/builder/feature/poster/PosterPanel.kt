package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.image.ImageCandidateRow
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher

/**
 * What every part of the poster reads, gathered once for the frame.
 *
 * @property[document] The document as stored, so the seed shows as it was typed. Anything that
 * shows what the target makes of it reads [result] instead (D35).
 * @property[result] The theme resolved for the document's target.
 * @property[capabilities] How each control shows up for the document's target, style and spec.
 * @property[preferences] What this browser remembers, the shuffle locks among it.
 * @property[projectName] The open project's name, empty until the session has opened one.
 * @property[saveStatus] Whether the open project's latest changes are saved.
 */
@Immutable
internal data class PosterContext(
    val document: ThemeDocument,
    val result: ThemeResult,
    val capabilities: Capabilities,
    val preferences: Preferences,
    val projectName: String,
    val saveStatus: SaveStatus,
)

/**
 * The poster, the seed as the hero with the style, contrast and fine tune rows under it, or the
 * seed strip when [rail] is true (F-66).
 *
 * The shell already stands it inside `PosterSurface`, so everything here reads the poster's ink
 * from the surrounding tokens and only the shapes follow the skin.
 */
@Composable
internal fun PosterPanel(
    state: WorkspaceModel.State,
    rail: Boolean,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val result = LocalThemeResult.current
    val context = remember(
        state.document,
        result,
        state.capabilities,
        state.preferences,
        state.projectName,
        state.saveStatus,
    ) {
        PosterContext(
            document = state.document,
            result = result,
            capabilities = state.capabilities,
            preferences = state.preferences,
            projectName = state.projectName,
            saveStatus = state.saveStatus,
        )
    }
    if (rail) {
        PosterRail(context, dispatcher, modifier)
    } else {
        PosterContent(context, dispatcher, modifier)
    }
}

/** The open poster, one column that scrolls, in the order F-66 lists. */
@Composable
private fun PosterContent(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    BuilderScrollArea(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.extraLarge),
        ) {
            PosterHeader(context, dispatcher)
            SeedHero(context, dispatcher)
            SeedActions(context, dispatcher)
            if (context.document.seedSource is SeedSource.Image) {
                ImageCandidateRow(context, dispatcher)
            }
            PrimaryExplainerLine(context, dispatcher)
            StyleChipsSection(context, dispatcher)
            ContrastSection(context, dispatcher)
            CoreColorsRow(context, dispatcher)
            SpecExtrasRow(context, dispatcher)
        }
    }
}
