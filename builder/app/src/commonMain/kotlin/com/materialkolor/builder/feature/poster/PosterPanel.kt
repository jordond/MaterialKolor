package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.image.ImageCandidateRow
import com.materialkolor.builder.feature.workspace.FineTuneSection
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.control.BuilderInsetSheetHost
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.PosterMode
import com.materialkolor.builder.kit.shell.InversePosterSurface
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher

/**
 * What every part of the poster reads, gathered once for the frame.
 *
 * @property[document] The document as stored, so the seed shows as it was typed. Anything that
 * shows what the target makes of it reads [result] instead.
 * @property[result] The theme resolved for the document's target.
 * @property[capabilities] How each control shows up for the document's target, style and spec.
 * @property[preferences] What this browser remembers, the shuffle locks among it.
 * @property[projectName] The open project's name, empty until the session has opened one.
 * @property[saveStatus] Whether the open project's latest changes are saved.
 * @property[openPanel] The panel open over the workspace, which tells the explainer dialog whether
 * it shows.
 * @property[visibleModes] The modes the preview shows, which the contrast readout and the style
 * chips follow.
 * @property[fineTune] The section the Fine-tune sheet is open at, or null while it is shut.
 * @property[sessionDismissedHints] The hints closed in this tab, whether or not storage kept that.
 */
@Immutable
internal data class PosterContext(
    val document: ThemeDocument,
    val result: ThemeResult,
    val capabilities: Capabilities,
    val preferences: Preferences,
    val projectName: String,
    val saveStatus: SaveStatus,
    val openPanel: Panel? = null,
    val visibleModes: PreviewMode = PreviewMode.Split,
    val fineTune: FineTuneSection? = null, // b-521
    val sessionDismissedHints: Set<String> = emptySet(),
)

/**
 * The poster, the seed as the hero with the style and contrast under it and the Fine-tune button at
 * its foot, or the seed strip when [rail] is true.
 *
 * The shell already stands it inside `PosterSurface`, so everything here reads the poster's ink
 * from the surrounding tokens and only the shapes follow the skin.
 *
 * @param[focus] The buttons that open a panel over the workspace, which the panel hands focus back
 * to once it closes.
 */
@Composable
internal fun PosterPanel(
    state: WorkspaceModel.State,
    rail: Boolean,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    focus: PosterFocus? = null,
) {
    val context = rememberPosterContext(state)
    if (rail) {
        PosterRail(context, dispatcher, modifier, focus)
    } else {
        PosterContent(context, dispatcher, focus, modifier)
    }
}

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
        state.panel,
        state.view.mode,
        state.fineTune, // b-521
        state.sessionDismissedHints,
    ) {
        PosterContext(
            document = state.document,
            result = result,
            capabilities = state.capabilities,
            preferences = state.preferences,
            projectName = state.projectName,
            saveStatus = state.saveStatus,
            openPanel = state.panel,
            visibleModes = state.view.mode,
            fineTune = state.fineTune, // b-521
            sessionDismissedHints = state.sessionDismissedHints,
        )
    }
}

/**
 * The poster buttons that open a panel or a dialog over the workspace, which get focus back once it
 * has gone. The workspace holds it, since those sit over the poster rather than in it.
 */
@Stable
internal class PosterFocus {
    /**
     * The Projects button, in the header or on the rail, whichever the poster shows.
     */
    val projects: PanelTrigger = PanelTrigger()

    /**
     * The explainer line's Why button.
     */
    val why: PanelTrigger = PanelTrigger()

    /**
     * The Fine-tune button, which the Fine-tune sheet hands focus back to once it closes.
     */
    val fineTune: PanelTrigger = PanelTrigger()

    /**
     * The hero's Copy hex button, which a refused copy's manual copy dialog hands focus back to.
     */
    val copyHex: PanelTrigger = PanelTrigger()

    /**
     * The hero's Copy Kotlin button, which a refused copy's manual copy dialog hands focus back to.
     */
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

/**
 * Where the share dialog hands focus back once it closes. Opened from the projects drawer's Get a
 * link it goes back to the Projects button that opened the drawer, since the dialog took the
 * drawer's place, and otherwise to [shareButton] in the top bar.
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

/**
 * Whether the share dialog replaced the projects drawer, worked out from the panels seen in turn.
 */
@Stable
private class ShareOpener {
    var fromProjects by mutableStateOf(false)
        private set

    private var last: Panel? = null

    /**
     * Takes in the panel a frame showed. Only the move onto Share changes the answer.
     */
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

