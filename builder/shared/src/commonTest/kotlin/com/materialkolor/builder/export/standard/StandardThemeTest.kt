package com.materialkolor.builder.export.standard

import androidx.compose.ui.graphics.Color
import com.materialkolor.Contrast
import com.materialkolor.builder.export.model.header
import com.materialkolor.builder.export.model.standard.standardThemeKt
import com.materialkolor.builder.settings.model.ColorSettings
import com.materialkolor.builder.settings.model.Settings
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class StandardThemeTest {

    @Test
    fun testStandardMultiplatformTheme() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF0000FF),
            ),
            isDarkMode = false,
            selectedImage = null,
        )

        val result = standardThemeKt(
            packageName = "com.example",
            themeName = "AppTheme",
            multiplatform = true,
            settings = settings,
        )

        val expected = """
${header(settings)}
package com.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val lightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

private val darkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit,
) {
    val colorScheme = when {
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
""".trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun testStandardAndroidTheme() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF0000FF),
            ),
            isDarkMode = false,
            selectedImage = null,
        )

        val result = standardThemeKt(
            packageName = "com.example",
            themeName = "AppTheme",
            multiplatform = false,
            settings = settings,
        )

        val expected = """
${header(settings)}
package com.example

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val lightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    surfaceDim = SurfaceDimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

private val darkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    surfaceDim = SurfaceDimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
        """.trimIndent()

        assertEquals(expected, result)
    }

    @Test
    fun testStandardReducedContrastTheme() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF0000FF),
            ),
            isDarkMode = false,
            contrast = Contrast.Reduced,
            selectedImage = null,
        )

        val result = standardThemeKt(
            packageName = "com.example",
            themeName = "AppTheme",
            multiplatform = false,
            settings = settings,
        )

        val colors = """
            private val reducedContrastLightColorScheme = lightColorScheme(
                primary = PrimaryLightReducedContrast,
                onPrimary = OnPrimaryLightReducedContrast,
                primaryContainer = PrimaryContainerLightReducedContrast,
                onPrimaryContainer = OnPrimaryContainerLightReducedContrast,
                secondary = SecondaryLightReducedContrast,
                onSecondary = OnSecondaryLightReducedContrast,
                secondaryContainer = SecondaryContainerLightReducedContrast,
                onSecondaryContainer = OnSecondaryContainerLightReducedContrast,
                tertiary = TertiaryLightReducedContrast,
                onTertiary = OnTertiaryLightReducedContrast,
                tertiaryContainer = TertiaryContainerLightReducedContrast,
                onTertiaryContainer = OnTertiaryContainerLightReducedContrast,
                error = ErrorLightReducedContrast,
                onError = OnErrorLightReducedContrast,
                errorContainer = ErrorContainerLightReducedContrast,
                onErrorContainer = OnErrorContainerLightReducedContrast,
                background = BackgroundLightReducedContrast,
                onBackground = OnBackgroundLightReducedContrast,
                surface = SurfaceLightReducedContrast,
                onSurface = OnSurfaceLightReducedContrast,
                surfaceVariant = SurfaceVariantLightReducedContrast,
                onSurfaceVariant = OnSurfaceVariantLightReducedContrast,
                outline = OutlineLightReducedContrast,
                outlineVariant = OutlineVariantLightReducedContrast,
                scrim = ScrimLightReducedContrast,
                inverseSurface = InverseSurfaceLightReducedContrast,
                inverseOnSurface = InverseOnSurfaceLightReducedContrast,
                inversePrimary = InversePrimaryLightReducedContrast,
                surfaceDim = SurfaceDimLightReducedContrast,
                surfaceBright = SurfaceBrightLightReducedContrast,
                surfaceContainerLowest = SurfaceContainerLowestLightReducedContrast,
                surfaceContainerLow = SurfaceContainerLowLightReducedContrast,
                surfaceContainer = SurfaceContainerLightReducedContrast,
                surfaceContainerHigh = SurfaceContainerHighLightReducedContrast,
                surfaceContainerHighest = SurfaceContainerHighestLightReducedContrast,
            )

            private val reducedContrastDarkColorScheme = darkColorScheme(
                primary = PrimaryDarkReducedContrast,
                onPrimary = OnPrimaryDarkReducedContrast,
                primaryContainer = PrimaryContainerDarkReducedContrast,
                onPrimaryContainer = OnPrimaryContainerDarkReducedContrast,
                secondary = SecondaryDarkReducedContrast,
                onSecondary = OnSecondaryDarkReducedContrast,
                secondaryContainer = SecondaryContainerDarkReducedContrast,
                onSecondaryContainer = OnSecondaryContainerDarkReducedContrast,
                tertiary = TertiaryDarkReducedContrast,
                onTertiary = OnTertiaryDarkReducedContrast,
                tertiaryContainer = TertiaryContainerDarkReducedContrast,
                onTertiaryContainer = OnTertiaryContainerDarkReducedContrast,
                error = ErrorDarkReducedContrast,
                onError = OnErrorDarkReducedContrast,
                errorContainer = ErrorContainerDarkReducedContrast,
                onErrorContainer = OnErrorContainerDarkReducedContrast,
                background = BackgroundDarkReducedContrast,
                onBackground = OnBackgroundDarkReducedContrast,
                surface = SurfaceDarkReducedContrast,
                onSurface = OnSurfaceDarkReducedContrast,
                surfaceVariant = SurfaceVariantDarkReducedContrast,
                onSurfaceVariant = OnSurfaceVariantDarkReducedContrast,
                outline = OutlineDarkReducedContrast,
                outlineVariant = OutlineVariantDarkReducedContrast,
                scrim = ScrimDarkReducedContrast,
                inverseSurface = InverseSurfaceDarkReducedContrast,
                inverseOnSurface = InverseOnSurfaceDarkReducedContrast,
                inversePrimary = InversePrimaryDarkReducedContrast,
                surfaceDim = SurfaceDimDarkReducedContrast,
                surfaceBright = SurfaceBrightDarkReducedContrast,
                surfaceContainerLowest = SurfaceContainerLowestDarkReducedContrast,
                surfaceContainerLow = SurfaceContainerLowDarkReducedContrast,
                surfaceContainer = SurfaceContainerDarkReducedContrast,
                surfaceContainerHigh = SurfaceContainerHighDarkReducedContrast,
                surfaceContainerHighest = SurfaceContainerHighestDarkReducedContrast,
            )
        """.trimIndent()

        assertContains(result, colors)
    }

    @Test
    fun testStandardMediumContrastTheme() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF0000FF),
            ),
            isDarkMode = false,
            contrast = Contrast.Medium,
            selectedImage = null,
        )

        val result = standardThemeKt(
            packageName = "com.example",
            themeName = "AppTheme",
            multiplatform = true,
            settings = settings,
        )

        val colors = """
            private val mediumContrastLightColorScheme = lightColorScheme(
                primary = PrimaryLightMediumContrast,
                onPrimary = OnPrimaryLightMediumContrast,
                primaryContainer = PrimaryContainerLightMediumContrast,
                onPrimaryContainer = OnPrimaryContainerLightMediumContrast,
                secondary = SecondaryLightMediumContrast,
                onSecondary = OnSecondaryLightMediumContrast,
                secondaryContainer = SecondaryContainerLightMediumContrast,
                onSecondaryContainer = OnSecondaryContainerLightMediumContrast,
                tertiary = TertiaryLightMediumContrast,
                onTertiary = OnTertiaryLightMediumContrast,
                tertiaryContainer = TertiaryContainerLightMediumContrast,
                onTertiaryContainer = OnTertiaryContainerLightMediumContrast,
                error = ErrorLightMediumContrast,
                onError = OnErrorLightMediumContrast,
                errorContainer = ErrorContainerLightMediumContrast,
                onErrorContainer = OnErrorContainerLightMediumContrast,
                background = BackgroundLightMediumContrast,
                onBackground = OnBackgroundLightMediumContrast,
                surface = SurfaceLightMediumContrast,
                onSurface = OnSurfaceLightMediumContrast,
                surfaceVariant = SurfaceVariantLightMediumContrast,
                onSurfaceVariant = OnSurfaceVariantLightMediumContrast,
                outline = OutlineLightMediumContrast,
                outlineVariant = OutlineVariantLightMediumContrast,
                scrim = ScrimLightMediumContrast,
                inverseSurface = InverseSurfaceLightMediumContrast,
                inverseOnSurface = InverseOnSurfaceLightMediumContrast,
                inversePrimary = InversePrimaryLightMediumContrast,
                surfaceDim = SurfaceDimLightMediumContrast,
                surfaceBright = SurfaceBrightLightMediumContrast,
                surfaceContainerLowest = SurfaceContainerLowestLightMediumContrast,
                surfaceContainerLow = SurfaceContainerLowLightMediumContrast,
                surfaceContainer = SurfaceContainerLightMediumContrast,
                surfaceContainerHigh = SurfaceContainerHighLightMediumContrast,
                surfaceContainerHighest = SurfaceContainerHighestLightMediumContrast,
            )

            private val mediumContrastDarkColorScheme = darkColorScheme(
                primary = PrimaryDarkMediumContrast,
                onPrimary = OnPrimaryDarkMediumContrast,
                primaryContainer = PrimaryContainerDarkMediumContrast,
                onPrimaryContainer = OnPrimaryContainerDarkMediumContrast,
                secondary = SecondaryDarkMediumContrast,
                onSecondary = OnSecondaryDarkMediumContrast,
                secondaryContainer = SecondaryContainerDarkMediumContrast,
                onSecondaryContainer = OnSecondaryContainerDarkMediumContrast,
                tertiary = TertiaryDarkMediumContrast,
                onTertiary = OnTertiaryDarkMediumContrast,
                tertiaryContainer = TertiaryContainerDarkMediumContrast,
                onTertiaryContainer = OnTertiaryContainerDarkMediumContrast,
                error = ErrorDarkMediumContrast,
                onError = OnErrorDarkMediumContrast,
                errorContainer = ErrorContainerDarkMediumContrast,
                onErrorContainer = OnErrorContainerDarkMediumContrast,
                background = BackgroundDarkMediumContrast,
                onBackground = OnBackgroundDarkMediumContrast,
                surface = SurfaceDarkMediumContrast,
                onSurface = OnSurfaceDarkMediumContrast,
                surfaceVariant = SurfaceVariantDarkMediumContrast,
                onSurfaceVariant = OnSurfaceVariantDarkMediumContrast,
                outline = OutlineDarkMediumContrast,
                outlineVariant = OutlineVariantDarkMediumContrast,
                scrim = ScrimDarkMediumContrast,
                inverseSurface = InverseSurfaceDarkMediumContrast,
                inverseOnSurface = InverseOnSurfaceDarkMediumContrast,
                inversePrimary = InversePrimaryDarkMediumContrast,
                surfaceDim = SurfaceDimDarkMediumContrast,
                surfaceBright = SurfaceBrightDarkMediumContrast,
                surfaceContainerLowest = SurfaceContainerLowestDarkMediumContrast,
                surfaceContainerLow = SurfaceContainerLowDarkMediumContrast,
                surfaceContainer = SurfaceContainerDarkMediumContrast,
                surfaceContainerHigh = SurfaceContainerHighDarkMediumContrast,
                surfaceContainerHighest = SurfaceContainerHighestDarkMediumContrast,
            )
        """.trimIndent()

        assertContains(result, colors)

        val darkSchemeName = "darkTheme -> mediumContrastDarkColorScheme"
        val lightSchemeName = "else -> mediumContrastLightColorScheme"

        assertContains(result, darkSchemeName)
        assertContains(result, lightSchemeName)
    }

    @Test
    fun testStandardHighContrastTheme() {
        val settings = Settings(
            colors = ColorSettings(
                seed = Color(0xFF0000FF),
            ),
            isDarkMode = false,
            contrast = Contrast.High,
            selectedImage = null,
        )

        val result = standardThemeKt(
            packageName = "com.example",
            themeName = "AppTheme",
            multiplatform = false,
            settings = settings,
        )

        val colors = """
            private val highContrastLightColorScheme = lightColorScheme(
                primary = PrimaryLightHighContrast,
                onPrimary = OnPrimaryLightHighContrast,
                primaryContainer = PrimaryContainerLightHighContrast,
                onPrimaryContainer = OnPrimaryContainerLightHighContrast,
                secondary = SecondaryLightHighContrast,
                onSecondary = OnSecondaryLightHighContrast,
                secondaryContainer = SecondaryContainerLightHighContrast,
                onSecondaryContainer = OnSecondaryContainerLightHighContrast,
                tertiary = TertiaryLightHighContrast,
                onTertiary = OnTertiaryLightHighContrast,
                tertiaryContainer = TertiaryContainerLightHighContrast,
                onTertiaryContainer = OnTertiaryContainerLightHighContrast,
                error = ErrorLightHighContrast,
                onError = OnErrorLightHighContrast,
                errorContainer = ErrorContainerLightHighContrast,
                onErrorContainer = OnErrorContainerLightHighContrast,
                background = BackgroundLightHighContrast,
                onBackground = OnBackgroundLightHighContrast,
                surface = SurfaceLightHighContrast,
                onSurface = OnSurfaceLightHighContrast,
                surfaceVariant = SurfaceVariantLightHighContrast,
                onSurfaceVariant = OnSurfaceVariantLightHighContrast,
                outline = OutlineLightHighContrast,
                outlineVariant = OutlineVariantLightHighContrast,
                scrim = ScrimLightHighContrast,
                inverseSurface = InverseSurfaceLightHighContrast,
                inverseOnSurface = InverseOnSurfaceLightHighContrast,
                inversePrimary = InversePrimaryLightHighContrast,
                surfaceDim = SurfaceDimLightHighContrast,
                surfaceBright = SurfaceBrightLightHighContrast,
                surfaceContainerLowest = SurfaceContainerLowestLightHighContrast,
                surfaceContainerLow = SurfaceContainerLowLightHighContrast,
                surfaceContainer = SurfaceContainerLightHighContrast,
                surfaceContainerHigh = SurfaceContainerHighLightHighContrast,
                surfaceContainerHighest = SurfaceContainerHighestLightHighContrast,
            )

            private val highContrastDarkColorScheme = darkColorScheme(
                primary = PrimaryDarkHighContrast,
                onPrimary = OnPrimaryDarkHighContrast,
                primaryContainer = PrimaryContainerDarkHighContrast,
                onPrimaryContainer = OnPrimaryContainerDarkHighContrast,
                secondary = SecondaryDarkHighContrast,
                onSecondary = OnSecondaryDarkHighContrast,
                secondaryContainer = SecondaryContainerDarkHighContrast,
                onSecondaryContainer = OnSecondaryContainerDarkHighContrast,
                tertiary = TertiaryDarkHighContrast,
                onTertiary = OnTertiaryDarkHighContrast,
                tertiaryContainer = TertiaryContainerDarkHighContrast,
                onTertiaryContainer = OnTertiaryContainerDarkHighContrast,
                error = ErrorDarkHighContrast,
                onError = OnErrorDarkHighContrast,
                errorContainer = ErrorContainerDarkHighContrast,
                onErrorContainer = OnErrorContainerDarkHighContrast,
                background = BackgroundDarkHighContrast,
                onBackground = OnBackgroundDarkHighContrast,
                surface = SurfaceDarkHighContrast,
                onSurface = OnSurfaceDarkHighContrast,
                surfaceVariant = SurfaceVariantDarkHighContrast,
                onSurfaceVariant = OnSurfaceVariantDarkHighContrast,
                outline = OutlineDarkHighContrast,
                outlineVariant = OutlineVariantDarkHighContrast,
                scrim = ScrimDarkHighContrast,
                inverseSurface = InverseSurfaceDarkHighContrast,
                inverseOnSurface = InverseOnSurfaceDarkHighContrast,
                inversePrimary = InversePrimaryDarkHighContrast,
                surfaceDim = SurfaceDimDarkHighContrast,
                surfaceBright = SurfaceBrightDarkHighContrast,
                surfaceContainerLowest = SurfaceContainerLowestDarkHighContrast,
                surfaceContainerLow = SurfaceContainerLowDarkHighContrast,
                surfaceContainer = SurfaceContainerDarkHighContrast,
                surfaceContainerHigh = SurfaceContainerHighDarkHighContrast,
                surfaceContainerHighest = SurfaceContainerHighestDarkHighContrast,
            )
        """.trimIndent()

        assertContains(result, colors)
    }
}
