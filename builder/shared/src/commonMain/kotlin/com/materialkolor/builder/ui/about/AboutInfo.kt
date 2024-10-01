package com.materialkolor.builder.ui.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.materialkolor.builder.ui.LocalWindowSizeClass

@Composable
fun AboutInfo(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = LocalWindowSizeClass.current,
) {
    if (!visible) return

    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> {
            AboutDialog(
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }
        else -> {
            AboutBottomSheet(
                onDismiss = onDismiss,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.75f)
                    .fillMaxHeight(0.9f)
                    .padding(16.dp),
            ) {
                Column {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(8.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }

                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        AboutInfoContent()
                    }
                }
            }
        },
    )
}

@Composable
private fun AboutBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        modifier = modifier,
    ) {
        Box {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    AboutInfoContent()
                }
            }
        }
    }
}
