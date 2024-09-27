package com.materialkolor.builder.ui.home.page.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.ui.home.LocalWindowSizeClass
import com.materialkolor.builder.ui.ktx.clickableWithoutRipple
import com.materialkolor.builder.ui.ktx.widthIsCompact
import com.materialkolor.builder.ui.theme.LocalUrlLauncher

fun Modifier.itemPadding() = padding(horizontal = 8.dp, vertical = 4.dp)

object GalleryContainerDefaults {
    val MinWidth = 200.dp
    val Width = 400.dp
    val ItemPadding = 8.dp
    val ItemPadding2 = 8.dp * 2
    val BoxPadding: Dp
        @Composable
        get() = if (LocalWindowSizeClass.current.widthIsCompact()) 8.dp else 16.dp
}

@Composable
fun GalleryContainer(
    title: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    toggle: (Boolean) -> Unit = {},
    boxPadding: Dp = GalleryContainerDefaults.BoxPadding,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .animateContentSize()
            .widthIn(min = GalleryContainerDefaults.Width)
            .padding(boxPadding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(min = GalleryContainerDefaults.Width)
                .clip(MaterialTheme.shapes.medium)
                .clickableWithoutRipple(onClick = { toggle(!expanded) }),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Thin,
                ),
            )

            val icon =
                if (expanded) Icons.Outlined.KeyboardArrowUp
                else Icons.Outlined.KeyboardArrowDown

            val text = if (expanded) "Collapse" else "Expand"

            Icon(imageVector = icon, contentDescription = text)
        }

        AnimatedVisibility(expanded) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun GalleryContainerChild(
    title: String,
    infoUrl: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val urlLauncher = LocalUrlLauncher.current
        Row(
            modifier = modifier.padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { urlLauncher.launch(infoUrl) },
                modifier = Modifier
                    .padding(4.dp)
                    .size(16.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = null)
            }

            Text(title, style = MaterialTheme.typography.bodyMedium)
        }

        content()
    }
}
