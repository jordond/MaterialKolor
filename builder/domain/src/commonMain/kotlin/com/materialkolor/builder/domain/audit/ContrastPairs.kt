package com.materialkolor.builder.domain.audit

import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RoleGroup
import com.materialkolor.builder.domain.model.SlotResolution

/**
 * One color a contrast pair can name, resolved per mode by whoever runs the audit.
 */
public sealed interface ColorRef {
    /**
     * A role of the Material 3 scheme.
     *
     * @property[role] The role.
     */
    public data class OfRole(
        public val role: Role,
    ) : ColorRef

    /**
     * One color of an accent family.
     *
     * @property[slot] Which accent and which of its four colors.
     */
    public data class OfAccent(
        public val slot: AccentSlot,
    ) : ColorRef

    /**
     * A slot of the Custom target.
     *
     * @property[slot] The slot.
     */
    public data class OfSlot(
        public val slot: CustomSlot,
    ) : ColorRef

    /**
     * A text color Fluent fixes itself, which no seed or contrast level can move.
     *
     * @property[text] The text color.
     */
    public data class OfFluentText(
        public val text: FluentText,
    ) : ColorRef

    /**
     * One of the seven accent shades Fluent cuts from the primary palette of the mode it shows.
     *
     * @property[shade] The shade.
     */
    public data class OfFluentShade(
        public val shade: FluentShade,
    ) : ColorRef
}

/**
 * A text color Fluent ships as a constant rather than taking from the accent ramp.
 */
public enum class FluentText {
    /**
     * Text and glyphs on an accent fill, white in light mode and black in dark mode.
     */
    OnAccentPrimary,

    /**
     * Quieter text on an accent fill, the same color with some of the fill showing through.
     */
    OnAccentSecondary,
}

/**
 * The accent shades a Fluent theme is built from, named the way Fluent's `Shades` names them.
 */
public enum class FluentShade {
    /**
     * The darkest shade.
     */
    Dark3,

    /**
     * The second darkest shade.
     */
    Dark2,

    /**
     * The shade just darker than the accent.
     */
    Dark1,

    /**
     * The accent itself.
     */
    Base,

    /**
     * The shade just lighter than the accent.
     */
    Light1,

    /**
     * The second lightest shade.
     */
    Light2,

    /**
     * The lightest shade.
     */
    Light3,
}

/**
 * Whether a pair is read as text or as a shape, which sets the ratio it has to clear.
 */
public enum class PairKind {
    /**
     * Words, held to 4.5 to 1 for AA.
     */
    Text,

    /**
     * Borders, focus rings and other shapes someone has to see, held to 3 to 1.
     */
    NonText,
}

/**
 * A color drawn on top of another, the unit the contrast audit rates.
 *
 * @property[foreground] The color drawn on top.
 * @property[background] The color it sits on.
 * @property[kind] Whether the foreground is text or a shape.
 */
public data class ContrastPair(
    public val foreground: ColorRef,
    public val background: ColorRef,
    public val kind: PairKind,
)

/**
 * The pairs the contrast audit checks for each target.
 */
public object ContrastPairs {
    /**
     * Every pair worth rating for [library].
     *
     * Material 3 and Unstyled share the Material roles, so they get the same list. It covers on-X
     * over X for each accent and fixed family, onSurface and onSurfaceVariant over every surface
     * level, the inverse pairs and outline over every surface level as a shape. That already
     * takes in every role something reads on, so a pinned role is always in it. Scrim, surface
     * tint and outline variant are the exceptions, nothing is drawn on them and nothing on them
     * needs contrast, so pinning one adds no pair.
     *
     * Custom rates its own slots instead, with the Material role pairs added for any pinned role
     * none of its slots takes.
     *
     * Fluent rates its accent fill against its fixed text colors and nothing else, since pins and
     * accents never reach it. The fill is named as [Role.Primary], because Fluent's default fill
     * sits at tone 40 of the primary ramp in light mode and tone 80 in dark mode, the tones
     * primary takes in the 2021 spec.
     *
     * Each accent adds its two on-X over X pairs on every target but Fluent. The list never
     * repeats a pair.
     *
     * @param[accentCount] How many accents the document carries.
     * @param[pinned] The roles the document pins.
     */
    public fun forTarget(
        library: Library,
        accentCount: Int,
        pinned: Set<Role>,
    ): List<ContrastPair> {
        require(accentCount >= 0) { "An accent count is 0 or more, got $accentCount" }
        val pairs = when (library) {
            Library.Material3,
            Library.Unstyled,
            -> rolePairs + accentPairs(accentCount)
            Library.Fluent -> fluentPairs
            Library.Custom -> slotPairs + accentPairs(accentCount) + pinnedRolePairs(pinned - rolesTakenBySlots)
        }
        return pairs.distinct()
    }

