package com.materialkolor.builder.ui.home.preview.gallery.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainer
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainerChild
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainerDefaults

private const val checkboxesInfoUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#Checkbox(kotlin.Boolean,kotlin.Function1,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.material3.CheckboxColors,androidx.compose.foundation.interaction.MutableInteractionSource)"

private const val chipsInfoUrl = "https://developer.android.com/jetpack/compose/components/chip"

private const val datePickerInfoUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#DatePicker(androidx.compose.material3.DatePickerState,androidx.compose.ui.Modifier,androidx.compose.material3.DatePickerFormatter,kotlin.Function0,kotlin.Function0,kotlin.Boolean,androidx.compose.material3.DatePickerColors)"

private const val menusInfoUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#DropdownMenu(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,androidx.compose.ui.unit.DpOffset,androidx.compose.foundation.ScrollState,androidx.compose.ui.window.PopupProperties,kotlin.Function1)"

private const val radioButtonInfoUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#RadioButton(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.material3.RadioButtonColors,androidx.compose.foundation.interaction.MutableInteractionSource)"

private const val sliderInfoUrl =
    "https://developer.android.com/jetpack/compose/components/slider"

private const val switchInfoUrl =
    "https://developer.android.com/jetpack/compose/components/switch"

private const val timePickerInfoUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#TimePicker(androidx.compose.material3.TimePickerState,androidx.compose.ui.Modifier,androidx.compose.material3.TimePickerColors,androidx.compose.material3.TimePickerLayoutType)"

@Composable
fun SelectionGallery(
    expanded: Boolean,
    toggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = GalleryContainerDefaults.MinWidth,
    width: Dp = GalleryContainerDefaults.Width,
    itemPadding: Dp = GalleryContainerDefaults.ItemPadding2,
) {
    GalleryContainer(
        title = "Selection",
        expanded = expanded,
        toggle = toggle,
        modifier = modifier,
    ) {
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GalleryContainerChild(title = "Checkboxes", checkboxesInfoUrl) {
                CheckboxesDemo(minWidth, width, itemPadding)
            }

            GalleryContainerChild(title = "Radio buttons", radioButtonInfoUrl) {
                RadioButtonsDemo(minWidth, width, itemPadding)
            }

            GalleryContainerChild(title = "Chips", chipsInfoUrl) {
                ChipsDemo(minWidth, width, itemPadding)
            }

            GalleryContainerChild(title = "Menus", menusInfoUrl) {
                MenuDemo(minWidth, width)
            }

            GalleryContainerChild(title = "Switches", switchInfoUrl) {
                SwitchesDemo(minWidth, width)
            }

            GalleryContainerChild(title = "Sliders", sliderInfoUrl) {
                SlidersDemo(minWidth, width)
            }

            GalleryContainerChild(title = "Date picker", datePickerInfoUrl) {
                DatePickerDemo(minWidth, width, itemPadding)
            }

            GalleryContainerChild(title = "Time picker", timePickerInfoUrl) {
                TimePickerDemo(minWidth, width, itemPadding)
            }
        }
    }
}

private val colorsMap = mapOf(
    "Blue" to Color.Blue,
    "Pink" to Color.Magenta,
    "Green" to Color.Green,
    "Yellow" to Color.Yellow,
    "Grey" to Color.Gray,
)
private val iconsMap = mapOf(
    "Smile" to Icons.Default.SentimentSatisfied,
    "Cloud" to Icons.Outlined.Cloud,
    "Brush" to Icons.Outlined.Brush,
    "Heart" to Icons.Default.Favorite,
)

