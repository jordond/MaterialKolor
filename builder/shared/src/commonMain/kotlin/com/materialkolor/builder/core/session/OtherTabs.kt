package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.data.ProjectRepository
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ProjectRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What [ProjectSession] does when another tab saves the open project.
 *
 * The other tab's document comes in as an undo step, so Undo brings this tab's back. If this tab
 * edited in the last [CONFLICT_WINDOW_MILLIS] a [Conflict] comes up instead, and this tab's saves
 * wait until [resolve] settles it. A newer save from the other tab while the conflict is up takes
 * the conflict's place and is never taken in silently. The rules are in [incomingSave].
 *
 * A conflict never loses anyone's work. When this tab has to save with a conflict up, on a flush or
 * before it opens another project, it keeps its own document. The other tab gets that save as an
 * ordinary change from another tab, a conflict there if it edited lately or an undo step otherwise,
 * so its work can still be brought back.
 *
 * Changes from other tabs are applied on the thread that opened the project, the UI thread, since
 * they resolve themes and move the undo steps.
 *
 * @param[tabId] This tab, so its own saves coming back are left alone.
 * @param[scope] The app scope, where other tabs' saves are watched.
 * @param[now] The time in epoch milliseconds, for the conflict window.
 * @param[state] The session's one state, read for the document showing and the conflict.
 * @param[setConflict] Puts up a conflict, or clears it with null, in the session's state.
 * @param[showing] The project the session shows now.
 * @param[showTheirs] Shows another tab's document as the newest step and clears the conflict in the
 * same write.
 * @param[commit] Hands a document to autosave.
 */
internal class OtherTabs(
    private val projects: ProjectRepository,
    private val tabId: String,
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val tracker: EditTracker,
    private val state: StateFlow<SessionState>,
    private val setConflict: (Conflict?) -> Unit,
    private val showing: () -> OpenProject,
    private val showTheirs: (ThemeDocument) -> Unit,
    private val commit: (ThemeDocument) -> Unit,
) {
    /**
     * Follow other tabs' saves to [open], the project [id], until it stops showing.
     */
    fun watch(
        open: OpenProject,
        id: String,
    ) {
        scope.launch(open.job + open.context) {
            projects.changes(id).collect { incoming -> onSavedElsewhere(open, incoming) }
        }
    }

    /**
     * Settle the conflict, keeping this tab's document when [keepMine] is true or taking the other
     * tab's as an undo step when it is false. Nothing happens with no conflict up.
     */
    fun resolve(keepMine: Boolean) {
        val conflict = state.value.conflict ?: return
        val open = showing()
        if (keepMine) {
            setConflict(null)
            val mine = state.value.document
            open.facts.update { facts -> facts.copy(held = conflict.theirs) }
            commit(mine)
        } else {
            // Adopting clears the conflict in the same write as the document it brings.
            adopt(open, conflict.theirs)
        }
    }

    private fun onSavedElsewhere(
        open: OpenProject,
        incoming: ProjectRecord,
    ) {
        if (showing() !== open) return
        val shown = state.value
        val outcome = incomingSave(
            held = open.facts.value.held,
            incoming = incoming,
            tabId = tabId,
            document = shown.document,
            conflicted = shown.conflict != null,
            lastEditAt = tracker.lastEditAt,
            now = now(),
        )
        when (outcome) {
            IncomingSave.Ignore -> {}
            IncomingSave.Matches -> {
                open.facts.update { facts -> facts.copy(name = incoming.name, held = incoming) }
                setConflict(null)
            }
            IncomingSave.RaiseConflict -> {
                open.facts.update { facts -> facts.copy(name = incoming.name) }
                setConflict(Conflict(incoming))
            }
            IncomingSave.Adopt -> {
                adopt(open, incoming)
            }
        }
    }

    /**
     * Take the other tab's [theirs] as an undo step, so Undo brings this tab's document back. It only
     * runs with no conflict up or while settling one, so the conflict goes away with the document.
     */
    private fun adopt(
        open: OpenProject,
        theirs: ProjectRecord,
    ) {
        val mine = state.value.document
        open.facts.update { facts -> facts.copy(name = theirs.name, held = theirs) }
        tracker.takeTheirs(mine, theirs.document)
        showTheirs(theirs.document)
        commit(theirs.document)
    }
}