    /**
     * How many buttons stand for this trigger, more than one while the rail and the poster swap.
     */
    internal var buttons by mutableIntStateOf(0)

    /**
     * Where the panel hands focus back, or null while no button stands for it.
     */
    val returnFocusTo: FocusRequester?
        get() = requester.takeIf { buttons > 0 }
}

/**
 * The modifier for [trigger]'s button, which counts it as on screen while it is composed.
 */
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
 * The open poster, one column that scrolls. Docked it runs from the header down to the Fine-tune
 * button, with the Fine-tune sheet over it, and in the phone sheet in the order the sheet's detents
 * show it.
 *
 * The docked sheet stands in a host around the scroll area rather than in it, so it keeps still
 * while the column under it scrolls. On a phone the workspace opens it over the whole screen
 * instead, see [FineTuneHost].
 */
@Composable
private fun PosterContent(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    focus: PosterFocus?,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val mode = LocalLayout.current.posterMode
    val sheet = mode == PosterMode.Sheet
    // The sheet's handle already stands above its content, so the sheet starts close under it.
    // b-524 The docked poster starts 16 in, which leaves Fine-tune in view at 900 tall.
    val top = if (sheet) spacing.extraSmall else spacing.large
    // 24 at 400 wide, a little under 28 so Shuffle, Pick and Image share a row with
    // Material's roomy buttons. The narrower poster and the sheet keep 20, so five chips a row fit.
    val side = if (mode == PosterMode.Docked400) spacing.extraLarge else spacing.large + spacing.extraSmall
    // b-524
    BuilderInsetSheetHost(
        sheet = {
            if (!sheet) {
                InversePosterSurface {
                    FineTuneSheet(context, dispatcher, returnFocusTo = focus?.fineTune?.returnFocusTo)
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        val scroll = rememberScrollState()
        val tuning = !sheet && context.fineTune != null
        // The sheet leaves the header and the hex in view above it, so a poster scrolled down to the
        // Fine-tune button goes back to its top as the sheet rises.
        LaunchedEffect(tuning) { if (tuning) scroll.animateScrollTo(0) }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewport = maxHeight
            // The poster always holds controls, so the scroll area needs no tab stop of its own.
            BuilderScrollArea(Modifier.fillMaxSize(), state = scroll, tabStop = false) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // At least as tall as the poster, so the room left over can push the
                        // Fine-tune button down to its foot.
                        .heightIn(min = viewport)
                        .padding(start = side, top = top, end = side, bottom = spacing.extraLarge),
                    // A gap inside a group. Each group adds its own room on top, see PosterGroupGap.
                    // b-524 The gap is tight, so the docked poster reaches down to Fine-tune at 900
                    // tall with the first run hint showing, and the sheet's peek still shows the
                    // contrast levels at rest.
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    if (sheet) {
                        PosterSheet(context, dispatcher, focus)
                    } else {
                        DockedSections(context, dispatcher, focus)
                    }
                }
            }
        }
    }
}

/**
 * The docked poster in board E's order. The header and the hero on top with the explainer's line
 * under the readout, then Shuffle, Pick and Image, the style and the contrast. The room left over
 * sits under the contrast, so the first run hint and the Fine-tune button rest at the poster's foot
 * when it is taller than what it holds, and scroll with the rest when it is not.
 */
@Composable
private fun ColumnScope.DockedSections(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    focus: PosterFocus?,
) {
    // b-524
    val group = Modifier.padding(top = PosterGroupGap)
    PosterHeader(context, dispatcher, focus = focus)
    SeedHero(context, dispatcher, modifier = group, focus = focus)
    PrimaryExplainerLine(context, dispatcher, why = focus?.why)
    SeedActions(context, dispatcher, modifier = Modifier.padding(top = LocalBuilderTokens.current.spacing.extraSmall))
    ImageCandidateRow(context, dispatcher)
    StyleChipsSection(context, dispatcher, modifier = group)
    ContrastSection(context, dispatcher, modifier = group)
    Spacer(Modifier.weight(1f))
    // The hint sits right over the button, its close button's touch target is room enough.
    Column {
        FirstRunHint(context, dispatcher)
        FineTuneButton(context, dispatcher, trigger = focus?.fineTune)
    }
}

/**
 * The room a group adds over the gap inside one, so groups stand about 20 dp apart and the parts of
 * one about 8.
 */
internal val PosterGroupGap: Dp
    @Composable get() = LocalBuilderTokens.current.spacing.medium
