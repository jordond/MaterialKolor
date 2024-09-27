package com.materialkolor.builder.ui.home.page.gallery.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.materialkolor.builder.ui.home.LocalDrawerState
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainer
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainerChild
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainerDefaults
import com.materialkolor.builder.ui.home.page.gallery.NavigationDrawerContent
import kotlinx.coroutines.launch

@Composable
fun NavigationGallery(
    expanded: Boolean,
    toggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = GalleryContainerDefaults.MinWidth,
    width: Dp = GalleryContainerDefaults.Width,
    boxPadding: Dp = GalleryContainerDefaults.BoxPadding,
) {
    GalleryContainer(
        title = "Navigation",
        expanded = expanded,
        toggle = toggle,
        modifier = modifier,
    ) {
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                GalleryContainerChild(
                    title = "Bottom app bar",
                    infoUrl = "https://developer.android.com/jetpack/compose/components/app-bars#bottom",
                ) {
                    GalleryBottomAppBar(minWidth, width, boxPadding)
                }

                GalleryContainerChild(
                    title = "Navigation bar",
                    infoUrl = "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#navigationbar",
                ) {
                    GalleryNavigationBar(minWidth, width, boxPadding)
                }

                GalleryContainerChild(
                    title = "Tabs",
                    infoUrl = "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#Tab(kotlin.Boolean,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,kotlin.Function0,kotlin.Function0,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.foundation.interaction.MutableInteractionSource)",
                ) {
                    GalleryTabs(minWidth, width, boxPadding)
                }
            }

            GalleryContainerChild(
                title = "Navigation drawer",
                infoUrl = "https://developer.android.com/jetpack/compose/components/drawer",
            ) {
                GalleryNavigationDrawer(minWidth, width, boxPadding)
            }

            GalleryContainerChild(
                title = "Navigation rail",
                infoUrl = "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#navigationrail",
            ) {
                GalleryNavigationRail(minWidth, width, boxPadding)
            }

            GalleryContainerChild(
                title = "Top app bars",
                infoUrl = "https://developer.android.com/jetpack/compose/components/app-bars",
            ) {
                GalleryTopAppBars(minWidth, width, boxPadding)
            }
        }
    }
}

@Composable
private fun GalleryBottomAppBar(
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
            BottomAppBar(
                actions = {
                    IconButton(onClick = { /* do something */ }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = null)
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { /* do something */ },
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                    }
                },
            )
        }
    }
}

@Composable
private fun GalleryNavigationBar(
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
                        Icon(
                            imageVector = if (selected == 0) Icons.Default.Explore else Icons.Outlined.Explore,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Explore") },
                )
                NavigationBarItem(
                    selected = selected == 1,
                    onClick = { selected = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selected == 1) Icons.Default.Pets else Icons.Outlined.Pets,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Pets") },
                )
                NavigationBarItem(
                    selected = selected == 2,
                    onClick = { selected = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selected == 2) Icons.Default.AccountBox else Icons.Outlined.AccountBox,
                            contentDescription = null,
                        )
                    },
                    label = { Text("Account") },
                )
            }
        }
    }
}

@Composable
private fun GalleryNavigationDrawer(
    minWidth: Dp,
    width: Dp,
    boxPadding: Dp,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(boxPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                border = BorderStroke(DividerDefaults.Thickness.value.dp, DividerDefaults.color),
            ) {
                PermanentNavigationDrawer(
                    modifier = Modifier.width(300.dp),
                    drawerContent = {
                        PermanentDrawerSheet { NavigationDrawerContent() }
                    },
                ) {}
            }
            val drawerState = LocalDrawerState.current
            val coroutineScope = rememberCoroutineScope()
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        if (drawerState.isClosed) {
                            drawerState.open()
                        } else {
                            drawerState.close()
                        }
                    }
                },
                content = { Text("Show modal navigation drawer") },
            )
        }
    }
}

@Composable
private fun GalleryNavigationRail(
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
            Surface(
                shadowElevation = 8.dp,
            ) {
                var selected by remember { mutableStateOf(0) }
                NavigationRail(
                    modifier = Modifier.padding(6.dp),
                    header = {
                        FloatingActionButton(
                            onClick = { /* do something */ },
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                    },
                ) {
                    Spacer(Modifier.size(40.dp))
                    NavigationRailItem(
                        selected = selected == 0,
                        onClick = { selected = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selected == 0) Icons.Default.Inbox else Icons.Outlined.Inbox,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Inbox") },
                    )
                    NavigationRailItem(
                        selected = selected == 1,
                        onClick = { selected = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selected == 1) Icons.AutoMirrored.Filled.Send else Icons.AutoMirrored.Outlined.Send,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Outbox") },
                    )
                    NavigationRailItem(
                        selected = selected == 2,
                        onClick = { selected = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selected == 2) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Favorite") },
                    )
                    NavigationRailItem(
                        selected = selected == 3,
                        onClick = { selected = 3 },
                        icon = {
                            Icon(
                                imageVector = if (selected == 3) Icons.Default.Delete else Icons.Outlined.Delete,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Trash") },
                    )
                    Spacer(Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
private fun GalleryTabs(
    minWidth: Dp,
    width: Dp,
    boxPadding: Dp,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(boxPadding),
        ) {
            var selected by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = selected,
            ) {
                Tab(
                    selected = selected == 0,
                    onClick = { selected = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                        )
                    },
                    text = { Text("Video") },
                )
                Tab(
                    selected = selected == 1,
                    onClick = { selected = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                        )
                    },
                    text = { Text("Photo") },
                )
                Tab(
                    selected = selected == 2,
                    onClick = { selected = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                        )
                    },
                    text = { Text("Audio") },
                )
            }
            Spacer(Modifier.size(16.dp))

            var selected2 by remember { mutableStateOf(0) }
            val items = List(10) { "Tab #$it" }
            ScrollableTabRow(
                selectedTabIndex = selected2,
            ) {
                items.forEachIndexed { i, item ->
                    Tab(
                        selected = selected2 == i,
                        onClick = { selected2 = i },
                        text = { Text(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryTopAppBars(
    minWidth: Dp,
    width: Dp,
    boxPadding: Dp,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(boxPadding),
        ) {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = { Text("CenterAligned") },
                actions = {
                    IconButton(
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                        )
                    }
                },
            )
            Spacer(Modifier.size(16.dp))

            @Composable
            fun RowScope.Actions() {
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                    )
                }
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                    )
                }
                IconButton(
                    onClick = {},
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                }
            }
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = { Text("Small") },
                actions = { Actions() },
            )
            Spacer(Modifier.size(16.dp))
            MediumTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = { Text("Medium") },
                actions = { Actions() },
            )
            Spacer(Modifier.size(16.dp))
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = {},
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                title = { Text("Large") },
                actions = { Actions() },
            )
        }
    }
}
