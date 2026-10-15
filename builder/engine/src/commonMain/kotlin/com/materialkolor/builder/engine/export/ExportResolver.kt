package com.materialkolor.builder.engine.export

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.export.AccentFamilyValues
import com.materialkolor.builder.domain.export.ContrastVariant
import com.materialkolor.builder.domain.export.CustomSlotValues
import com.materialkolor.builder.domain.export.FluentShadeValues
import com.materialkolor.builder.domain.export.FluentShades
import com.materialkolor.builder.domain.export.ResolvedExport
import com.materialkolor.builder.domain.export.RoleTable
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportMode
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
 * this blind to which target asked, and the extra work is a few table reads. So callers pass the
 * document as the target sees it, `document.forTarget(target)`, and never the raw one. The export
 * entry and the preview both do, so a setting another target left behind never reaches the colors.
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
     * The standard variant is the document at its own contrast. Only a frozen export that asks for
     * every contrast gets medium and high too, the same document resolved at those levels, so a
     * dynamic export with every contrast left over from a frozen one still gets standard alone.
     * Pins, AMOLED and custom tones land on every variant. Accents and Fluent shades have one set
     * of values, since contrast never moves them.
     *
     * @param[document] The document as the target sees it, from `forTarget`.
     */
    public fun resolve(
        document: ThemeDocument,
        prefs: ExportPrefs,
    ): ResolvedExport {
        val standard = themes.resolve(document)
        val variants = variantsFor(prefs).associateWith { variant ->
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

    private fun variantsFor(prefs: ExportPrefs): List<ContrastVariant> =
        when (prefs.mode) {
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
 * The Fluent shades of both modes, each cut from that mode's own primary palette at the tones
 * `toFluentShades` in the Fluent module uses.
 *
 * `toFluentColors` builds its shades from the scheme of the mode it shows, and the 2025 spec gives
 * TonalSpot and Expressive a softer dark primary palette, so each mode reads its own. The contrast
 * audit rates a Fluent shade from the same cut.
 */
internal fun ThemeResult.fluentShades(): FluentShades =
    FluentShades(
        light = light.primaryPalette.shades(),
        dark = dark.primaryPalette.shades(),
    )

private fun TonalPalette.shades(): FluentShadeValues =
    FluentShadeValues(
        dark3 = shade(DARK_3_TONE),
        dark2 = shade(DARK_2_TONE),
        dark1 = shade(DARK_1_TONE),
        base = shade(BASE_TONE),
        light1 = shade(LIGHT_1_TONE),
        light2 = shade(LIGHT_2_TONE),
        light3 = shade(LIGHT_3_TONE),
    )

private fun TonalPalette.shade(tone: Int): Argb = Argb(tone(tone))

private const val DARK_3_TONE = 15
private const val DARK_2_TONE = 30
private const val DARK_1_TONE = 40
private const val BASE_TONE = 50
private const val LIGHT_1_TONE = 60
private const val LIGHT_2_TONE = 80
private const val LIGHT_3_TONE = 90
