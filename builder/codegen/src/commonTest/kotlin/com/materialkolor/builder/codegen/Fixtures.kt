package com.materialkolor.builder.codegen

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.export.AccentColors
import com.materialkolor.builder.domain.export.AccentFamilyValues
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.CustomSlotValues
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.export.FluentShades
import com.materialkolor.builder.domain.export.ResolvedExport
import com.materialkolor.builder.domain.export.RoleTable
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.FamilyTones
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SchemePlatform
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.FrozenVariants

/**
 * One named export input the codegen tests share.
 *
 * @property[name] A short kebab case name, which golden cases are named after.
 * @property[input] The input itself.
 */
internal class Fixture(
    val name: String,
    val input: ExportInput,
) {
    /** This fixture with its document or prefs changed, and its colors and link redone to match. */
    fun with(
        document: ThemeDocument = input.document,
        prefs: ExportPrefs = input.prefs,
    ): Fixture = Fixture(name, Fixtures.input(document, prefs))

    override fun toString(): String = name
}

/**
 * The export inputs every codegen test draws from.
 *
 * Each one changes a single thing about the default theme, so a golden diff points straight at
 * the option that caused it. The resolved colors are made up by a fixed rule rather than computed,
 * which keeps codegen tests free of the engine. They only have to be stable and distinct, not
 * right.
 */
internal object Fixtures {
    val Versions: ExportVersions = ExportVersions(
        builder = "2.0.0",
        materialKolor = "6.0.0",
        fluent = "v0.1.0",
        composeUnstyled = "2.10.0",
    )

    // Declared ahead of the fixtures, which read them while the object is still being built.
    private val Black: Argb = Argb(0)
    private val AmoledBlackRoles: Set<Role> = setOf(
        Role.Background,
        Role.Surface,
        Role.SurfaceDim,
        Role.SurfaceContainerLowest,
        Role.SurfaceContainerLow,
        Role.SurfaceContainer,
    )

    val Default: Fixture = fixture("default", ThemeDocument.Default)

    val PrimaryOverride: Fixture = fixture(
        name = "primary-override",
        document = ThemeDocument.Default.copy(keyColors = KeyColors(primary = argb(0xFF6750A4))),
    )

    val AllOverrides: Fixture = fixture(
        name = "all-overrides",
        document = ThemeDocument.Default.copy(
            keyColors = KeyColors(
                primary = argb(0xFF6750A4),
                secondary = argb(0xFF625B71),
                tertiary = argb(0xFF7D5260),
                error = argb(0xFFB3261E),
                neutral = argb(0xFF605D62),
                neutralVariant = argb(0xFF605D66),
            ),
        ),
    )

    val Cmf: Fixture = fixture(
        name = "cmf",
        document = ThemeDocument.Default.copy(
            style = Style.Cmf,
            cmfTertiarySeed = argb(0xFF2E7D32),
            spec = SpecVersion.Spec2026,
        ),
    )

    val ReducedContrast: Fixture = fixture(
        name = "reduced-contrast",
        document = ThemeDocument.Default.copy(contrast = ContrastLevel.Reduced),
    )

    val HighContrast: Fixture = fixture(
        name = "high-contrast",
        document = ThemeDocument.Default.copy(contrast = ContrastLevel.High),
    )

    val Amoled: Fixture = fixture(
        name = "amoled",
        document = ThemeDocument.Default.copy(amoled = true),
    )

    val Watch2025: Fixture = fixture(
        name = "watch-2025",
        document = ThemeDocument.Default.copy(platform = SchemePlatform.Watch, spec = SpecVersion.Spec2025),
    )

    val ThreeAccents: Fixture = fixture(
        name = "three-accents",
        document = ThemeDocument.Default.copy(
            accents = listOf(
                Accent(name = "Brand", seed = argb(0xFF1E88E5)),
                Accent(name = "Success", seed = argb(0xFF43A047), harmonize = false),
                Accent(
                    name = "Warning",
                    seed = argb(0xFFFB8C00),
                    light = FamilyTones(color = 50, container = 95),
                    dark = FamilyTones(color = 70, container = 20),
                    threshold = OnColorThreshold.Aaa,
                ),
            ),
        ),
    )

