package com.materialkolor.sample.fluent.ui.tasks

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.theme.SampleTheme
import com.materialkolor.sample.fluent.ui.component.TagChip
import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.ui.SampleCopy
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.CheckBox
import io.github.composefluent.component.Icon
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Checkmark
import io.github.composefluent.icons.regular.Clock
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.Edit

@Composable
internal fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val colors = FluentTheme.colors
    val fill by animateColorAsState(
        targetValue = if (hovered) colors.subtleFill.secondary else colors.subtleFill.transparent,
        label = "TaskRowFill",
    )
    val titleColor by animateColorAsState(
        targetValue = if (task.isDone) colors.text.text.secondary else colors.text.text.primary,
        label = "TaskTitleColor",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(FluentTheme.shapes.control)
            .background(fill)
            .hoverable(interaction)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
    ) {
        CheckBox(
            checked = task.isDone,
            onCheckStateChange = { onToggle() },
        )

        Text(
            text = task.title,
            color = titleColor,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f),
        )

        TagChip(tag = task.tag)

        SubtleButton(
            onClick = onDelete,
            iconOnly = true,
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun EmptyState(
    filter: TaskFilter,
    modifier: Modifier = Modifier,
) {
    val tint = SampleTheme.colors.accentTint

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(tint.container),
        ) {
            Icon(
                imageVector = filter.emptyIcon(),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = tint.content,
            )
        }

        Text(
            text = SampleCopy.empty(filter),
            color = FluentTheme.colors.text.text.secondary,
        )
    }
}

private fun TaskFilter.emptyIcon(): ImageVector =
    when (this) {
        TaskFilter.All -> Icons.Default.Edit
        TaskFilter.Active -> Icons.Default.Checkmark
        TaskFilter.Done -> Icons.Default.Clock
    }
