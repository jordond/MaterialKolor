package com.materialkolor.unstyled

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.LocalContentColor
import com.composeunstyled.theme.ColorScheme
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeComposableV2
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken
import com.composeunstyled.theme.buildTheme
import com.composeunstyled.theme.buildThemeV2
import com.materialkolor.MaterialKolors
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.DynamicScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DynamicColorsTest {
    private val seed = Color(0xff4285f4)

    private val otherSeed = Color(0xffb3261e)

    private val radii = ThemeProperty<Dp>("radii")

    private val card = ThemeToken<Dp>("card")

    @Test
    fun rememberDynamicColors_pinnedLightReadsTheBaseValues() =
        runComposeUiTest {
            val theme = lightAndDarkTheme { seed }
            var primary: Color? = null

            setContent {
                theme(ColorScheme.Light) {
                    primary = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                }
            }

            waitForIdle()
            assertEquals(kolors(seed, isDark = false).primary(), primary)
        }

    @Test
    fun rememberDynamicColors_pinnedDarkReadsTheDarkBlock() =
        runComposeUiTest {
            val theme = lightAndDarkTheme { seed }
            var primary: Color? = null

            setContent {
                theme(ColorScheme.Dark) {
                    primary = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                }
            }

            waitForIdle()
            assertEquals(kolors(seed, isDark = true).primary(), primary)
        }

    @Test
    fun rememberDynamicColors_followsStateReadInsideTheBuilder() =
        runComposeUiTest {
            var currentSeed by mutableStateOf(seed)
            val theme = lightAndDarkTheme { currentSeed }
            var primary: Color? = null

            setContent {
                theme(ColorScheme.Dark) {
                    primary = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                }
            }

            waitForIdle()
            val first = primary
            currentSeed = otherSeed
            waitForIdle()

            assertNotEquals(first, primary)
            assertEquals(kolors(otherSeed, isDark = true).primary(), primary)
        }

    @Test
    fun rememberDynamicColors_givesEveryToken() =
        runComposeUiTest {
            val theme = lightAndDarkTheme { seed }
            val read = mutableMapOf<String, Color>()

            setContent {
                theme(ColorScheme.Light) {
                    val values = Theme[MaterialKolorTokens.colors]
                    for (token in MaterialKolorTokens.all) {
                        read[token.name] = values[token]
                    }
                }
            }

            waitForIdle()
            val expected = DynamicScheme(seedColor = seed, isDark = false)
                .toThemeValues()
                .mapKeys { (token, _) -> token.name }
            assertEquals(expected, read)
        }

    @Test
    fun rememberDynamicColors_sharesTheDarkBlockWithTheThemesOwnSettings() =
        runComposeUiTest {
            val theme = buildThemeV2 {
                defaultContentColor = Color.Black
                properties[radii] = mapOf(card to 12.dp)
                properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seed, isDark = false)

                colorScheme(ColorScheme.Dark) {
                    defaultContentColor = Color.White
                    properties[radii] = mapOf(card to 4.dp)
                    properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seed, isDark = true)
                }
            }
            val primary = mutableMapOf<ColorScheme, Color>()
            val radius = mutableMapOf<ColorScheme, Dp>()
            val contentColor = mutableMapOf<ColorScheme, Color>()

            setContent {
                for (scheme in listOf(ColorScheme.Light, ColorScheme.Dark)) {
                    theme(scheme) {
                        primary[scheme] = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                        radius[scheme] = Theme[radii][card]
                        contentColor[scheme] = LocalContentColor.current
                    }
                }
            }

            waitForIdle()
            assertEquals(kolors(seed, isDark = false).primary(), primary[ColorScheme.Light])
            assertEquals(kolors(seed, isDark = true).primary(), primary[ColorScheme.Dark])
            assertEquals(12.dp, radius[ColorScheme.Light])
            assertEquals(4.dp, radius[ColorScheme.Dark])
            assertEquals(Color.Black, contentColor[ColorScheme.Light])
            assertEquals(Color.White, contentColor[ColorScheme.Dark])
        }

    @Test
    fun rememberDynamicColors_fillsACustomScheme() =
        runComposeUiTest {
            val sepia = ColorScheme("sepia")
            val theme = buildThemeV2 {
                properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seed, isDark = false)

                colorScheme(sepia) {
                    properties[MaterialKolorTokens.colors] =
                        rememberDynamicColors(seedColor = otherSeed, isDark = false)
                }
            }
            var primary: Color? = null

            setContent {
                theme(sepia) {
                    primary = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                }
            }

            waitForIdle()
            assertEquals(kolors(otherSeed, isDark = false).primary(), primary)
        }

    @Test
    fun rememberDynamicColors_baseValuesCoverACustomSchemeWithoutColors() =
        runComposeUiTest {
            val sepia = ColorScheme("sepia")
            val theme = buildThemeV2 {
                properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seed, isDark = false)

                colorScheme(sepia) {
                    defaultContentColor = Color.White
                }
            }
            var primary: Color? = null

            setContent {
                theme(sepia) {
                    primary = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                }
            }

            waitForIdle()
            assertEquals(kolors(seed, isDark = false).primary(), primary)
        }

    @Test
    fun rememberDynamicColors_keepsTheSameValuesAcrossRecomposition() =
        runComposeUiTest {
            var tick by mutableStateOf(0)
            val seen = mutableListOf<Pair<Int, Map<ThemeToken<Color>, Color>>>()

            setContent {
                val current = tick
                val values = rememberDynamicColors(seedColor = seed, isDark = false)
                SideEffect { seen += current to values }
            }

            waitForIdle()
            tick = 1
            waitForIdle()

            assertTrue(seen.any { (current, _) -> current == 1 }, "The content never recomposed")
            assertSame(seen.first().second, seen.last().second)
        }

    @Test
    fun rememberDynamicColors_worksInAThemeWithoutColorSchemes() =
        runComposeUiTest {
            val theme = buildTheme {
                properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seed, isDark = true)
            }
            var primary: Color? = null

            setContent {
                theme {
                    primary = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                }
            }

            waitForIdle()
            assertEquals(kolors(seed, isDark = true).primary(), primary)
        }

    @Test
    fun rememberDynamicLightDarkColors_matchesTheSingleModeValues() =
        runComposeUiTest {
            var pair: Pair<Map<ThemeToken<Color>, Color>, Map<ThemeToken<Color>, Color>>? = null
            var light: Map<ThemeToken<Color>, Color>? = null
            var dark: Map<ThemeToken<Color>, Color>? = null

            setContent {
                pair = rememberDynamicLightDarkColors(seedColor = seed, style = PaletteStyle.Vibrant)
                light = rememberDynamicColors(seedColor = seed, isDark = false, style = PaletteStyle.Vibrant)
                dark = rememberDynamicColors(seedColor = seed, isDark = true, style = PaletteStyle.Vibrant)
            }

            waitForIdle()
            assertEquals(light, assertNotNull(pair).first)
            assertEquals(dark, assertNotNull(pair).second)
        }

    @Test
    fun rememberDynamicLightDarkColors_fillsTheBaseValuesAndTheDarkBlock() =
        runComposeUiTest {
            val theme = buildThemeV2 {
                val (light, dark) = rememberDynamicLightDarkColors(seedColor = seed)
                properties[MaterialKolorTokens.colors] = light

                colorScheme(ColorScheme.Dark) {
                    properties[MaterialKolorTokens.colors] = dark
                }
            }
            val primary = mutableMapOf<ColorScheme, Color>()

            setContent {
                for (scheme in listOf(ColorScheme.Light, ColorScheme.Dark)) {
                    theme(scheme) {
                        primary[scheme] = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                    }
                }
            }

            waitForIdle()
            assertEquals(kolors(seed, isDark = false).primary(), primary[ColorScheme.Light])
            assertEquals(kolors(seed, isDark = true).primary(), primary[ColorScheme.Dark])
        }

    private fun lightAndDarkTheme(seedColor: () -> Color): ThemeComposableV2 =
        buildThemeV2 {
            properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seedColor(), isDark = false)

            colorScheme(ColorScheme.Dark) {
                properties[MaterialKolorTokens.colors] = rememberDynamicColors(seedColor = seedColor(), isDark = true)
            }
        }

    private fun kolors(
        seedColor: Color,
        isDark: Boolean,
    ): MaterialKolors = MaterialKolors(DynamicScheme(seedColor = seedColor, isDark = isDark))
}
