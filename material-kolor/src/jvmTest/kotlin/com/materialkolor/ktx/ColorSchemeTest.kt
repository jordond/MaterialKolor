package com.materialkolor.ktx

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class ColorSchemeTest {
    private val testLightScheme = lightColorScheme(
        primary = Color.Red,
        secondary = Color.Blue,
        tertiary = Color.Green,
        surface = Color.White,
        background = Color(0xFFFFFBFE),
    )

    private val testDarkScheme = darkColorScheme(
        primary = Color.Cyan,
        secondary = Color.Magenta,
        tertiary = Color.Yellow,
        surface = Color.Black,
        background = Color(0xFF1C1B1F),
    )

    @Test
    fun animateColorScheme_preservesAllPrimaryColors() = runComposeUiTest {
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = testLightScheme,
                label = "test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testLightScheme.primary, result.primary)
        assertEquals(testLightScheme.onPrimary, result.onPrimary)
        assertEquals(testLightScheme.primaryContainer, result.primaryContainer)
        assertEquals(testLightScheme.onPrimaryContainer, result.onPrimaryContainer)
        assertEquals(testLightScheme.inversePrimary, result.inversePrimary)
    }

    @Test
    fun animateColorScheme_preservesSecondaryColors() = runComposeUiTest {
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = testLightScheme,
                label = "test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testLightScheme.secondary, result.secondary)
        assertEquals(testLightScheme.onSecondary, result.onSecondary)
        assertEquals(testLightScheme.secondaryContainer, result.secondaryContainer)
        assertEquals(testLightScheme.onSecondaryContainer, result.onSecondaryContainer)
    }

    @Test
    fun animateColorScheme_preservesTertiaryColors() = runComposeUiTest {
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = testLightScheme,
                label = "test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testLightScheme.tertiary, result.tertiary)
        assertEquals(testLightScheme.onTertiary, result.onTertiary)
        assertEquals(testLightScheme.tertiaryContainer, result.tertiaryContainer)
        assertEquals(testLightScheme.onTertiaryContainer, result.onTertiaryContainer)
    }

    @Test
    fun animateColorScheme_preservesSurfaceColors() = runComposeUiTest {
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = testLightScheme,
                label = "test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testLightScheme.surface, result.surface)
        assertEquals(testLightScheme.onSurface, result.onSurface)
        assertEquals(testLightScheme.surfaceVariant, result.surfaceVariant)
        assertEquals(testLightScheme.onSurfaceVariant, result.onSurfaceVariant)
        assertEquals(testLightScheme.surfaceTint, result.surfaceTint)
        assertEquals(testLightScheme.surfaceBright, result.surfaceBright)
        assertEquals(testLightScheme.surfaceDim, result.surfaceDim)
        assertEquals(testLightScheme.surfaceContainer, result.surfaceContainer)
        assertEquals(testLightScheme.surfaceContainerHigh, result.surfaceContainerHigh)
        assertEquals(testLightScheme.surfaceContainerHighest, result.surfaceContainerHighest)
        assertEquals(testLightScheme.surfaceContainerLow, result.surfaceContainerLow)
        assertEquals(testLightScheme.surfaceContainerLowest, result.surfaceContainerLowest)
    }

    @Test
    fun animateColorScheme_preservesErrorAndBackgroundColors() = runComposeUiTest {
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = testLightScheme,
                label = "test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testLightScheme.background, result.background)
        assertEquals(testLightScheme.onBackground, result.onBackground)
        assertEquals(testLightScheme.error, result.error)
        assertEquals(testLightScheme.onError, result.onError)
        assertEquals(testLightScheme.errorContainer, result.errorContainer)
        assertEquals(testLightScheme.onErrorContainer, result.onErrorContainer)
    }

    @Test
    fun animateColorScheme_preservesUtilityColors() = runComposeUiTest {
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = testLightScheme,
                label = "test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testLightScheme.inverseSurface, result.inverseSurface)
        assertEquals(testLightScheme.inverseOnSurface, result.inverseOnSurface)
        assertEquals(testLightScheme.outline, result.outline)
        assertEquals(testLightScheme.outlineVariant, result.outlineVariant)
        assertEquals(testLightScheme.scrim, result.scrim)
    }

    @Test
    fun animateColorScheme_preservesFixedColors() = runComposeUiTest {
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = testLightScheme,
                label = "test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testLightScheme.primaryFixed, result.primaryFixed)
        assertEquals(testLightScheme.primaryFixedDim, result.primaryFixedDim)
        assertEquals(testLightScheme.onPrimaryFixed, result.onPrimaryFixed)
        assertEquals(testLightScheme.onPrimaryFixedVariant, result.onPrimaryFixedVariant)
        assertEquals(testLightScheme.secondaryFixed, result.secondaryFixed)
        assertEquals(testLightScheme.secondaryFixedDim, result.secondaryFixedDim)
        assertEquals(testLightScheme.onSecondaryFixed, result.onSecondaryFixed)
        assertEquals(testLightScheme.onSecondaryFixedVariant, result.onSecondaryFixedVariant)
        assertEquals(testLightScheme.tertiaryFixed, result.tertiaryFixed)
        assertEquals(testLightScheme.tertiaryFixedDim, result.tertiaryFixedDim)
        assertEquals(testLightScheme.onTertiaryFixed, result.onTertiaryFixed)
        assertEquals(testLightScheme.onTertiaryFixedVariant, result.onTertiaryFixedVariant)
    }

    @Test
    fun animateColorScheme_handlesColorSchemeTransition() = runComposeUiTest {
        var colorScheme by mutableStateOf(testLightScheme)
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = colorScheme,
                label = "transition_test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testLightScheme.primary, result.primary)

        colorScheme = testDarkScheme
        waitForIdle()

        assertNotNull(result)
    }

    @Test
    fun animateColorScheme_worksWithDarkColorScheme() = runComposeUiTest {
        var result: ColorScheme? = null

        setContent {
            result = animateColorScheme(
                colorScheme = testDarkScheme,
                label = "dark_test",
            )
        }

        waitForIdle()
        assertNotNull(result)
        assertEquals(testDarkScheme.primary, result.primary)
        assertEquals(testDarkScheme.secondary, result.secondary)
        assertEquals(testDarkScheme.tertiary, result.tertiary)
    }
}
