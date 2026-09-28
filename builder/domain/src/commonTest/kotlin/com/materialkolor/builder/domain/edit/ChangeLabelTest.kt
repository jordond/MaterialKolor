package com.materialkolor.builder.domain.edit

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangeLabelTest {
    private val red = Argb(0xFFFF0000.toInt())

    @Test
    fun label_everyChange_producesItsTextKeyAndDetail() {
        val expected = listOf(
            DocumentChange.SetSeed(red, SeedSource.Typed) to ("change_seed" to "#FF0000"),
            DocumentChange.SetSeed(red, SeedSource.Image(name = "cactus.png")) to ("change_seed" to "#FF0000"),
            DocumentChange.SetSeed(red, SeedSource.Preset(id = "monstera")) to ("change_preset" to "monstera"),
            DocumentChange.SetKeyColor(KeyColor.Tertiary, red) to ("change_key_color" to "Tertiary"),
            DocumentChange.ResetKeyColors to ("change_reset_key_colors" to null),
            DocumentChange.SetStyle(Style.Vibrant) to ("change_style" to "Vibrant"),
            DocumentChange.SetCmfSeed(red) to ("change_cmf_seed" to "#FF0000"),
            DocumentChange.SetCmfSeed(null) to ("change_cmf_seed" to null),
            DocumentChange.SetContrast(ContrastLevel.Medium) to ("change_contrast" to null),
            DocumentChange.SetSpec(SpecVersion.Spec2026) to ("change_spec" to "Spec2026"),
            DocumentChange.SetPlatform(SchemePlatform.Watch) to ("change_platform" to "Watch"),
            DocumentChange.SetAmoled(true) to ("change_amoled" to null),
            DocumentChange.AddAccent(Accent(name = "brand", seed = red)) to ("change_add_accent" to "brand"),
            DocumentChange.UpdateAccent(0, Accent(name = "sage", seed = red)) to ("change_update_accent" to "sage"),
            DocumentChange.RemoveAccent(0) to ("change_remove_accent" to null),
            DocumentChange.SetPin(Role.Outline, PinMode.Dark, red) to ("change_pin" to "Outline"),
            DocumentChange.ClearPins to ("change_clear_pins" to null),
            DocumentChange.SetLibrary(Library.Fluent, expressive = false) to ("change_library" to "Fluent"),
            // A pick that carries the style along is still named as a library change.
            DocumentChange.SetLibrary(Library.Material3, true, Style.Expressive, SpecVersion.Spec2025) to
                ("change_library" to "Material3"),
            DocumentChange.SetMotionScheme(MotionSchemeChoice.Standard) to ("change_motion_scheme" to "Standard"),
            DocumentChange.SetThemeName("PlumTheme") to ("change_theme_name" to "PlumTheme"),
            DocumentChange.SetCustomTone(CustomSlot.FocusRing, CustomTone(dark = 70)) to
                ("change_custom_tone" to "FocusRing"),
            DocumentChange.Replace(ThemeDocument.Default) to ("change_replace" to null),
        )

        expected.forEach { (change, label) ->
            val (textKey, detail) = label
            assertEquals(textKey, change.label.textKey, change.toString())
            assertEquals(detail, change.label.detail, change.toString())
        }
    }

    @Test
    fun changeKind_everyEntry_hasItsOwnSnakeCaseTextKey() {
        val keys = ChangeKind.entries.map { kind -> kind.textKey }

        assertEquals(keys.size, keys.toSet().size, "Two kinds share a text key")
        keys.forEach { key -> assertTrue(Regex("change_[a-z_]+").matches(key), key) }
    }

    @Test
    fun merges_bigAndStructuralChanges_neverMerge() {
        val neverMerge = listOf(
            DocumentChange.SetStyle(Style.Vibrant),
            DocumentChange.SetContrast(ContrastLevel.High),
            DocumentChange.SetLibrary(Library.Fluent, expressive = true),
            DocumentChange.SetSeed(red, SeedSource.Preset(id = "plum")),
            DocumentChange.Replace(ThemeDocument.Default),
            DocumentChange.AddAccent(Accent(name = "brand", seed = red)),
            DocumentChange.RemoveAccent(0),
            DocumentChange.ResetKeyColors,
            DocumentChange.ClearPins,
        )

        neverMerge.forEach { change -> assertFalse(change.merges, change.toString()) }
    }

    @Test
    fun merges_valueEdits_mayMerge() {
        val mayMerge = listOf(
            DocumentChange.SetSeed(red, SeedSource.Typed),
            DocumentChange.SetKeyColor(KeyColor.Primary, red),
            DocumentChange.SetCmfSeed(red),
            DocumentChange.SetSpec(SpecVersion.Spec2025),
            DocumentChange.SetPlatform(SchemePlatform.Watch),
            DocumentChange.SetAmoled(true),
            DocumentChange.UpdateAccent(0, Accent(name = "brand", seed = red)),
            DocumentChange.SetPin(Role.Primary, PinMode.Light, red),
            DocumentChange.SetMotionScheme(MotionSchemeChoice.Standard),
            DocumentChange.SetThemeName("Plum"),
            DocumentChange.SetCustomTone(CustomSlot.Shadow, null),
        )

        mayMerge.forEach { change -> assertTrue(change.merges, change.toString()) }
    }

    @Test
    fun coalesceKey_sameTargetDifferentValue_matches() {
        assertEquals(
            DocumentChange.SetThemeName("A").coalesceKey,
            DocumentChange.SetThemeName("Ab").coalesceKey,
        )
        assertEquals(
            DocumentChange.SetPin(Role.Primary, PinMode.Light, red).coalesceKey,
            DocumentChange.SetPin(Role.Primary, PinMode.Light, null).coalesceKey,
        )
    }

    @Test
    fun coalesceKey_differentTarget_differs() {
        assertTrue(
            DocumentChange.SetPin(Role.Primary, PinMode.Light, red).coalesceKey !=
                DocumentChange.SetPin(Role.Primary, PinMode.Dark, red).coalesceKey,
        )
        assertTrue(
            DocumentChange.SetKeyColor(KeyColor.Primary, red).coalesceKey !=
                DocumentChange.SetKeyColor(KeyColor.Secondary, red).coalesceKey,
        )
        assertTrue(
            DocumentChange.UpdateAccent(0, Accent(name = "a", seed = red)).coalesceKey !=
                DocumentChange.UpdateAccent(1, Accent(name = "a", seed = red)).coalesceKey,
        )
        assertTrue(
            DocumentChange.SetCustomTone(CustomSlot.Primary, null).coalesceKey !=
                DocumentChange.SetCustomTone(CustomSlot.Secondary, null).coalesceKey,
        )
    }
}
