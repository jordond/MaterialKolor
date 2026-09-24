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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.TextFieldValue
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.headless.DropdownList
import com.materialkolor.builder.kit.headless.HeadlessDropdown
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree
import com.materialkolor.builder.kit.icon.IconId

/**
 * Material's exposed dropdown, a read only outlined field over a `DropdownMenu`.
 *
 * The field already shows the choice as its text, so on the web its name is the label alone, with
 * the disabled note while it is disabled, and the choice is read once (S5 row 10).
 *
 * Where overlays render in the page (D40) the field stays and the options open in the headless
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
        // b-228a
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
                ).semantics {
                    role = Role.DropdownList
                    stateDescription = current
                }.foldState(label, null, enabled),
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
        // b-228a
        MaterialChoiceField(
            current = current,
            label = label,
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false }
                .semantics {
                    role = Role.DropdownList
                    stateDescription = current
                }.foldState(label, null),
        )
        DropdownList(materialMenuStyle(), modifier = Modifier.fillMaxWidth()) {
            MaterialSelectRows(options, selected, optionLabel, onSelect, selectedRow = null)
        }
    }
}

// b-228a

/**
 * The choice in Material's read only outlined field with its chevron. The field keeps its own
 * selection, as `OutlinedTextField` does for text, so a tap that moves the caret recomposes the field
 * alone and not the menu box around it, which would drop the tap that opens the menu.
 */
@Composable
private fun MaterialChoiceField(
    current: String,
    label: String,
    modifier: Modifier,
    enabled: Boolean = true,
) {
    var shown by remember { mutableStateOf(TextFieldValue(current)) }
    MaterialOutlinedField(
        value = shown.copy(text = current),
        onValueChange = { next -> shown = next },
        label = label,
        modifier = modifier,
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
                }.foldState(optionLabel(option), ControlState.Selected(isSelected)),
            trailingIcon = if (isSelected) {
                { BuilderIcon(IconId.Check, contentDescription = null) }
            } else {
                null
            },
        )
    }
}
