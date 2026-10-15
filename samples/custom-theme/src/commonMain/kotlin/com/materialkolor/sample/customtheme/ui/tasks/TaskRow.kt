package com.materialkolor.sample.customtheme.ui.tasks

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.materialkolor.sample.customtheme.ui.component.Glyph
import com.materialkolor.sample.customtheme.ui.component.IconButton
import com.materialkolor.sample.customtheme.ui.component.Stamp
import com.materialkolor.sample.customtheme.ui.component.Text
import com.materialkolor.sample.customtheme.ui.component.highlighter
import com.materialkolor.sample.customtheme.ui.component.ink
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
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .then(if (isHovered) Modifier.ink(colors.paperShade, colors) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { dispatch(SampleAction.ToggleTask(task.id)) },
        )

        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = AppType.Body,
                color = if (task.isDone) colors.inkSoft else colors.ink,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                maxLines = 1,
                modifier = if (task.isDone) Modifier.highlighter(colors.highlight, colors) else Modifier,
            )
        }

        Stamp(
            text = SampleCopy.label(task.tag),
            accent = task.tag.accent(colors),
            tilt = Tilts[(task.id % Tilts.size).toInt()],
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
        TaskTag.Personal -> Accent(container = colors.pink, content = colors.onPink)
        TaskTag.Work -> Accent(container = colors.blue, content = colors.onBlue)
        TaskTag.Errand -> Accent(container = colors.yellow, content = colors.onYellow)
    }

private val Tilts = listOf(-3f, 2f, -1.5f, 2.5f, -2f)
