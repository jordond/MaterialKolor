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
            packageName = "foo.bar.biz.buzz"
        )

        val result = standardThemeKt(
            themeName = "AppTheme",
            multiplatform = true,
            settings = settings,
        )

        val expected = """
${header(settings)}
package foo.bar.biz.buzz

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
    inversePrimary = InversePrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = SurfaceTintLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceDim = SurfaceDimLight,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)

private val darkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = SurfaceTintDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceDim = SurfaceDimDark,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
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
            themeName = "AppTheme",
            multiplatform = false,
            settings = settings,
        )

        val expected = """
${header(settings)}
package com.example.app

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
    inversePrimary = InversePrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = SurfaceTintLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceDim = SurfaceDimLight,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)

private val darkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = SurfaceTintDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceDim = SurfaceDimDark,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)

@Composable
fun AppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDarkTheme -> darkColorScheme
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
                inversePrimary = InversePrimaryLightReducedContrast,
                secondary = SecondaryLightReducedContrast,
                onSecondary = OnSecondaryLightReducedContrast,
                secondaryContainer = SecondaryContainerLightReducedContrast,
                onSecondaryContainer = OnSecondaryContainerLightReducedContrast,
                tertiary = TertiaryLightReducedContrast,
                onTertiary = OnTertiaryLightReducedContrast,
                tertiaryContainer = TertiaryContainerLightReducedContrast,
                onTertiaryContainer = OnTertiaryContainerLightReducedContrast,
                background = BackgroundLightReducedContrast,
                onBackground = OnBackgroundLightReducedContrast,
                surface = SurfaceLightReducedContrast,
                onSurface = OnSurfaceLightReducedContrast,
                surfaceVariant = SurfaceVariantLightReducedContrast,
                onSurfaceVariant = OnSurfaceVariantLightReducedContrast,
                surfaceTint = SurfaceTintLightReducedContrast,
                inverseSurface = InverseSurfaceLightReducedContrast,
                inverseOnSurface = InverseOnSurfaceLightReducedContrast,
                error = ErrorLightReducedContrast,
                onError = OnErrorLightReducedContrast,
                errorContainer = ErrorContainerLightReducedContrast,
                onErrorContainer = OnErrorContainerLightReducedContrast,
                outline = OutlineLightReducedContrast,
                outlineVariant = OutlineVariantLightReducedContrast,
                scrim = ScrimLightReducedContrast,
                surfaceBright = SurfaceBrightLightReducedContrast,
                surfaceContainer = SurfaceContainerLightReducedContrast,
                surfaceContainerHigh = SurfaceContainerHighLightReducedContrast,
                surfaceContainerHighest = SurfaceContainerHighestLightReducedContrast,
                surfaceContainerLow = SurfaceContainerLowLightReducedContrast,
                surfaceContainerLowest = SurfaceContainerLowestLightReducedContrast,
                surfaceDim = SurfaceDimLightReducedContrast,
                primaryFixed = PrimaryFixedReducedContrast,
                primaryFixedDim = PrimaryFixedDimReducedContrast,
                onPrimaryFixed = OnPrimaryFixedReducedContrast,
                onPrimaryFixedVariant = OnPrimaryFixedVariantReducedContrast,
                secondaryFixed = SecondaryFixedReducedContrast,
                secondaryFixedDim = SecondaryFixedDimReducedContrast,
                onSecondaryFixed = OnSecondaryFixedReducedContrast,
                onSecondaryFixedVariant = OnSecondaryFixedVariantReducedContrast,
                tertiaryFixed = TertiaryFixedReducedContrast,
                tertiaryFixedDim = TertiaryFixedDimReducedContrast,
                onTertiaryFixed = OnTertiaryFixedReducedContrast,
                onTertiaryFixedVariant = OnTertiaryFixedVariantReducedContrast,
            )

            private val reducedContrastDarkColorScheme = darkColorScheme(
                primary = PrimaryDarkReducedContrast,
                onPrimary = OnPrimaryDarkReducedContrast,
                primaryContainer = PrimaryContainerDarkReducedContrast,
                onPrimaryContainer = OnPrimaryContainerDarkReducedContrast,
                inversePrimary = InversePrimaryDarkReducedContrast,
                secondary = SecondaryDarkReducedContrast,
                onSecondary = OnSecondaryDarkReducedContrast,
                secondaryContainer = SecondaryContainerDarkReducedContrast,
                onSecondaryContainer = OnSecondaryContainerDarkReducedContrast,
                tertiary = TertiaryDarkReducedContrast,
                onTertiary = OnTertiaryDarkReducedContrast,
                tertiaryContainer = TertiaryContainerDarkReducedContrast,
                onTertiaryContainer = OnTertiaryContainerDarkReducedContrast,
                background = BackgroundDarkReducedContrast,
                onBackground = OnBackgroundDarkReducedContrast,
                surface = SurfaceDarkReducedContrast,
                onSurface = OnSurfaceDarkReducedContrast,
                surfaceVariant = SurfaceVariantDarkReducedContrast,
                onSurfaceVariant = OnSurfaceVariantDarkReducedContrast,
                surfaceTint = SurfaceTintDarkReducedContrast,
                inverseSurface = InverseSurfaceDarkReducedContrast,
                inverseOnSurface = InverseOnSurfaceDarkReducedContrast,
                error = ErrorDarkReducedContrast,
                onError = OnErrorDarkReducedContrast,
                errorContainer = ErrorContainerDarkReducedContrast,
                onErrorContainer = OnErrorContainerDarkReducedContrast,
                outline = OutlineDarkReducedContrast,
                outlineVariant = OutlineVariantDarkReducedContrast,
                scrim = ScrimDarkReducedContrast,
                surfaceBright = SurfaceBrightDarkReducedContrast,
                surfaceContainer = SurfaceContainerDarkReducedContrast,
                surfaceContainerHigh = SurfaceContainerHighDarkReducedContrast,
                surfaceContainerHighest = SurfaceContainerHighestDarkReducedContrast,
                surfaceContainerLow = SurfaceContainerLowDarkReducedContrast,
                surfaceContainerLowest = SurfaceContainerLowestDarkReducedContrast,
                surfaceDim = SurfaceDimDarkReducedContrast,
                primaryFixed = PrimaryFixedReducedContrast,
                primaryFixedDim = PrimaryFixedDimReducedContrast,
                onPrimaryFixed = OnPrimaryFixedReducedContrast,
                onPrimaryFixedVariant = OnPrimaryFixedVariantReducedContrast,
                secondaryFixed = SecondaryFixedReducedContrast,
                secondaryFixedDim = SecondaryFixedDimReducedContrast,
                onSecondaryFixed = OnSecondaryFixedReducedContrast,
                onSecondaryFixedVariant = OnSecondaryFixedVariantReducedContrast,
                tertiaryFixed = TertiaryFixedReducedContrast,
                tertiaryFixedDim = TertiaryFixedDimReducedContrast,
                onTertiaryFixed = OnTertiaryFixedReducedContrast,
                onTertiaryFixedVariant = OnTertiaryFixedVariantReducedContrast,
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
                inversePrimary = InversePrimaryLightMediumContrast,
                secondary = SecondaryLightMediumContrast,
                onSecondary = OnSecondaryLightMediumContrast,
                secondaryContainer = SecondaryContainerLightMediumContrast,
                onSecondaryContainer = OnSecondaryContainerLightMediumContrast,
                tertiary = TertiaryLightMediumContrast,
                onTertiary = OnTertiaryLightMediumContrast,
                tertiaryContainer = TertiaryContainerLightMediumContrast,
                onTertiaryContainer = OnTertiaryContainerLightMediumContrast,
                background = BackgroundLightMediumContrast,
                onBackground = OnBackgroundLightMediumContrast,
                surface = SurfaceLightMediumContrast,
                onSurface = OnSurfaceLightMediumContrast,
                surfaceVariant = SurfaceVariantLightMediumContrast,
                onSurfaceVariant = OnSurfaceVariantLightMediumContrast,
                surfaceTint = SurfaceTintLightMediumContrast,
                inverseSurface = InverseSurfaceLightMediumContrast,
                inverseOnSurface = InverseOnSurfaceLightMediumContrast,
                error = ErrorLightMediumContrast,
                onError = OnErrorLightMediumContrast,
                errorContainer = ErrorContainerLightMediumContrast,
                onErrorContainer = OnErrorContainerLightMediumContrast,
                outline = OutlineLightMediumContrast,
                outlineVariant = OutlineVariantLightMediumContrast,
                scrim = ScrimLightMediumContrast,
                surfaceBright = SurfaceBrightLightMediumContrast,
                surfaceContainer = SurfaceContainerLightMediumContrast,
                surfaceContainerHigh = SurfaceContainerHighLightMediumContrast,
                surfaceContainerHighest = SurfaceContainerHighestLightMediumContrast,
                surfaceContainerLow = SurfaceContainerLowLightMediumContrast,
                surfaceContainerLowest = SurfaceContainerLowestLightMediumContrast,
                surfaceDim = SurfaceDimLightMediumContrast,
                primaryFixed = PrimaryFixedMediumContrast,
                primaryFixedDim = PrimaryFixedDimMediumContrast,
                onPrimaryFixed = OnPrimaryFixedMediumContrast,
                onPrimaryFixedVariant = OnPrimaryFixedVariantMediumContrast,
                secondaryFixed = SecondaryFixedMediumContrast,
                secondaryFixedDim = SecondaryFixedDimMediumContrast,
                onSecondaryFixed = OnSecondaryFixedMediumContrast,
                onSecondaryFixedVariant = OnSecondaryFixedVariantMediumContrast,
                tertiaryFixed = TertiaryFixedMediumContrast,
                tertiaryFixedDim = TertiaryFixedDimMediumContrast,
                onTertiaryFixed = OnTertiaryFixedMediumContrast,
                onTertiaryFixedVariant = OnTertiaryFixedVariantMediumContrast,
            )

            private val mediumContrastDarkColorScheme = darkColorScheme(
                primary = PrimaryDarkMediumContrast,
                onPrimary = OnPrimaryDarkMediumContrast,
                primaryContainer = PrimaryContainerDarkMediumContrast,
                onPrimaryContainer = OnPrimaryContainerDarkMediumContrast,
                inversePrimary = InversePrimaryDarkMediumContrast,
                secondary = SecondaryDarkMediumContrast,
                onSecondary = OnSecondaryDarkMediumContrast,
                secondaryContainer = SecondaryContainerDarkMediumContrast,
                onSecondaryContainer = OnSecondaryContainerDarkMediumContrast,
                tertiary = TertiaryDarkMediumContrast,
                onTertiary = OnTertiaryDarkMediumContrast,
                tertiaryContainer = TertiaryContainerDarkMediumContrast,
                onTertiaryContainer = OnTertiaryContainerDarkMediumContrast,
                background = BackgroundDarkMediumContrast,
                onBackground = OnBackgroundDarkMediumContrast,
                surface = SurfaceDarkMediumContrast,
                onSurface = OnSurfaceDarkMediumContrast,
                surfaceVariant = SurfaceVariantDarkMediumContrast,
                onSurfaceVariant = OnSurfaceVariantDarkMediumContrast,
                surfaceTint = SurfaceTintDarkMediumContrast,
                inverseSurface = InverseSurfaceDarkMediumContrast,
                inverseOnSurface = InverseOnSurfaceDarkMediumContrast,
                error = ErrorDarkMediumContrast,
                onError = OnErrorDarkMediumContrast,
                errorContainer = ErrorContainerDarkMediumContrast,
                onErrorContainer = OnErrorContainerDarkMediumContrast,
                outline = OutlineDarkMediumContrast,
                outlineVariant = OutlineVariantDarkMediumContrast,
                scrim = ScrimDarkMediumContrast,
                surfaceBright = SurfaceBrightDarkMediumContrast,
                surfaceContainer = SurfaceContainerDarkMediumContrast,
                surfaceContainerHigh = SurfaceContainerHighDarkMediumContrast,
                surfaceContainerHighest = SurfaceContainerHighestDarkMediumContrast,
                surfaceContainerLow = SurfaceContainerLowDarkMediumContrast,
                surfaceContainerLowest = SurfaceContainerLowestDarkMediumContrast,
                surfaceDim = SurfaceDimDarkMediumContrast,
                primaryFixed = PrimaryFixedMediumContrast,
                primaryFixedDim = PrimaryFixedDimMediumContrast,
                onPrimaryFixed = OnPrimaryFixedMediumContrast,
                onPrimaryFixedVariant = OnPrimaryFixedVariantMediumContrast,
                secondaryFixed = SecondaryFixedMediumContrast,
                secondaryFixedDim = SecondaryFixedDimMediumContrast,
                onSecondaryFixed = OnSecondaryFixedMediumContrast,
                onSecondaryFixedVariant = OnSecondaryFixedVariantMediumContrast,
                tertiaryFixed = TertiaryFixedMediumContrast,
                tertiaryFixedDim = TertiaryFixedDimMediumContrast,
                onTertiaryFixed = OnTertiaryFixedMediumContrast,
                onTertiaryFixedVariant = OnTertiaryFixedVariantMediumContrast,
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
                inversePrimary = InversePrimaryLightHighContrast,
                secondary = SecondaryLightHighContrast,
                onSecondary = OnSecondaryLightHighContrast,
                secondaryContainer = SecondaryContainerLightHighContrast,
                onSecondaryContainer = OnSecondaryContainerLightHighContrast,
                tertiary = TertiaryLightHighContrast,
                onTertiary = OnTertiaryLightHighContrast,
                tertiaryContainer = TertiaryContainerLightHighContrast,
                onTertiaryContainer = OnTertiaryContainerLightHighContrast,
                background = BackgroundLightHighContrast,
                onBackground = OnBackgroundLightHighContrast,
                surface = SurfaceLightHighContrast,
                onSurface = OnSurfaceLightHighContrast,
                surfaceVariant = SurfaceVariantLightHighContrast,
                onSurfaceVariant = OnSurfaceVariantLightHighContrast,
                surfaceTint = SurfaceTintLightHighContrast,
                inverseSurface = InverseSurfaceLightHighContrast,
                inverseOnSurface = InverseOnSurfaceLightHighContrast,
                error = ErrorLightHighContrast,
                onError = OnErrorLightHighContrast,
                errorContainer = ErrorContainerLightHighContrast,
                onErrorContainer = OnErrorContainerLightHighContrast,
                outline = OutlineLightHighContrast,
                outlineVariant = OutlineVariantLightHighContrast,
                scrim = ScrimLightHighContrast,
                surfaceBright = SurfaceBrightLightHighContrast,
                surfaceContainer = SurfaceContainerLightHighContrast,
                surfaceContainerHigh = SurfaceContainerHighLightHighContrast,
                surfaceContainerHighest = SurfaceContainerHighestLightHighContrast,
                surfaceContainerLow = SurfaceContainerLowLightHighContrast,
                surfaceContainerLowest = SurfaceContainerLowestLightHighContrast,
                surfaceDim = SurfaceDimLightHighContrast,
                primaryFixed = PrimaryFixedHighContrast,
                primaryFixedDim = PrimaryFixedDimHighContrast,
                onPrimaryFixed = OnPrimaryFixedHighContrast,
                onPrimaryFixedVariant = OnPrimaryFixedVariantHighContrast,
                secondaryFixed = SecondaryFixedHighContrast,
                secondaryFixedDim = SecondaryFixedDimHighContrast,
                onSecondaryFixed = OnSecondaryFixedHighContrast,
                onSecondaryFixedVariant = OnSecondaryFixedVariantHighContrast,
                tertiaryFixed = TertiaryFixedHighContrast,
                tertiaryFixedDim = TertiaryFixedDimHighContrast,
                onTertiaryFixed = OnTertiaryFixedHighContrast,
                onTertiaryFixedVariant = OnTertiaryFixedVariantHighContrast,
            )

            private val highContrastDarkColorScheme = darkColorScheme(
                primary = PrimaryDarkHighContrast,
                onPrimary = OnPrimaryDarkHighContrast,
                primaryContainer = PrimaryContainerDarkHighContrast,
                onPrimaryContainer = OnPrimaryContainerDarkHighContrast,
                inversePrimary = InversePrimaryDarkHighContrast,
                secondary = SecondaryDarkHighContrast,
                onSecondary = OnSecondaryDarkHighContrast,
                secondaryContainer = SecondaryContainerDarkHighContrast,
                onSecondaryContainer = OnSecondaryContainerDarkHighContrast,
                tertiary = TertiaryDarkHighContrast,
                onTertiary = OnTertiaryDarkHighContrast,
                tertiaryContainer = TertiaryContainerDarkHighContrast,
                onTertiaryContainer = OnTertiaryContainerDarkHighContrast,
                background = BackgroundDarkHighContrast,
                onBackground = OnBackgroundDarkHighContrast,
                surface = SurfaceDarkHighContrast,
                onSurface = OnSurfaceDarkHighContrast,
                surfaceVariant = SurfaceVariantDarkHighContrast,
                onSurfaceVariant = OnSurfaceVariantDarkHighContrast,
                surfaceTint = SurfaceTintDarkHighContrast,
                inverseSurface = InverseSurfaceDarkHighContrast,
                inverseOnSurface = InverseOnSurfaceDarkHighContrast,
                error = ErrorDarkHighContrast,
                onError = OnErrorDarkHighContrast,
                errorContainer = ErrorContainerDarkHighContrast,
                onErrorContainer = OnErrorContainerDarkHighContrast,
                outline = OutlineDarkHighContrast,
                outlineVariant = OutlineVariantDarkHighContrast,
                scrim = ScrimDarkHighContrast,
                surfaceBright = SurfaceBrightDarkHighContrast,
                surfaceContainer = SurfaceContainerDarkHighContrast,
                surfaceContainerHigh = SurfaceContainerHighDarkHighContrast,
                surfaceContainerHighest = SurfaceContainerHighestDarkHighContrast,
                surfaceContainerLow = SurfaceContainerLowDarkHighContrast,
                surfaceContainerLowest = SurfaceContainerLowestDarkHighContrast,
                surfaceDim = SurfaceDimDarkHighContrast,
                primaryFixed = PrimaryFixedHighContrast,
                primaryFixedDim = PrimaryFixedDimHighContrast,
                onPrimaryFixed = OnPrimaryFixedHighContrast,
                onPrimaryFixedVariant = OnPrimaryFixedVariantHighContrast,
                secondaryFixed = SecondaryFixedHighContrast,
                secondaryFixedDim = SecondaryFixedDimHighContrast,
                onSecondaryFixed = OnSecondaryFixedHighContrast,
                onSecondaryFixedVariant = OnSecondaryFixedVariantHighContrast,
                tertiaryFixed = TertiaryFixedHighContrast,
                tertiaryFixedDim = TertiaryFixedDimHighContrast,
                onTertiaryFixed = OnTertiaryFixedHighContrast,
                onTertiaryFixedVariant = OnTertiaryFixedVariantHighContrast,
            )
        """.trimIndent()

        assertContains(result, colors)
    }
}
