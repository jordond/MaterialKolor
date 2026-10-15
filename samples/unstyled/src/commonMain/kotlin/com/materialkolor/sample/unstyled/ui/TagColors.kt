package com.materialkolor.sample.unstyled.ui

import androidx.compose.runtime.Composable
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.unstyled.theme.accent
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.onAccent
import com.materialkolor.sample.unstyled.ui.component.TagColors

@Composable
internal fun TaskTag.colors(): TagColors = TagColors(container = accent.color, content = onAccent.color)
