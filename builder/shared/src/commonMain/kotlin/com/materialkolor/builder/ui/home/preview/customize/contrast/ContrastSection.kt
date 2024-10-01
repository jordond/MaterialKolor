package com.materialkolor.builder.ui.home.preview.customize.contrast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.Contrast
import com.materialkolor.builder.ui.home.components.ContrastSelector
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun ContrastSection(
    selected: Contrast,
    onUpdate: (Contrast) -> Unit,
    modifier: Modifier = Modifier,
    options: PersistentList<Contrast> = Contrast.entries.sortedBy { it.value }.toPersistentList(),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Text(
            text = "Contrast level",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Adjust the contrast setting to reduced, standard, medium, or high to " +
                "fine-tune the visual distinction between colors in your generated color scheme.",
            style = MaterialTheme.typography.bodySmall,
        )

        ContrastSelector(
            selected = selected,
            onUpdate = onUpdate,
            options = options,
        )
    }
}
