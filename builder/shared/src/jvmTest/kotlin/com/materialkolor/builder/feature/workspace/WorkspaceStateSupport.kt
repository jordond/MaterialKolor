package com.materialkolor.builder.feature.workspace

import com.materialkolor.builder.core.session.HistoryState
import com.materialkolor.builder.core.session.SaveStatus
import com.materialkolor.builder.core.session.SessionState
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.Preferences
import com.materialkolor.builder.domain.persist.ProjectViewState

/**
 * A workspace state for a test that draws the UI without a model. It builds the session's value
 * around [document] and works out the capabilities the way the model does.
 */
internal fun workspaceStateOf(
    document: ThemeDocument = ThemeDocument.Default,
    history: HistoryState = HistoryState(),
    view: ProjectViewState = ProjectViewState(),
    preferences: Preferences = Preferences(),
    projectName: String = "",
    saveStatus: SaveStatus = SaveStatus.Idle,
    projectGeneration: Int = 0,
): WorkspaceModel.State =
    WorkspaceModel.State(
        session = SessionState.initial().copy(
            document = document,
            generation = projectGeneration,
            history = history,
            view = view,
            saveStatus = saveStatus,
        ),
        capabilities = capabilitiesOf(document),
        preferences = preferences,
        projectName = projectName,
    )

/**
 * This state with the preview set up as [view], the way the session hands a view change back.
 */
internal fun WorkspaceModel.State.withView(view: ProjectViewState): WorkspaceModel.State =
    copy(session = session.copy(view = view))
