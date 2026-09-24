package com.materialkolor.builder.preview.unstyled

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/** True on the web, where the page reads the dashboard through the CMP accessibility mirror. */
internal expect val dashboardOnWeb: Boolean

/**
 * Whether the dashboard folds a control's state into its name (D37).
 *
 * The web mirror in CMP 1.12.1 drops selected, expanded and state descriptions, and it reads every
 * clickable as a button (D40 P3). So on the web the state word travels in the name, as in "Week,
 * selected". The kit's one public fold, `foldedToggleName`, speaks for a checkbox, and the dashboard
 * takes nothing from the kit but its motion, so it folds the state word alone and leaves the role
 * word out. It starts from [dashboardOnWeb], and it is a local so a test can fold the app on the JVM.
 */
internal val LocalDashboardFoldsState: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { dashboardOnWeb }

/** The word for whether what a button shows is open. */
internal fun expandedWord(expanded: Boolean): String = if (expanded) DashboardCopy.Expanded else DashboardCopy.Collapsed

/** The word for whether a destination, a tab or a pick is the current one. */
internal fun selectedWord(selected: Boolean): String =
    if (selected) DashboardCopy.Selected else DashboardCopy.NotSelected

/** [name] followed by the [state] word where the dashboard folds state, and [name] alone elsewhere. */
@Composable
internal fun stateName(
    name: String,
    state: String,
): String =
    if (LocalDashboardFoldsState.current) {
        "$name, ${state.replaceFirstChar { char -> char.lowercaseChar() }}"
    } else {
        name
    }

/**
 * Folds the [state] word into the name of a control its own text names, where the dashboard folds
 * state. Elsewhere it adds nothing and the text stays the name.
 */
@Composable
internal fun Modifier.foldState(
    name: String,
    state: String,
): Modifier {
    if (!LocalDashboardFoldsState.current) return this
    val spoken = stateName(name, state)
    return semantics { contentDescription = spoken }
}

/**
 * The state of a button that shows a panel of its own. Its state description says whether the
 * panel is open, and assistive tech can open or close it with [onToggle].
 */
internal fun Modifier.expandedSemantics(
    expanded: Boolean,
    onToggle: () -> Unit,
): Modifier {
    val spoken = expandedWord(expanded)
    return semantics {
        stateDescription = spoken
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
}
