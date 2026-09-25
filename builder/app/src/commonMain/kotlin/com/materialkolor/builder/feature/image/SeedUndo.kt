package com.materialkolor.builder.feature.image

import com.materialkolor.builder.domain.model.ThemeDocument

/**
 * The Undo on the toast of one image seed, which only ever undoes that seed.
 *
 * The toast stays up for ten seconds, and in that time the document can move on through another
 * edit, a newer image or a project switch. An Undo then would step back over something else, so
 * it only holds while the document is still the one the seed made, in the project it made it in.
 * The toast is taken back as soon as the document is neither the one the seed started from nor the
 * one it made, even when no frame ever showed the seed landing.
 *
 * @property[before] The document the seed starts from.
 * @property[made] The document the seed makes.
 * @property[project] The generation of the project it lands in.
 */
internal class SeedUndo(
    private val before: ThemeDocument, // b-311c
    private val made: ThemeDocument,
    private val project: Int,
) {
    private var over = false
    private var withdraw: (() -> Unit)? = null

    /**
     * The toast is up, and [withdraw] takes it back. One that comes up after the end goes at once.
     */
    fun shown(withdraw: () -> Unit) {
        if (over) withdraw() else this.withdraw = withdraw
    }

    /**
     * Whether an Undo on [document] in [project] would still undo this seed and nothing else.
     */
    fun holds(
        document: ThemeDocument,
        project: Int,
    ): Boolean = !over && document == made && project == this.project

    /**
     * Keeps up with the workspace, and ends once [document] is neither the one the seed started
     * from nor the one it made, so a later pass back through the seed's document never brings the
     * Undo back.
     */
    fun follow(
        document: ThemeDocument,
        project: Int,
    ) {
        when {
            over -> Unit
            project != this.project -> end()
            document != before && document != made -> end() // b-311c
        }
    }

    /**
     * Takes the toast back and lets the Undo go, for good.
     */
    fun end() {
        over = true
        withdraw?.invoke()
        withdraw = null
    }
}
