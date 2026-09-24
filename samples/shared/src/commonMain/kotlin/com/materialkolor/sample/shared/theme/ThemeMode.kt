package com.materialkolor.sample.shared.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Whether the theme is light, dark or whatever the system says.
 */
public enum class ThemeMode {
    /** Follow the system setting. */
    System,

    /** Always light. */
    Light,

    /** Always dark. */
    Dark,
}

/**
 * Whether this mode resolves to a dark theme right now.
 */
@Composable
@ReadOnlyComposable
public fun ThemeMode.isDark(): Boolean =
    when (this) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
