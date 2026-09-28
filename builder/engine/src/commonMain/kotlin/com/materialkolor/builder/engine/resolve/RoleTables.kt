package com.materialkolor.builder.engine.resolve

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap

/**
 * What a role resolved to in one mode.
 *
 * @property[argb] The color the role takes.
 * @property[tone] The HCT tone of [argb], read from the scheme at runtime, never a label from a
 * table. A pinned role reports the tone of its pin.
 */
@Immutable
public data class RoleEntry(
    public val argb: Argb,
    public val tone: Double,
)

/**
 * Every [Role] of a theme in both modes, with AMOLED and pins already applied.
 *
 * The values come straight from the library's dynamic colors, so they match what the Material 3
 * module puts in a `ColorScheme` for the same scheme. Two things then land on top, in this order.
 * AMOLED first, the way the Material 3 module applies it, then the document's pins, so a pin always
 * wins.
 *
 * @property[light] Every role in light mode, in [Role] order.
 * @property[dark] Every role in dark mode, in [Role] order.
 */
@Immutable
public class RoleTables internal constructor(
    public val light: ImmutableMap<Role, RoleEntry>,
    public val dark: ImmutableMap<Role, RoleEntry>,
) {
    /**
     * Every role in the mode [isDark] picks.
     */
    public fun mode(isDark: Boolean): ImmutableMap<Role, RoleEntry> = if (isDark) dark else light

    /**
     * What [role] resolved to in the mode [isDark] picks.
     */
    public operator fun get(
        role: Role,
        isDark: Boolean,
    ): RoleEntry = mode(isDark).getValue(role)

    internal companion object {
        /**
         * Read every role of [light] and [dark], then apply the AMOLED setting and the pins of
         * [document].
         */
        fun from(
            light: DynamicScheme,
            dark: DynamicScheme,
            document: ThemeDocument,
        ): RoleTables =
            RoleTables(
                light = table(light, amoled = false, pins = document.pins, pinOf = RolePin::light),
                dark = table(dark, amoled = document.amoled, pins = document.pins, pinOf = RolePin::dark),
            )

        private fun table(
            scheme: DynamicScheme,
            amoled: Boolean,
            pins: Map<Role, RolePin>,
            pinOf: (RolePin) -> Argb?,
        ): ImmutableMap<Role, RoleEntry> {
            val colors = MaterialDynamicColors()
            return Role.entries
                .associateWith { role ->
                    val pinned = pins[role]?.let(pinOf)
                    val blackout = if (amoled) AmoledColors[role] else null
                    when {
                        pinned != null -> fixedEntry(pinned)
                        blackout != null -> fixedEntry(blackout)
                        else -> generatedEntry(role.dynamicColor(colors), scheme)
                    }
                }.toImmutableMap()
        }

        /**
         * The colors AMOLED forces on roles in dark mode.
         *
         * This replicates `MaterialKolors` with `isAmoled` set, which is what the Material 3
         * module's `toColorScheme` reads through. Background and surface go to pure black and the
         * content drawn on them goes to pure white. Every other role, the surface containers
         * included, keeps its generated color.
         */
        private val AmoledColors: Map<Role, Argb> = mapOf(
            Role.Background to Argb(0x000000),
            Role.Surface to Argb(0x000000),
            Role.OnBackground to Argb(0xFFFFFF),
            Role.OnSurface to Argb(0xFFFFFF),
        )

        private fun generatedEntry(
            color: DynamicColor,
            scheme: DynamicScheme,
        ): RoleEntry = RoleEntry(argb = Argb(color.getArgb(scheme)), tone = color.getHct(scheme).tone)

        private fun fixedEntry(argb: Argb): RoleEntry = RoleEntry(argb = argb, tone = Hct.fromInt(argb.value).tone)
    }
}

/**
 * The library's dynamic color for this role.
 *
 * Each call builds a fresh [DynamicColor], which is how the library hands them out, so read the
 * color and its tone from the same instance to share its cache.
 */
internal fun Role.dynamicColor(colors: MaterialDynamicColors): DynamicColor =
    when (this) {
        Role.Primary -> colors.primary
        Role.OnPrimary -> colors.onPrimary
        Role.PrimaryContainer -> colors.primaryContainer
        Role.OnPrimaryContainer -> colors.onPrimaryContainer
        Role.InversePrimary -> colors.inversePrimary
        Role.Secondary -> colors.secondary
        Role.OnSecondary -> colors.onSecondary
        Role.SecondaryContainer -> colors.secondaryContainer
        Role.OnSecondaryContainer -> colors.onSecondaryContainer
        Role.Tertiary -> colors.tertiary
        Role.OnTertiary -> colors.onTertiary
        Role.TertiaryContainer -> colors.tertiaryContainer
        Role.OnTertiaryContainer -> colors.onTertiaryContainer
        Role.Background -> colors.background
        Role.OnBackground -> colors.onBackground
        Role.Surface -> colors.surface
        Role.OnSurface -> colors.onSurface
        Role.SurfaceVariant -> colors.surfaceVariant
        Role.OnSurfaceVariant -> colors.onSurfaceVariant
        Role.SurfaceTint -> colors.surfaceTint
        Role.InverseSurface -> colors.inverseSurface
        Role.InverseOnSurface -> colors.inverseOnSurface
        Role.Error -> colors.error
        Role.OnError -> colors.onError
        Role.ErrorContainer -> colors.errorContainer
        Role.OnErrorContainer -> colors.onErrorContainer
        Role.Outline -> colors.outline
        Role.OutlineVariant -> colors.outlineVariant
        Role.Scrim -> colors.scrim
        Role.SurfaceBright -> colors.surfaceBright
        Role.SurfaceDim -> colors.surfaceDim
        Role.SurfaceContainer -> colors.surfaceContainer
        Role.SurfaceContainerHigh -> colors.surfaceContainerHigh
        Role.SurfaceContainerHighest -> colors.surfaceContainerHighest
        Role.SurfaceContainerLow -> colors.surfaceContainerLow
        Role.SurfaceContainerLowest -> colors.surfaceContainerLowest
        Role.PrimaryFixed -> colors.primaryFixed
        Role.PrimaryFixedDim -> colors.primaryFixedDim
        Role.OnPrimaryFixed -> colors.onPrimaryFixed
        Role.OnPrimaryFixedVariant -> colors.onPrimaryFixedVariant
        Role.SecondaryFixed -> colors.secondaryFixed
        Role.SecondaryFixedDim -> colors.secondaryFixedDim
        Role.OnSecondaryFixed -> colors.onSecondaryFixed
        Role.OnSecondaryFixedVariant -> colors.onSecondaryFixedVariant
        Role.TertiaryFixed -> colors.tertiaryFixed
        Role.TertiaryFixedDim -> colors.tertiaryFixedDim
        Role.OnTertiaryFixed -> colors.onTertiaryFixed
        Role.OnTertiaryFixedVariant -> colors.onTertiaryFixedVariant
    }
