package com.materialkolor.builder.engine.shuffle

import com.materialkolor.builder.domain.persist.Preferences

/**
 * What a shuffle has to leave alone.
 *
 * The defaults match a browser that has never touched the locks, only the style is kept.
 *
 * @property[hue] Whether a new seed keeps the current seed's hue and only varies chroma and tone.
 * @property[style] Whether the palette style stays as it is.
 * @property[seed] Whether the seed stays as it is.
 */
public data class ShuffleLocks(
    public val hue: Boolean = false,
    public val style: Boolean = true,
    public val seed: Boolean = false,
)

/** The shuffle locks these preferences hold. */
public fun Preferences.shuffleLocks(): ShuffleLocks =
    ShuffleLocks(
        hue = hueLock,
        style = styleLock,
        seed = seedLock,
    )
