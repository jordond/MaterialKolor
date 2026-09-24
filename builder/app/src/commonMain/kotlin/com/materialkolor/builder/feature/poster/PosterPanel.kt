package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.materialkolor.builder.domain.persist.FineTuneRow
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.image.ImageCandidateRow
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
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
 * @property[openPanel] The panel open over the workspace, which tells the explainer line whether
 * its panel is showing.
 * @property[visibleModes] The modes the preview shows, which the contrast readout and the style
 * chips follow.
 * @property[openFineTuneRows] The fine tune rows open in this project.
 */
@Immutable
internal data class PosterContext(
    val document: ThemeDocument,
    val result: ThemeResult,
    val capabilities: Capabilities,
    val preferences: Preferences,
    val projectName: String,
    val saveStatus: SaveStatus,
    // b-304
    val openPanel: Panel? = null,
    val visibleModes: PreviewMode = PreviewMode.Split,
    // b-305
    val openFineTuneRows: Set<FineTuneRow> = emptySet(),
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
        // b-304
        state.panel,
        state.view.mode,
        // b-305
        state.view.openFineTuneRows,
    ) {
        PosterContext(
            document = state.document,
            result = result,
            capabilities = state.capabilities,
            preferences = state.preferences,
            projectName = state.projectName,
            saveStatus = state.saveStatus,
            // b-304
            openPanel = state.panel,
            visibleModes = state.view.mode,
            // b-305
            openFineTuneRows = state.view.openFineTuneRows,
        )
    }
    if (rail) {
        PosterRail(context, dispatcher, modifier)
    } else {
        PosterContent(context, dispatcher, modifier)
    }
}

/**
 * The open poster, one column that scrolls. Docked it runs in the order F-66 lists, and in the
 * phone sheet in the order the sheet's detents show it.
 */
@Composable
private fun PosterContent(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val sheet = LocalLayout.current.posterMode == PosterMode.Sheet
    // The sheet's handle already stands above its content, so the sheet starts close under it.
    val top = if (sheet) spacing.extraSmall else spacing.extraLarge
    // b-305
    // The poster always holds controls, so the scroll area needs no tab stop of its own.
    BuilderScrollArea(modifier.fillMaxSize(), tabStop = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.extraLarge, top = top, end = spacing.extraLarge, bottom = spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.extraLarge),
        ) {
            if (sheet) {
                SheetSections(context, dispatcher)
            } else {
                DockedSections(context, dispatcher)
            }
        }
    }
}

/** The docked poster, the header and the hero on top as F-66 lists it. */
@Composable
private fun ColumnScope.DockedSections(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
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

/**
 * The poster in the phone sheet, in the order its detents show it (D38). The peek leads with the
 * seed row and Shuffle, which is all a phone on its side sees, and upright it goes on to Pick,
 * Image, the style and the contrast. Half adds the explainer and the fine tune rows, and full ends
 * with the hero for editing the hex and the header.
 */
@Composable
private fun ColumnScope.SheetSections(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    SeedPeekRow(context, dispatcher)
    SeedActions(context, dispatcher, shuffle = false)
    if (context.document.seedSource is SeedSource.Image) {
        ImageCandidateRow(context, dispatcher)
    }
    StyleChipsSection(context, dispatcher)
    ContrastSection(context, dispatcher)
    PrimaryExplainerLine(context, dispatcher)
    CoreColorsRow(context, dispatcher)
    SpecExtrasRow(context, dispatcher)
    SeedHero(context, dispatcher)
    PosterHeader(context, dispatcher)
}
