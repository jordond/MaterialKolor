package com.materialkolor.builder.ui.home.preview.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.materialkolor.builder.ui.components.CopyIcon
import com.materialkolor.builder.ui.home.model.ThemeColor
import com.materialkolor.builder.ui.home.model.ThemeGroup
import com.materialkolor.builder.ui.home.model.ThemePair
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.BoxPadding
import com.materialkolor.builder.ui.home.preview.theme.ThemeSectionDefaults.InnerDivider

@Composable
fun ColorGroupContainer(
    group: ThemeGroup,
    onClick: (String, Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(InnerDivider),
        modifier = modifier,
    ) {
        ColorPairContainer(group.main, onClick)
        ColorPairContainer(group.container, onClick)
    }
}

@Composable
fun ColorPairContainer(
    pair: ThemePair,
    onClick: (String, Color) -> Unit,
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    Column(
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalContentColor provides pair.onColor.color()) {
            ColorBox(
                themeColor = pair.color,
                onClick = onClick,
                lines = lines,
                modifier = Modifier.fillMaxSize(),
            )
        }

        CompositionLocalProvider(LocalContentColor provides pair.color.color()) {
            SingleLineColorBox(
                themeColor = pair.onColor,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun ColorBox(
    themeColor: ThemeColor,
    onClick: (String, Color) -> Unit,
    lines: Int = 3,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
) {
    val color = themeColor.color()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    ContentColorWrapper(themeColor, textColor) {
        Box(
            modifier
                .background(themeColor.color())
                .hoverable(interactionSource)
                .clickable(onClick = { onClick(themeColor.title, color) })
                .padding(BoxPadding),
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = themeColor.title,
                    minLines = lines,
                    maxLines = lines,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(text = themeColor.swatchNumber, modifier = Modifier.align(Alignment.End))
            }

            CopyIcon(visble = isHovered, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
fun SingleLineColorBox(
    themeColor: ThemeColor,
    onClick: (String, Color) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
) {
    val color = themeColor.color()

    ContentColorWrapper(themeColor, textColor) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .background(themeColor.color())
                .clickable(onClick = { onClick(themeColor.title, color) })
                .padding(BoxPadding),
        ) {
            Text(
                text = themeColor.title,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = themeColor.swatchNumber,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

@Composable
private fun ContentColorWrapper(
    themeColor: ThemeColor,
    textColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val contentColor by animateColorAsState(
        textColor ?: contentColorFor(themeColor.color()),
    )

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        content()
    }
}
