package com.materialkolor.builder.preview.unstyled

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics

// A control's name and state go through the kit's public fold modifiers, `foldedExpandedName`,
// `foldedTabName`, `foldedOptionName` and `foldedSelectedName`, which carry the state onto the web
// (D37). What they leave to the app is here.

/**
 * Lets assistive tech open or close the panel a button shows with [onToggle], whichever way
 * [expanded] stands. Pair it with `foldedExpandedName`, which names the button and says whether the
 * panel is open.
 */
internal fun Modifier.expandActions(
    expanded: Boolean,
    onToggle: () -> Unit,
): Modifier =
    semantics {
        if (expanded) {
            collapse {
                onToggle()
                true
            }
        } else {
            expand {
                onToggle()
                true
            }
        }
    }
