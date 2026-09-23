package com.materialkolor.builder.engine.export

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.export.AccentFamilyValues
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.CustomSlotValues
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.export.ResolvedExport
import com.materialkolor.builder.domain.export.RoleTable
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.FrozenVariants
import com.materialkolor.builder.engine.resolve.AccentFamily
import com.materialkolor.builder.engine.resolve.CustomSlotColors
import com.materialkolor.builder.engine.resolve.RoleTables
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.palettes.TonalPalette

/**
 * Works out every color an export writes, so the code generator only has to read them.
 *
 * Each contrast variant is the document resolved through [ThemeResolver] at that contrast, so an
 * export writes the very roles, slots and accents the builder shows for it. They come from the
 * document's own schemes. The chrome schemes floor contrast for the builder's own UI and never
 * reach an export.
 *
 * Every part is filled whatever the target, custom slots and Fluent shades included. That keeps
 * this blind to which target asked, and the extra work is a few table reads.
 *
 * Like the resolver it goes through, this belongs to the thread that created it.
 *
 * @param[themes] The resolver every variant goes through. Pass the app's own to share its caches.
 */
public class ExportResolver(
    private val themes: ThemeResolver = ThemeResolver(),
) {
    /**
     * Every color the export of [document] writes with [prefs].
     *
     * The standard variant is the document at its own contrast. When [prefs] asks for every
     * contrast, medium and high are the same document resolved at those levels. Pins, AMOLED and
     * custom tones land on every variant. Accents have one set of values, since contrast never
     * moves them.
     */
    public fun resolve(
        document: ThemeDocument,
        prefs: ExportPrefs,
    ): ResolvedExport {
        val standard = themes.resolve(document)
        val variants = variantsFor(prefs.frozenVariants).associateWith { variant ->
            when (variant) {
                ContrastVariant.Standard -> standard
                ContrastVariant.Medium -> themes.resolve(document.copy(contrast = ContrastLevel.Medium))
                ContrastVariant.High -> themes.resolve(document.copy(contrast = ContrastLevel.High))
            }
        }

        return ResolvedExport(
            roles = variants.mapValues { (_, result) -> result.roles.toRoleTable() },
            accents = standard.accents.families.map { family -> family.toValues() },
            customSlots = variants.mapValues { (_, result) -> result.customSlots.toValues() },
            fluentShades = standard.fluentShades(),
        )
    }

    private fun variantsFor(frozen: FrozenVariants): List<ContrastVariant> =
        when (frozen) {
            FrozenVariants.StandardOnly -> listOf(ContrastVariant.Standard)
            FrozenVariants.AllContrasts -> ContrastVariant.entries
        }
}

private fun RoleTables.toRoleTable(): RoleTable =
    RoleTable(
        light = light.mapValues { (_, entry) -> entry.argb },
        dark = dark.mapValues { (_, entry) -> entry.argb },
    )

private fun CustomSlotColors.toValues(): CustomSlotValues = CustomSlotValues(light = light, dark = dark)

private fun AccentFamily.toValues(): AccentFamilyValues =
    AccentFamilyValues(name = accent.name, light = light, dark = dark)

/**
 * The seven Fluent shades, cut at the tones `toFluentShades` in the Fluent module uses.
 *
 * Fluent takes one set for both modes, so they come from the light scheme's primary palette. The
 * 2025 spec gives some styles a softer dark primary palette, and a frozen Fluent theme keeps the
 * light one in both modes.
 */
private fun ThemeResult.fluentShades(): FluentShadeValues {
    val palette = light.primaryPalette
    return FluentShadeValues(
        dark3 = palette.shade(DARK_3_TONE),
        dark2 = palette.shade(DARK_2_TONE),
        dark1 = palette.shade(DARK_1_TONE),
        base = palette.shade(BASE_TONE),
        light1 = palette.shade(LIGHT_1_TONE),
        light2 = palette.shade(LIGHT_2_TONE),
        light3 = palette.shade(LIGHT_3_TONE),
    )
}

private fun TonalPalette.shade(tone: Int): Argb = Argb(tone(tone))

private const val DARK_3_TONE = 15
private const val DARK_2_TONE = 30
private const val DARK_1_TONE = 40
private const val BASE_TONE = 50
private const val LIGHT_1_TONE = 60
private const val LIGHT_2_TONE = 80
private const val LIGHT_3_TONE = 90
