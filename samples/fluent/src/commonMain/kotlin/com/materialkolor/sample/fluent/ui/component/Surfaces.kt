package com.materialkolor.sample.fluent.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.fluent.theme.SampleTheme
import com.materialkolor.sample.shared.model.TaskTag
import com.materialkolor.sample.shared.ui.SampleCopy
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.BackgroundSizing
import io.github.composefluent.background.Layer
import io.github.composefluent.component.Text

@Composable
internal fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Layer(
        modifier = modifier.fillMaxWidth(),
        shape = FluentTheme.shapes.overlay,
        color = FluentTheme.colors.background.card.default,
        border = BorderStroke(width = 1.dp, color = FluentTheme.colors.stroke.card.default),
        backgroundSizing = BackgroundSizing.InnerBorderEdge,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
internal fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = FluentTheme.typography.caption,
        color = FluentTheme.colors.text.text.secondary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
internal fun TagChip(
    tag: TaskTag,
    modifier: Modifier = Modifier,
) {
    val tint = SampleTheme.colors.tint(tag)

    Box(
        modifier = modifier
            .widthIn(min = 72.dp)
            .clip(CircleShape)
            .background(tint.container)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = SampleCopy.label(tag),
            style = FluentTheme.typography.caption,
            color = tint.content,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(FluentTheme.colors.stroke.divider.default),
    )
}
