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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.theme.SampleTheme
import com.materialkolor.sample.fluent.ui.component.IconButton
import com.materialkolor.sample.fluent.ui.component.TagChip
import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.TaskFilter
import com.materialkolor.sample.shared.ui.SampleCopy
import com.materialkolor.sample.shared.ui.SampleTags
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.CheckBox
import io.github.composefluent.component.Icon
import io.github.composefluent.component.Text
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Checkmark
import io.github.composefluent.icons.regular.Clock
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.Edit

/**
 * One task. The checkbox, the title, the tag chip and a delete button, with a soft fill on hover like a Fluent
 * list item.
 *
 * @param[task] The task to show.
 * @param[onToggle] Called when the checkbox is flipped.
 * @param[onDelete] Called when the delete button is pressed.
 * @param[modifier] The modifier for the row.
 */
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
        modifier = modifier
            .fillMaxWidth()
            .testTag(SampleTags.taskRow(task.id))
            .clip(FluentTheme.shapes.control)
            .background(fill)
            .hoverable(interaction)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Fluent's checkbox has the checkbox role but never says whether it is ticked, so the state goes on here.
        CheckBox(
            checked = task.isDone,
            modifier = Modifier
                .testTag(SampleTags.taskCheck(task.id))
                .semantics { toggleableState = ToggleableState(task.isDone) },
            onCheckStateChange = { onToggle() },
        )
        Text(
            text = task.title,
            modifier = Modifier
                .weight(1f)
                .testTag(SampleTags.taskTitle(task.id)),
            color = titleColor,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
        )
        TagChip(
            tag = task.tag,
            modifier = Modifier.testTag(SampleTags.taskTag(task.id)),
        )
        IconButton(
            icon = Icons.Default.Delete,
            description = SampleCopy.deleteTask(task.title),
            onClick = onDelete,
            testTag = SampleTags.taskDelete(task.id),
        )
    }
}

/**
 * What the list shows when [filter] lets no task through.
 *
 * @param[filter] The current filter, which picks the message and the icon.
 * @param[modifier] The modifier for the empty state.
 */
@Composable
internal fun EmptyState(
    filter: TaskFilter,
    modifier: Modifier = Modifier,
) {
    val tint = SampleTheme.colors.accentTint

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(SampleTags.EmptyState)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(tint.container),
            contentAlignment = Alignment.Center,
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
