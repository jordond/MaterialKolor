package com.materialkolor.builder.domain.model

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Where the seed color came from.
 *
 * The seed itself lives on the document. This says how it got there, which is what the seed panel
 * needs to show the right control, and what an image seed needs to offer its other candidates
 * again without asking for the picture a second time.
 */
@Immutable
@Serializable
public sealed interface SeedSource {
    /**
     * Typed in as hex.
     */
    @Immutable
    @Serializable
    @SerialName("Typed")
    public data object Typed : SeedSource

    /**
     * Chosen in the color picker.
     */
    @Immutable
    @Serializable
    @SerialName("Picked")
    public data object Picked : SeedSource

    /**
     * Lifted off the screen with the eyedropper.
     */
    @Immutable
    @Serializable
    @SerialName("Eyedropper")
    public data object Eyedropper : SeedSource

    /**
     * Rolled at random.
     */
    @Immutable
    @Serializable
    @SerialName("Shuffled")
    public data object Shuffled : SeedSource

    /**
     * Picked from the preset list.
     *
     * @property[id] The preset that was picked.
     */
    @Immutable
    @Serializable
    @SerialName("Preset")
    public data class Preset(
        @SerialName("id")
        public val id: String,
    ) : SeedSource

    /**
     * Extracted from a picture.
     *
     * The picture is not part of the document, only what was pulled out of it, so a shared theme
     * stays small and needs nothing from the machine it was made on.
     *
     * @property[name] The file the colors were pulled from, shown beside the seed.
     * @property[candidates] The other colors the extractor offered, most likely first.
     */
    @Immutable
    @Serializable
    @SerialName("Image")
    public data class Image(
        @SerialName("name")
        public val name: String,
        @SerialName("candidates")
        public val candidates: List<Argb> = emptyList(),
    ) : SeedSource {
        init {
            require(candidates.size <= MAX_IMAGE_CANDIDATES) {
                "An image seed keeps at most $MAX_IMAGE_CANDIDATES candidates, got ${candidates.size}"
            }
        }
    }
}

/**
 * The extractor offers a handful of colors and the seed panel shows them in one row.
 */
private const val MAX_IMAGE_CANDIDATES: Int = 5
