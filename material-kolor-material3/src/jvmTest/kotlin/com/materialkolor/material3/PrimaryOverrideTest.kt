package com.materialkolor.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.DynamicScheme
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

    private val styles = listOf(
        PaletteStyle.TonalSpot,
        PaletteStyle.Vibrant,
        PaletteStyle.Expressive,
        PaletteStyle.Fidelity,
    )

    @Test
    fun dynamicColorScheme_withPrimaryOverride_matchesCoreScheme() {
        forEachCombination { seed, override, style, isDark ->
            val expected = DynamicScheme(
                seedColor = seed,
                isDark = isDark,
                primary = override,
                style = style,
            ).toColorScheme()

            val actual = dynamicColorScheme(
                seedColor = seed,
                isDark = isDark,
                primary = override,
                style = style,
            )

            assertEquals(
                expected.roles(),
                actual.roles(),
                describe(seed, override, style, isDark),
            )
        }
    }

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
    fun deprecatedDynamicColorScheme_matchesItsReplacement() {
        for (color in overrides) {
            for (isDark in listOf(false, true)) {
                val deprecated = dynamicColorScheme(primary = color, isDark = isDark)
                val replacement = dynamicColorScheme(
                    seedColor = color,
                    isDark = isDark,
                    primary = color,
                )

                assertEquals(replacement.roles(), deprecated.roles(), color.toString())
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedRememberDynamicColorScheme_matchesItsReplacement() =
        runComposeUiTest {
            var deprecated: ColorScheme? = null
            var replacement: ColorScheme? = null

            setContent {
                deprecated = rememberDynamicColorScheme(primary = overrides.first(), isDark = false)
                replacement = rememberDynamicColorScheme(
                    seedColor = overrides.first(),
                    isDark = false,
                    primary = overrides.first(),
                )
            }

            waitForIdle()
            assertEquals(assertNotNull(replacement).roles(), assertNotNull(deprecated).roles())
        }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedRememberDynamicMaterialThemeState_matchesItsReplacement() =
        runComposeUiTest {
            var deprecated: ColorScheme? = null
            var replacement: ColorScheme? = null

            setContent {
                deprecated = rememberDynamicMaterialThemeState(
                    primary = overrides.first(),
                    isDark = true,
                ).colorScheme

                replacement = rememberDynamicMaterialThemeState(
                    seedColor = overrides.first(),
                    isDark = true,
                    primary = overrides.first(),
                ).colorScheme
            }

            waitForIdle()
            assertEquals(assertNotNull(replacement).roles(), assertNotNull(deprecated).roles())
        }

    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDynamicMaterialTheme_matchesItsReplacement() =
        runComposeUiTest {
            var deprecated: ColorScheme? = null
            var replacement: ColorScheme? = null

            setContent {
                DynamicMaterialTheme(primary = overrides.first(), isDark = false) {
                    deprecated = MaterialTheme.colorScheme
                }

                DynamicMaterialTheme(
                    seedColor = overrides.first(),
                    isDark = false,
                    primary = overrides.first(),
                ) {
                    replacement = MaterialTheme.colorScheme
                }
            }

            waitForIdle()
            assertEquals(assertNotNull(replacement).roles(), assertNotNull(deprecated).roles())
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Suppress("DEPRECATION")
    @Test
    fun deprecatedDynamicMaterialExpressiveTheme_matchesItsReplacement() =
        runComposeUiTest {
            var deprecated: ColorScheme? = null
            var replacement: ColorScheme? = null

            setContent {
                DynamicMaterialExpressiveTheme(primary = overrides.first(), isDark = false) {
                    deprecated = MaterialTheme.colorScheme
                }

                DynamicMaterialExpressiveTheme(
                    seedColor = overrides.first(),
                    isDark = false,
                    primary = overrides.first(),
                ) {
                    replacement = MaterialTheme.colorScheme
                }
            }

            waitForIdle()
            assertEquals(assertNotNull(replacement).roles(), assertNotNull(deprecated).roles())
        }

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
                "inverseOnSurface" to inverseOnSurface,
                "inversePrimary" to inversePrimary,
                "inverseSurface" to inverseSurface,
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
