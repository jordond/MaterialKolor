package com.materialkolor.builder.ui.home.preview.device.components.frame.status

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.ui.home.preview.device.components.frame.PhotoFrameScope
import com.materialkolor.builder.ui.ktx.clickableWithoutRipple
import com.materialkolor.builder.ui.theme.icons.BatteryFullAlt
import kotlin.time.Clock

@Suppress("UnusedReceiverParameter")
@Composable
fun PhotoFrameScope.IPhoneStatusBar(
    time: Long = Clock.System.now().toEpochMilliseconds(),
    modifier: Modifier = Modifier,
) {
    val formattedTime = rememberFormattedTime(time)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
        ) {
            Text(
                text = formattedTime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }

        DynamicIsland(modifier = Modifier.padding(top = 16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.NetworkCell,
                contentDescription = "Cell Signal",
                modifier = Modifier.size(16.dp),
            )
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "WiFi",
                modifier = Modifier.size(16.dp),
            )
            Icon(
                imageVector = Icons.BatteryFullAlt,
                contentDescription = "Battery",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun DynamicIsland(
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = if (expanded) 175.dp else 125.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(width)
                .height(37.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
                .padding(4.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(200.dp)
                .height(60.dp)
                .clickableWithoutRipple { expanded = !expanded },
        )
    }
}
