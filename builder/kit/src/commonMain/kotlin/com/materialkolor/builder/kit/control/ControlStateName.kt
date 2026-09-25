package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.materialkolor.builder.kit.a11y.LocalWebKeyboard
import com.materialkolor.builder.kit.a11y.onWebMirror
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.role_checkbox
import com.materialkolor.builder.kit.generated.resources.role_dialog
import com.materialkolor.builder.kit.generated.resources.role_menu_item
import com.materialkolor.builder.kit.generated.resources.role_option
import com.materialkolor.builder.kit.generated.resources.role_pop_up_button
import com.materialkolor.builder.kit.generated.resources.role_progress_bar
import com.materialkolor.builder.kit.generated.resources.role_radio
import com.materialkolor.builder.kit.generated.resources.role_slider
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
 * The web mirror in CMP 1.12.1 builds each element from the merged semantics node. It writes the
 * role, the content description as `aria-label`, the text as the element's own text,
 * `contenteditable` and test tags. Selected, toggle state, state description and disabled never
 * reach the page. So where this is set the kit folds the state into the content description of the
 * control's merged node, and the label is read once with its state. It folds in the role's word
 * too where a click handler hides the role or the mirror has none for it (P3, D40), and it names a
 * node with no role through its text, since screen readers drop an `aria-label` there
 * ([roleLessName]). The Compose semantics stay in place everywhere, for when CMP 1.13 carries them
 * across.
 *
 * It starts from [onWebMirror], and it is a local so a test can fold a whole tree on the JVM. The
 * web's keyboard habits outlive the fold, so they read [LocalWebKeyboard] instead.
 */
internal val LocalFoldsStateIntoName: ProvidableCompositionLocal<Boolean> =
    staticCompositionLocalOf { onWebMirror }

/**
 * The state a control reports.
 */
internal sealed interface ControlState {
    /**
     * A chip, a segment, a tab or a row that is or is not the current one.
     */
    data class Selected(
        val selected: Boolean,
    ) : ControlState

    /**
     * A checkbox or a toggle button.
     */
    data class Checked(
        val checked: Boolean,
    ) : ControlState

    /**
     * A switch.
     */
    data class Switched(
        val on: Boolean,
    ) : ControlState

    /**
     * Something that opens and closes, a disclosure or a button that shows and hides a panel of its
     * own.
     */
    data class Expanded(
        val expanded: Boolean,
    ) : ControlState

    /**
     * A slider's value, a progress bar's percentage or a select's choice, already in words.
     */
    data class Value(
        val text: String,
    ) : ControlState
}

/**
 * A role the web mirror loses. CMP 1.12.1 lets a click handler replace the role, so every clickable
 * reads as a button there (P3), and it has no role at all for a slider, a progress bar or a dialog.
 * While that stands the role's word travels in the name (D40).
 */
internal enum class FoldedRole {
    /**
     * A box that is ticked or not.
     */
    Checkbox,

    /**
     * A switch that is on or off.
     */
    Switch,

    /**
     * One option of a single choice, a choice chip, a segment or a scheme chip.
     */
    Radio,

    /**
     * One tab of a tab row.
     */
    Tab,

    /**
     * A value set along a track, a slider or a channel of the color picker.
     */
    Slider,

    /**
     * A bar that shows how far along some work is.
     */
    ProgressBar,

    /**
     * A select's field, which opens the list of its options.
     */
    PopUpButton,

    /**
     * One row of a menu.
     */
    MenuItem,

    /**
     * One row of a select's list.
     */
    Option,

