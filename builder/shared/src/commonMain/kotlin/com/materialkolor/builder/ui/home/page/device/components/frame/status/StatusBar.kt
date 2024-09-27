package com.materialkolor.builder.ui.home.page.device.components.frame.status

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun rememberFormattedTime(time: Long): String {
    return remember(time) {
        val date = Instant.fromEpochMilliseconds(time)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        "${date.hour}:${date.minute.toString().padStart(2, '0')}"
    }
}