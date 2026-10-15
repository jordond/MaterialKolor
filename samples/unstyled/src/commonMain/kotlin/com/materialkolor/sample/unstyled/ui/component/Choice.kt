package com.materialkolor.sample.unstyled.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
internal data class Choice<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val badge: String? = null,
)
