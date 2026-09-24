package com.materialkolor.builder.engine.resolve

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.ktx.contrastRatio
import kotlin.test.Test
import kotlin.test.assertTrue

/** The least a focus ring stands from the ground it is drawn on (WCAG 2.2, 1.4.11). */
private const val RingContrast = 3.0

/** Seeds of every hue family and a grey, since the ring's tone and not its hue fixes the ratio. */
private val Seeds: List<Argb> = listOf(
    Argb(0x6750A4),
    Argb(0x0000FF),
    Argb(0xFF0000),
    Argb(0x00FF00),
    Argb(0xFFFF00),
    Argb(0x00B8D4),
    Argb(0x808080),
)

/**
 * The Custom grounds the kit rings on, the panel, the raised panel and the canvas and code ground
 * under them.
 */
private val RingGrounds: List<CustomSlot> =
    listOf(CustomSlot.Surface, CustomSlot.SurfaceRaised, CustomSlot.SurfaceSunken)

/**
 * The Custom focus color stands 3 to 1 from every ground the kit draws a ring on, in both modes, for
 * any seed and style (D46).
 */
class CustomFocusRingTest {
    @Test
    fun focusRing_defaultTones_standThreeToOneOnEveryGroundTheKitRingsOn() {
        val resolver = ThemeResolver()
        val misses = buildList {
            for (seed in Seeds) {
                for (style in Style.entries) {
                    val result = resolver.resolve(ThemeDocument(seed = seed, style = style))
                    for (slots in listOf(result.customSlots, result.chromeCustomSlots)) {
                        for (isDark in listOf(false, true)) {
                            val ring = slots[CustomSlot.FocusRing, isDark].toColor()
                            for (ground in RingGrounds) {
                                val ratio = ring.contrastRatio(slots[ground, isDark].toColor())
                                if (ratio < RingContrast) add("${seed.toHex()} $style dark $isDark on $ground $ratio")
                            }
                        }
                    }
                }
            }
        }
        assertTrue(misses.isEmpty(), misses.joinToString("\n"))
    }
}
