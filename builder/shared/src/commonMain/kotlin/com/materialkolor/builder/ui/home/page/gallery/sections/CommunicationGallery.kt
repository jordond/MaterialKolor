package com.materialkolor.builder.ui.home.page.gallery.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.ui.home.LocalSnackbarHostState
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainer
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainerChild
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainerDefaults
import kotlinx.coroutines.launch

@Composable
fun CommunicationGallery(
    expanded: Boolean,
    toggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = GalleryContainerDefaults.MinWidth,
    width: Dp = GalleryContainerDefaults.Width,
    boxPadding: Dp = GalleryContainerDefaults.BoxPadding,
) {
    GalleryContainer(
        title = "Communication",
        expanded = expanded,
        toggle = toggle,
        modifier = modifier,
    ) {
        GalleryContainerChild(
            title = "Badges",
            infoUrl = "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#Badge(androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,kotlin.Function1)",
        ) {
            Badges(minWidth, width, boxPadding)
        }

        GalleryContainerChild(
            title = "Progress indicators",
            infoUrl = "https://developer.android.com/jetpack/compose/components/progress",
        ) {
            ProgressIndicators(minWidth, width, boxPadding)
        }

        GalleryContainerChild(
            title = "Snackbar",
            infoUrl = "https://developer.android.com/jetpack/compose/components/snackbar",
        ) {
            SnackbarButton(minWidth, width, boxPadding)
        }
    }
}

@Composable
private fun Badges(
    minWidth: Dp,
    width: Dp,
    boxPadding: Dp,
) {
    OutlinedCard {
        Box(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(boxPadding),
        ) {
            var selected by remember { mutableStateOf(0) }
            NavigationBar {
                NavigationBarItem(
                    selected = selected == 0,
                    onClick = { selected = 0 },
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge { Text("999+") }
                            },
                        ) {
                            Icon(
                                imageVector = if (selected == 0) Icons.Default.Mail else Icons.Default.MailOutline,
                                contentDescription = null,
                            )
                        }
                    },
                    label = { Text("Mail") },
                )
                NavigationBarItem(
                    selected = selected == 1,
                    onClick = { selected = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge { Text("10") }
                            },
                        ) {
                            Icon(
                                imageVector = if (selected == 1) Icons.Default.ChatBubble else Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                            )
                        }
                    },
                    label = { Text("Chat") },
                )
                NavigationBarItem(
                    selected = selected == 2,
                    onClick = { selected = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge()
                            },
                        ) {
                            Icon(
                                imageVector = if (selected == 2) Icons.Default.People else Icons.Default.PeopleOutline,
                                contentDescription = null,
                            )
                        }
                    },
                    label = { Text("Room") },
                )
                NavigationBarItem(
                    selected = selected == 3,
                    onClick = { selected = 3 },
                    icon = {
                        BadgedBox(
                            badge = {
                                Badge { Text("3") }
                            },
                        ) {
                            Icon(
                                imageVector = if (selected == 3) Icons.Default.Videocam else Icons.Outlined.Videocam,
                                contentDescription = null,
                            )
                        }
                    },
                    label = { Text("Meet") },
                )
            }
        }
    }
}

@Composable
private fun ProgressIndicators(
    minWidth: Dp,
    width: Dp,
    boxPadding: Dp,
) {
    OutlinedCard {
        Row(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(boxPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            var isProgress by remember { mutableStateOf(false) }
            IconButton(
                onClick = { isProgress = !isProgress },
            ) {
                Icon(
                    imageVector = if (isProgress) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
            if (isProgress) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.size(16.dp))
                LinearProgressIndicator(modifier = Modifier.weight(1f))
            } else {
                CircularProgressIndicator(progress = { 0.7f })
                Spacer(modifier = Modifier.size(16.dp))
                LinearProgressIndicator(
                    progress = { 0.7f },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SnackbarButton(
    minWidth: Dp,
    width: Dp,
    boxPadding: Dp,
) {
    OutlinedCard {
        Box(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(boxPadding),
            contentAlignment = Alignment.Center,
        ) {
            val coroutineScope = rememberCoroutineScope()
            val snackbarHostState = LocalSnackbarHostState.current
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = "This is a snackbar",
                            actionLabel = "Close",
                        )
                    }
                },
            ) {
                Text("Show snackbar")
            }
        }
    }
}
