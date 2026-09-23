package com.materialkolor.builder.domain.edit

/**
 * Where an edit is in its gesture, which is what decides how the history files it.
 */
public enum class EditPhase {
    /** A single edit that is finished the moment it happens, a click, a keystroke, an arrow nudge. */
    Discrete,

    /** One step of a drag that is still going, a slider or a picker being held. */
    Dragging,

    /** The drag let go, and this is where it ended up. */
    Released,
}
