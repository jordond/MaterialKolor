package com.materialkolor.material3

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot
import kotlin.test.Test
import kotlin.test.assertEquals

class ToColorSchemeTest {
    /**
     * Upstream Java MCU output for a tonal spot light scheme seeded with 6750A4.
     */
    private val upstreamLight = mapOf(
        "background" to Color(0xFFFDF7FE),
        "error" to Color(0xFFA8364B),
        "errorContainer" to Color(0xFFF97386),
        "inverseOnSurface" to Color(0xFFA09BA1),
        "inversePrimary" to Color(0xFFD4C3FD),
        "inverseSurface" to Color(0xFF0F0D12),
        "onBackground" to Color(0xFF34313A),
        "onError" to Color(0xFFFFF7F7),
        "onErrorContainer" to Color(0xFF6E0523),
        "onPrimary" to Color(0xFFFDF7FF),
        "onPrimaryContainer" to Color(0xFF493C6C),
        "onPrimaryFixed" to Color(0xFF352857),
        "onPrimaryFixedVariant" to Color(0xFF524576),
        "onSecondary" to Color(0xFFFDF7FF),
        "onSecondaryContainer" to Color(0xFF554F63),
        "onSecondaryFixed" to Color(0xFF423C50),
        "onSecondaryFixedVariant" to Color(0xFF5F586E),
        "onSurface" to Color(0xFF34313A),
        "onSurfaceVariant" to Color(0xFF615D68),
        "onTertiary" to Color(0xFFFFF7F9),
        "onTertiaryContainer" to Color(0xFF5F3956),
        "onTertiaryFixed" to Color(0xFF4A2642),
        "onTertiaryFixedVariant" to Color(0xFF694260),
        "outline" to Color(0xFF7D7983),
        "outlineVariant" to Color(0xFFB5B0BB),
        "primary" to Color(0xFF655789),
        "primaryContainer" to Color(0xFFD4C3FD),
        "primaryFixed" to Color(0xFFD4C3FD),
        "primaryFixedDim" to Color(0xFFC6B6EE),
        "scrim" to Color(0xFF000000),
        "secondary" to Color(0xFF625C71),
        "secondaryContainer" to Color(0xFFE8DEF8),
        "secondaryFixed" to Color(0xFFE8DEF8),
        "secondaryFixedDim" to Color(0xFFDAD0EA),
        "surface" to Color(0xFFFDF7FE),
        "surfaceBright" to Color(0xFFFDF7FE),
        "surfaceContainer" to Color(0xFFF2ECF5),
        "surfaceContainerHigh" to Color(0xFFECE6F0),
        "surfaceContainerHighest" to Color(0xFFE7E0EC),
        "surfaceContainerLow" to Color(0xFFF8F1FA),
        "surfaceContainerLowest" to Color(0xFFFFFFFF),
        "surfaceDim" to Color(0xFFDED8E4),
        "surfaceTint" to Color(0xFF655789),
        "surfaceVariant" to Color(0xFFE7E0EC),
        "tertiary" to Color(0xFF7B5270),
        "tertiaryContainer" to Color(0xFFF4BFE3),
        "tertiaryFixed" to Color(0xFFF4BFE3),
        "tertiaryFixedDim" to Color(0xFFE5B2D5),
    )

    /**
     * Upstream Java MCU output for a tonal spot dark scheme seeded with 6750A4.
     */
    private val upstreamDark = mapOf(
        "background" to Color(0xFF0F0D12),
        "error" to Color(0xFFF97386),
        "errorContainer" to Color(0xFF871C34),
        "inverseOnSurface" to Color(0xFF575459),
        "inversePrimary" to Color(0xFF645980),
        "inverseSurface" to Color(0xFFFDF7FE),
        "onBackground" to Color(0xFFEAE3EF),
        "onError" to Color(0xFF490013),
        "onErrorContainer" to Color(0xFFFF97A3),
        "onPrimary" to Color(0xFF443A5F),
        "onPrimaryContainer" to Color(0xFFE9DEFF),
        "onPrimaryFixed" to Color(0xFF3C3256),
        "onPrimaryFixedVariant" to Color(0xFF594E74),
        "onSecondary" to Color(0xFF433D51),
        "onSecondaryContainer" to Color(0xFFC4BBD4),
        "onSecondaryFixed" to Color(0xFF423C50),
        "onSecondaryFixedVariant" to Color(0xFF5F586E),
        "onSurface" to Color(0xFFEAE3EF),
        "onSurfaceVariant" to Color(0xFFAEA9B4),
        "onTertiary" to Color(0xFF69415F),
        "onTertiaryContainer" to Color(0xFF5F3956),
        "onTertiaryFixed" to Color(0xFF4A2642),
        "onTertiaryFixedVariant" to Color(0xFF694260),
        "outline" to Color(0xFF78737E),
        "outlineVariant" to Color(0xFF4A4650),
        "primary" to Color(0xFFCDC0EC),
        "primaryContainer" to Color(0xFF574D72),
        "primaryFixed" to Color(0xFFDED0FE),
        "primaryFixedDim" to Color(0xFFD0C3EF),
        "scrim" to Color(0xFF000000),
        "secondary" to Color(0xFFCBC2DB),
        "secondaryContainer" to Color(0xFF3E384C),
        "secondaryFixed" to Color(0xFFE8DEF8),
        "secondaryFixedDim" to Color(0xFFDAD0EA),
        "surface" to Color(0xFF0F0D12),
        "surfaceBright" to Color(0xFF2E2B34),
        "surfaceContainer" to Color(0xFF1B181F),
        "surfaceContainerHigh" to Color(0xFF211E26),
        "surfaceContainerHighest" to Color(0xFF27242D),
        "surfaceContainerLow" to Color(0xFF141218),
        "surfaceContainerLowest" to Color(0xFF000000),
        "surfaceDim" to Color(0xFF0F0D12),
        "surfaceTint" to Color(0xFFCDC0EC),
        "surfaceVariant" to Color(0xFF27242D),
        "tertiary" to Color(0xFFFFCFEF),
        "tertiaryContainer" to Color(0xFFF4BFE3),
        "tertiaryFixed" to Color(0xFFF4BFE3),
        "tertiaryFixedDim" to Color(0xFFE5B2D5),
    )

    @Test
    fun toColorScheme_mapsEveryRoleToItsFieldInALightScheme() {
        assertEquals(upstreamLight, tonalSpot(isDark = false).toColorScheme().fields())
    }

    @Test
    fun toColorScheme_mapsEveryRoleToItsFieldInADarkScheme() {
        assertEquals(upstreamDark, tonalSpot(isDark = true).toColorScheme().fields())
    }

    private fun tonalSpot(isDark: Boolean): DynamicScheme =
        SchemeTonalSpot(
            Hct.fromInt(0xFF6750A4.toInt()),
            isDark,
            0.0,
            ColorSpec.SpecVersion.SPEC_2025,
            DynamicScheme.Platform.PHONE,
        )
}
