package com.materialkolor.builder.ui.home.page.gallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf

@Immutable
private data class DrawerItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val mail = persistentListOf(
    DrawerItem("Inbox", Icons.Default.Inbox, Icons.Outlined.Inbox),
    DrawerItem("Outbox", Icons.AutoMirrored.Default.Send, Icons.AutoMirrored.Outlined.Send),
    DrawerItem("Favorite", Icons.Default.Favorite, Icons.Default.FavoriteBorder),
    DrawerItem("Trash", Icons.Default.Delete, Icons.Outlined.Delete),
)

private val labels = persistentListOf(
    DrawerItem("Family", Icons.Default.Bookmark, Icons.Default.BookmarkBorder),
    DrawerItem("School", Icons.Default.Bookmark, Icons.Default.BookmarkBorder),
    DrawerItem("Work", Icons.Default.Bookmark, Icons.Default.BookmarkBorder),
)

@Composable
fun NavigationDrawerContent() {
    Column(
        modifier = Modifier.padding(8.dp),
    ) {
        var selected by remember { mutableIntStateOf(0) }

        Text(text = "Mail", modifier = Modifier.padding(16.dp))
        mail.forEachIndexed { index, item ->
            NavigationDrawerItem(
                selected = selected == index,
                onClick = { selected = index },
                icon = {
                    val icon = if (selected == index) item.selectedIcon else item.unselectedIcon
                    Icon(
                        imageVector = icon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
            )
        }

        HorizontalDivider(Modifier.padding(8.dp))

        Text(text = "Labels", modifier = Modifier.padding(16.dp))
        labels.forEachIndexed { index, item ->
            NavigationDrawerItem(
                selected = selected == index + 4,
                onClick = { selected = index + 4 },
                icon = {
                    val icon = if (selected == index + 4) item.selectedIcon else item.unselectedIcon
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                    )
                },
                label = { Text(item.label) },
            )
        }
    }
}
