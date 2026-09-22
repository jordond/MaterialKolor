package com.materialkolor.unstyled

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.composeunstyled.theme.ColorScheme
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.buildThemeV2
import com.materialkolor.MaterialKolors
import com.materialkolor.ktx.DynamicScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class ThemeBuilderTest {
    private val seed = Color(0xff4285f4)

    private val otherSeed = Color(0xffb3261e)

    @Test
    fun dynamicColorSchemes_pinnedLightSchemeReadsTheLightPrimary() =
        runComposeUiTest {
            val theme = buildThemeV2 { dynamicColorSchemes(seedColor = seed) }
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
    fun dynamicColorSchemes_pinnedDarkSchemeReadsTheDarkPrimary() =
        runComposeUiTest {
            val theme = buildThemeV2 { dynamicColorSchemes(seedColor = seed) }
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
    fun dynamicColorSchemes_followsStateReadInsideTheBuilder() =
        runComposeUiTest {
            var currentSeed by mutableStateOf(seed)
            val theme = buildThemeV2 { dynamicColorSchemes(seedColor = currentSeed) }
            var primary: Color? = null

            setContent {
                theme(ColorScheme.Light) {
                    primary = Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
                }
            }

            waitForIdle()
            val first = primary
            currentSeed = otherSeed
            waitForIdle()

            assertNotEquals(first, primary)
            assertEquals(kolors(otherSeed, isDark = false).primary(), primary)
        }

    @Test
    fun dynamicColors_exposesEveryToken() =
        runComposeUiTest {
            val scheme = DynamicScheme(seedColor = seed, isDark = false)
            val theme = buildThemeV2 { dynamicColors(scheme) }
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
            val expected = scheme.toThemeValues().mapKeys { (token, _) -> token.name }
            assertEquals(expected, read)
        }

    private fun kolors(
        seedColor: Color,
        isDark: Boolean,
    ): MaterialKolors = MaterialKolors(DynamicScheme(seedColor = seedColor, isDark = isDark))
}
