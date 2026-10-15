package com.materialkolor.sample.material3.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.material3.ui.component.TagLabel
import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (hovered) colors.surfaceContainerHigh else colors.surfaceContainerHigh.copy(alpha = 0f),
        label = "TaskRowFill",
    )
    val titleColor by animateColorAsState(
        targetValue = if (task.isDone) colors.onSurfaceVariant else colors.onSurface,
        label = "TaskTitleColor",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interaction)
            .background(fill)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { onToggle() },
        )

        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            color = titleColor,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )

        TagLabel(tag = task.tag)

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
            )
        }
    }
}

@Composable
internal fun EmptyState(
    filter: TaskFilter,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(
                imageVector = filter.emptyIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Text(
            text = SampleCopy.empty(filter),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val TaskFilter.emptyIcon: ImageVector
    get() = when (this) {
        TaskFilter.All -> Icons.Outlined.EditNote
        TaskFilter.Active -> Icons.Outlined.Celebration
        TaskFilter.Done -> Icons.Outlined.HourglassEmpty
    }
