package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.DeviceWidth

/**
 * How wide an app screen is laid out for this device, one width inside each window class so the
 * screen re-lays itself out rather than scaling.
 */
public val DeviceWidth.screenWidth: Dp
    get() = when (this) {
        DeviceWidth.Phone -> 412.dp
        DeviceWidth.Tablet -> 840.dp
        DeviceWidth.Desktop -> 1280.dp
    }
