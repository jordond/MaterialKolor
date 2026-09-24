package com.materialkolor.sample.customtheme.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.customtheme.theme.AppColors
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.Accent
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.Checkbox
import com.materialkolor.sample.customtheme.ui.component.Chip
import com.materialkolor.sample.customtheme.ui.component.Glyph
import com.materialkolor.sample.customtheme.ui.component.IconButton
import com.materialkolor.sample.customtheme.ui.component.Text
import com.materialkolor.sample.shared.model.Task
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.state.SampleAction
import com.materialkolor.sample.shared.ui.SampleCopy

@Composable
internal fun TaskRow(
    task: Task,
    dispatch: (SampleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .then(if (isHovered) Modifier.background(colors.onSurface.copy(alpha = HOVER_ALPHA)) else Modifier)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { dispatch(SampleAction.ToggleTask(task.id)) },
        )

        Text(
            text = task.title,
            style = AppType.Body,
            color = if (task.isDone) colors.textMuted else colors.textStrong,
            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        Chip(
            text = SampleCopy.label(task.tag),
            accent = task.tag.accent(colors),
        )

        IconButton(
            glyph = Glyph.Cross,
            onClick = { dispatch(SampleAction.DeleteTask(task.id)) },
            activeTint = colors.error,
        )
    }
}

internal fun TaskTag.accent(colors: AppColors): Accent =
    when (this) {
        TaskTag.Personal -> Accent(container = colors.loveContainer, content = colors.onLoveContainer)
        TaskTag.Work -> Accent(container = colors.coldContainer, content = colors.onColdContainer)
        TaskTag.Errand -> Accent(container = colors.warmContainer, content = colors.onWarmContainer)
    }

private const val HOVER_ALPHA = 0.04f
