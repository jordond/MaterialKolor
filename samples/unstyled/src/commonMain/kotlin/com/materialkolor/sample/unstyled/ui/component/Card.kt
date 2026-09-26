package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.materialkolor.sample.unstyled.theme.Shapes
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.unstyled.MaterialKolorTokens

@Composable
internal fun Card(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        content = content,
        modifier = modifier
            .clip(Shapes.Card)
            .background(MaterialKolorTokens.surfaceContainerLow.color)
            .border(width = 1.dp, color = MaterialKolorTokens.outlineVariant.color, shape = Shapes.Card),
    )
}
