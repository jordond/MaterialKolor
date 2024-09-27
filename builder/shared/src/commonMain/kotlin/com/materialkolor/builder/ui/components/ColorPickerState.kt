package com.materialkolor.builder.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.materialkolor.builder.settings.model.KeyColor

@Stable
data class ColorPickerState(
    val keyColor: KeyColor,
    val initial: Color,
    val mode: ColorPickerMode = ColorPickerMode.HSV,
    val image: ImageBitmap? = null,
    val loading: Boolean = false,
) {

    fun toggleMode(): ColorPickerState = copy(
        mode = if (mode == ColorPickerMode.HSV) ColorPickerMode.Image else ColorPickerMode.HSV,
    )
}

enum class ColorPickerMode {
    HSV,
    Image,
}
