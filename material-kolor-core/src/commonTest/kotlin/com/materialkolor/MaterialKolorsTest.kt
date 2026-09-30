package com.materialkolor

import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeVibrant
import kotlin.test.Test
import kotlin.test.assertEquals

class MaterialKolorsTest {
    private val darkScheme = vibrant(isDark = true)

    /**
     * Upstream Java MCU output for a vibrant dark scheme seeded with 4285F4 at contrast 0.5.
     */
    private val upstreamDarkRoles = mapOf(
        "primaryPaletteKeyColor" to Color(0xFF047AFF),
        "secondaryPaletteKeyColor" to Color(0xFF5770D5),
        "tertiaryPaletteKeyColor" to Color(0xFFA758B1),
        "errorPaletteKeyColor" to Color(0xFFDB3A3D),
        "neutralPaletteKeyColor" to Color(0xFF6C759E),
        "neutralVariantPaletteKeyColor" to Color(0xFF6774AD),
        "background" to Color(0xFF020A2F),
        "onBackground" to Color(0xFFFFFFFF),
        "surface" to Color(0xFF020A2F),
        "surfaceDim" to Color(0xFF020A2F),
        "surfaceBright" to Color(0xFF182760),
        "surfaceContainerLowest" to Color(0xFF000000),
        "surfaceContainerLow" to Color(0xFF040F38),
        "surfaceContainer" to Color(0xFF091542),
        "surfaceContainerHigh" to Color(0xFF0E1B4C),
        "surfaceContainerHighest" to Color(0xFF132156),
        "onSurface" to Color(0xFFFFFFFF),
        "surfaceVariant" to Color(0xFF132156),
        "onSurfaceVariant" to Color(0xFFAEB7E4),
        "inverseSurface" to Color(0xFFFBF8FF),
        "inverseOnSurface" to Color(0xFF2D365B),
        "outline" to Color(0xFF8991BC),
        "outlineVariant" to Color(0xFF6B739D),
        "shadow" to Color(0xFF000000),
        "scrim" to Color(0xFF000000),
        "surfaceTint" to Color(0xFF97B8FF),
        "primary" to Color(0xFF97B8FF),
        "onPrimary" to Color(0xFF002A61),
        "primaryContainer" to Color(0xFF6D9FFF),
        "onPrimaryContainer" to Color(0xFF001334),
        "inversePrimary" to Color(0xFF0051AF),
        "secondary" to Color(0xFFA5B5FF),
        "onSecondary" to Color(0xFF00217A),
        "secondaryContainer" to Color(0xFF546DD3),
        "onSecondaryContainer" to Color(0xFFFFFFFF),
        "tertiary" to Color(0xFFFBB4FF),
        "onTertiary" to Color(0xFF61156E),
        "tertiaryContainer" to Color(0xFFF79FFE),
        "onTertiaryContainer" to Color(0xFF560463),
        "error" to Color(0xFFFF9F99),
        "onError" to Color(0xFF60000A),
        "errorContainer" to Color(0xFFD7383B),
        "onErrorContainer" to Color(0xFFFFFFFF),
        "primaryFixed" to Color(0xFF6D9FFF),
        "primaryFixedDim" to Color(0xFF5291FF),
        "onPrimaryFixed" to Color(0xFF000000),
        "onPrimaryFixedVariant" to Color(0xFF000000),
        "secondaryFixed" to Color(0xFFC7CFFF),
        "secondaryFixedDim" to Color(0xFFB4C1FF),
        "onSecondaryFixed" to Color(0xFF000831),
        "onSecondaryFixedVariant" to Color(0xFF00288D),
        "tertiaryFixed" to Color(0xFFF79FFE),
        "tertiaryFixedDim" to Color(0xFFE891EF),
        "onTertiaryFixed" to Color(0xFF000000),
        "onTertiaryFixedVariant" to Color(0xFF450051),
        "controlActivated" to Color(0xFF6D9FFF),
        "controlNormal" to Color(0xFFAEB7E4),
        "controlHighlight" to Color(0x33FFFFFF),
        "textPrimaryInverse" to Color(0xFF2D365B),
        "textSecondaryAndTertiaryInverse" to Color(0xFF364379),
        "textPrimaryInverseDisableOnly" to Color(0xFF0F193D),
        "textSecondaryAndTertiaryInverseDisabled" to Color(0xFF0F193D),
        "textHintInverse" to Color(0xFF0F193D),
    )

    @Test
    fun everyRoleMatchesTheUpstreamScheme() {
        assertEquals(upstreamDarkRoles, MaterialKolors(darkScheme).roles())
    }

    @Test
    fun highestSurface_readsTheSchemeItIsGiven() {
        assertEquals(Color(0xFF182760), MaterialKolors(vibrant(isDark = false)).highestSurface(darkScheme))
    }

