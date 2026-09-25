package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.TextFieldValue
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.foldOption
import com.materialkolor.builder.kit.control.selectFieldName
import com.materialkolor.builder.kit.control.shownChoiceName
import com.materialkolor.builder.kit.headless.DropdownList
import com.materialkolor.builder.kit.headless.HeadlessDropdown
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree
import com.materialkolor.builder.kit.icon.IconId

/**
 * Material's exposed dropdown, a read only outlined field over a `DropdownMenu`.
 *
 * On the web the field reads as a button named like every skin's select field, "Style, pop-up
 * button, Tonal spot", rather than as an editable text box ([MaterialChoiceField]).
 *
 * Where overlays render in the page the field stays and the options open in the headless
 * dropdown in Material's menu container, as wide as the field, with focus on the chosen option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> MaterialSelect(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    enabled: Boolean,
    modifier: Modifier,
) {
    val inTree = LocalOverlaysInTree.current
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableIntStateOf(0) }
    val field = remember { FocusRequester() }
    val selectedRow = remember { FocusRequester() }
    val current = optionLabel(selected)
    val choose = { option: T ->
        expanded = false
        onSelect(option)
    }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { open -> expanded = open && enabled },
        modifier = modifier,
    ) {
        MaterialChoiceField(
            current = current,
            label = label,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .then(
                    if (inTree) {
                        Modifier.focusRequester(field).onSizeChanged { size -> fieldWidth = size.width }
                    } else {
                        Modifier
                    },
                ),
            enabled = enabled,
        )
        if (inTree) {
            HeadlessDropdown(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                style = materialMenuStyle(),
                minWidth = with(density) { fieldWidth.toDp() },
                initialFocus = if (selected in options) selectedRow else null,
                returnFocusTo = field,
            ) { close ->
                val pick = { option: T ->
                    close()
                    onSelect(option)
                }
                MaterialSelectRows(options, selected, optionLabel, pick, selectedRow)
            }
        } else {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                MaterialSelectRows(options, selected, optionLabel, choose, null)
            }
        }
    }
}

/**
 * Material's exposed dropdown drawn open where it stands, its outlined field over the rows of
 * [MaterialSelect] in Material's menu container, with nothing floating. The field only shows the
 * choice and takes no focus, so Tab goes straight to the options.
 */
@Composable
internal fun <T> MaterialSelectPanel(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier,
) {
    val current = optionLabel(selected)
    Column(modifier.width(IntrinsicSize.Max)) {
        MaterialChoiceField(
            current = current,
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false },
            opens = false,
        )
        DropdownList(materialMenuStyle(), modifier = Modifier.fillMaxWidth()) {
            MaterialSelectRows(options, selected, optionLabel, onSelect, selectedRow = null)
        }
    }
}

/**
 * The choice in Material's read only outlined field with its chevron. The field keeps its own
 * selection, as `OutlinedTextField` does for text, so a tap that moves the caret recomposes the field
 * alone and not the menu box around it, which would drop the tap that opens the menu.
 *
 * It reads as a dropdown list whose state is the choice. On the web the mirror would read the text
 * field inside as an editable text box, so there the field's own semantics are cleared. A field
 * that [opens] its menu is named by [selectFieldName], with the disabled note while it is disabled.
 * The menu anchor comes in [modifier], outside what is cleared, so its click and its dropdown list
 * role stay, and the mirror reads the two as a button. A disabled anchor adds neither, so the
 * disabled field sets them itself, a click that does nothing, and still reads as a button the way
 * the headless select's does rather than as a bare group. A field over an open panel only shows
 * the choice and reads as text through [shownChoiceName]. The text field and the clip that keeps
 * its touch selection handles off are left as they are.
 */
@Composable
private fun MaterialChoiceField(
    current: String,
    label: String,
    modifier: Modifier,
    enabled: Boolean = true,
    opens: Boolean = true,
) {
    var shown by remember { mutableStateOf(TextFieldValue(current)) }
    val named = when {
        !LocalFoldsStateIntoName.current -> {
            Modifier.semantics {
                role = Role.DropdownList
                stateDescription = current
            }
        }
        opens -> {
            val name = selectFieldName(label, current, enabled)
            Modifier.clearAndSetSemantics {
                contentDescription = name
                stateDescription = current
                if (!enabled) {
                    disabled()
                    role = Role.DropdownList
                    onClick { false }
                }
            }
        }
        else -> {
            Modifier.shownChoiceName(label, current)
        }
    }
    MaterialOutlinedField(
        value = shown.copy(text = current),
        onValueChange = { next -> shown = next },
        label = label,
        modifier = modifier.then(named),
        enabled = enabled,
        readOnly = true,
        trailingIcon = { BuilderIcon(IconId.ChevronDown, contentDescription = null) },
    )
}

@Composable
private fun <T> MaterialSelectRows(
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onChoose: (T) -> Unit,
    selectedRow: FocusRequester?,
) {
    for (option in options) {
        val isSelected = option == selected
        DropdownMenuItem(
            text = { Text(optionLabel(option)) },
            onClick = { onChoose(option) },
            modifier = Modifier
                .then(if (isSelected && selectedRow != null) Modifier.focusRequester(selectedRow) else Modifier)
                .semantics {
                    role = Role.RadioButton
                    this.selected = isSelected
                }.foldOption(optionLabel(option), isSelected),
            trailingIcon = if (isSelected) {
                { BuilderIcon(IconId.Check, contentDescription = null) }
            } else {
                null
            },
        )
    }
}
