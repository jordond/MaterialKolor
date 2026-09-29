package com.materialkolor.builder.domain.capability

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ForTargetTest {
    /**
     * A document field [ThemeDocument.forTarget] can clear, read off a document.
     */
    private enum class Field(
        val read: (ThemeDocument) -> Any?,
    ) {
        PrimaryKeyColor(read = { document -> document.keyColors.primary }),
        OtherKeyColors(read = { document -> document.keyColors.copy(primary = null) }),
        CmfSecondSeed(read = { document -> document.cmfTertiarySeed }),
        Contrast(read = { document -> document.contrast }),
        Platform(read = { document -> document.platform }),
        Pins(read = { document -> document.pins }),
        Amoled(read = { document -> document.amoled }),
        Accents(read = { document -> document.accents }),
        MotionScheme(read = { document -> document.motionScheme }),
        CustomTones(read = { document -> document.customTones }),
    }

    /**
     * Every field set away from its default, on Cmf at the 2026 spec, which is the one context where
     * the Cmf second seed and the platform both show.
     */
    private val full = ThemeDocument(
        seed = Argb(0xFF6750A4.toInt()),
        keyColors = KeyColors(
            primary = Argb(0xFF8B1A10.toInt()),
            secondary = Argb(0xFF625B71.toInt()),
            tertiary = Argb(0xFF7D5260.toInt()),
            error = Argb(0xFFB3261E.toInt()),
            neutral = Argb(0xFF605D62.toInt()),
            neutralVariant = Argb(0xFF605D66.toInt()),
        ),
        style = Style.Cmf,
        cmfTertiarySeed = Argb(0xFF2E7D32.toInt()),
        contrast = ContrastLevel.High,
        spec = SpecVersion.Spec2026,
        platform = SchemePlatform.Watch,
        amoled = true,
        accents = listOf(Accent(name = "Brand", seed = Argb(0xFF1E88E5.toInt()))),
        pins = mapOf(Role.Primary to RolePin(light = Argb(0xFF123456.toInt()))),
        motionScheme = MotionSchemeChoice.Standard,
        customTones = mapOf(CustomSlot.PrimaryPressed to CustomTone(light = 35)),
    )

    /**
     * The same document with every field on its default, which is what a cleared field reads.
     */
    private val blank = ThemeDocument(seed = full.seed)

    /**
     * What each target clears off [full], written out by hand. The reason is the one the capability
     * table gives, or null for a row that is hidden without one.
     */
    private val cleared: Map<ExportTarget, Map<Field, Reason?>> = mapOf(
        ExportTarget.Material3 to mapOf(
            Field.MotionScheme to null,
            Field.CustomTones to null,
        ),
        ExportTarget.Material3Expressive to mapOf(
            Field.CustomTones to null,
        ),
        ExportTarget.Unstyled to mapOf(
            Field.Amoled to Reason.UnstyledNoAmoled,
            Field.MotionScheme to null,
            Field.CustomTones to null,
        ),
        ExportTarget.Fluent to mapOf(
            Field.OtherKeyColors to Reason.FluentOneRamp,
            Field.CmfSecondSeed to Reason.FluentOneRamp,
            Field.Contrast to Reason.FluentFixedText,
            Field.Pins to Reason.FluentUsesNoRoles,
            Field.Amoled to null,
            Field.Accents to Reason.FluentNoAccents,
            Field.MotionScheme to null,
            Field.CustomTones to null,
        ),
        ExportTarget.Custom to mapOf(
            Field.MotionScheme to null,
        ),
        ExportTarget.Inklet to mapOf(
            Field.MotionScheme to null,
            Field.CustomTones to null,
        ),
    )

    /**
     * The reasons with no document field, which the KDoc of [ThemeDocument.forTarget] names.
     */
    private val withoutField = setOf(
        Reason.FluentStyleChroma,
        Reason.ToneSlotsKeepTones,
        Reason.PlatformLimits,
        Reason.DimRolesUnexposed,
    )

    @Test
    fun forTarget_everyTargetAndField_clearsItWhereTurnedOffAndKeepsItWhereOn() {
        for (target in ExportTarget.entries) {
            val seen = full.forTarget(target)
            for (field in Field.entries) {
                val expected = if (field in cleared.getValue(target)) field.read(blank) else field.read(full)
                assertEquals(expected, field.read(seen), "$field for $target")
            }
        }
    }

    @Test
    fun forTarget_everyTargetAndReason_clearsTheReasonsFieldExactlyWhereTheTableTurnsItOff() {
        for (target in ExportTarget.entries) {
            val turnedOff = reasonsTurningOff(target)
            val clearing = cleared
                .getValue(target)
                .values
                .filterNotNull()
                .toSet()
            for (reason in Reason.entries) {
                val expected = reason in turnedOff && reason !in withoutField
                assertEquals(expected, reason in clearing, "$reason for $target")
            }
        }
    }

    @Test
    fun forTarget_reasonsWithoutAField_neverClearAnything() {
        cleared.values.forEach { fields ->
            fields.values.filterNotNull().forEach { reason -> assertFalse(reason in withoutField, "$reason") }
        }
    }

    @Test
    fun forTarget_styleOtherThanCmf_dropsTheCmfSecondSeedForEveryTarget() {
        val document = ThemeDocument(seed = full.seed, style = Style.TonalSpot, cmfTertiarySeed = full.cmfTertiarySeed)

        for (target in ExportTarget.entries) {
            assertEquals(null, document.forTarget(target).cmfTertiarySeed, "$target")
        }
    }

    @Test
    fun forTarget_effective2021_putsThePlatformBackForEveryTarget() {
        val document = ThemeDocument(
            seed = full.seed,
            style = Style.Rainbow,
            spec = SpecVersion.Spec2025,
            platform = SchemePlatform.Watch,
        )

        for (target in ExportTarget.entries) {
            assertEquals(SchemePlatform.Phone, document.forTarget(target).platform, "$target")
            val revised = document.copy(style = Style.TonalSpot).forTarget(target)
            assertEquals(SchemePlatform.Watch, revised.platform, "$target on TonalSpot")
        }
    }

    @Test
    fun forTarget_keepsTheSeedStyleSpecLibraryAndName() {
        val named = full.copy(library = Library.Unstyled, expressive = true, themeName = "Brand")

        for (target in ExportTarget.entries) {
            val seen = named.forTarget(target)
            assertEquals(named.seed, seen.seed, "$target")
            assertEquals(named.style, seen.style, "$target")
            assertEquals(named.spec, seen.spec, "$target")
            assertEquals(named.library, seen.library, "$target")
            assertEquals(named.expressive, seen.expressive, "$target")
            assertEquals(named.themeName, seen.themeName, "$target")
        }
    }

    @Test
    fun forTarget_appliedTwice_isTheSameAsOnce() {
        for (target in ExportTarget.entries) {
            val once = full.forTarget(target)
            assertEquals(once, once.forTarget(target), "$target")
        }
    }

    @Test
    fun forTarget_defaultDocument_isLeftAsItIs() {
        for (target in ExportTarget.entries) {
            assertEquals(ThemeDocument.Default, ThemeDocument.Default.forTarget(target), "$target")
        }
    }

    /**
     * Every reason the table gives a disabled or hidden control for [target] on the context of [full].
     */
    private fun reasonsTurningOff(target: ExportTarget): Set<Reason> {
        val library = when (target) {
            ExportTarget.Material3,
            ExportTarget.Material3Expressive,
            -> Library.Material3
            ExportTarget.Unstyled -> Library.Unstyled
            ExportTarget.Fluent -> Library.Fluent
            ExportTarget.Custom -> Library.Custom
            ExportTarget.Inklet -> Library.Inklet
        }
        val capabilities = Capabilities.of(
            library = library,
            expressive = target == ExportTarget.Material3Expressive,
            style = full.style,
            effectiveSpec = SpecVersion.Spec2026,
        )

        return Control.entries.mapNotNullTo(mutableSetOf()) { control ->
            when (val state = capabilities[control]) {
                is ControlState.Enabled -> null
                is ControlState.Hidden -> state.reason
                is ControlState.Disabled -> state.reason
            }
        }
    }
}
