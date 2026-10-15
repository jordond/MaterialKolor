package com.materialkolor.builder.domain.capability

import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CapabilitiesTest {
    private data class Column(
        val name: String,
        val library: Library,
        val expressive: Boolean,
    )

    /**
     * The five matrix columns, in the order each row of [matrix] lists its cells.
     */
    private val columns = listOf(
        Column(name = "M3", library = Library.Material3, expressive = false),
        Column(name = "M3 Expressive", library = Library.Material3, expressive = true),
        Column(name = "Unstyled", library = Library.Unstyled, expressive = false),
        Column(name = "Fluent", library = Library.Fluent, expressive = false),
        Column(name = "Custom", library = Library.Custom, expressive = false),
    )

    private val yes = ControlState.Enabled()

    private val hidden = ControlState.Hidden()

    private fun yes(note: Reason) = ControlState.Enabled(note)

    private fun hidden(reason: Reason) = ControlState.Hidden(reason)

    private fun no(reason: Reason) = ControlState.Disabled(reason)

    /**
     * One row per control and one cell per column.
     *
     * Written with the style on Cmf at the 2026 spec, which is the one context where the Cmf
     * second seed and the platform picker both show. The rows that move with style and spec have
     * tests of their own below.
     */
    private val matrix: Map<Control, List<ControlState>> = mapOf(
        Control.SeedEntryPoints to listOf(yes, yes, yes, yes, yes),
        Control.PrimaryOverride to listOf(yes, yes, yes, yes, yes),
        Control.OtherOverrides to listOf(yes, yes, yes, no(Reason.FluentOneRamp), yes),
        Control.Style to listOf(yes, yes, yes, yes(Reason.FluentStyleChroma), yes),
        Control.CmfSecondSeed to listOf(yes, yes, yes, no(Reason.FluentOneRamp), yes),
        Control.Contrast to listOf(yes, yes, yes, no(Reason.FluentFixedText), yes(Reason.ToneSlotsKeepTones)),
        Control.SpecVersion to listOf(yes, yes, yes, yes, yes),
        Control.Platform to listOf(yes, yes, yes, yes, yes),
        Control.RolePins to listOf(yes, yes, yes, no(Reason.FluentUsesNoRoles), yes),
        Control.AmoledDark to listOf(yes, yes, no(Reason.UnstyledNoAmoled), hidden, yes),
        Control.MotionScheme to listOf(hidden, yes, hidden, hidden, hidden),
        Control.ColorAnimation to listOf(yes, yes, yes, yes, hidden),
        Control.ExtendedColors to listOf(yes, yes, yes, no(Reason.FluentNoAccents), yes),
        Control.CustomToneTable to listOf(hidden, hidden, hidden, hidden, yes),
        Control.PreviewModes to listOf(yes, yes, yes, yes, yes),
        Control.DeviceWidth to listOf(yes, yes, yes, yes, yes),
        Control.Inspect to listOf(yes, yes, yes, yes, yes),
        Control.FrozenExport to listOf(yes, yes, yes, yes, yes),
        Control.KmpOrAndroid to listOf(yes, yes, yes(Reason.PlatformLimits), yes(Reason.PlatformLimits), yes),
        Control.VersionCatalog to listOf(yes, yes, yes, yes, yes),
        Control.DimRoles to List(columns.size) { hidden(Reason.DimRolesUnexposed) },
    )

    /**
     * The rows whose cells move with the style or the effective spec.
     */
    private val contextRows = setOf(Control.CmfSecondSeed, Control.Platform)

    private fun Column.capabilities(
        style: Style,
        effectiveSpec: SpecVersion,
    ): Capabilities =
        Capabilities.of(library = library, expressive = expressive, style = style, effectiveSpec = effectiveSpec)

    @Test
    fun matrix_everyControl_hasARow() {
        assertEquals(Control.entries.toSet(), matrix.keys)
        matrix.forEach { (control, cells) ->
            assertEquals(columns.size, cells.size, "$control has the wrong cell count")
        }
    }

    @Test
    fun of_everyTargetOnCmf_matchesTheSpecMatrix() {
        columns.forEachIndexed { index, column ->
            val capabilities = column.capabilities(style = Style.Cmf, effectiveSpec = SpecVersion.Spec2026)
            Control.entries.forEach { control ->
                assertEquals(matrix.getValue(control)[index], capabilities[control], "$control for ${column.name}")
            }
        }
    }

    @Test
    fun of_everyStyleAndSpec_leavesTheOtherRowsAsTheMatrixSays() {
        for (style in Style.entries) {
            for (spec in SpecVersion.entries) {
                columns.forEachIndexed { index, column ->
                    val capabilities = column.capabilities(style = style, effectiveSpec = spec)
                    (Control.entries - contextRows).forEach { control ->
                        assertEquals(
                            expected = matrix.getValue(control)[index],
                            actual = capabilities[control],
                            message = "$control for ${column.name} on $style at $spec",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun of_effective2021_hidesThePlatformEverywhere() {
        for (column in columns) {
            val capabilities = column.capabilities(style = Style.Rainbow, effectiveSpec = SpecVersion.Spec2021)
            assertEquals(hidden, capabilities[Control.Platform], column.name)
        }
    }

    @Test
    fun of_effective2025Or2026_showsThePlatformEverywhere() {
        for (column in columns) {
            for (spec in listOf(SpecVersion.Spec2025, SpecVersion.Spec2026)) {
                val capabilities = column.capabilities(style = Style.TonalSpot, effectiveSpec = spec)
                assertEquals(yes, capabilities[Control.Platform], "${column.name} at $spec")
            }
        }
    }

    @Test
    fun of_styleOtherThanCmf_hidesTheCmfSecondSeedEverywhere() {
        for (style in Style.entries - Style.Cmf) {
            for (column in columns) {
                val capabilities = column.capabilities(style = style, effectiveSpec = SpecVersion.Spec2021)
                assertEquals(hidden, capabilities[Control.CmfSecondSeed], "${column.name} on $style")
            }
        }
    }

    @Test
    fun of_expressiveOnALibraryWithoutIt_readsAsThatLibrary() {
        for (library in Library.entries - Library.Material3) {
            val plain = Capabilities.of(
                library,
                expressive = false,
                style = Style.TonalSpot,
                effectiveSpec = SpecVersion.Spec2025,
            )
            val expressive = Capabilities.of(
                library,
                expressive = true,
                style = Style.TonalSpot,
                effectiveSpec = SpecVersion.Spec2025,
            )
            assertEquals(plain, expressive, library.name)
        }
    }

    @Test
    fun reason_everyEntry_hasItsOwnSnakeCaseKey() {
        val keys = Reason.entries.map { reason -> reason.key }

        assertEquals(keys.size, keys.toSet().size, "Two reasons share a key")
        keys.forEach { key -> assertTrue(key.matches(Regex("[a-z0-9_]+")), "$key is not a resource name") }
    }

    @Test
    fun reason_everyEntry_isUsedByTheMatrix() {
        val used = matrix.values.flatten().mapNotNullTo(mutableSetOf()) { state ->
            when (state) {
                is ControlState.Enabled -> state.note
                is ControlState.Hidden -> state.reason
                is ControlState.Disabled -> state.reason
            }
        }

        assertEquals(Reason.entries.toSet(), used)
    }
}
