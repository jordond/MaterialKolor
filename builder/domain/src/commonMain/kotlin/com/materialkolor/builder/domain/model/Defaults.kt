package com.materialkolor.builder.domain.model

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorNames
import kotlin.random.Random

/**
 * The seed every new theme starts from, the burnt orange the builder has always opened on.
 */
public val DEFAULT_SEED: Argb = Argb(0xFFD9653B.toInt())

/**
 * The seeds a new project starts from once the first one is made, each a color [ColorNames] lists by
 * name, so no two of them share one. They go round the hue wheel from red to brown and all make a
 * theme worth starting on. [DEFAULT_SEED] is left out, since the first project already has it.
 */
public val STARTER_SEEDS: List<Argb> =
    listOf(
        0xFFAF4B3D, // Paprika Spice
        0xFFF1A723, // Marigold Bloom
        0xFFA8921A, // Mustard Pot
        0xFF667811, // Avocado Skin
        0xFF437F3F, // Basil Leaf
        0xFF15825F, // Emerald Cut
        0xFF157F78, // Teal Lagoon
        0xFF1478A1, // Cerulean Sea
        0xFF3F70AD, // Cobalt Blue
        0xFF5B60CA, // Blue Iris
        0xFF804EC5, // Amethyst Crystal
        0xFF9849A4, // Grape Jelly
        0xFFA64781, // Berry Smoothie
        0xFFC71759, // Ruby Glow
        0xFF96603E, // Cinnamon Stick
    ).map { value -> Argb(value.toInt()) }

/**
 * A seed from [STARTER_SEEDS] for a new project, picked with [random] among those whose name no
 * project in [takenNames] has yet. When every name is taken it picks from all of them.
 */
public fun starterSeed(
    takenNames: Set<String>,
    random: Random,
): Argb {
    val free = STARTER_SEEDS.filter { seed -> ColorNames.nameOf(seed) !in takenNames }
    return free.ifEmpty { STARTER_SEEDS }.random(random)
}
