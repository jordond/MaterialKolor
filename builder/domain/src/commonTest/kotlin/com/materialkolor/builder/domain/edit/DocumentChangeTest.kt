package com.materialkolor.builder.domain.edit

import com.materialkolor.builder.domain.DocumentArb
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class DocumentChangeTest {
    private val arb = DocumentArb()
    private val red = Argb(0xFFFF0000.toInt())
    private val blue = Argb(0xFF0000FF.toInt())
    private val accent = Accent(name = "brand", seed = red)

    /**
     * One of every change, each paired with what it should do to any document.
     *
     * Spelling the expected result out as a copy of the input is what proves a change touches
     * nothing but the fields it names.
     */
    private val cases: List<Pair<DocumentChange, (ThemeDocument) -> ThemeDocument>> =
        listOf(
            DocumentChange.SetSeed(red, SeedSource.Picked) to { document ->
                document.copy(seed = red, seedSource = SeedSource.Picked)
            },
            DocumentChange.SetKeyColor(KeyColor.Tertiary, blue) to { document ->
                document.copy(keyColors = document.keyColors.copy(tertiary = blue))
            },
            DocumentChange.SetKeyColor(KeyColor.Error, null) to { document ->
                document.copy(keyColors = document.keyColors.copy(error = null))
            },
            DocumentChange.ResetKeyColors to { document -> document.copy(keyColors = KeyColors()) },
            DocumentChange.SetStyle(Style.Vibrant) to { document -> document.copy(style = Style.Vibrant) },
            DocumentChange.SetCmfSeed(blue) to { document -> document.copy(cmfTertiarySeed = blue) },
            DocumentChange.SetCmfSeed(null) to { document -> document.copy(cmfTertiarySeed = null) },
            DocumentChange.SetContrast(ContrastLevel.High) to { document ->
                document.copy(contrast = ContrastLevel.High)
            },
            DocumentChange.SetSpec(SpecVersion.Spec2025) to { document -> document.copy(spec = SpecVersion.Spec2025) },
            DocumentChange.SetPlatform(SchemePlatform.Watch) to { document ->
                document.copy(platform = SchemePlatform.Watch)
            },
            DocumentChange.SetAmoled(true) to { document -> document.copy(amoled = true) },
            DocumentChange.AddAccent(accent) to { document -> document.copy(accents = document.accents + accent) },
            DocumentChange.ClearPins to { document -> document.copy(pins = emptyMap()) },
            DocumentChange.SetLibrary(Library.Fluent, expressive = true) to { document ->
                document.copy(library = Library.Fluent, expressive = true)
            },
            DocumentChange.SetMotionScheme(MotionSchemeChoice.Standard) to { document ->
                document.copy(motionScheme = MotionSchemeChoice.Standard)
            },
            DocumentChange.SetThemeName("BrandTheme") to { document -> document.copy(themeName = "BrandTheme") },
            DocumentChange.SetCustomTone(CustomSlot.PrimaryPressed, CustomTone(light = 30)) to { document ->
                document.copy(
                    customTones = document.customTones + (CustomSlot.PrimaryPressed to CustomTone(light = 30)),
                )
            },
            DocumentChange.SetCustomTone(CustomSlot.Primary, null) to { document ->
                document.copy(customTones = document.customTones - CustomSlot.Primary)
            },
            DocumentChange.Replace(ThemeDocument.Default) to { _ -> ThemeDocument.Default },
        )

    @Test
    fun documentChange_everyChange_touchesOnlyTheFieldsItNames() {
        arb.documents(count = 200).forEach { document ->
            cases.forEach { (change, expected) ->
                assertEquals(expected(document), change.apply(document), change.toString())
            }
        }
    }

    @Test
    fun documentChange_everyChange_leavesItsInputAlone() {
        arb.documents(count = 200).forEach { document ->
            val untouched = document.copy()
            allChanges(document).forEach { change ->
                change.apply(document)
                assertEquals(untouched, document, change.toString())
            }
        }
    }

    @Test
    fun setLibrary_togglingExpressive_neverMovesStyleOrSpec() {
        arb.documents(count = 200).forEach { document ->
            val toggled = DocumentChange.SetLibrary(document.library, !document.expressive).apply(document)

            assertEquals(!document.expressive, toggled.expressive)
            assertEquals(document.style, toggled.style)
            assertEquals(document.spec, toggled.spec)
        }
    }

    @Test
    fun setStyle_anyStyle_leavesTheRequestedSpec() {
        arb.documents(count = 50).forEach { document ->
            Style.entries.forEach { style ->
                val changed = DocumentChange.SetStyle(style).apply(document)

                assertEquals(style, changed.style)
                assertEquals(document.spec, changed.spec, "$style moved the spec")
            }
        }
    }

    @Test
    fun setSpec_anySpec_leavesTheStyle() {
        arb.documents(count = 50).forEach { document ->
            SpecVersion.entries.forEach { spec ->
                val changed = DocumentChange.SetSpec(spec).apply(document)

                assertEquals(spec, changed.spec)
                assertEquals(document.style, changed.style, "$spec moved the style")
            }
        }
    }

    @Test
    fun setPin_oneMode_keepsTheOtherMode() {
        val document = ThemeDocument.Default.copy(pins = mapOf(Role.Primary to RolePin(light = red)))

        val changed = DocumentChange.SetPin(Role.Primary, PinMode.Dark, blue).apply(document)

        assertEquals(RolePin(light = red, dark = blue), changed.pins[Role.Primary])
    }

    @Test
    fun setPin_nullOnOneOfTwoModes_removesOnlyThatMode() {
        val document = ThemeDocument.Default.copy(pins = mapOf(Role.Primary to RolePin(light = red, dark = blue)))

        val changed = DocumentChange.SetPin(Role.Primary, PinMode.Light, null).apply(document)

        assertEquals(RolePin(dark = blue), changed.pins[Role.Primary])
    }

    @Test
    fun setPin_nullOnTheLastMode_removesThePin() {
        val document = ThemeDocument.Default.copy(
            pins = mapOf(Role.Primary to RolePin(dark = blue), Role.Secondary to RolePin(light = red)),
        )

        val changed = DocumentChange.SetPin(Role.Primary, PinMode.Dark, null).apply(document)

        assertNull(changed.pins[Role.Primary])
        assertEquals(mapOf(Role.Secondary to RolePin(light = red)), changed.pins)
    }

    @Test
    fun setPin_unpinnedRole_addsAPinWithOneMode() {
        val changed = DocumentChange.SetPin(Role.Outline, PinMode.Light, red).apply(ThemeDocument.Default)

        assertEquals(mapOf(Role.Outline to RolePin(light = red)), changed.pins)
    }

    @Test
    fun setPin_nullOnAnUnpinnedRole_changesNothing() {
        val changed = DocumentChange.SetPin(Role.Outline, PinMode.Dark, null).apply(ThemeDocument.Default)

        assertEquals(ThemeDocument.Default, changed)
    }

    @Test
    fun updateAccent_indexInRange_replacesOnlyThatAccent() {
        val first = Accent(name = "first", seed = red)
        val second = Accent(name = "second", seed = blue)
        val document = ThemeDocument.Default.copy(accents = listOf(first, second))

        val changed = DocumentChange.UpdateAccent(index = 1, accent = accent).apply(document)

        assertEquals(listOf(first, accent), changed.accents)
    }

    @Test
    fun updateAccent_indexPastTheEnd_changesNothing() {
        val document = ThemeDocument.Default.copy(accents = listOf(accent))

        assertSame(document, DocumentChange.UpdateAccent(index = 1, accent = accent).apply(document))
    }

    @Test
    fun removeAccent_indexInRange_removesOnlyThatAccent() {
        val first = Accent(name = "first", seed = red)
        val second = Accent(name = "second", seed = blue)
        val document = ThemeDocument.Default.copy(accents = listOf(first, accent, second))

        val changed = DocumentChange.RemoveAccent(index = 1).apply(document)

        assertEquals(listOf(first, second), changed.accents)
    }

    @Test
    fun removeAccent_indexPastTheEnd_changesNothing() {
        assertSame(ThemeDocument.Default, DocumentChange.RemoveAccent(index = 0).apply(ThemeDocument.Default))
    }

    @Test
    fun accentChange_negativeIndex_isRejected() {
        assertFailsWith<IllegalArgumentException> { DocumentChange.UpdateAccent(index = -1, accent = accent) }
        assertFailsWith<IllegalArgumentException> { DocumentChange.RemoveAccent(index = -1) }
    }

    @Test
    fun setCustomTone_tone_storesItAsGiven() {
        val tone = CustomTone(light = 12, dark = 88)

        val changed = DocumentChange.SetCustomTone(CustomSlot.SurfaceSunken, tone).apply(ThemeDocument.Default)

        assertEquals(mapOf(CustomSlot.SurfaceSunken to tone), changed.customTones)
    }

    @Test
    fun setCustomTone_null_dropsTheOverride() {
        val document = ThemeDocument.Default.copy(customTones = mapOf(CustomSlot.Primary to CustomTone(light = 50)))

        val changed = DocumentChange.SetCustomTone(CustomSlot.Primary, null).apply(document)

        assertEquals(emptyMap(), changed.customTones)
    }

    @Test
    fun replace_anyDocument_returnsTheReplacement() {
        val replacement = arb.nextDocument()

        arb.documents(count = 20).forEach { document ->
            assertSame(replacement, DocumentChange.Replace(replacement).apply(document))
        }
    }

    @Test
    fun resetKeyColors_anyDocument_leavesEveryPaletteOnTheSeed() {
        arb.documents(count = 50).forEach { document ->
            val changed = DocumentChange.ResetKeyColors.apply(document)

            assertEquals(KeyColors(), changed.keyColors)
            assertFalse(KeyColor.entries.any { slot -> changed.keyColors[slot] != null })
        }
    }

    private fun allChanges(document: ThemeDocument): List<DocumentChange> =
        cases.map { (change, _) -> change } +
            listOf(
                DocumentChange.SetPin(Role.Primary, PinMode.Light, red),
                DocumentChange.SetPin(Role.Primary, PinMode.Dark, null),
                DocumentChange.UpdateAccent(index = 0, accent = accent),
                DocumentChange.RemoveAccent(index = 0),
            ) +
            document.pins.keys.map { role -> DocumentChange.SetPin(role, PinMode.Light, null) }
}
