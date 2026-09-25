package com.materialkolor.builder.feature.poster

import com.materialkolor.builder.domain.capability.EffectiveSpec
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.style_description_cmf
import com.materialkolor.builder.generated.resources.style_description_content
import com.materialkolor.builder.generated.resources.style_description_expressive
import com.materialkolor.builder.generated.resources.style_description_fidelity
import com.materialkolor.builder.generated.resources.style_description_fruit_salad
import com.materialkolor.builder.generated.resources.style_description_monochrome
import com.materialkolor.builder.generated.resources.style_description_neutral
import com.materialkolor.builder.generated.resources.style_description_rainbow
import com.materialkolor.builder.generated.resources.style_description_tonal_spot
import com.materialkolor.builder.generated.resources.style_description_vibrant
import com.materialkolor.builder.generated.resources.style_display_cmf
import com.materialkolor.builder.generated.resources.style_display_content
import com.materialkolor.builder.generated.resources.style_display_expressive
import com.materialkolor.builder.generated.resources.style_display_fidelity
import com.materialkolor.builder.generated.resources.style_display_fruit_salad
import com.materialkolor.builder.generated.resources.style_display_monochrome
import com.materialkolor.builder.generated.resources.style_display_neutral
import com.materialkolor.builder.generated.resources.style_display_rainbow
import com.materialkolor.builder.generated.resources.style_display_tonal_spot
import com.materialkolor.builder.generated.resources.style_display_vibrant
import com.materialkolor.builder.generated.resources.style_name_cmf
import com.materialkolor.builder.generated.resources.style_name_content
import com.materialkolor.builder.generated.resources.style_name_expressive
import com.materialkolor.builder.generated.resources.style_name_fidelity
import com.materialkolor.builder.generated.resources.style_name_fruit_salad
import com.materialkolor.builder.generated.resources.style_name_monochrome
import com.materialkolor.builder.generated.resources.style_name_neutral
import com.materialkolor.builder.generated.resources.style_name_rainbow
import com.materialkolor.builder.generated.resources.style_name_tonal_spot
import com.materialkolor.builder.generated.resources.style_name_vibrant
import com.materialkolor.builder.generated.resources.style_spec_classic
import com.materialkolor.builder.generated.resources.style_spec_cmf
import com.materialkolor.builder.generated.resources.style_spec_revised
import com.materialkolor.builder.generated.resources.style_tooltip_cmf
import com.materialkolor.builder.generated.resources.style_tooltip_content
import com.materialkolor.builder.generated.resources.style_tooltip_expressive
import com.materialkolor.builder.generated.resources.style_tooltip_fidelity
import com.materialkolor.builder.generated.resources.style_tooltip_fruit_salad
import com.materialkolor.builder.generated.resources.style_tooltip_monochrome
import com.materialkolor.builder.generated.resources.style_tooltip_neutral
import com.materialkolor.builder.generated.resources.style_tooltip_rainbow
import com.materialkolor.builder.generated.resources.style_tooltip_tonal_spot
import com.materialkolor.builder.generated.resources.style_tooltip_vibrant
import org.jetbrains.compose.resources.StringResource

// b-510

/**
 * What [style] is called under its chip, in words with spaces, as in "Tonal Spot".
 */
internal fun styleDisplayName(style: Style): StringResource =
    when (style) {
        Style.TonalSpot -> Res.string.style_display_tonal_spot
        Style.Neutral -> Res.string.style_display_neutral
        Style.Vibrant -> Res.string.style_display_vibrant
        Style.Expressive -> Res.string.style_display_expressive
        Style.Rainbow -> Res.string.style_display_rainbow
        Style.FruitSalad -> Res.string.style_display_fruit_salad
        Style.Monochrome -> Res.string.style_display_monochrome
        Style.Fidelity -> Res.string.style_display_fidelity
        Style.Content -> Res.string.style_display_content
        Style.Cmf -> Res.string.style_display_cmf
    }

/**
 * The spec picking [style] would put [document] on, or null while that is the spec it runs on now.
 * A chip only carries a spec tag when choosing it would move the theme to another spec.
 */
internal fun specTag(
    style: Style,
    document: ThemeDocument,
): SpecVersion? {
    val now = EffectiveSpec.of(document.style, document.spec)
    val picked = EffectiveSpec.of(style, document.spec)
    return picked.takeIf { spec -> spec != now }
}

/**
 * The one spec [style] runs in whatever the document asks for, or null for a style that follows
 * the spec asked for. The Style label names it while that style is chosen.
 */
internal fun forcedSpec(style: Style): SpecVersion? = EffectiveSpec.offered(style).singleOrNull()

/**
 * What [style] is called, the way the library spells it.
 */
internal fun styleName(style: Style): StringResource =
    when (style) {
        Style.TonalSpot -> Res.string.style_name_tonal_spot
        Style.Neutral -> Res.string.style_name_neutral
        Style.Vibrant -> Res.string.style_name_vibrant
        Style.Expressive -> Res.string.style_name_expressive
        Style.Rainbow -> Res.string.style_name_rainbow
        Style.FruitSalad -> Res.string.style_name_fruit_salad
        Style.Monochrome -> Res.string.style_name_monochrome
        Style.Fidelity -> Res.string.style_name_fidelity
        Style.Content -> Res.string.style_name_content
        Style.Cmf -> Res.string.style_name_cmf
    }

/**
 * The short hint [style]'s chip shows on hover and focus.
 */
internal fun styleTooltip(style: Style): StringResource =
    when (style) {
        Style.TonalSpot -> Res.string.style_tooltip_tonal_spot
        Style.Neutral -> Res.string.style_tooltip_neutral
        Style.Vibrant -> Res.string.style_tooltip_vibrant
        Style.Expressive -> Res.string.style_tooltip_expressive
        Style.Rainbow -> Res.string.style_tooltip_rainbow
        Style.FruitSalad -> Res.string.style_tooltip_fruit_salad
        Style.Monochrome -> Res.string.style_tooltip_monochrome
        Style.Fidelity -> Res.string.style_tooltip_fidelity
        Style.Content -> Res.string.style_tooltip_content
        Style.Cmf -> Res.string.style_tooltip_cmf
    }

/**
 * The one line on what [style] does with the seed.
 */
internal fun styleDescription(style: Style): StringResource =
    when (style) {
        Style.TonalSpot -> Res.string.style_description_tonal_spot
        Style.Neutral -> Res.string.style_description_neutral
        Style.Vibrant -> Res.string.style_description_vibrant
        Style.Expressive -> Res.string.style_description_expressive
        Style.Rainbow -> Res.string.style_description_rainbow
        Style.FruitSalad -> Res.string.style_description_fruit_salad
        Style.Monochrome -> Res.string.style_description_monochrome
        Style.Fidelity -> Res.string.style_description_fidelity
        Style.Content -> Res.string.style_description_content
        Style.Cmf -> Res.string.style_description_cmf
    }

/**
 * The specs [style] runs in, as its chip's tooltip says them.
 */
internal fun specSupport(style: Style): StringResource {
    val offered = EffectiveSpec.offered(style)
    return when {
        SpecVersion.Spec2026 in offered -> Res.string.style_spec_cmf
        SpecVersion.Spec2025 in offered -> Res.string.style_spec_revised
        else -> Res.string.style_spec_classic
    }
}