    /**
     * A modal pane, a dialog, a sheet or a side panel.
     */
    Dialog,
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
    val slider: String,
    val progressBar: String,
    val popUpButton: String,
    val menuItem: String,
    val option: String,
    val dialog: String,
) {
    /**
     * How [state] reads on its own, as a state description.
     */
    fun of(state: ControlState): String =
        when (state) {
            is ControlState.Selected -> if (state.selected) selected else notSelected
            is ControlState.Checked -> if (state.checked) checked else notChecked
            is ControlState.Switched -> if (state.on) on else off
            is ControlState.Expanded -> if (state.expanded) expanded else collapsed
            is ControlState.Value -> state.text
        }

    /**
     * How [state] reads after a name. A word drops its capital there, a value keeps its own.
     */
    fun afterName(state: ControlState): String =
        when (state) {
            is ControlState.Selected,
            is ControlState.Checked,
            is ControlState.Switched,
            is ControlState.Expanded,
            -> of(state).lowerFirst()
            is ControlState.Value -> state.text
        }

    /**
     * The disabled word as it reads after a name.
     */
    val disabledAfterName: String get() = disabled.lowerFirst()

    /**
     * How [role] reads after a name. Role words are written that way already, so they keep their case.
     */
    fun roleWord(role: FoldedRole): String =
        when (role) {
            FoldedRole.Checkbox -> checkbox
            FoldedRole.Switch -> switch
            FoldedRole.Radio -> radio
            FoldedRole.Tab -> tab
            FoldedRole.Slider -> slider
            FoldedRole.ProgressBar -> progressBar
            FoldedRole.PopUpButton -> popUpButton
            FoldedRole.MenuItem -> menuItem
            FoldedRole.Option -> option
            FoldedRole.Dialog -> dialog
        }
}

private fun String.lowerFirst(): String = replaceFirstChar { char -> char.lowercaseChar() }

/**
 * The kit's state words in the current locale.
 */
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
        slider = stringResource(Res.string.role_slider),
        progressBar = stringResource(Res.string.role_progress_bar),
        popUpButton = stringResource(Res.string.role_pop_up_button),
        menuItem = stringResource(Res.string.role_menu_item),
        option = stringResource(Res.string.role_option),
        dialog = stringResource(Res.string.role_dialog),
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
 * Folds a menu row's role and state into its name, "Duplicate, menu item". A row that knows whether
 * it is [selected] is the current one of a set and reads as checked or not, "Dark, menu item,
 * checked", the way a menu reads a radio row. A plain command carries only the disabled note.
 *
 * The name is set whether the row is enabled or not. The web mirror never takes an `aria-label`
 * back, so a name set only while disabled would still read disabled once the row is enabled again.
 */
@Composable
internal fun Modifier.foldMenuRow(
    name: String,
    selected: Boolean?,
    enabled: Boolean,
): Modifier {
    val state = selected?.let { current -> ControlState.Checked(current) }
    return foldState(name, state, enabled, role = FoldedRole.MenuItem)
}

/**
 * Folds a select option's role and whether it is [selected] into its name, "Vibrant, option, not
 * selected". Like [foldMenuRow] it names the row whether it is enabled or not.
 */
@Composable
internal fun Modifier.foldOption(
    name: String,
    selected: Boolean,
    enabled: Boolean = true,
): Modifier = foldState(name, ControlState.Selected(selected), enabled, role = FoldedRole.Option)

/**
 * The name a select's field goes by, "Style, pop-up button, Tonal spot" where
 * [LocalFoldsStateIntoName] is set and [label] elsewhere. Every skin's field reads the same. The
 * field over an open panel opens nothing and reads through [shownChoiceName] instead.
 */
@Composable
internal fun selectFieldName(
    label: String,
    current: String,
    enabled: Boolean,
): String = stateName(label, ControlState.Value(current), enabled, role = FoldedRole.PopUpButton)

/**
 * Names a select's field that only shows the choice, the one over an open panel, where
 * [LocalFoldsStateIntoName] is set. It opens nothing, so there it plays no role, since the mirror
 * writes a dropdown list as a menu and a button would do nothing. Its label and choice go in as
 * text through [roleLessName], "Style, Vibrant", with no role word. It stays in the mirror, as the
 * only label the options under it have. Elsewhere it adds nothing and the field keeps its own
 * semantics.
 *
 * It clears the semantics of the field it lands on, so it goes first on the field, before the
 * modifiers that set its role and state.
 */
@Composable
internal fun Modifier.shownChoiceName(
    label: String,
    current: String,
): Modifier {
    if (!LocalFoldsStateIntoName.current) return this
    val name = stateName(label, ControlState.Value(current))
    return clearAndSetSemantics { roleLessName(name, asText = true) }
}

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