    val Pins: Fixture = fixture(
        name = "pins",
        document = ThemeDocument.Default.copy(
            pins = mapOf(
                Role.Primary to RolePin(light = argb(0xFF8B1A10), dark = argb(0xFFFFB4A8)),
                Role.Surface to RolePin(light = argb(0xFFFFFBFF)),
                Role.Outline to RolePin(dark = argb(0xFF9A8C89)),
            ),
        ),
    )

    val AndroidOnly: Fixture = fixture(
        name = "android-only",
        document = ThemeDocument.Default,
        prefs = ExportPrefs(multiplatform = false),
    )

    val NoCatalog: Fixture = fixture(
        name = "no-catalog",
        document = ThemeDocument.Default,
        prefs = ExportPrefs(versionCatalog = false),
    )

    val Animated: Fixture = fixture(
        name = "animated",
        document = ThemeDocument.Default,
        prefs = ExportPrefs(animate = true, animationDurationMs = 500),
    )

    val ExpressiveOnTonalSpot2021: Fixture = fixture(
        name = "expressive-tonal-spot-2021",
        document = ThemeDocument.Default.copy(expressive = true),
    )

    val all: List<Fixture> = listOf(
        Default,
        PrimaryOverride,
        AllOverrides,
        Cmf,
        ReducedContrast,
        HighContrast,
        Amoled,
        Watch2025,
        ThreeAccents,
        Pins,
        AndroidOnly,
        NoCatalog,
        Animated,
        ExpressiveOnTonalSpot2021,
    )

    /** An export input for [document], with made up colors that follow from it and its real link. */
    fun input(
        document: ThemeDocument,
        prefs: ExportPrefs = ExportPrefs(),
        versions: ExportVersions = Versions,
    ): ExportInput =
        ExportInput(
            document = document,
            prefs = prefs,
            resolved = resolve(document, prefs),
            versions = versions,
            shareUrl = SHARE_URL_BASE + ShareCodec.encode(document),
        )

    /**
     * Colors for every role, accent and slot the export could ask for, at the contrast variants
     * [prefs] asks for. Like the engine, only a frozen export gets more than the standard one.
     *
     * Each color is its palette's source nudged by the role and the contrast, so overrides, pins,
     * AMOLED and contrast each change the colors the way a reviewer would expect to see in a diff.
     */
    fun resolve(
        document: ThemeDocument,
        prefs: ExportPrefs,
    ): ResolvedExport {
        val variants = when (prefs.mode) {
            ExportMode.Dynamic -> {
                listOf(ContrastVariant.Standard)
            }
            ExportMode.Frozen -> {
                when (prefs.frozenVariants) {
                    FrozenVariants.StandardOnly -> listOf(ContrastVariant.Standard)
                    FrozenVariants.AllContrasts -> ContrastVariant.entries
                }
            }
        }

        return ResolvedExport(
            roles = variants.associateWith { variant -> roleTable(document, contrastOf(document, variant)) },
            accents = document.accents.map(::accentFamily),
            customSlots = if (document.library == Library.Custom) {
                variants.associateWith { variant -> customSlots(document, contrastOf(document, variant)) }
            } else {
                emptyMap()
            },
            fluentShades = if (document.library == Library.Fluent) fluentShades(document.seed) else null,
        )
    }

    private fun fixture(
        name: String,
        document: ThemeDocument,
        prefs: ExportPrefs = ExportPrefs(),
    ): Fixture = Fixture(name, input(document, prefs))

    private fun contrastOf(
        document: ThemeDocument,
        variant: ContrastVariant,
    ): Int =
        when (variant) {
            ContrastVariant.Standard -> document.contrast.hundredths
            ContrastVariant.Medium -> ContrastLevel.Medium.hundredths
            ContrastVariant.High -> ContrastLevel.High.hundredths
        }

