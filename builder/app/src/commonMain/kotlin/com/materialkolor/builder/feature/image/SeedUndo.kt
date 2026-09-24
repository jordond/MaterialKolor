package com.materialkolor.builder.feature.image

import com.materialkolor.builder.domain.model.ThemeDocument

/**
 * The Undo on the toast of one image seed, which only ever undoes that seed.
 *
 * The toast stays up for ten seconds, and in that time the document can move on through another
 * edit, a newer image or a project switch. An Undo then would step back over something else, so
 * it only holds while the document is still the one the seed made, in the project it made it in.
 * Once the seed has landed and the document moves on, the toast is taken back.
 *
 * @property[made] The document the seed makes.
 * @property[project] The generation of the project it lands in.
 */
internal class SeedUndo(
    private val made: ThemeDocument,
    private val project: Int,
) {
    private var landed = false
    private var over = false
    private var withdraw: (() -> Unit)? = null

    /** The toast is up, and [withdraw] takes it back. One that comes up after the end goes at once. */
    fun shown(withdraw: () -> Unit) {
        if (over) withdraw() else this.withdraw = withdraw
    }

    /** Whether an Undo on [document] in [project] would still undo this seed and nothing else. */
    fun holds(
        document: ThemeDocument,
        project: Int,
    ): Boolean = !over && document == made && project == this.project

    /** Keeps up with the workspace, and ends once the seed has landed and [document] moved on. */
    fun follow(
        document: ThemeDocument,
        project: Int,
    ) {
        when {
            over -> Unit
            project != this.project -> end()
            document == made -> landed = true
            landed -> end()
        }
    }

    /** Takes the toast back and lets the Undo go, for good. */
    fun end() {
        over = true
        withdraw?.invoke()
        withdraw = null
    }
}
