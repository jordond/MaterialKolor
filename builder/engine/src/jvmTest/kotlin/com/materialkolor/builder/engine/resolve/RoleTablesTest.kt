package com.materialkolor.builder.engine.resolve

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.TestDocuments
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.hct.Hct
import com.materialkolor.material3.dynamicColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RoleTablesTest {
    private val document = ThemeDocument(seed = Argb(0x6750A4))

    @Test
    fun from_randomDocuments_matchesTheMaterial3ColorScheme() {
        val resolver = ThemeResolver()
        val documents = TestDocuments().documents(20)
        assertTrue(documents.any { candidate -> candidate.amoled }, "the sample should hold an AMOLED document")
        assertTrue(documents.any { candidate -> candidate.pins.isNotEmpty() }, "the sample should hold a pin")

        for (document in documents) {
            val roles = resolver.resolve(document).roles
            for (isDark in listOf(false, true)) {
                val expected = referenceColorScheme(document, isDark)
                for (role in Role.entries) {
                    val pin = document.pins[role]?.let { pin -> if (isDark) pin.dark else pin.light }
                    val want = pin ?: Argb(expected.color(role).toArgb())

                    assertEquals(want, roles[role, isDark].argb, "$role, dark $isDark, in $document")
                }
            }
        }
    }

    @Test
    fun from_tonalSpot2021Light_primaryReadsToneForty() {
        val roles = ThemeResolver().resolve(document).roles

        assertEquals(40.0, roles[Role.Primary, false].tone, absoluteTolerance = 0.5)
        assertEquals(80.0, roles[Role.Primary, true].tone, absoluteTolerance = 0.5)
    }

    @Test
    fun from_highContrast_readsTheToneTheSchemePicked() {
        val roles = ThemeResolver().resolve(document.copy(contrast = ContrastLevel.High)).roles
        val primary = roles[Role.Primary, false]

        assertNotEquals(40.0, primary.tone, absoluteTolerance = 0.5)
        assertEquals(Hct.fromInt(primary.argb.value).tone, primary.tone, absoluteTolerance = 0.5)
    }

    @Test
    fun from_amoled_blackensDarkSurfacesAfterGeneration() {
        val resolver = ThemeResolver()
        val plain = resolver.resolve(document)
        val amoled = resolver.resolve(document.copy(amoled = true))

        assertSame(plain.dark, amoled.dark)
        assertEquals(Argb(0x000000), amoled.roles[Role.Background, true].argb)
        assertEquals(Argb(0x000000), amoled.roles[Role.Surface, true].argb)
        assertEquals(0.0, amoled.roles[Role.Surface, true].tone, absoluteTolerance = 0.01)
        assertEquals(Argb(0xFFFFFF), amoled.roles[Role.OnBackground, true].argb)
        assertEquals(Argb(0xFFFFFF), amoled.roles[Role.OnSurface, true].argb)
        assertEquals(100.0, amoled.roles[Role.OnSurface, true].tone, absoluteTolerance = 0.01)
        assertEquals(plain.roles[Role.SurfaceContainer, true], amoled.roles[Role.SurfaceContainer, true])
        assertEquals(plain.roles.light, amoled.roles.light)
    }

    @Test
    fun from_pins_overlayAfterAmoled() {
        val pinned = Argb(0x223344)
        val result = ThemeResolver().resolve(
            document.copy(
                amoled = true,
                pins = mapOf(
                    Role.Surface to RolePin(dark = pinned),
                    Role.Primary to RolePin(light = pinned),
                ),
            ),
        )

        assertEquals(RoleEntry(pinned, Hct.fromInt(pinned.value).tone), result.roles[Role.Surface, true])
        assertEquals(pinned, result.roles[Role.Primary, false].argb)
        assertNotEquals(pinned, result.roles[Role.Primary, true].argb)
        assertNotEquals(pinned, result.roles[Role.Surface, false].argb)
        assertEquals(Argb(0x000000), result.roles[Role.Background, true].argb)
    }

    @Test
    fun from_everyMode_holdsEveryRoleInOrder() {
        val roles = ThemeResolver().resolve(document).roles

        assertEquals(Role.entries, roles.light.keys.toList())
        assertEquals(Role.entries, roles.dark.keys.toList())
        assertSame(roles.dark, roles.mode(isDark = true))
    }

    /**
     * What the Material 3 module builds for [document], reached through its own public factory and
     * with the document types mapped here rather than through the engine's mapping.
     */
    private fun referenceColorScheme(
        document: ThemeDocument,
        isDark: Boolean,
    ): ColorScheme =
        dynamicColorScheme(
            seedColor = Color(document.seed.value),
            isDark = isDark,
            isAmoled = document.amoled,
            primary = document.keyColors.primary?.let { color -> Color(color.value) },
            secondary = document.keyColors.secondary?.let { color -> Color(color.value) },
            tertiary = document.keyColors.tertiary?.let { color -> Color(color.value) },
            neutral = document.keyColors.neutral?.let { color -> Color(color.value) },
            neutralVariant = document.keyColors.neutralVariant?.let { color -> Color(color.value) },
            error = document.keyColors.error?.let { color -> Color(color.value) },
            style = referenceStyle(document),
            contrastLevel = document.contrast.hundredths / 100.0,
            specVersion = referenceSpec(document.spec),
            platform = referencePlatform(document.platform),
        )

    private fun referenceStyle(document: ThemeDocument): PaletteStyle =
        if (document.style == Style.Cmf) {
            PaletteStyle.Cmf(document.cmfTertiarySeed?.let { color -> Color(color.value) })
        } else {
            PaletteStyle.fromName(document.style.name)
        }

    private fun referenceSpec(spec: SpecVersion): ColorSpec.SpecVersion =
        ColorSpec.SpecVersion.valueOf("SPEC_" + spec.name.removePrefix("Spec"))

    private fun referencePlatform(platform: SchemePlatform): DynamicScheme.Platform =
        DynamicScheme.Platform.valueOf(platform.name.uppercase())

    private fun ColorScheme.color(role: Role): Color =
        when (role) {
            Role.Primary -> primary
            Role.OnPrimary -> onPrimary
            Role.PrimaryContainer -> primaryContainer
            Role.OnPrimaryContainer -> onPrimaryContainer
            Role.InversePrimary -> inversePrimary
            Role.Secondary -> secondary
            Role.OnSecondary -> onSecondary
            Role.SecondaryContainer -> secondaryContainer
            Role.OnSecondaryContainer -> onSecondaryContainer
            Role.Tertiary -> tertiary
            Role.OnTertiary -> onTertiary
            Role.TertiaryContainer -> tertiaryContainer
            Role.OnTertiaryContainer -> onTertiaryContainer
            Role.Background -> background
            Role.OnBackground -> onBackground
            Role.Surface -> surface
            Role.OnSurface -> onSurface
            Role.SurfaceVariant -> surfaceVariant
            Role.OnSurfaceVariant -> onSurfaceVariant
            Role.SurfaceTint -> surfaceTint
            Role.InverseSurface -> inverseSurface
            Role.InverseOnSurface -> inverseOnSurface
            Role.Error -> error
            Role.OnError -> onError
            Role.ErrorContainer -> errorContainer
            Role.OnErrorContainer -> onErrorContainer
            Role.Outline -> outline
            Role.OutlineVariant -> outlineVariant
            Role.Scrim -> scrim
            Role.SurfaceBright -> surfaceBright
            Role.SurfaceDim -> surfaceDim
            Role.SurfaceContainer -> surfaceContainer
            Role.SurfaceContainerHigh -> surfaceContainerHigh
            Role.SurfaceContainerHighest -> surfaceContainerHighest
            Role.SurfaceContainerLow -> surfaceContainerLow
            Role.SurfaceContainerLowest -> surfaceContainerLowest
            Role.PrimaryFixed -> primaryFixed
            Role.PrimaryFixedDim -> primaryFixedDim
            Role.OnPrimaryFixed -> onPrimaryFixed
            Role.OnPrimaryFixedVariant -> onPrimaryFixedVariant
            Role.SecondaryFixed -> secondaryFixed
            Role.SecondaryFixedDim -> secondaryFixedDim
            Role.OnSecondaryFixed -> onSecondaryFixed
            Role.OnSecondaryFixedVariant -> onSecondaryFixedVariant
            Role.TertiaryFixed -> tertiaryFixed
            Role.TertiaryFixedDim -> tertiaryFixedDim
            Role.OnTertiaryFixed -> onTertiaryFixed
            Role.OnTertiaryFixedVariant -> onTertiaryFixedVariant
        }
}
