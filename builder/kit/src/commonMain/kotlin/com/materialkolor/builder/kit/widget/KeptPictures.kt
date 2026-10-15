package com.materialkolor.builder.kit.widget

import com.materialkolor.builder.kit.a11y.KitTestApi
import kotlin.concurrent.Volatile

/**
 * Forgets every plane picture kept so far, so the next plane builds its own as a first one would.
 */
@KitTestApi
public fun forgetPlanePictures() {
    KeptPictures.clear()
}

/**
 * The pictures of the last [KeptHues] whole hues, the most recently shown last. The main thread
 * reads and writes it, and a test clears it from its own thread in between, so every change swaps
 * in a whole new map.
 */
internal object KeptPictures {
    @Volatile
    private var pictures: Map<Int, PlanePicture> = emptyMap()

    fun take(hue: Int): PlanePicture? {
        val picture = pictures[hue] ?: return null
        pictures = pictures - hue + (hue to picture)
        return picture
    }

    fun keep(picture: PlanePicture) {
        pictures = (pictures - picture.hue + (picture.hue to picture))
            .entries
            .toList()
            .takeLast(KeptHues)
            .associate { (hue, kept) -> hue to kept }
    }

    fun clear() {
        pictures = emptyMap()
    }
}

/**
 * How many whole hues keep their picture.
 */
internal const val KeptHues: Int = 48
