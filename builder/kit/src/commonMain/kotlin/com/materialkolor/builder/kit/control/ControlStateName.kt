package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.state_checked
import com.materialkolor.builder.kit.generated.resources.state_collapsed
import com.materialkolor.builder.kit.generated.resources.state_disabled
import com.materialkolor.builder.kit.generated.resources.state_expanded
import com.materialkolor.builder.kit.generated.resources.state_not_checked
import com.materialkolor.builder.kit.generated.resources.state_not_selected
import com.materialkolor.builder.kit.generated.resources.state_off
import com.materialkolor.builder.kit.generated.resources.state_on
import com.materialkolor.builder.kit.generated.resources.state_selected
import org.jetbrains.compose.resources.stringResource

/**
 * Whether a control's state has to travel in its accessible name (D37).
 *
 * The web mirror in CMP 1.12.1 builds each element from the merged semantics node. It turns the
 * content description into `aria-label` and the text into the element's own text, and nothing else.
 * Selected, toggle state, state description and disabled never reach the page. So on wasm the kit
 * folds the state into the content description of the control's merged node, and the label is read
 * once with its state. The Compose semantics stay in place everywhere, for when CMP 1.13 carries
 * them across.
 */
internal expect val foldsStateIntoName: Boolean

/** [foldsStateIntoName] as a local, so a test can fold a whole tree on the JVM. */
internal val LocalFoldsStateIntoName: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { foldsStateIntoName }

/** The state a control reports. */
internal sealed interface ControlState {
    /** A chip, a segment, a tab or a row that is or is not the current one. */
    data class Selected(
        val selected: Boolean,
    ) : ControlState

    /** A checkbox or a toggle button. */
    data class Checked(
        val checked: Boolean,
    ) : ControlState

    /** A switch. */
    data class Switched(
        val on: Boolean,
    ) : ControlState

    /** A disclosure or a select that opens and closes. */
    data class Expanded(
        val expanded: Boolean,
    ) : ControlState

    /** A slider's value, a progress bar's percentage or a select's choice, already in words. */
    data class Value(
        val text: String,
    ) : ControlState
}

/**
 * The kit's state words for the current locale, as they stand alone.
 *
 * Resolved in composition, so the semantics blocks and other plain functions take them as a value.
 */
@Immutable
internal class StateWords(
    val selected: String,
    val notSelected: String,
    val checked: String,
    val notChecked: String,
    val on: String,
    val off: String,
    val expanded: String,
    val collapsed: String,
    val disabled: String,
) {
    /** How [state] reads on its own, as a state description. */
    fun of(state: ControlState): String =
        when (state) {
            is ControlState.Selected -> if (state.selected) selected else notSelected
            is ControlState.Checked -> if (state.checked) checked else notChecked
            is ControlState.Switched -> if (state.on) on else off
            is ControlState.Expanded -> if (state.expanded) expanded else collapsed
            is ControlState.Value -> state.text
        }

    /** How [state] reads after a name. A word drops its capital there, a value keeps its own. */
    fun afterName(state: ControlState): String =
        when (state) {
            is ControlState.Selected,
            is ControlState.Checked,
            is ControlState.Switched,
            is ControlState.Expanded,
            -> of(state).lowerFirst()
            is ControlState.Value -> state.text
        }

    /** The disabled word as it reads after a name. */
    val disabledAfterName: String get() = disabled.lowerFirst()
}

private fun String.lowerFirst(): String = replaceFirstChar { char -> char.lowercaseChar() }

/** The kit's state words in the current locale. */
@Composable
internal fun stateWords(): StateWords =
    StateWords(
        selected = stringResource(Res.string.state_selected),
        notSelected = stringResource(Res.string.state_not_selected),
        checked = stringResource(Res.string.state_checked),
        notChecked = stringResource(Res.string.state_not_checked),
        on = stringResource(Res.string.state_on),
        off = stringResource(Res.string.state_off),
        expanded = stringResource(Res.string.state_expanded),
        collapsed = stringResource(Res.string.state_collapsed),
        disabled = stringResource(Res.string.state_disabled),
    )

/**
 * [name] followed by [state] and, when the control is off, the disabled word, "Tonal spot, selected"
 * or "Dark mode, off, disabled". Blank parts are left out.
 */
internal fun foldStateIntoName(
    name: String,
    state: ControlState?,
    enabled: Boolean,
    words: StateWords,
): String =
    listOfNotNull(
        name,
        state?.let(words::afterName),
        if (enabled) null else words.disabledAfterName,
    ).filter { part -> part.isNotBlank() }
        .joinToString(", ")

/**
 * The name a stateful control goes by. Where [LocalFoldsStateIntoName] is set it carries [state]
 * and a disabled note as well, elsewhere it is [name] as it is.
 *
 * For a control that already sets its own content description on the node that merges it.
 */
@Composable
internal fun stateName(
    name: String,
    state: ControlState?,
    enabled: Boolean = true,
): String = if (LocalFoldsStateIntoName.current) foldStateIntoName(name, state, enabled, stateWords()) else name

/**
 * Folds [state] into the name of the node this lands on, or of the control that merges it, where
 * [LocalFoldsStateIntoName] is set. Elsewhere it adds nothing.
 *
 * @param[name] What the control's text says, the name it goes by without the fold.
 * @param[state] The control's state, or null for none but the disabled note.
 * @param[enabled] Whether the control takes input.
 */
@Composable
internal fun Modifier.foldState(
    name: String,
    state: ControlState?,
    enabled: Boolean = true,
): Modifier {
    if (!LocalFoldsStateIntoName.current) return this
    val folded = foldStateIntoName(name, state, enabled, stateWords())
    return semantics { contentDescription = folded }
}
