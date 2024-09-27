package com.materialkolor.builder.ui.home.page.gallery.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainer
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainerChild
import com.materialkolor.builder.ui.home.page.gallery.GalleryContainerDefaults

private const val modalBottomSheetInfoUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary?hl=en#ModalBottomSheet(kotlin.Function0,androidx.compose.ui.Modifier,androidx.compose.material3.SheetState,androidx.compose.ui.unit.Dp,androidx.compose.ui.graphics.Shape,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.Dp,androidx.compose.ui.graphics.Color,kotlin.Function0,androidx.compose.foundation.layout.WindowInsets,androidx.compose.ui.window.SecureFlagPolicy,kotlin.Function1)"

private const val cardsInfoUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#Card(androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Shape,androidx.compose.material3.CardColors,androidx.compose.material3.CardElevation,androidx.compose.foundation.BorderStroke,kotlin.Function1)"

private const val dividersInfoUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#Divider(androidx.compose.ui.Modifier,androidx.compose.ui.unit.Dp,androidx.compose.ui.graphics.Color)"

@Composable
fun ContainmentGallery(
    expanded: Boolean,
    toggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = GalleryContainerDefaults.MinWidth,
    width: Dp = GalleryContainerDefaults.Width,
    boxPadding: Dp = GalleryContainerDefaults.BoxPadding,
) {
    GalleryContainer(
        title = "Containment",
        expanded = expanded,
        toggle = toggle,
        modifier = modifier,
    ) {
        GalleryContainerChild(
            title = "Dialog / Bottom Sheet",
            infoUrl = modalBottomSheetInfoUrl,
        ) {
            DialogsDemo(minWidth, width, boxPadding)
        }

        GalleryContainerChild(
            title = "Cards",
            infoUrl = cardsInfoUrl,
        ) {
            CardsDemo(minWidth, width, boxPadding)
        }

        GalleryContainerChild(
            title = "Dividers",
            infoUrl = dividersInfoUrl,
        ) {
            DividersDemo(minWidth, width)
        }
    }
}

@Composable
private fun DividersDemo(
    minWidth: Dp,
    width: Dp,
) {
    OutlinedCard {
        Row(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(32.dp),
        ) {
            HorizontalDivider()
        }
    }
}

@Composable
private fun DialogsDemo(
    minWidth: Dp,
    width: Dp,
    boxPadding: Dp,
) {
    val openAlertDialog = remember { mutableStateOf(false) }
    var isModalSheetOpen by rememberSaveable { mutableStateOf(false) }

    OutlinedCard {
        Row(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(boxPadding),
        ) {
            OutlinedButton(
                enabled = true,
                onClick = { openAlertDialog.value = true },
                content = { Text("Show dialog", fontSize = 12.sp) },
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .weight(1f),
            )

            OutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .weight(1f),
                enabled = true,
                onClick = {
                    isModalSheetOpen = true
                },
                content = { Text("Bottom sheet", fontSize = 12.sp) },
            )
        }
    }

    if (openAlertDialog.value) {
        AlertDialog(
            title = { Text(text = "What is a dialog?") },
            text = {
                Text(
                    text = "A dialog is a type of modal window that appears in front of " +
                        "app content to provide critical information, or prompt for a decision " +
                        "to be made.",
                )
            },
            onDismissRequest = { openAlertDialog.value = false },
            confirmButton = {
                TextButton(
                    onClick = { openAlertDialog.value = false },
                    content = { Text("Okay") },
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { openAlertDialog.value = false },
                    content = { Text("Dismiss") },
                )
            },
        )
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (isModalSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { isModalSheetOpen = false },
            sheetState = bottomSheetState,
        ) {
            BottomSheetContent()
        }
    }
}

@Composable
private fun CardsDemo(
    minWidth: Dp,
    width: Dp,
    boxPadding: Dp,
) {
    @Composable
    fun CardTemplate(
        title: String,
        elevation: CardElevation,
        colors: CardColors,
        border: BorderStroke? = null,
    ) {
        Card(
            elevation = elevation,
            colors = colors,
            border = border,
            modifier = Modifier.size(width = 115.dp, height = 100.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp, 5.dp, 5.dp, 10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.BottomStart,
                    modifier = Modifier.matchParentSize(),
                ) {
                    Text(title, fontSize = 14.sp)
                }

                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier.matchParentSize(),
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            }
        }
    }

    OutlinedCard {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(boxPadding),
        ) {
            CardTemplate(
                title = "Elevated",
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.elevatedCardColors(),
            )
            Spacer(modifier = Modifier.width(16.dp))
            CardTemplate(
                title = "Filled",
                elevation = CardDefaults.cardElevation(),
                colors = CardDefaults.cardColors(),
            )
            Spacer(modifier = Modifier.width(16.dp))
            CardTemplate(
                title = "Outlined",
                elevation = CardDefaults.outlinedCardElevation(),
                colors = CardDefaults.outlinedCardColors(),
                border = BorderStroke(1.dp, Color.DarkGray),
            )
        }
    }
}

@Composable
internal fun ColumnScope.BottomSheetContent() {
    Row(modifier = Modifier.padding(vertical = 20.dp).align(Alignment.CenterHorizontally)) {
        BottomSheetButton(
            title = "Share",
            icon = Icons.Outlined.Share,
        )
        BottomSheetButton(
            title = "Add to",
            icon = Icons.Outlined.Add,
        )
        BottomSheetButton(
            title = "Trash",
            icon = Icons.Outlined.Delete,
        )
        BottomSheetButton(
            title = "Archive",
            icon = Icons.Outlined.Archive,
        )
        BottomSheetButton(
            title = "Settings",
            icon = Icons.Outlined.Settings,
        )
        BottomSheetButton(
            title = "Favorite",
            icon = Icons.Outlined.FavoriteBorder,
        )
    }
}

@Composable
private fun BottomSheetButton(
    icon: ImageVector,
    title: String,
) {
    NavigationRailItem(
        selected = false,
        onClick = {},
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        label = { Text(title) },
    )
}
