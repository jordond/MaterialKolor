package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.domain.capability.Capabilities
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
 * @property[openPanel] The panel open over the workspace, which tells the explainer dialog whether
 * it shows.
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
 *
 * @param[focus] The buttons that open a panel over the workspace, which the panel hands focus back
 * to once it closes (AR-09).
 */
@Composable
internal fun PosterPanel(
    state: WorkspaceModel.State,
    rail: Boolean,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    focus: PosterFocus? = null, // b-221f
) {
    val context = rememberPosterContext(state) // b-221f
    if (rail) {
        PosterRail(context, dispatcher, modifier, focus)
    } else {
        PosterContent(context, dispatcher, focus, modifier)
    }
}

// b-221f

/**
 * What the poster reads from [state] this frame, with the theme resolved at the root. The poster
 * and the panels it opens over the workspace, such as the explainer, gather it the same way.
 */
@Composable
internal fun rememberPosterContext(state: WorkspaceModel.State): PosterContext {
    val result = LocalThemeResult.current
    return remember(
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
}

/**
 * The poster buttons that open a panel or a dialog over the workspace, which get focus back once it
 * has gone (AR-09). The workspace holds it, since those sit over the poster rather than in it.
 */
@Stable
internal class PosterFocus {
    /** The Projects button, in the header or on the rail, whichever the poster shows. */
    val projects: PanelTrigger = PanelTrigger()

    /** The explainer line's Why button. */
    val why: PanelTrigger = PanelTrigger()

    // b-306c

    /** The hero's Copy hex button, which a refused copy's manual copy dialog hands focus back to. */
    val copyHex: PanelTrigger = PanelTrigger()

    /** The hero's Copy Kotlin button, which a refused copy's manual copy dialog hands focus back to. */
    val copyKotlin: PanelTrigger = PanelTrigger()

    /**
     * Where a dialog that [opener] opened hands focus back. A copy button's requester only counts
     * while the hero shows it, so a poster that turned into the rail meanwhile gets no request.
     * Anything else comes back as it is.
     */
    fun returnFocusFor(opener: FocusRequester?): FocusRequester? =
        when (opener) {
            copyHex.requester -> copyHex.returnFocusTo
            copyKotlin.requester -> copyKotlin.returnFocusTo
            else -> opener
        }
}

// b-306c

/**
 * Where the share dialog hands focus back once it closes (AR-09). Opened from the projects
 * drawer's Get a link it goes back to the Projects button that opened the drawer, since the dialog
 * took the drawer's place, and otherwise to [shareButton] in the top bar.
 *
 * @param[panel] The panel open now, whose changes say what the dialog replaced.
 */
@Composable
internal fun PosterFocus.shareReturn(
    panel: Panel?,
    shareButton: FocusRequester?,
): FocusRequester? {
    val opener = remember(this) { ShareOpener() }
    SideEffect { opener.saw(panel) }
    return if (opener.fromProjects) projects.returnFocusTo else shareButton
}

/** Whether the share dialog replaced the projects drawer, worked out from the panels seen in turn. */
@Stable
private class ShareOpener {
    var fromProjects by mutableStateOf(false)
        private set

    private var last: Panel? = null

    /** Takes in the panel a frame showed. Only the move onto Share changes the answer. */
    fun saw(panel: Panel?) {
        if (panel == Panel.Share && last != Panel.Share) fromProjects = last == Panel.Projects
        last = panel
    }
}

/**
 * A button that opens a panel or a dialog. It offers no focus while no button stands for it, so one
 * opened without it, over a collapsed poster or from the command palette, focuses nothing.
 */
@Stable
internal class PanelTrigger {
    internal val requester = FocusRequester()

    /** How many buttons stand for this trigger, more than one while the rail and the poster swap. */
    internal var buttons by mutableIntStateOf(0)

    /** Where the panel hands focus back, or null while no button stands for it. */
    val returnFocusTo: FocusRequester?
        get() = requester.takeIf { buttons > 0 }
}

/** The modifier for [trigger]'s button, which counts it as on screen while it is composed. */
@Composable
internal fun triggerFocus(trigger: PanelTrigger?): Modifier {
    if (trigger == null) return Modifier
    DisposableEffect(trigger) {
        trigger.buttons++
        onDispose { trigger.buttons-- }
    }
    return Modifier.focusRequester(trigger.requester)
}

/**
 * The open poster, one column that scrolls. Docked it runs in the order F-66 lists, and in the
 * phone sheet in the order the sheet's detents show it.
 */
@Composable
private fun PosterContent(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    focus: PosterFocus?,
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
                SheetSections(context, dispatcher, focus)
            } else {
                DockedSections(context, dispatcher, focus)
            }
        }
    }
}

/** The docked poster, the header and the hero on top as F-66 lists it. */
@Composable
private fun ColumnScope.DockedSections(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    focus: PosterFocus?,
) {
    PosterHeader(context, dispatcher, focus = focus)
    SeedHero(context, dispatcher, focus = focus) // b-306c
    SeedActions(context, dispatcher)
    FirstRunHint(context, dispatcher) // b-314
    ImageCandidateRow(context, dispatcher) // b-311
    PrimaryExplainerLine(context, dispatcher, why = focus?.why)
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
    focus: PosterFocus?,
) {
    SeedPeekRow(context, dispatcher)
    SeedActions(context, dispatcher, shuffle = false)
    FirstRunHint(context, dispatcher) // b-314
    ImageCandidateRow(context, dispatcher) // b-311
    StyleChipsSection(context, dispatcher)
    ContrastSection(context, dispatcher)
    PrimaryExplainerLine(context, dispatcher, why = focus?.why)
    CoreColorsRow(context, dispatcher)
    SpecExtrasRow(context, dispatcher)
    SeedHero(context, dispatcher, focus = focus) // b-306c
    PosterHeader(context, dispatcher, focus = focus)
}
