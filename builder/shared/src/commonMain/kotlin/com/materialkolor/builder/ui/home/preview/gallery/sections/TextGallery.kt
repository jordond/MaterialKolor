package com.materialkolor.builder.ui.home.preview.gallery.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainer
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainerChild
import com.materialkolor.builder.ui.home.preview.gallery.GalleryContainerDefaults

@Composable
fun TextGallery(
    expanded: Boolean,
    toggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minWidth: Dp = GalleryContainerDefaults.MinWidth,
    width: Dp = GalleryContainerDefaults.Width,
    itemPadding: Dp = GalleryContainerDefaults.ItemPadding,
) {
    GalleryContainer(
        title = "Text inputs",
        expanded = expanded,
        toggle = toggle,
        modifier = modifier,
    ) {
        GalleryContainerChild(
            title = "Text fields",
            infoUrl = "https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#textfield",
        ) {
            TextFields(minWidth, width, itemPadding)
        }
    }
}

@Composable
private fun TextFields(
    minWidth: Dp,
    width: Dp,
    itemPadding: Dp,
) {
    OutlinedCard {
        Column(
            modifier = Modifier
                .requiredWidthIn(minWidth)
                .width(width)
                .padding(16.dp),
        ) {
            var text by remember { mutableStateOf("") }
            TextField(
                singleLine = true,
                value = text,
                onValueChange = { text = it },
                label = { Text("Filled") },
                supportingText = { Text("supporting text") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    IconButton(
                        onClick = { text = "" },
                        content = { Icon(Icons.Default.Close, null) },
                    )
                },
                placeholder = { Text("placeholder") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(itemPadding),
            )
            Row {
                TextField(
                    singleLine = true,
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Filled", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    supportingText = { Text("error text") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { text = "" },
                            content = { Icon(Icons.Default.Close, null) },
                        )
                    },
                    placeholder = { Text("placeholder", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    isError = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(itemPadding),
                )
                TextField(
                    singleLine = true,
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Disabled", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    supportingText = { Text("supporting text") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { text = "" },
                            content = { Icon(Icons.Default.Close, null) },
                        )
                    },
                    placeholder = { Text("placeholder", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    enabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .padding(itemPadding),
                )
            }
            var text2 by remember { mutableStateOf("") }
            OutlinedTextField(
                singleLine = true,
                value = text2,
                onValueChange = { text2 = it },
                label = { Text("Outlined") },
                supportingText = { Text("supporting text") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    IconButton(
                        onClick = { text2 = "" },
                        content = { Icon(Icons.Default.Close, null) },
                    )
                },
                placeholder = { Text("placeholder") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(itemPadding),
            )
            Row {
                OutlinedTextField(
                    singleLine = true,
                    value = text2,
                    onValueChange = { text2 = it },
                    label = { Text("Outlined", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    supportingText = { Text("error text") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { text2 = "" },
                            content = { Icon(Icons.Default.Close, null) },
                        )
                    },
                    placeholder = { Text("placeholder", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    isError = true,
                    modifier = Modifier.weight(1f).padding(itemPadding),
                )
                OutlinedTextField(
                    singleLine = true,
                    value = text2,
                    onValueChange = { text2 = it },
                    label = { Text("Disabled", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    supportingText = { Text("supporting text") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { text2 = "" },
                            content = { Icon(Icons.Default.Close, null) },
                        )
                    },
                    placeholder = { Text("placeholder", overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    enabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .padding(itemPadding),
                )
            }
        }
    }
}
