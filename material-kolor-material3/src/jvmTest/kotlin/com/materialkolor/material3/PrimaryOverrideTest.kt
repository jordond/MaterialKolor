package com.materialkolor.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.PaletteStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class PrimaryOverrideTest {
    private val seeds = listOf(
        Color(0xFF4285F4),
        Color(0xFFB33951),
        Color(0xFF2E7D32),
        Color(0xFFFFC107),
        Color(0xFF6A1B9A),
    )

    private val overrides = listOf(
        Color(0xFFFF5722),
        Color(0xFF00838F),
        Color(0xFF9E9E9E),
    )

    private val deprecatedColor = Color(0xFFFF5722)

    /**
     * Upstream Java MCU accents for FF5722 as the seed of a SPEC_2025 tonal spot scheme, with its
     * primary palette pinned to FF5722.
     */
    private val tonalSpotLight = accents(primary = 0xFFB12F00, secondary = 0xFF78574E, tertiary = 0xFF765B25)

    private val tonalSpotDark = accents(primary = 0xFFFFB5A0, secondary = 0xFFE7BDB2, tertiary = 0xFFFFE8C4)

    /**
     * The same for a SPEC_2025 expressive scheme, which the expressive theme defaults to.
     */
    private val expressiveLight = accents(primary = 0xFFB12F00, secondary = 0xFF416377, tertiary = 0xFF006786)

    private val styles = listOf(
        PaletteStyle.TonalSpot,
        PaletteStyle.Vibrant,
        PaletteStyle.Expressive,
        PaletteStyle.Fidelity,
    )

    @Test
    fun dynamicColorScheme_withPrimaryOverride_keepsSeedDrivenRoles() {
        forEachCombination { seed, override, style, isDark ->
            val fromSeed = dynamicColorScheme(
                seedColor = seed,
                isDark = isDark,
                style = style,
            )

            val pinned = dynamicColorScheme(
                seedColor = seed,
                isDark = isDark,
                primary = override,
                style = style,
            )

            assertEquals(
                fromSeed.seedDrivenRoles(),
                pinned.seedDrivenRoles(),
                describe(seed, override, style, isDark),
            )
        }
    }

    @Test
    fun dynamicColorScheme_withPrimaryOverride_changesThePrimaryRole() {
        val seed = seeds.first()
        val override = overrides.first()

        val fromSeed = dynamicColorScheme(seedColor = seed, isDark = false)
        val pinned = dynamicColorScheme(seedColor = seed, isDark = false, primary = override)

        assertNotEquals(fromSeed.primary, pinned.primary)
    }

    @Test
    fun themeState_withPrimaryOverride_matchesStatelessScheme() =
        runComposeUiTest {
            val results = mutableMapOf<String, Pair<Map<String, Color>, Map<String, Color>>>()

            setContent {
                for (seed in seeds) {
                    for (override in overrides) {
                        for (style in styles) {
                            for (isDark in listOf(false, true)) {
                                val state = rememberDynamicMaterialThemeState(
                                    seedColor = seed,
                                    isDark = isDark,
                                    primary = override,
                                    style = style,
                                )

                                results[describe(seed, override, style, isDark)] =
                                    state.colorScheme.roles() to
                                    state.dynamicScheme.toColorScheme().roles()
                            }
                        }
                    }
                }
            }

            waitForIdle()
            forEachCombination { seed, override, style, isDark ->
                val key = describe(seed, override, style, isDark)
                val expected = dynamicColorScheme(
                    seedColor = seed,
                    isDark = isDark,
                    primary = override,
                    style = style,
                ).roles()

                val (fromColorScheme, fromDynamicScheme) = assertNotNull(results[key], key)
                assertEquals(expected, fromColorScheme, key)
                assertEquals(expected, fromDynamicScheme, key)
            }
        }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDynamicColorScheme_everyStyle_matchesItsReplacement() {
        for (style in PaletteStyle.KnownStyles) {
            for (color in overrides) {
                for (isDark in listOf(false, true)) {
                    val deprecated = dynamicColorScheme(primary = color, isDark = isDark, style = style)
                    val replacement = dynamicColorScheme(
                        seedColor = color,
                        isDark = isDark,
                        primary = color,
                        style = style,
                    )

                    assertEquals(replacement.roles(), deprecated.roles(), "style=$style color=$color isDark=$isDark")
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDynamicColorScheme_seedsAndPinsPrimaryWithTheColor() {
        assertEquals(tonalSpotLight, dynamicColorScheme(primary = deprecatedColor, isDark = false).accents())
        assertEquals(tonalSpotDark, dynamicColorScheme(primary = deprecatedColor, isDark = true).accents())
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedRememberDynamicColorScheme_seedsAndPinsPrimaryWithTheColor() =
        runComposeUiTest {
            var deprecated: ColorScheme? = null

            setContent {
                deprecated = rememberDynamicColorScheme(primary = deprecatedColor, isDark = false)
            }

            waitForIdle()
            assertEquals(tonalSpotLight, assertNotNull(deprecated).accents())
        }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedRememberDynamicMaterialThemeState_seedsAndPinsPrimaryWithTheColor() =
        runComposeUiTest {
            var deprecated: ColorScheme? = null

            setContent {
                deprecated = rememberDynamicMaterialThemeState(primary = deprecatedColor, isDark = true).colorScheme
            }

            waitForIdle()
            assertEquals(tonalSpotDark, assertNotNull(deprecated).accents())
        }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDynamicMaterialTheme_seedsAndPinsPrimaryWithTheColor() =
        runComposeUiTest {
            var deprecated: ColorScheme? = null

            setContent {
                DynamicMaterialTheme(primary = deprecatedColor, isDark = false) {
                    deprecated = MaterialTheme.colorScheme
                }
            }

            waitForIdle()
            assertEquals(tonalSpotLight, assertNotNull(deprecated).accents())
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDynamicMaterialExpressiveTheme_seedsAndPinsPrimaryWithTheColor() =
        runComposeUiTest {
            var deprecated: ColorScheme? = null

            setContent {
                DynamicMaterialExpressiveTheme(primary = deprecatedColor, isDark = false) {
                    deprecated = MaterialTheme.colorScheme
                }
            }

            waitForIdle()
            assertEquals(expressiveLight, assertNotNull(deprecated).accents())
        }

    private fun accents(
        primary: Long,
        secondary: Long,
        tertiary: Long,
    ): Map<String, Color> =
        mapOf(
            "primary" to Color(primary),
            "secondary" to Color(secondary),
            "tertiary" to Color(tertiary),
        )

    private fun ColorScheme.accents(): Map<String, Color> =
        mapOf(
            "primary" to primary,
            "secondary" to secondary,
            "tertiary" to tertiary,
        )

    private fun forEachCombination(
        block: (seed: Color, override: Color, style: PaletteStyle, isDark: Boolean) -> Unit,
    ) {
        for (seed in seeds) {
            for (override in overrides) {
                for (style in styles) {
                    for (isDark in listOf(false, true)) {
                        block(seed, override, style, isDark)
                    }
                }
            }
        }
    }

    private fun describe(
        seed: Color,
        override: Color,
        style: PaletteStyle,
        isDark: Boolean,
    ): String = "seed=$seed override=$override style=$style isDark=$isDark"

    // Roles the seed keeps driving when the primary palette is pinned, so a role that starts
    // following the override shows up here rather than going unnoticed.
    private fun ColorScheme.seedDrivenRoles(): Map<String, Color> =
        mapOf(
            "background" to background,
            "error" to error,
            "errorContainer" to errorContainer,
            "inverseOnSurface" to inverseOnSurface,
            "inverseSurface" to inverseSurface,
            "onBackground" to onBackground,
            "onError" to onError,
            "onErrorContainer" to onErrorContainer,
            "onSecondary" to onSecondary,
            "onSecondaryContainer" to onSecondaryContainer,
            "onSecondaryFixed" to onSecondaryFixed,
            "onSecondaryFixedVariant" to onSecondaryFixedVariant,
            "onSurface" to onSurface,
            "onSurfaceVariant" to onSurfaceVariant,
            "onTertiary" to onTertiary,
            "onTertiaryContainer" to onTertiaryContainer,
            "onTertiaryFixed" to onTertiaryFixed,
            "onTertiaryFixedVariant" to onTertiaryFixedVariant,
            "outline" to outline,
            "outlineVariant" to outlineVariant,
            "secondary" to secondary,
            "secondaryContainer" to secondaryContainer,
            "secondaryFixed" to secondaryFixed,
            "secondaryFixedDim" to secondaryFixedDim,
            "surface" to surface,
            "surfaceBright" to surfaceBright,
            "surfaceContainer" to surfaceContainer,
            "surfaceContainerHigh" to surfaceContainerHigh,
            "surfaceContainerHighest" to surfaceContainerHighest,
            "surfaceContainerLow" to surfaceContainerLow,
            "surfaceContainerLowest" to surfaceContainerLowest,
            "surfaceDim" to surfaceDim,
            "surfaceVariant" to surfaceVariant,
            "tertiary" to tertiary,
            "tertiaryContainer" to tertiaryContainer,
            "tertiaryFixed" to tertiaryFixed,
            "tertiaryFixedDim" to tertiaryFixedDim,
        )

    private fun ColorScheme.roles(): Map<String, Color> =
        seedDrivenRoles() +
            mapOf(
                "inversePrimary" to inversePrimary,
                "onPrimary" to onPrimary,
                "onPrimaryContainer" to onPrimaryContainer,
                "onPrimaryFixed" to onPrimaryFixed,
                "onPrimaryFixedVariant" to onPrimaryFixedVariant,
                "primary" to primary,
                "primaryContainer" to primaryContainer,
                "primaryFixed" to primaryFixed,
                "primaryFixedDim" to primaryFixedDim,
                "scrim" to scrim,
                "surfaceTint" to surfaceTint,
            )
}
