package com.materialkolor.builder.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.core.UrlLink
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.theme.LocalUrlLauncher
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Github

@Composable
fun RowScope.TopBarActions(
    settings: Settings,
    onReset: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onAboutClicked: () -> Unit,
) {
    val urlLauncher = LocalUrlLauncher.current

    AnimatedVisibility(visible = settings.isModified) {
        IconButton(onClick = onReset) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Undo,
                contentDescription = "Reset to default",
            )
        }
    }

    IconButton(onClick = onToggleDarkMode) {
        val icon =
            if (settings.isDarkMode) Icons.Outlined.LightMode
            else Icons.Outlined.DarkMode

        Icon(
            imageVector = icon,
            contentDescription = if (settings.isDarkMode) "Light mode" else "Dark mode",
        )
    }

    IconButton(onClick = onAboutClicked) {
        Icon(
            imageVector = Icons.Outlined.QuestionMark,
            contentDescription = "About MaterialKolor Builder",
        )
    }

    IconButton(onClick = { urlLauncher.launch(UrlLink.Github) }) {
        Icon(
            imageVector = FontAwesomeIcons.Brands.Github,
            contentDescription = "MaterialKolor source code on GitHub",
            modifier = Modifier.size(24.dp),
        )
    }
}
