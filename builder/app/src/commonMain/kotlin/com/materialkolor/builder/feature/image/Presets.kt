package com.materialkolor.builder.feature.image

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.image.SeedExtractor
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.image_preset_1
import com.materialkolor.builder.generated.resources.image_preset_2
import com.materialkolor.builder.generated.resources.image_preset_3
import com.materialkolor.builder.generated.resources.image_preset_4
import com.materialkolor.builder.generated.resources.image_preset_5
import com.materialkolor.builder.generated.resources.image_starter_baseline
import com.materialkolor.builder.generated.resources.image_starter_blossom
import com.materialkolor.builder.generated.resources.image_starter_forest
import com.materialkolor.builder.generated.resources.image_starter_ink
import com.materialkolor.builder.generated.resources.image_starter_mono
import com.materialkolor.builder.generated.resources.image_starter_ocean
import com.materialkolor.builder.generated.resources.image_starter_sand
import com.materialkolor.builder.generated.resources.image_starter_sunset
import com.materialkolor.builder.generated.resources.preset_1
import com.materialkolor.builder.generated.resources.preset_2
import com.materialkolor.builder.generated.resources.preset_3
import com.materialkolor.builder.generated.resources.preset_4
import com.materialkolor.builder.generated.resources.preset_5
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * A preset image or a starter theme, one of the ready made seeds the Image menu offers (F-09).
 *
 * The document keeps only the [id], as `SeedSource.Preset`, so an id names the same preset for good.
 * It is never renamed and never handed to another preset, even once the one it named is gone.
 */
@Immutable
internal sealed interface Preset {
    /**
     * What the document keeps of it.
     */
    val id: String

    /**
     * What the picker calls it.
     */
    val name: StringResource

    /**
     * The seed choosing it sets.
     */
    val seed: Argb

    /**
     * The edit choosing it makes to [document], one undo entry that never folds into another.
     */
    fun change(document: ThemeDocument): DocumentChange

    /**
     * One of the old builder's pictures at 256 px, with the colors pulled from it worked out ahead of
     * time, so choosing it costs no extraction. It sets only the seed, to the best of [candidates],
     * and the row under the seed actions offers the rest.
     *
     * @property[drawable] The picture.
     * @property[candidates] What the extractor makes of the picture at 128 px, the best first.
     */
    @Immutable
    class Image(
        override val id: String,
        override val name: StringResource,
        val drawable: DrawableResource,
        val candidates: List<Argb>,
    ) : Preset {
        init {
            require(candidates.size in 1..SeedExtractor.MAX_CANDIDATES) {
                "A preset image offers 1 to ${SeedExtractor.MAX_CANDIDATES} candidates, got ${candidates.size}"
            }
        }

        override val seed: Argb
            get() = candidates.first()

        override fun change(document: ThemeDocument): DocumentChange =
            DocumentChange.SetSeed(seed, SeedSource.Preset(id))
    }

    /**
     * A seed with the style and contrast it looks best in. Choosing it sets those three and nothing
     * else, so the target, the overrides, the pins and the accents stay as they were.
     */
    @Immutable
    class Starter(
        override val id: String,
        override val name: StringResource,
        override val seed: Argb,
        val style: Style,
        val contrast: ContrastLevel,
    ) : Preset {
        override fun change(document: ThemeDocument): DocumentChange =
            DocumentChange.Replace(
                document.copy(seed = seed, seedSource = SeedSource.Preset(id), style = style, contrast = contrast),
            )
    }
}

/**
 * Every preset, the pictures first and then the starters.
 *
 * The pictures keep the ids the old builder's links carried, so an old link still finds its picture.
 * The starters are a first pass the owner may rename or swap out later, under new ids when they do.
 */
internal object Presets {
    val all: List<Preset> = listOf(
        Preset.Image(
            id = "res-0",
            name = Res.string.image_preset_1,
            drawable = Res.drawable.preset_1,
            candidates = argbs(0xFF589008),
        ),
        Preset.Image(
            id = "res-1",
            name = Res.string.image_preset_2,
            drawable = Res.drawable.preset_2,
            candidates = argbs(0xFFE0E8E8, 0xFFA0A890, 0xFFA09870, 0xFFC0C0C8),
        ),
        Preset.Image(
            id = "res-2",
            name = Res.string.image_preset_3,
            drawable = Res.drawable.preset_3,
            candidates = argbs(0xFFF0B0D0, 0xFF28A8E0, 0xFFA090E8, 0xFFD8C098, 0xFFA8C0B8),
        ),
        Preset.Image(
            id = "res-3",
            name = Res.string.image_preset_4,
            drawable = Res.drawable.preset_4,
            candidates = argbs(0xFFA83810, 0xFFB07070),
        ),
        Preset.Image(
            id = "res-4",
            name = Res.string.image_preset_5,
            drawable = Res.drawable.preset_5,
            candidates = argbs(0xFF4858E0, 0xFF102840, 0xFF081818, 0xFF082018, 0xFF182018),
        ),
        starter("starter-baseline", Res.string.image_starter_baseline, 0xFF6750A4, Style.TonalSpot),
        starter("starter-ocean", Res.string.image_starter_ocean, 0xFF0B6BCB, Style.TonalSpot),
        starter("starter-forest", Res.string.image_starter_forest, 0xFF2E7D32, Style.Fidelity),
        starter("starter-sunset", Res.string.image_starter_sunset, 0xFFF4511E, Style.Vibrant),
        starter("starter-blossom", Res.string.image_starter_blossom, 0xFFD81B60, Style.Expressive),
        starter("starter-sand", Res.string.image_starter_sand, 0xFFC9A227, Style.Neutral),
        starter("starter-ink", Res.string.image_starter_ink, 0xFF1A237E, Style.TonalSpot, ContrastLevel.Medium),
        starter("starter-mono", Res.string.image_starter_mono, 0xFF5F6368, Style.Monochrome),
    )

    /**
     * The pictures, in the order the picker shows them.
     */
    val images: List<Preset.Image> = all.filterIsInstance<Preset.Image>()

    /**
     * The starters, in the order the picker shows them.
     */
    val starters: List<Preset.Starter> = all.filterIsInstance<Preset.Starter>()

    init {
        require(all.map { preset -> preset.id }.toSet().size == all.size) { "Every preset needs an id of its own" }
        // b-311d
        require(starters.all { starter -> starter.contrast in ContrastLevel.Stops }) {
            "Every starter's contrast sits on a named stop, so its card can name it"
        }
    }

    /**
     * The preset [id] names, or null for one this build does not know.
     */
    fun of(id: String): Preset? = all.firstOrNull { preset -> preset.id == id }

    /**
     * The picture [source] came from, or null when it came from anything else.
     */
    fun imageOf(source: SeedSource): Preset.Image? =
        (source as? SeedSource.Preset)?.let { preset -> of(preset.id) as? Preset.Image }

    private fun argbs(vararg colors: Long): List<Argb> = colors.map { color -> Argb(color.toInt()) }

    private fun starter(
        id: String,
        name: StringResource,
        seed: Long,
        style: Style,
        contrast: ContrastLevel = ContrastLevel.Standard,
    ): Preset.Starter = Preset.Starter(id, name, Argb(seed.toInt()), style, contrast)
}
