package com.materialkolor.builder.ui.home.page.device.components.frame.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalWifiStatusbar4Bar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.ui.home.page.device.components.frame.PhotoFrameScope
import kotlinx.datetime.Clock

@Suppress("UnusedReceiverParameter")
@Composable
fun PhotoFrameScope.AndroidStatusBar(
    time: Long = Clock.System.now().toEpochMilliseconds(),
    modifier: Modifier = Modifier,
) {
    val formattedTime = rememberFormattedTime(time)

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = formattedTime,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiStatusbar4Bar,
                contentDescription = "Wifi",
                modifier = Modifier.size(16.dp),
            )

            Icon(
                Icons.Default.SignalCellular4Bar,
                contentDescription = "Signal",
                modifier = Modifier.size(16.dp),
            )

            Icon(
                imageVector = Icons.Default.BatteryFull,
                contentDescription = "Battery",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