    private fun roleTable(
        document: ThemeDocument,
        contrast: Int,
    ): RoleTable {
        fun color(
            role: Role,
            dark: Boolean,
        ): Argb {
            val pin = document.pins[role]
            val pinned = if (dark) pin?.dark else pin?.light

            return when {
                pinned != null -> pinned
                dark && document.amoled && role in AmoledBlackRoles -> Black
                else -> nudge(sourceOf(role, document), role.code * ROLE_SPREAD + contrast, dark)
            }
        }

        return RoleTable(
            light = Role.entries.associateWith { role -> color(role, dark = false) },
            dark = Role.entries.associateWith { role -> color(role, dark = true) },
        )
    }

    private fun sourceOf(
        role: Role,
        document: ThemeDocument,
    ): Argb {
        val name = role.name
        val keys = document.keyColors
        val override = when {
            "Primary" in name -> keys.primary
            "Secondary" in name -> keys.secondary
            "Tertiary" in name -> document.cmfTertiarySeed ?: keys.tertiary
            "Error" in name -> keys.error
            "Variant" in name || "Outline" in name -> keys.neutralVariant
            else -> keys.neutral
        }

        return override ?: document.seed
    }

    private fun accentFamily(accent: Accent): AccentFamilyValues {
        fun colors(dark: Boolean): AccentColors =
            AccentColors(
                color = nudge(accent.seed, salt = 1, dark = dark),
                onColor = nudge(accent.seed, salt = 2, dark = dark),
                container = nudge(accent.seed, salt = 3, dark = dark),
                onContainer = nudge(accent.seed, salt = 4, dark = dark),
            )

        return AccentFamilyValues(name = accent.name, light = colors(dark = false), dark = colors(dark = true))
    }

    private fun customSlots(
        document: ThemeDocument,
        contrast: Int,
    ): CustomSlotValues =
        CustomSlotValues(
            light = CustomSlot.entries.associateWith { slot ->
                nudge(document.seed, slot.code * SLOT_SPREAD + contrast, dark = false)
            },
            dark = CustomSlot.entries.associateWith { slot ->
                nudge(document.seed, slot.code * SLOT_SPREAD + contrast, dark = true)
            },
        )

    private fun fluentShades(seed: Argb): FluentShades =
        FluentShades(
            light = fluentShadeValues(seed, from = 0),
            dark = fluentShadeValues(seed, from = DARK_SHADES_SALT),
        )

    /** The seven shades of one mode, nudged on from the salt [from]. Dark starts further along, so the sets differ. */
    private fun fluentShadeValues(
        seed: Argb,
        from: Int,
    ): FluentShadeValues =
        FluentShadeValues(
            dark3 = nudge(seed, salt = from + 1, dark = true),
            dark2 = nudge(seed, salt = from + 2, dark = true),
            dark1 = nudge(seed, salt = from + 3, dark = true),
            base = nudge(seed, salt = from, dark = false),
            light1 = nudge(seed, salt = from + 1, dark = false),
            light2 = nudge(seed, salt = from + 2, dark = false),
            light3 = nudge(seed, salt = from + 3, dark = false),
        )

    /** [source] moved by [salt] steps, and lifted for dark mode. Plain Int math, so every platform agrees. */
    private fun nudge(
        source: Argb,
        salt: Int,
        dark: Boolean,
    ): Argb = Argb(source.value + salt * SALT_STEP + if (dark) DARK_LIFT else 0)

    private fun argb(value: Long): Argb = Argb(value.toInt())
}

private const val SHARE_URL_BASE = "https://materialkolor.com/t/"
private const val SALT_STEP = 0x00030507
private const val DARK_LIFT = 0x00808080
private const val ROLE_SPREAD = 7
private const val SLOT_SPREAD = 11
private const val DARK_SHADES_SALT = 4
