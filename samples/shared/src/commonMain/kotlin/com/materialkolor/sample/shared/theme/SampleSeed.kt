package com.materialkolor.sample.shared.theme

import androidx.compose.ui.graphics.Color

/**
 * Violet is the Material baseline and the starting seed. Blue is the Windows accent. The rest are spread
 * around the hue wheel, and Slate is there to show what a nearly gray seed does.
 */
public enum class SampleSeed(
    public val color: Color,
) {
    Violet(Color(0xFF6750A4)),
    Blue(Color(0xFF0078D4)),
    Teal(Color(0xFF00695C)),
    Forest(Color(0xFF2E7D32)),
    Amber(Color(0xFFF0A202)),
    Crimson(Color(0xFFB3261E)),
    Slate(Color(0xFF7A7A7E)),
}
