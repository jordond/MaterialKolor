package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.role_checkbox
import com.materialkolor.builder.kit.generated.resources.role_radio
import com.materialkolor.builder.kit.generated.resources.role_switch
import com.materialkolor.builder.kit.generated.resources.role_tab
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

    /** A disclosure that opens and closes. */
    data class Expanded(
        val expanded: Boolean,
    ) : ControlState

    /** A slider's value, a progress bar's percentage or a select's choice, already in words. */
    data class Value(
        val text: String,
    ) : ControlState
}

/**
 * A role the web mirror loses. CMP 1.12.1 lets a click handler replace the role, so every clickable
 * reads as a button there (P3). While that stands the role's word travels in the name (D40).
 */
internal enum class FoldedRole {
    /** A box that is ticked or not. */
    Checkbox,

    /** A switch that is on or off. */
    Switch,

    /** One option of a single choice, a choice chip, a segment or a scheme chip. */
    Radio,

    /** One tab of a tab row. */
    Tab,
}

/**
 * The kit's state words for the current locale, as they stand alone, and its role words, as they
 * read after a name.
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
    val checkbox: String,
    val switch: String,
    val radio: String,
    val tab: String,
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

    /** How [role] reads after a name. Role words are written that way already, so they keep their case. */
    fun roleWord(role: FoldedRole): String =
        when (role) {
            FoldedRole.Checkbox -> checkbox
            FoldedRole.Switch -> switch
            FoldedRole.Radio -> radio
            FoldedRole.Tab -> tab
        }
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
        checkbox = stringResource(Res.string.role_checkbox),
        switch = stringResource(Res.string.role_switch),
        radio = stringResource(Res.string.role_radio),
        tab = stringResource(Res.string.role_tab),
    )

/**
 * [name] followed by the word for [role], then [state] and, when the control is disabled, the
 * disabled word, "Tonal spot, radio, selected" or "Dark mode, off, disabled". Blank parts are left
 * out.
 */
internal fun foldStateIntoName(
    name: String,
    state: ControlState?,
    enabled: Boolean,
    words: StateWords,
    role: FoldedRole? = null,
): String =
    listOfNotNull(
        name,
        role?.let(words::roleWord),
        state?.let(words::afterName),
        if (enabled) null else words.disabledAfterName,
    ).filter { part -> part.isNotBlank() }
        .joinToString(", ")

/**
 * The name a stateful control goes by. Where [LocalFoldsStateIntoName] is set it carries [state]
 * and a disabled note as well, elsewhere it is [name] as it is.
 *
 * For a control that already sets its own content description on the node that merges it. A
 * control that loaded [words] for its own state description passes them in, so they load once.
 * Left null, they load only when the fold is on. A control whose [role] the web loses names it too.
 */
@Composable
internal fun stateName(
    name: String,
    state: ControlState?,
    enabled: Boolean = true,
    words: StateWords? = null,
    role: FoldedRole? = null,
): String =
    if (LocalFoldsStateIntoName.current) {
        foldStateIntoName(name, state, enabled, words ?: stateWords(), role)
    } else {
        name
    }

/**
 * Folds [state] into the name of the node this lands on, or of the control that merges it, where
 * [LocalFoldsStateIntoName] is set. Elsewhere it adds nothing.
 *
 * @param[name] What the control's text says, the name it goes by without the fold.
 * @param[state] The control's state, or null for none but the disabled note.
 * @param[enabled] Whether the control takes input.
 * @param[words] The state words, for a control that already loaded them for its own state
 * description. Left null, they load only when the fold is on.
 * @param[role] The role the web loses for this control (P3), or null for one it keeps.
 */
@Composable
internal fun Modifier.foldState(
    name: String,
    state: ControlState?,
    enabled: Boolean = true,
    words: StateWords? = null,
    role: FoldedRole? = null,
): Modifier {
    if (!LocalFoldsStateIntoName.current) return this
    val folded = foldStateIntoName(name, state, enabled, words ?: stateWords(), role)
    return semantics { contentDescription = folded }
}

/**
 * Folds the disabled note into the name of a control with no other state, and only while it is
 * disabled, so an enabled control keeps the name its text gives it.
 */
@Composable
internal fun Modifier.foldDisabled(
    name: String,
    enabled: Boolean,
): Modifier = if (enabled) this else foldState(name, state = null, enabled = false)

/**
 * Folds a menu row's state into its name. A row that knows whether it is [selected] is an option
 * and always carries that state, a plain command only the disabled note.
 */
@Composable
internal fun Modifier.foldMenuRow(
    name: String,
    selected: Boolean?,
    enabled: Boolean,
): Modifier =
    if (selected == null) foldDisabled(name, enabled) else foldState(name, ControlState.Selected(selected), enabled)

/**
 * Names a node that plays no role, such as a slider, a progress bar or a group of options.
 *
 * Where [asText] is set, on the web, the name goes in as text. The mirror keeps an `aria-label` on
 * a plain element, but ARIA forbids a name there and screen readers drop it, while both keep text
 * (S5 answer 1). Elsewhere it stays the content description.
 */
internal fun SemanticsPropertyReceiver.roleLessName(
    name: String,
    asText: Boolean,
) {
    if (asText) text = AnnotatedString(name) else contentDescription = name
}
