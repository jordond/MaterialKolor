package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.materialkolor.builder.LocalThemeResolver
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.history.History
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.workspace.FineTuneSection
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.shell.PosterSurface
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import dev.stateholder.dispatcher.Dispatcher
import dev.stateholder.dispatcher.rememberDispatcher

/**
 * Stands in for the workspace behind one poster section. Edits land on [document] and in a real
 * [History] straight away, each far enough from the last that nothing merges by time, so a test can
 * count the undo entries an action leaves.
 */
internal class PosterHarness(
    document: ThemeDocument,
    openPanel: Panel? = null,
) {
    val actions = mutableListOf<WorkspaceAction>()
    val resolver = ThemeResolver()
    var document by mutableStateOf(document)
    var openPanel by mutableStateOf(openPanel)
    var fineTune by mutableStateOf<FineTuneSection?>(null)

    var preferences by mutableStateOf(Preferences())

    var projectName by mutableStateOf("")
    private val history = History()
    private var now = 0L

    fun dispatch(action: WorkspaceAction) {
        actions += action
        when (action) {
            is WorkspaceAction.Edit -> edit(action.change, action.phase)
            is WorkspaceAction.EditWithReveal -> edit(action.change, EditPhase.Discrete)
            is WorkspaceAction.OpenPanel -> openPanel = action.panel
            WorkspaceAction.ClosePanel -> openPanel = null
            is WorkspaceAction.OpenFineTune -> fineTune = action.section ?: FineTuneSection.Locks
            WorkspaceAction.CloseFineTune -> fineTune = null
            is WorkspaceAction.SetColorAnimation -> setColorAnimation(action.target, action.on)
            is WorkspaceAction.SetColorAnimationDuration -> setColorAnimationDuration(action.target, action.durationMs)
            is WorkspaceAction.DismissHint -> dismissHint(action.id)
            else -> Unit
        }
    }

    /**
     * Turns color animation on or off in the export options of [target] alone, as the workspace does.
     */
    private fun setColorAnimation(
        target: ExportTarget,
        on: Boolean,
    ) {
        updateExportPrefs(target) { prefs -> prefs.copy(animate = on) }
    }

    /**
     * Sets how long the color animation of [target] runs, as the workspace does.
     */
    private fun setColorAnimationDuration(
        target: ExportTarget,
        durationMs: Int,
    ) {
        updateExportPrefs(target) { prefs -> prefs.copy(animationDurationMs = durationMs) }
    }

    /**
     * Keeps what [block] makes of the export options of [target], the way the preferences repository does.
     */
    private fun updateExportPrefs(
        target: ExportTarget,
        block: (ExportPrefs) -> ExportPrefs,
    ) {
        preferences = preferences.withExportPrefs(target, block(preferences.exportPrefsFor(target)))
    }

    /**
     * Keeps [id] closed, the way the preferences repository does.
     */
    private fun dismissHint(id: String) {
        preferences = preferences.copy(dismissedHints = preferences.dismissedHints + id)
    }

    /**
     * How many steps undo walks back before the history runs out.
     */
    fun undoEntries(): Int = generateSequence { history.undo() }.count()

    private fun edit(
        change: DocumentChange,
        phase: EditPhase,
    ) {
        val before = document
        val after = change.apply(before)
        now += MERGE_GAP
        history.record(before, after, change, phase, now)
        document = after
    }

    private companion object {
        /**
         * Longer than any merge window, so only the phase decides what folds.
         */
        const val MERGE_GAP = 60_000L
    }
}

/**
 * Shows [section] on the poster surface, the way the poster panel stands it, fed by [harness].
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.showSection(
    harness: PosterHarness,
    section: @Composable (context: PosterContext, dispatcher: Dispatcher<WorkspaceAction>) -> Unit,
) {
    setContent {
        val dispatcher = rememberDispatcher<WorkspaceAction> { action -> harness.dispatch(action) }
        val document = harness.document
        val result = remember(document) { harness.resolver.resolve(document) }
        val context = PosterContext(
            document = document,
            result = result,
            capabilities = capabilitiesOf(document),
            preferences = harness.preferences,
            projectName = harness.projectName,
            saveStatus = SaveStatus.Idle,
            openPanel = harness.openPanel,
            visibleModes = PreviewMode.Split,
            fineTune = harness.fineTune,
        )
        BuilderTheme(
            skin = Skin(library = SkinLibrary.Material3, expressive = false),
            result = result,
            isDark = false,
            reducedMotion = true,
        ) {
            ProvideBuilderLayout(coarsePointer = false, modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalThemeResult provides result,
                    LocalThemeResolver provides harness.resolver,
                ) {
                    PosterSurface(result.poster) {
                        Column { section(context, dispatcher) }
                    }
                }
            }
        }
    }
}