    /**
     * Every surface level, the roles onSurface is drawn on.
     */
    private val surfaceLevels: List<Role> = Role.entries.filter { role -> role.onPair == Role.OnSurface }

    private val fixedVariants: Map<Role, Role> = mapOf(
        Role.OnPrimaryFixed to Role.OnPrimaryFixedVariant,
        Role.OnSecondaryFixed to Role.OnSecondaryFixedVariant,
        Role.OnTertiaryFixed to Role.OnTertiaryFixedVariant,
    )

    private val rolePairs: List<ContrastPair> = buildList {
        val groups = listOf(RoleGroup.Accent, RoleGroup.Fixed, RoleGroup.Surface, RoleGroup.OutlineInverse)
        for (group in groups) {
            for (background in Role.entries.filter { role -> role.group == group }) {
                val onColor = background.onPair ?: continue
                add(roleText(foreground = onColor, background = background))
                fixedVariants[onColor]?.let { variant -> add(roleText(foreground = variant, background = background)) }
            }
        }
        surfaceLevels.forEach { level -> add(roleText(foreground = Role.OnSurfaceVariant, background = level)) }
        add(roleText(foreground = Role.InversePrimary, background = Role.InverseSurface))
        surfaceLevels.forEach { level -> add(roleShape(foreground = Role.Outline, background = level)) }
    }

    private fun pinnedRolePairs(pinned: Set<Role>): List<ContrastPair> =
        rolePairs.filter { pair ->
            pair.roles().any { role -> role in pinned }
        }

    private fun ContrastPair.roles(): List<Role> =
        listOf(foreground, background).mapNotNull { ref -> (ref as? ColorRef.OfRole)?.role }

    /**
     * The role a slot takes, or null for a slot cut off a ramp.
     */
    private val CustomSlot.role: Role?
        get() = (resolution as? SlotResolution.FromRole)?.role

    private val slotByRole: Map<Role, CustomSlot> =
        CustomSlot.entries.mapNotNull { slot -> slot.role?.let { role -> role to slot } }.toMap()

    private val rolesTakenBySlots: Set<Role> = slotByRole.keys

    /**
     * The three surface steps a Custom theme lays content on.
     */
    private val customSurfaces: List<CustomSlot> =
        listOf(CustomSlot.Surface, CustomSlot.SurfaceRaised, CustomSlot.SurfaceSunken)

    private val slotPairs: List<ContrastPair> = buildList {
        for (background in CustomSlot.entries) {
            val onColor = background.role?.onPair?.let { onRole -> slotByRole[onRole] } ?: continue
            add(slotPair(foreground = onColor, background = background, kind = PairKind.Text))
        }
        for (pressedOrRaised in listOf(CustomSlot.PrimaryPressed, CustomSlot.PrimaryRaised)) {
            add(slotPair(foreground = CustomSlot.OnPrimary, background = pressedOrRaised, kind = PairKind.Text))
        }
        for (surface in customSurfaces) {
            for (text in listOf(CustomSlot.OnSurface, CustomSlot.TextStrong, CustomSlot.TextMuted)) {
                add(slotPair(foreground = text, background = surface, kind = PairKind.Text))
            }
            for (shape in listOf(CustomSlot.BorderStrong, CustomSlot.FocusRing)) {
                add(slotPair(foreground = shape, background = surface, kind = PairKind.NonText))
            }
        }
    }

    private val fluentPairs: List<ContrastPair> =
        FluentText.entries.map { text ->
            ContrastPair(
                foreground = ColorRef.OfFluentText(text),
                background = ColorRef.OfRole(Role.Primary),
                kind = PairKind.Text,
            )
        }

    private fun accentPairs(accentCount: Int): List<ContrastPair> =
        (0 until accentCount).flatMap { index ->
            listOf(
                accentText(index = index, foreground = AccentPart.OnColor, background = AccentPart.Color),
                accentText(index = index, foreground = AccentPart.OnContainer, background = AccentPart.Container),
            )
        }

    private fun roleText(
        foreground: Role,
        background: Role,
    ): ContrastPair = ContrastPair(ColorRef.OfRole(foreground), ColorRef.OfRole(background), PairKind.Text)

    private fun roleShape(
        foreground: Role,
        background: Role,
    ): ContrastPair = ContrastPair(ColorRef.OfRole(foreground), ColorRef.OfRole(background), PairKind.NonText)

    private fun slotPair(
        foreground: CustomSlot,
        background: CustomSlot,
        kind: PairKind,
    ): ContrastPair = ContrastPair(ColorRef.OfSlot(foreground), ColorRef.OfSlot(background), kind)

    private fun accentText(
        index: Int,
        foreground: AccentPart,
        background: AccentPart,
    ): ContrastPair =
        ContrastPair(
            foreground = ColorRef.OfAccent(AccentSlot(index = index, part = foreground)),
            background = ColorRef.OfAccent(AccentSlot(index = index, part = background)),
            kind = PairKind.Text,
        )
}
