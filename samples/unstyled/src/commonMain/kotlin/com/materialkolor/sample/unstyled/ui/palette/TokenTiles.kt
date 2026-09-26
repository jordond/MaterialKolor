package com.materialkolor.sample.unstyled.ui.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.sample.unstyled.theme.Gradient
import com.materialkolor.sample.unstyled.theme.GradientTokens
import com.materialkolor.sample.unstyled.theme.ShadowStyle
import com.materialkolor.sample.unstyled.theme.ShadowTokens
import com.materialkolor.sample.unstyled.theme.ShapeTokens
import com.materialkolor.sample.unstyled.theme.Spacing
import com.materialkolor.sample.unstyled.theme.TasksType
import com.materialkolor.sample.unstyled.theme.brush
import com.materialkolor.sample.unstyled.theme.color
import com.materialkolor.sample.unstyled.theme.shadow
import com.materialkolor.sample.unstyled.theme.shape
import com.materialkolor.sample.unstyled.ui.component.raised
import com.materialkolor.sample.unstyled.ui.component.sunken
import com.materialkolor.unstyled.MaterialKolorTokens
import kotlin.math.roundToInt

@Composable
internal fun GradientTile(
    token: ThemeToken<Gradient>,
    modifier: Modifier = Modifier,
) {
    val gradient = Theme[GradientTokens.gradients][token]
    val shape = ShapeTokens.card.shape
    TokenTile(
        name = token.name,
        details = listOf(gradient.start.name, "to ${gradient.end.name}"),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(token.brush, shape)
                .border(width = 1.dp, color = MaterialKolorTokens.outlineVariant.color, shape = shape),
        )
    }
}

@Composable
internal fun ShadowTile(
    token: ThemeToken<ShadowStyle>,
    modifier: Modifier = Modifier,
) {
    val style = Theme[ShadowTokens.shadows][token]
    val shape = ShapeTokens.card.shape
    val surface = if (token == ShadowTokens.inset) {
        Modifier.sunken(shape)
    } else {
        Modifier.raised(shape = shape, shadow = token.shadow)
    }
    TokenTile(
        name = token.name,
        details = listOf(style.color.name, "${style.radius.value.roundToInt()}dp blur"),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .then(surface),
        )
    }
}

@Composable
internal fun ShapeTile(
    token: ThemeToken<Shape>,
    modifier: Modifier = Modifier,
) {
    TokenTile(
        name = token.name,
        details = emptyList(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialKolorTokens.primaryContainer.color, token.shape),
        )
    }
}

@Composable
private fun TokenTile(
    name: String,
    details: List<String>,
    modifier: Modifier,
    sample: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        modifier = modifier,
    ) {
        sample()

        Column {
            Text(text = name, style = TasksType.Label, maxLines = 1)

            for (detail in details) {
                Text(
                    text = detail,
                    style = TasksType.Small,
                    color = MaterialKolorTokens.onSurfaceVariant.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
