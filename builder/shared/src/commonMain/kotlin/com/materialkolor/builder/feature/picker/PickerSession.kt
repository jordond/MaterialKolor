package com.materialkolor.builder.feature.picker

import com.materialkolor.builder.domain.capability.Capabilities
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.poster.usable
import com.materialkolor.builder.feature.workspace.WorkspaceAction

/**
 * The color picker's session, from the moment it opens on a target until Done, Cancel or anything
 * else closes it. One session is one undo entry.
 *
 * While it is open every color, from a drag, a key, a typed value or the eyedropper, goes out as a
 * drag, so the preview repaints on each and the history folds them all into one step. Done sends
 * the last color once more as a release, which closes that step. Cancel sends back the value the
 * target stored when the session opened, "no override" and the seed's source included, and the
 * step it closes ends where it began, so nothing is left behind. Was sends the same and leaves the
 * picker open, see [goBack]. Back, another panel and a swap to another target put the value back
 * the same way, while a switch to another project drops the session, since that document is gone.
 *
 * It only works out what to send. The caller dispatches the actions it hands back, in order.
 */
internal class PickerSession {
    private var open: Open? = null

    /**
     * What the open session edits, or null when none is open.
     */
    val target: PickerTarget?
        get() = open?.target

    /**
     * Brings the session in line with [target], the picker's target in the workspace now, or null
     * once it has closed. A session on another target, or on a closed picker, puts its value back.
     * One from another project than [generation] is dropped with no edit. A target that [document]
     * cannot take, a control [capabilities] turns off or an accent it lacks, closes the picker.
     */
    fun sync(
        target: PickerTarget?,
        document: ThemeDocument,
        generation: Int,
        capabilities: Capabilities,
    ): List<WorkspaceAction> {
        if (open?.generation != generation) open = null
        val actions = mutableListOf<WorkspaceAction>()
        var current = document
        open?.let { session ->
            val takes = session.target.takes(current, capabilities)
            if (session.target == target && takes) return actions
            session.restore()?.let { restore ->
                actions += restore
                current = restore.change.apply(current)
            }
            open = null
            if (session.target == target) return actions + WorkspaceAction.ClosePanel
        }
        if (target != null) {
            val opened = if (target.takes(current, capabilities)) Open.of(target, current, generation) else null
            open = opened
            if (opened == null) actions += WorkspaceAction.ClosePanel
        }
        return actions
    }

    /**
     * The drag edit that shows [argb] on the target, or null with no session open. A color off the
     * screen marks the seed as [SeedSource.Eyedropper] when [fromScreen] is true.
     */
    fun pick(
        argb: Argb,
        fromScreen: Boolean = false,
    ): WorkspaceAction.Edit? {
        val session = open ?: return null
        session.last = argb
        session.fromScreen = fromScreen
        return WorkspaceAction.Edit(session.changeTo(argb, fromScreen), EditPhase.Dragging)
    }

    /**
     * Goes back to the value the target stored when the session opened, "no override" and the seed's
     * source included, and keeps the picker open. Null with no session open or nothing sent yet.
     *
     * It lands as the release that closes the session's step, the same edit Cancel sends, so the
     * step ends where it began and leaves no entry. A drag would fold only into a step its change
     * merges with, and a seed that came from an image or a preset merges with none, so going back
     * to one as a drag would leave a second step. The session then stands as it did at open, so
     * Done files nothing, Cancel has nothing to put back, and the next pick starts a new step.
     */
    fun goBack(): WorkspaceAction.Edit? {
        val session = open ?: return null
        val restore = session.restore() ?: return null
        session.last = null
        session.fromScreen = false
        return restore
    }

    /**
     * Ends the session on the color it shows, as one undo entry, and closes the picker.
     */
    fun done(): List<WorkspaceAction> {
        val session = open
        open = null
        val last = session?.last ?: return listOf(WorkspaceAction.ClosePanel)
        val release = WorkspaceAction.Edit(session.changeTo(last, session.fromScreen), EditPhase.Released)
        return listOf(release, WorkspaceAction.ClosePanel)
    }

    /**
     * Puts back the value the target had when the session opened and closes the picker.
     */
    fun cancel(): List<WorkspaceAction> {
        val restore = open?.restore()
        open = null
        return listOfNotNull(restore, WorkspaceAction.ClosePanel)
    }

    /**
     * An open session on [target], with the change that puts back the value stored at open.
     *
     * @property[accent] The accent as it stood at open, for an accent target.
     */
    private class Open(
        val target: PickerTarget,
        val generation: Int,
        val initial: DocumentChange,
        val accent: Accent?,
    ) {
        /**
         * The last color sent, or null while the session has sent nothing.
         */
        var last: Argb? = null

        /**
         * Whether [last] came off the screen.
         */
        var fromScreen: Boolean = false

        fun changeTo(
            argb: Argb,
            fromScreen: Boolean,
        ): DocumentChange =
            when (target) {
                PickerTarget.Seed -> {
                    DocumentChange.SetSeed(argb, if (fromScreen) SeedSource.Eyedropper else SeedSource.Picked)
                }
                is PickerTarget.KeyColorOverride -> {
                    DocumentChange.SetKeyColor(target.slot, argb)
                }
                is PickerTarget.Pin -> {
                    DocumentChange.SetPin(target.role, target.mode, argb)
                }
                is PickerTarget.Accent -> {
                    DocumentChange.UpdateAccent(target.index, accentOf(accent).copy(seed = argb))
                }
                PickerTarget.CmfSeed -> {
                    DocumentChange.SetCmfSeed(argb)
                }
            }

        /**
         * The release that puts the stored value back, or null when nothing moved it.
         */
        fun restore(): WorkspaceAction.Edit? = last?.let { WorkspaceAction.Edit(initial, EditPhase.Released) }

        companion object {
            fun of(
                target: PickerTarget,
                document: ThemeDocument,
                generation: Int,
            ): Open {
                val stored = target.storedIn(document)
                val accent = (target as? PickerTarget.Accent)?.let { accentTarget ->
                    document.accents.getOrNull(accentTarget.index)
                }
                val initial = when (target) {
                    PickerTarget.Seed -> DocumentChange.SetSeed(document.seed, document.seedSource)
                    is PickerTarget.KeyColorOverride -> DocumentChange.SetKeyColor(target.slot, stored)
                    is PickerTarget.Pin -> DocumentChange.SetPin(target.role, target.mode, stored)
                    is PickerTarget.Accent -> DocumentChange.UpdateAccent(target.index, accentOf(accent))
                    PickerTarget.CmfSeed -> DocumentChange.SetCmfSeed(stored)
                }
                return Open(target, generation, initial, accent)
            }
        }
    }
}

/**
 * The accent an accent session holds, which it only opens on once the document has it.
 */
private fun accentOf(accent: Accent?): Accent =
    requireNotNull(accent) { "An accent session opens only on an accent the document has" }

/**
 * Whether [document] has a place for [this] and [capabilities] let it take input.
 */
private fun PickerTarget.takes(
    document: ThemeDocument,
    capabilities: Capabilities,
): Boolean {
    if (this is PickerTarget.Accent && index !in document.accents.indices) return false
    return capabilities[control].usable
}
