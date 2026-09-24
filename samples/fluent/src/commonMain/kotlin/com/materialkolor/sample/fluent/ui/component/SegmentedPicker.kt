package com.materialkolor.sample.fluent.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.composefluent.component.SegmentedButton
import io.github.composefluent.component.SegmentedControl
import io.github.composefluent.component.SegmentedItemPosition

@Composable
internal fun <T> SegmentedPicker(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable (T) -> Unit)? = null,
    text: @Composable (T) -> Unit,
) {
    SegmentedControl(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                checked = option == selected,
                onCheckedChanged = { onSelect(option) },
                position = when (index) {
                    0 -> SegmentedItemPosition.Start
                    options.lastIndex -> SegmentedItemPosition.End
                    else -> SegmentedItemPosition.Center
                },
                icon = icon?.let { content -> { content(option) } },
                text = { text(option) },
            )
        }
    }
}
