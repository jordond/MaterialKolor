package com.materialkolor.sample.material3.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun TagLabel(
    tag: TaskTag,
    modifier: Modifier = Modifier,
) {
    val colors = tag.colors

    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.container,
        contentColor = colors.content,
        modifier = modifier,
    ) {
        Text(
            text = SampleCopy.label(tag),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
