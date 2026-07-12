package com.materialkolor.builder.ui.home.preview.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.ui.home.preview.device.components.content.PhoneContent
import com.materialkolor.builder.ui.home.preview.device.components.content.PhoneContentScreen
import com.materialkolor.builder.ui.home.preview.device.components.frame.AndroidPhoneFrame
import com.materialkolor.builder.ui.home.preview.device.components.frame.IosPhoneFrame

@Composable
fun DeviceSection(
    modifier: Modifier = Modifier,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.fillMaxWidth(),
    ) {
        AndroidPhoneFrame {
            PhoneContent(
                screen = PhoneContentScreen.Home,
            )
        }

        IosPhoneFrame {
            PhoneContent(
                screen = PhoneContentScreen.Details,
            )
        }
    }
}
