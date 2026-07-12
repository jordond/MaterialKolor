package com.materialkolor.builder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.settings.model.Settings
import com.materialkolor.builder.ui.LocalWindowSizeClass
import com.materialkolor.builder.ui.ktx.widthIsCompact

@Composable
fun AppTopBar(
    settings: Settings,
    toggleDarkMode: () -> Unit,
    toggleAboutDialog: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    windowSizeClass: WindowSizeClass = LocalWindowSizeClass.current,
) {
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            AnimatedVisibility(visible = showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = "MaterialKolor Builder",
                    tint = MaterialTheme.colorScheme.primary,
                )

                val text = if (windowSizeClass.widthIsCompact()) "MKB"
                else "MaterialKolor Builder"

                Text(
                    text = text,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        },
        actions = {
            TopBarActions(
                settings = settings,
                onToggleDarkMode = toggleDarkMode,
                onAboutClicked = { toggleAboutDialog(true) },
            )
        },
    )
}
