package com.materialkolor.builder.engine.shuffle

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument

/**
 * What one press of Shuffle came up with.
 */
public sealed interface ShuffleResult {
    /**
     * A new seed, and a new style when the style is unlocked.
     *
     * @property[seed] The seed to use. It is the document's own seed when the seed is locked.
     * @property[style] The style to switch to, or null to keep the document's style.
     */
    public data class Shuffled(
        public val seed: Argb,
        public val style: Style?,
    ) : ShuffleResult {
        /**
         * [document] with this shuffle applied.
         *
         * Only the seed, where it came from and the style can move. A seed that did not change
         * keeps its source, so a locked seed picked from an image still shows its image.
         */
        public fun applyTo(document: ThemeDocument): ThemeDocument {
            val seedChanged = seed != document.seed
            return document.copy(
                seed = seed,
                seedSource = if (seedChanged) SeedSource.Shuffled else document.seedSource,
                style = style ?: document.style,
            )
        }
    }

    /**
     * The seed is locked and the style is locked too or has no other style to go to.
     */
    public data object NothingToShuffle : ShuffleResult
}