    @Test
    fun amoled_blackensTheBackgroundAndSurfaceOfADarkScheme() {
        val amoled = MaterialKolors(darkScheme, isAmoled = true).roles()
        val expected = upstreamDarkRoles + mapOf(
            "background" to Color.Black,
            "onBackground" to Color.White,
            "surface" to Color.Black,
            "onSurface" to Color.White,
        )

        assertEquals(expected, amoled)
    }

    @Test
    fun amoled_leavesALightSchemeAlone() {
        val lightScheme = vibrant(isDark = false)

        assertEquals(MaterialKolors(lightScheme).roles(), MaterialKolors(lightScheme, isAmoled = true).roles())
    }

    private fun vibrant(isDark: Boolean): DynamicScheme =
        SchemeVibrant(
            Hct.fromInt(0xFF4285F4.toInt()),
            isDark,
            0.5,
            ColorSpec.SpecVersion.SPEC_2025,
            DynamicScheme.Platform.PHONE,
        )

    private fun MaterialKolors.roles(): Map<String, Color> =
        mapOf(
            "primaryPaletteKeyColor" to primaryPaletteKeyColor(),
            "secondaryPaletteKeyColor" to secondaryPaletteKeyColor(),
            "tertiaryPaletteKeyColor" to tertiaryPaletteKeyColor(),
            "errorPaletteKeyColor" to errorPaletteKeyColor(),
            "neutralPaletteKeyColor" to neutralPaletteKeyColor(),
            "neutralVariantPaletteKeyColor" to neutralVariantPaletteKeyColor(),
            "background" to background(),
            "onBackground" to onBackground(),
            "surface" to surface(),
            "surfaceDim" to surfaceDim(),
            "surfaceBright" to surfaceBright(),
            "surfaceContainerLowest" to surfaceContainerLowest(),
            "surfaceContainerLow" to surfaceContainerLow(),
            "surfaceContainer" to surfaceContainer(),
            "surfaceContainerHigh" to surfaceContainerHigh(),
            "surfaceContainerHighest" to surfaceContainerHighest(),
            "onSurface" to onSurface(),
            "surfaceVariant" to surfaceVariant(),
            "onSurfaceVariant" to onSurfaceVariant(),
            "inverseSurface" to inverseSurface(),
            "inverseOnSurface" to inverseOnSurface(),
            "outline" to outline(),
            "outlineVariant" to outlineVariant(),
            "shadow" to shadow(),
            "scrim" to scrim(),
            "surfaceTint" to surfaceTint(),
            "primary" to primary(),
            "onPrimary" to onPrimary(),
            "primaryContainer" to primaryContainer(),
            "onPrimaryContainer" to onPrimaryContainer(),
            "inversePrimary" to inversePrimary(),
            "secondary" to secondary(),
            "onSecondary" to onSecondary(),
            "secondaryContainer" to secondaryContainer(),
            "onSecondaryContainer" to onSecondaryContainer(),
            "tertiary" to tertiary(),
            "onTertiary" to onTertiary(),
            "tertiaryContainer" to tertiaryContainer(),
            "onTertiaryContainer" to onTertiaryContainer(),
            "error" to error(),
            "onError" to onError(),
            "errorContainer" to errorContainer(),
            "onErrorContainer" to onErrorContainer(),
            "primaryFixed" to primaryFixed(),
            "primaryFixedDim" to primaryFixedDim(),
            "onPrimaryFixed" to onPrimaryFixed(),
            "onPrimaryFixedVariant" to onPrimaryFixedVariant(),
            "secondaryFixed" to secondaryFixed(),
            "secondaryFixedDim" to secondaryFixedDim(),
            "onSecondaryFixed" to onSecondaryFixed(),
            "onSecondaryFixedVariant" to onSecondaryFixedVariant(),
            "tertiaryFixed" to tertiaryFixed(),
            "tertiaryFixedDim" to tertiaryFixedDim(),
            "onTertiaryFixed" to onTertiaryFixed(),
            "onTertiaryFixedVariant" to onTertiaryFixedVariant(),
            "controlActivated" to controlActivated(),
            "controlNormal" to controlNormal(),
            "controlHighlight" to controlHighlight(),
            "textPrimaryInverse" to textPrimaryInverse(),
            "textSecondaryAndTertiaryInverse" to textSecondaryAndTertiaryInverse(),
            "textPrimaryInverseDisableOnly" to textPrimaryInverseDisableOnly(),
            "textSecondaryAndTertiaryInverseDisabled" to textSecondaryAndTertiaryInverseDisabled(),
            "textHintInverse" to textHintInverse(),
        )
}
