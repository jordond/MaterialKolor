package com.materialkolor.builder.ui.home.preview.gallery.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainer
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainerChild
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainerDefaults
import com.materialkolor.builder.ui.home.preview.gallery.itemPadding
import kotlinx.collections.immutable.persistentListOf

private const val buttonUrl = "https://developer.android.com/jetpack/compose/components/button"
private const val fabUrl = "https://developer.android.com/jetpack/compose/components/fab"
private const val iconButtonUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#IconButton(kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.material3.IconButtonColors,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function0)"
private const val segmentedButtonUrl =
    "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#(androidx.compose.material3.MultiChoiceSegmentedButtonRowScope).SegmentedButton(kotlin.Boolean,kotlin.Function1,androidx.compose.ui.graphics.Shape,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.material3.SegmentedButtonColors,androidx.compose.foundation.BorderStroke,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function0,kotlin.Function0)"

@Composable
fun ActionGallery(
    expanded: Boolean,
    toggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = GalleryContainerDefaults.MinWidth,
    width: Dp = GalleryContainerDefaults.Width,
    itemPadding: Dp = GalleryContainerDefaults.ItemPadding,
) {
    GalleryContainer(
        title = "Actions",
        expanded = expanded,
        toggle = toggle,
        modifier = modifier,
    ) {
        GalleryContainerChild(title = "Common buttons", infoUrl = buttonUrl) {
            CommonButtons(minWidth, width, itemPadding)
        }

        GalleryContainerChild(title = "Floating action buttons", infoUrl = fabUrl) {
            FloatingActionButtons(minWidth, width, itemPadding)
        }

        GalleryContainerChild(title = "Icon buttons", infoUrl = iconButtonUrl) {
            IconButtons(minWidth, width, itemPadding)
        }

        GalleryContainerChild(title = "Segmented buttons", infoUrl = segmentedButtonUrl) {
            SegmentedButtons(minWidth, width, itemPadding)
        }
    }
}

@Composable
private fun CommonButtons(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .widthIn(minWidth, width)
                .padding(itemPadding),
        ) {
            Row {
                ElevatedButton(
                    onClick = {},
                    content = { Text("Elevated") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                ElevatedButton(
                    onClick = {},
                    content = {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Icon")
                    },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                ElevatedButton(
                    enabled = false,
                    onClick = {},
                    content = { Text("Elevated") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
            }
            Row {
                Button(
                    onClick = {},
                    content = { Text("Filled") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                Button(
                    onClick = {},
                    content = {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Icon")
                    },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                Button(
                    enabled = false,
                    onClick = {},
                    content = { Text("Filled") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
            }
            Row {
                FilledTonalButton(
                    onClick = {},
                    content = { Text("Tonal") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                FilledTonalButton(
                    onClick = {},
                    content = {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Icon")
                    },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                FilledTonalButton(
                    enabled = false,
                    onClick = {},
                    content = { Text("Tonal") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
            }
            Row {
                OutlinedButton(
                    onClick = {},
                    content = { Text("Outlined") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                OutlinedButton(
                    onClick = {},
                    content = {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Icon")
                    },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                OutlinedButton(
                    enabled = false,
                    onClick = {},
                    content = { Text("Outlined") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
            }
            Row {
                TextButton(
                    onClick = {},
                    content = { Text("Text") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                TextButton(
                    onClick = {},
                    content = {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Icon")
                    },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
                TextButton(
                    enabled = false,
                    onClick = {},
                    content = { Text("Text") },
                    modifier = Modifier
                        .itemPadding()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FloatingActionButtons(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    OutlinedCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(itemPadding),
        ) {
            SmallFloatingActionButton(
                onClick = {},
                modifier = Modifier.padding(itemPadding),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
            ExtendedFloatingActionButton(
                onClick = {},
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Create") },
                modifier = Modifier.padding(itemPadding),
            )
            FloatingActionButton(
                onClick = {},
                modifier = Modifier.padding(itemPadding),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
            LargeFloatingActionButton(
                onClick = {},
                modifier = Modifier.padding(itemPadding),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }
}

@Composable
private fun IconButtons(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    OutlinedCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(itemPadding),
        ) {
            IconButton(
                onClick = {},
                enabled = true,
                content = {
                    Icon(Icons.Default.Settings, contentDescription = null)
                },
            )
            IconButton(
                onClick = {},
                enabled = false,
                content = {
                    Icon(Icons.Default.Settings, contentDescription = null)
                },
            )
        }
    }
}

@Composable
private fun SegmentedButtons(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {

    OutlinedCard {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(itemPadding),
        ) {
            var selectedIndex by remember { mutableIntStateOf(0) }
            val options = remember { persistentListOf("Day", "Month", "Week") }
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size,
                        ),
                        onClick = { selectedIndex = index },
                        selected = index == selectedIndex,
                        label = { Text(label) },
                    )
                }
            }

            val checkedList = remember { mutableStateListOf<Int>() }
            val options2 = listOf("Favorites", "Trending", "Saved")
            val icons = remember {
                persistentListOf(
                    Icons.Filled.StarBorder,
                    Icons.AutoMirrored.Filled.TrendingUp,
                    Icons.Filled.BookmarkBorder,
                )
            }

            MultiChoiceSegmentedButtonRow {
                options2.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size,
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = index in checkedList) {
                                Icon(
                                    imageVector = icons[index],
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        onCheckedChange = {
                            if (index in checkedList) {
                                checkedList.remove(index)
                            } else {
                                checkedList.add(index)
                            }
                        },
                        checked = index in checkedList,
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}