@Composable
private fun MenuDemo(
    minWidth: Dp,
    width: Dp,
) {
    @Composable
    fun DropDownMenuDemo() {
        Box {
            var menuExpanded by remember { mutableStateOf(false) }
            FilledTonalButton(
                onClick = { menuExpanded = true },
                content = { Text("Show menu") },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )

            val items = listOf(
                "Item 1" to Icons.Outlined.People,
                "Item 2" to Icons.Outlined.Visibility,
                "Item 3" to Icons.Outlined.Refresh,
            )

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                items.forEach { item ->
                    key(item.first) {
                        DropdownMenuItem(
                            text = { Text(item.first) },
                            onClick = {},
                            leadingIcon = {
                                Icon(imageVector = item.second, contentDescription = null)
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Box {
            // TODO: Use CascadeDropDownMenu (maybe NestedDropDownMenu?) when/if it becomes available
            var menuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = null)
            }
            val items = (1..3).map { "Menu $it" }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                items.forEachIndexed { ix, item ->
                    key(item) {
                        if (ix == 2) HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {},
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ExposedDropDownMenuDemo() {
        var selectedColor by remember { mutableStateOf("") }
        Box {
            var expanded by remember { mutableStateOf(false) }
            var textValue by remember { mutableStateOf(selectedColor) }
            val filteredColors = derivedStateOf {
                if (textValue.isBlank()) return@derivedStateOf colorsMap
                return@derivedStateOf colorsMap.filter { entry ->
                    entry.key.contains(textValue, ignoreCase = true)
                }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        selectedColor = ""
                    },
                    placeholder = { Text("Color") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier
                        .width(180.dp)
                        .menuAnchor(enabled = true, type = MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    filteredColors.value.forEach { selectionOption ->
                        key(selectionOption) {
                            DropdownMenuItem(
                                text = { Text(selectionOption.key) },
                                onClick = {
                                    selectedColor = selectionOption.key
                                    textValue = selectionOption.key
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        var selectedIcon by remember { mutableStateOf(iconsMap.keys.first()) }
        Box {
            var expanded by remember { mutableStateOf(false) }
            var textValue by remember { mutableStateOf(selectedIcon) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        selectedIcon = ""
                    },
                    label = { Text("Icon") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier
                        .width(180.dp)
                        .menuAnchor(enabled = true, type = MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    iconsMap.forEach { selectionOption ->
                        key(selectionOption.key) {
                            DropdownMenuItem(
                                text = { Text(selectionOption.key) },
                                onClick = {
                                    selectedIcon = selectionOption.key
                                    textValue = selectionOption.key
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            imageVector = iconsMap[selectedIcon] ?: iconsMap[iconsMap.keys.first()]!!,
            tint = colorsMap[selectedColor] ?: colorsMap["Grey"]!!,
            contentDescription = null,
        )
    }

    OutlinedCard {
        Column(
            modifier = Modifier
                .selectableGroup()
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                DropDownMenuDemo()
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExposedDropDownMenuDemo()
            }
        }
    }
}

@Composable
private fun TimePickerDemo(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    var openDialog by remember { mutableStateOf(false) }

    OutlinedCard {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .selectableGroup()
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(itemPadding),
        ) {
            OutlinedButton(
                onClick = { openDialog = true },
            ) {
                Text("Show time picker")
            }
        }
    }

    if (openDialog) {
        val state = rememberTimePickerState()

        // TODO: Use TimePickerDialog when we update to a newer version of material3. It's not available in 1.1.2
        Dialog(onDismissRequest = { openDialog = false }) {
            Card(shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.padding(itemPadding)) {
                    TimePicker(
                        state,
                        layoutType = TimePickerLayoutType.Vertical,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { openDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        TextButton(onClick = { openDialog = false }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchesDemo(
    minWidth: Dp,
    width: Dp,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .selectableGroup()
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(32.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                var checked by remember { mutableStateOf(true) }
                Switch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                )

                var checked2 by remember { mutableStateOf(true) }
                Switch(
                    checked = checked2,
                    onCheckedChange = { checked2 = it },
                    thumbContent = if (checked2) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Switch(
                    enabled = false,
                    checked = false,
                    onCheckedChange = null,
                )
                Switch(
                    enabled = false,
                    checked = true,
                    onCheckedChange = null,
                    thumbContent = {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SlidersDemo(
    minWidth: Dp,
    width: Dp,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .selectableGroup()
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(32.dp),
        ) {
            var sliderPosition by remember { mutableFloatStateOf(0f) }
            Slider(
                value = sliderPosition,
                onValueChange = { sliderPosition = it },
            )
            Spacer(modifier = Modifier.height(32.dp))
            var sliderPosition2 by remember { mutableFloatStateOf(0f) }
            Slider(
                value = sliderPosition2,
                onValueChange = { sliderPosition2 = it },
                steps = 5,
                valueRange = 0f..100f,
            )
            Spacer(modifier = Modifier.height(32.dp))
            var sliderPosition3 by remember { mutableStateOf(0f..100f) }
            RangeSlider(
                value = sliderPosition3,
                steps = 5,
                onValueChange = { range -> sliderPosition3 = range },
                valueRange = 0f..100f,
            )
        }
    }
}

private val radioOptions = listOf("Option 1", "Option 2", "Option 3")

@Composable
private fun RadioButtonsDemo(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[0]) }

    OutlinedCard {
        Column(
            modifier = Modifier
                .selectableGroup()
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(itemPadding),
        ) {

            radioOptions.forEachIndexed { ix, text ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .selectable(
                            enabled = ix < 2,
                            selected = (text == selectedOption),
                            onClick = { onOptionSelected(text) },
                            role = Role.RadioButton,
                        )
                        .padding(horizontal = 16.dp),
                ) {
                    RadioButton(
                        enabled = ix < 2,
                        selected = (text == selectedOption),
                        onClick = null,
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (ix < 2) 1f else 0.5f),
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }

        }
    }
}

@Composable
private fun DatePickerDemo(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    var openDialog by remember { mutableStateOf(false) }

    OutlinedCard {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(itemPadding),
        ) {
            OutlinedButton(
                onClick = { openDialog = true },
            ) {
                Text("Show date picker")
            }
        }
    }

    if (openDialog) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { openDialog = false },
            confirmButton = {
                TextButton(onClick = { openDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { openDialog = false }) {
                    Text("Cancel")
                }
            },
            content = {
                DatePicker(state)
            },
        )
    }
}

@Composable
private fun ChipsDemo(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    var filterChipSelected by remember { mutableStateOf(true) }

    @Composable
    fun ChipsRow(enabled: Boolean = true) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AssistChip(
                enabled = enabled,
                onClick = {},
                label = { Text("Assist") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )

            FilterChip(
                enabled = enabled,
                onClick = { filterChipSelected = !filterChipSelected },
                label = { Text("Filter") },
                selected = filterChipSelected,
                leadingIcon = if (!filterChipSelected) null
                else {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = "Done icon",
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                },
            )

            InputChip(
                onClick = {},
                label = { Text("Input") },
                selected = true,
                enabled = enabled,
                avatar = null,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(InputChipDefaults.IconSize),
                    )
                },
            )

            SuggestionChip(
                enabled = enabled,
                onClick = {},
                label = { Text("Suggestion") },
            )
        }
    }

    OutlinedCard {
        Column(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(itemPadding),
        ) {
            ChipsRow(enabled = true)
            ChipsRow(enabled = false)
        }
    }
}

@Composable
private fun CheckboxesDemo(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(itemPadding),
        ) {

            val state1 = remember { mutableStateOf(true) }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { state1.value = !state1.value }
                    .padding(itemPadding),
            ) {
                Text("Option 1")
                Checkbox(
                    checked = state1.value,
                    onCheckedChange = null,
                )
            }

            val state2 = remember { mutableStateOf(ToggleableState.Indeterminate) }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .height(56.dp)
                    .clickable { state2.value = state2.value.nextState() }
                    .padding(itemPadding),
            ) {
                Text("Option 2")
                TriStateCheckbox(
                    state = state2.value,
                    onClick = null,
                )
            }

            val state3 = remember { mutableStateOf(ToggleableState.Off) }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { state3.value = state3.value.nextState() }
                    .padding(itemPadding),
            ) {
                Text("Option 3")
                TriStateCheckbox(
                    state = state3.value,
                    onClick = null,
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(enabled = false) { }
                    .padding(itemPadding),
            ) {
                Text("Option 4")
                Checkbox(
                    checked = true,
                    enabled = false,
                    onCheckedChange = null,
                )
            }
        }
    }
}

private fun ToggleableState.nextState(): ToggleableState {
    return when (this) {
        ToggleableState.Indeterminate -> ToggleableState.Off
        ToggleableState.On -> ToggleableState.Indeterminate
        ToggleableState.Off -> ToggleableState.On
    }
}
