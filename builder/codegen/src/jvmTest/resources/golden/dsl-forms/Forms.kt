package com.example.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.ContrastThreshold
import com.materialkolor.ktx.harmonize
import com.materialkolor.ktx.onTone
import com.materialkolor.material3.DynamicMaterialExpressiveTheme
import com.materialkolor.palettes.TonalPalette

val SeedColor = Color(0xFF6750A4)

val ModifyScheme: (ColorScheme) -> ColorScheme = { scheme -> scheme.copy(primary = SeedColor) }

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
)

class ExtendedColors(
    val brand: ColorFamily,
    val isDark: Boolean = false,
) {
    val accent: Color = if (isDark) Color(0xFFB3E5FC) else Color(0xFF01579B)

    fun onBrand(palette: TonalPalette): Color {
        val tone = if (isDark) 80 else 40
        return palette.onTone(tone, ContrastThreshold.WCAG_AA_NORMAL_TEXT)
    }
}

internal object ThemeDefaults {
    val Style = PaletteStyle.Expressive
}

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColors> { error("No ExtendedColors provided") }

internal val ColorScheme.brand: Color
    get() = primary.harmonize(SeedColor)

private fun Color.toFamily(isDark: Boolean): ColorFamily {
    val onColor = when {
        isDark -> Color(0xFF000000)
        else -> Color(0xFFFFFFFF)
    }
    return ColorFamily(color = this, onColor = onColor)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val brand = MaterialTheme.colorScheme.brand
    val extendedColors = remember(brand, darkTheme) {
        val family = brand.toFamily(darkTheme)
        ExtendedColors(brand = family, isDark = darkTheme)
    }
    val motionScheme = when (ThemeDefaults.Style) {
        PaletteStyle.Expressive -> MotionScheme.expressive()
        else -> MotionScheme.standard()
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        DynamicMaterialExpressiveTheme(
            seedColor = SeedColor,
            motionScheme = motionScheme,
            isDark = darkTheme,
            modifyColorScheme = { scheme ->
                scheme.copy(
                    primary = if (darkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4),
                    tertiary = if (darkTheme) {
                        extendedColors.brand.color.harmonize(scheme.tertiary)
                    } else {
                        extendedColors.brand.onColor.harmonize(scheme.tertiary)
                    },
                )
            },
            content = content,
        )
    }
}
