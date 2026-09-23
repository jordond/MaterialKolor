package com.materialkolor.builder.domain.export

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Role

/**
 * Every color an export writes out, already worked out.
 *
 * The engine fills this in and the code generator only reads it, so the generator never needs the
 * color engine and a frozen export always writes the colors the preview showed. Pins, AMOLED dark
 * and the document's contrast are already baked in.
 *
 * @property[roles] The scheme's roles at each contrast variant the export writes. Standard is
 * always there, Medium and High only when a frozen export asks for every contrast.
 * @property[accents] One entry per accent, in the document's order.
 * @property[customSlots] The Custom target's slots at the same contrast variants as [roles], or
 * empty for every other target.
 * @property[fluentShades] The seven Fluent shades, or null for every target but Fluent.
 */
public data class ResolvedExport(
    public val roles: Map<ContrastVariant, RoleTable>,
    public val accents: List<AccentFamilyValues> = emptyList(),
    public val customSlots: Map<ContrastVariant, CustomSlotValues> = emptyMap(),
    public val fluentShades: FluentShadeValues? = null,
) {
    init {
        require(ContrastVariant.Standard in roles) { "A resolved export always carries the standard role table" }
        require(customSlots.isEmpty() || customSlots.keys == roles.keys) {
            "Custom slots are resolved at the same contrast variants as the roles, got ${customSlots.keys} and ${roles.keys}"
        }
    }
}

/**
 * The contrast a frozen export writes a variant of the theme at.
 */
public enum class ContrastVariant {
    /** The contrast the document is set to. */
    Standard,

    /** The medium contrast variant. */
    Medium,

    /** The high contrast variant. */
    High,
}

/**
 * Every Material role at one contrast variant, once per mode.
 *
 * Both maps hold every [Role], so a generator can read any role without a fallback.
 *
 * @property[light] The roles in light mode.
 * @property[dark] The roles in dark mode.
 */
public data class RoleTable(
    public val light: Map<Role, Argb>,
    public val dark: Map<Role, Argb>,
) {
    init {
        require(light.keys == Role.entries.toSet()) { "The light role table is missing ${Role.entries - light.keys}" }
        require(dark.keys == Role.entries.toSet()) { "The dark role table is missing ${Role.entries - dark.keys}" }
    }
}

/**
 * The four colors of one accent family, once per mode.
 *
 * @property[name] The accent's name, which the exported property is named after.
 * @property[light] The family in light mode.
 * @property[dark] The family in dark mode.
 */
public data class AccentFamilyValues(
    public val name: String,
    public val light: AccentColors,
    public val dark: AccentColors,
)

/**
 * The four colors an accent family has in one mode, the same four an M3 color family has.
 *
 * @property[color] The family's own color.
 * @property[onColor] The color that reads on [color].
 * @property[container] The family's container.
 * @property[onContainer] The color that reads on [container].
 */
public data class AccentColors(
    public val color: Argb,
    public val onColor: Argb,
    public val container: Argb,
    public val onContainer: Argb,
)

/**
 * Every Custom slot at one contrast variant, once per mode.
 *
 * Both maps hold every [CustomSlot], so a generator can read any slot without a fallback.
 *
 * @property[light] The slots in light mode.
 * @property[dark] The slots in dark mode.
 */
public data class CustomSlotValues(
    public val light: Map<CustomSlot, Argb>,
    public val dark: Map<CustomSlot, Argb>,
) {
    init {
        require(
            light.keys == CustomSlot.entries.toSet(),
        ) { "The light slots are missing ${CustomSlot.entries - light.keys}" }
        require(
            dark.keys == CustomSlot.entries.toSet(),
        ) { "The dark slots are missing ${CustomSlot.entries - dark.keys}" }
    }
}

/**
 * The seven shades a Fluent theme is built from, named the way Fluent's `Shades` names them.
 *
 * Fluent uses the same seven in both modes and picks different ones for light and dark itself, so
 * there is only one set.
 *
 * @property[dark3] The darkest shade.
 * @property[dark2] The second darkest shade.
 * @property[dark1] The shade just darker than [base].
 * @property[base] The accent itself.
 * @property[light1] The shade just lighter than [base].
 * @property[light2] The second lightest shade.
 * @property[light3] The lightest shade.
 */
public data class FluentShadeValues(
    public val dark3: Argb,
    public val dark2: Argb,
    public val dark1: Argb,
    public val base: Argb,
    public val light1: Argb,
    public val light2: Argb,
    public val light3: Argb,
)
