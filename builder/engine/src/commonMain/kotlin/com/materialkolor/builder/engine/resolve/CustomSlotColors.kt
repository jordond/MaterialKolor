package com.materialkolor.builder.engine.resolve

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.CustomTone
import com.materialkolor.builder.domain.model.SlotResolution
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.model.TonalRamp
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap

/**
 * Every slot of the Custom target in both modes.
 *
 * This is the recipe of the custom theme sample. A slot that names a role reads it from the role
 * tables, so pins and AMOLED reach it the same way they reach the Roles tab. A slot cut off a ramp
 * takes the scheme's own palette for that mode at its tone, and an on color is found on the same
 * ramp with `onTone`. The document's custom tones move a slot as [CustomTone] describes.
 *
 * @property[light] Every slot in light mode, in [CustomSlot] order.
 * @property[dark] Every slot in dark mode, in [CustomSlot] order.
 */
@Immutable
public class CustomSlotColors internal constructor(
    public val light: ImmutableMap<CustomSlot, Argb>,
    public val dark: ImmutableMap<CustomSlot, Argb>,
) {
    /** Every slot in the mode [isDark] picks. */
    public fun mode(isDark: Boolean): ImmutableMap<CustomSlot, Argb> = if (isDark) dark else light

    /** What [slot] resolved to in the mode [isDark] picks. */
    public operator fun get(
        slot: CustomSlot,
        isDark: Boolean,
    ): Argb = mode(isDark).getValue(slot)

    internal companion object {
        /** Resolve every slot of the Custom target for [document] out of its schemes and roles. */
        fun from(
            document: ThemeDocument,
            light: DynamicScheme,
            dark: DynamicScheme,
            roles: RoleTables,
        ): CustomSlotColors =
            CustomSlotColors(
                light = table(document, light, roles, isDark = false),
                dark = table(document, dark, roles, isDark = true),
            )

        private fun table(
            document: ThemeDocument,
            scheme: DynamicScheme,
            roles: RoleTables,
            isDark: Boolean,
        ): ImmutableMap<CustomSlot, Argb> =
            CustomSlot.entries
                .associateWith { slot ->
                    resolve(slot.resolution, document.customTones[slot], scheme, roles, isDark)
                }.toImmutableMap()

        /**
         * The color [resolution] gives in the mode [isDark] picks, with [tone] moved in.
         *
         * A tone moves the slot it hangs on. A ramp slot is cut at the new tone. An on color is
         * taken at the new tone as it is, with no contrast search, while the tone it reads against
         * stays put. A role slot keeps its role, because a role is moved with a pin.
         */
        fun resolve(
            resolution: SlotResolution,
            tone: CustomTone?,
            scheme: DynamicScheme,
            roles: RoleTables,
            isDark: Boolean,
        ): Argb {
            val moved = tone?.let { custom -> if (isDark) custom.dark else custom.light }
            return when (resolution) {
                is SlotResolution.FromRole -> {
                    roles[resolution.role, isDark].argb
                }
                is SlotResolution.FromRamp -> {
                    val cut = moved ?: if (isDark) resolution.dark else resolution.light
                    scheme.palette(resolution.ramp).toneColor(cut).toDomain()
                }
                is SlotResolution.OnRamp -> {
                    val palette = scheme.palette(resolution.ramp)
                    val background = if (isDark) resolution.dark else resolution.light
                    val color = if (moved != null) palette.toneColor(moved) else palette.onTone(background)
                    color.toDomain()
                }
            }
        }

        private fun DynamicScheme.palette(ramp: TonalRamp): TonalPalette =
            when (ramp) {
                TonalRamp.Primary -> primaryPalette
                TonalRamp.Secondary -> secondaryPalette
                TonalRamp.Tertiary -> tertiaryPalette
                TonalRamp.Error -> errorPalette
                TonalRamp.Neutral -> neutralPalette
                TonalRamp.NeutralVariant -> neutralVariantPalette
            }
    }
}
