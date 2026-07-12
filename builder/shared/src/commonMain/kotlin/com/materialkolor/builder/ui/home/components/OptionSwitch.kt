package com.materialkolor.builder.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.ui.ktx.clickableWithoutRipple

@Composable
fun OptionSwitch(
    text: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    leadingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .semantics { contentDescription = "Toggle $text" }
            .fillMaxWidth()
            .clickableWithoutRipple {
                onValueChange(!value)
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingContent != null) {
                leadingContent()
                Spacer(Modifier.width(4.dp))
            }

            Text(text = text)
        }

        val tint = LocalContentColor.current
        Switch(
            checked = value,
            onCheckedChange = { onValueChange(it) },
            thumbContent = {
                if (value) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.fillMaxSize(0.8f),
                    )
                }
            },
        )
    }
}
