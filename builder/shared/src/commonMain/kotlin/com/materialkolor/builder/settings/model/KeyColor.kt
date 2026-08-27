package com.materialkolor.builder.settings.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

enum class KeyColor {
    Seed,
    Primary,
    Secondary,
    Tertiary,
    Error,
    Neutral,
    NeutralVariant,
}

@Composable
fun KeyColor.name() = remember(this) {
    when (this) {
        KeyColor.Seed -> "Seed"
        KeyColor.Primary -> "Primary"
        KeyColor.Secondary -> "Secondary"
        KeyColor.Tertiary -> "Tertiary"
        KeyColor.Error -> "Error"
        KeyColor.Neutral -> "Neutral"
        KeyColor.NeutralVariant -> "Neutral Variant"
    }
}