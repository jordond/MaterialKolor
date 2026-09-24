package com.materialkolor.sample.fluent.ui.palette

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.composefluent.FluentTheme
import io.github.composefluent.background.BackgroundSizing
import io.github.composefluent.background.Layer
import io.github.composefluent.component.Text

/**
 * Four of the Fluent tokens that follow the accent ramp, each with the token Fluent puts on top of it.
 *
 * @param[modifier] The modifier for the grid.
 */
@Composable
internal fun AccentTokens(modifier: Modifier = Modifier) {
    val colors = FluentTheme.colors
    val tokens = listOf(
        AccentToken(
            name = "Accent fill",
            tokens = "fillAccent.default with text.onAccent.primary",
            container = colors.fillAccent.default,
            content = colors.text.onAccent.primary,
        ),
        AccentToken(
            name = "Accent text",
            tokens = "text.accent.primary on background.mica.base",
            container = colors.background.mica.base,
            content = colors.text.accent.primary,
        ),
        AccentToken(
            name = "Accent acrylic",
            tokens = "background.accentAcrylic.base with text.text.primary",
            container = colors.background.accentAcrylic.base,
            content = colors.text.text.primary,
        ),
        AccentToken(
            name = "Selected text",
            tokens = "fillAccent.selectedTextBackground with text.onAccent.selectedText",
            container = colors.fillAccent.selectedTextBackground,
            content = colors.text.onAccent.selectedText,
        ),
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (pair in tokens.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (token in pair) {
                    TokenTile(
                        token = token,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private class AccentToken(
    val name: String,
    val tokens: String,
    val container: Color,
    val content: Color,
)

@Composable
private fun TokenTile(
    token: AccentToken,
    modifier: Modifier = Modifier,
) {
    Layer(
        modifier = modifier.heightIn(min = 80.dp),
        shape = FluentTheme.shapes.overlay,
        color = token.container,
        contentColor = token.content,
        border = BorderStroke(width = 1.dp, color = FluentTheme.colors.stroke.card.default),
        backgroundSizing = BackgroundSizing.InnerBorderEdge,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = token.name,
                style = FluentTheme.typography.bodyStrong,
                color = token.content,
            )
            Text(
                text = token.tokens,
                style = FluentTheme.typography.caption,
                color = token.content,
            )
        }
    }
}
