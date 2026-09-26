package com.materialkolor.unstyled

import androidx.compose.ui.graphics.Color
import com.composeunstyled.theme.ThemeToken
import com.materialkolor.MaterialKolors
import com.materialkolor.ktx.DynamicScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ThemeValuesTest {
    private val seed = Color(0xff4285f4)

    private val lightKolors = MaterialKolors(DynamicScheme(seedColor = seed, isDark = false))

    private val darkKolors = MaterialKolors(DynamicScheme(seedColor = seed, isDark = true))

    @Test
    fun toThemeValues_coversEveryToken() {
        assertEquals(MaterialKolorTokens.all.toSet(), lightKolors.toThemeValues().keys)
        assertEquals(MaterialKolorTokens.all.toSet(), darkKolors.toThemeValues().keys)
    }

    @Test
    fun toThemeValues_matchesTheRoleAccessors() {
        assertEquals(lightKolors.roles(), lightKolors.toThemeValues())
        assertEquals(darkKolors.roles(), darkKolors.toThemeValues())
    }

    @Test
    fun toThemeValues_lightAndDarkDiffer() {
        assertNotEquals(lightKolors.toThemeValues(), darkKolors.toThemeValues())
    }

    @Test
    fun tokenNamesAreUnique() {
        val names = MaterialKolorTokens.all.map { token -> token.name }
        assertEquals(names.size, names.toSet().size, "Duplicate token names: $names")
    }

    @Test
    fun tokenNamesMatchTheRoleAccessors() {
        val names = MaterialKolorTokens.all.map { token -> token.name }.toSet()
        assertTrue(lightKolors.roles().keys.all { token -> token.name in names })
    }

    // The roles written out once more, so a role that changes what it returns fails here rather
    // than agreeing with a broken mapping.
    private fun MaterialKolors.roles(): Map<ThemeToken<Color>, Color> =
        mapOf(
            MaterialKolorTokens.primaryPaletteKeyColor to primaryPaletteKeyColor(),
            MaterialKolorTokens.secondaryPaletteKeyColor to secondaryPaletteKeyColor(),
            MaterialKolorTokens.tertiaryPaletteKeyColor to tertiaryPaletteKeyColor(),
            MaterialKolorTokens.errorPaletteKeyColor to errorPaletteKeyColor(),
            MaterialKolorTokens.neutralPaletteKeyColor to neutralPaletteKeyColor(),
            MaterialKolorTokens.neutralVariantPaletteKeyColor to neutralVariantPaletteKeyColor(),
            MaterialKolorTokens.background to background(),
            MaterialKolorTokens.onBackground to onBackground(),
            MaterialKolorTokens.surface to surface(),
            MaterialKolorTokens.surfaceDim to surfaceDim(),
            MaterialKolorTokens.surfaceBright to surfaceBright(),
            MaterialKolorTokens.surfaceContainerLowest to surfaceContainerLowest(),
            MaterialKolorTokens.surfaceContainerLow to surfaceContainerLow(),
            MaterialKolorTokens.surfaceContainer to surfaceContainer(),
            MaterialKolorTokens.surfaceContainerHigh to surfaceContainerHigh(),
            MaterialKolorTokens.surfaceContainerHighest to surfaceContainerHighest(),
            MaterialKolorTokens.onSurface to onSurface(),
            MaterialKolorTokens.surfaceVariant to surfaceVariant(),
            MaterialKolorTokens.onSurfaceVariant to onSurfaceVariant(),
            MaterialKolorTokens.inverseSurface to inverseSurface(),
            MaterialKolorTokens.inverseOnSurface to inverseOnSurface(),
            MaterialKolorTokens.outline to outline(),
            MaterialKolorTokens.outlineVariant to outlineVariant(),
            MaterialKolorTokens.shadow to shadow(),
            MaterialKolorTokens.scrim to scrim(),
            MaterialKolorTokens.surfaceTint to surfaceTint(),
            MaterialKolorTokens.primary to primary(),
            MaterialKolorTokens.onPrimary to onPrimary(),
            MaterialKolorTokens.primaryContainer to primaryContainer(),
            MaterialKolorTokens.onPrimaryContainer to onPrimaryContainer(),
            MaterialKolorTokens.inversePrimary to inversePrimary(),
            MaterialKolorTokens.secondary to secondary(),
            MaterialKolorTokens.onSecondary to onSecondary(),
            MaterialKolorTokens.secondaryContainer to secondaryContainer(),
            MaterialKolorTokens.onSecondaryContainer to onSecondaryContainer(),
            MaterialKolorTokens.tertiary to tertiary(),
            MaterialKolorTokens.onTertiary to onTertiary(),
            MaterialKolorTokens.tertiaryContainer to tertiaryContainer(),
            MaterialKolorTokens.onTertiaryContainer to onTertiaryContainer(),
            MaterialKolorTokens.error to error(),
            MaterialKolorTokens.onError to onError(),
            MaterialKolorTokens.errorContainer to errorContainer(),
            MaterialKolorTokens.onErrorContainer to onErrorContainer(),
            MaterialKolorTokens.primaryFixed to primaryFixed(),
            MaterialKolorTokens.primaryFixedDim to primaryFixedDim(),
            MaterialKolorTokens.onPrimaryFixed to onPrimaryFixed(),
            MaterialKolorTokens.onPrimaryFixedVariant to onPrimaryFixedVariant(),
            MaterialKolorTokens.secondaryFixed to secondaryFixed(),
            MaterialKolorTokens.secondaryFixedDim to secondaryFixedDim(),
            MaterialKolorTokens.onSecondaryFixed to onSecondaryFixed(),
            MaterialKolorTokens.onSecondaryFixedVariant to onSecondaryFixedVariant(),
            MaterialKolorTokens.tertiaryFixed to tertiaryFixed(),
            MaterialKolorTokens.tertiaryFixedDim to tertiaryFixedDim(),
            MaterialKolorTokens.onTertiaryFixed to onTertiaryFixed(),
            MaterialKolorTokens.onTertiaryFixedVariant to onTertiaryFixedVariant(),
            MaterialKolorTokens.controlActivated to controlActivated(),
            MaterialKolorTokens.controlNormal to controlNormal(),
            MaterialKolorTokens.controlHighlight to controlHighlight(),
            MaterialKolorTokens.textPrimaryInverse to textPrimaryInverse(),
            MaterialKolorTokens.textSecondaryAndTertiaryInverse to textSecondaryAndTertiaryInverse(),
            MaterialKolorTokens.textPrimaryInverseDisableOnly to textPrimaryInverseDisableOnly(),
            MaterialKolorTokens.textSecondaryAndTertiaryInverseDisabled to textSecondaryAndTertiaryInverseDisabled(),
            MaterialKolorTokens.textHintInverse to textHintInverse(),
        )
}
