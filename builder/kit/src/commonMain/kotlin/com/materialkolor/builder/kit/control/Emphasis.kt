package com.materialkolor.builder.kit.control

import androidx.compose.ui.graphics.Color
import com.materialkolor.builder.kit.token.BuilderTokens

/**
 * How loudly a control, a label or an icon speaks.
 *
 * Every control takes one, so a row of buttons or a toolbar can be ranked the same way in every
 * skin. Each skin decides what a rank looks like for its own components.
 */
public enum class Emphasis {
    /** The one thing on screen the user most likely wants next. */
    Primary,

    /** A companion to the primary action, or ordinary content. */
    Secondary,

    /** Something that should stay out of the way until it is needed. */
    Subtle,

    /** Something that destroys or discards. */
    Danger,
}

/**
 * The ink text and icons of this rank use on a panel.
 *
 * Subtle ink is the same muted ink as Secondary because builder text keeps to WCAG AA. A subtle
 * control is told apart by its container, not by fading its label.
 */
internal fun Emphasis.ink(tokens: BuilderTokens): Color =
    when (this) {
        Emphasis.Primary -> tokens.textStrong
        Emphasis.Secondary -> tokens.textMuted
        Emphasis.Subtle -> tokens.textMuted
        Emphasis.Danger -> tokens.danger
    }
