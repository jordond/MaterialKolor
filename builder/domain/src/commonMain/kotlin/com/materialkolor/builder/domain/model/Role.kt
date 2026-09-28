package com.materialkolor.builder.domain.model

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.color.Argb
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A color slot of a Material 3 scheme.
 *
 * The list is every role the Material 3 module fills, in the order that module writes them. The
 * 2025 dim accents are deliberately absent, they are not part of a `ColorScheme` the builder can
 * hand back, so there is nothing for the Roles tab to show or for an export to write.
 *
 * @property[code] The number the share codec writes for this role.
 * @property[group] Which section of the Roles tab the role is listed under.
 */
@Serializable
public enum class Role(
    override val code: Int,
    public val group: RoleGroup,
) : CodedEnum {
    @SerialName("Primary")
    Primary(code = 0, group = RoleGroup.Accent),

    @SerialName("OnPrimary")
    OnPrimary(code = 1, group = RoleGroup.Accent),

    @SerialName("PrimaryContainer")
    PrimaryContainer(code = 2, group = RoleGroup.Accent),

    @SerialName("OnPrimaryContainer")
    OnPrimaryContainer(code = 3, group = RoleGroup.Accent),

    @SerialName("InversePrimary")
    InversePrimary(code = 4, group = RoleGroup.OutlineInverse),

    @SerialName("Secondary")
    Secondary(code = 5, group = RoleGroup.Accent),

    @SerialName("OnSecondary")
    OnSecondary(code = 6, group = RoleGroup.Accent),

    @SerialName("SecondaryContainer")
    SecondaryContainer(code = 7, group = RoleGroup.Accent),

    @SerialName("OnSecondaryContainer")
    OnSecondaryContainer(code = 8, group = RoleGroup.Accent),

    @SerialName("Tertiary")
    Tertiary(code = 9, group = RoleGroup.Accent),

    @SerialName("OnTertiary")
    OnTertiary(code = 10, group = RoleGroup.Accent),

    @SerialName("TertiaryContainer")
    TertiaryContainer(code = 11, group = RoleGroup.Accent),

    @SerialName("OnTertiaryContainer")
    OnTertiaryContainer(code = 12, group = RoleGroup.Accent),

    @SerialName("Background")
    Background(code = 13, group = RoleGroup.Surface),

    @SerialName("OnBackground")
    OnBackground(code = 14, group = RoleGroup.Surface),

    @SerialName("Surface")
    Surface(code = 15, group = RoleGroup.Surface),

    @SerialName("OnSurface")
    OnSurface(code = 16, group = RoleGroup.Surface),

    @SerialName("SurfaceVariant")
    SurfaceVariant(code = 17, group = RoleGroup.Surface),

    @SerialName("OnSurfaceVariant")
    OnSurfaceVariant(code = 18, group = RoleGroup.Surface),

    @SerialName("SurfaceTint")
    SurfaceTint(code = 19, group = RoleGroup.Surface),

    @SerialName("InverseSurface")
    InverseSurface(code = 20, group = RoleGroup.OutlineInverse),

    @SerialName("InverseOnSurface")
    InverseOnSurface(code = 21, group = RoleGroup.OutlineInverse),

    @SerialName("Error")
    Error(code = 22, group = RoleGroup.Accent),

    @SerialName("OnError")
    OnError(code = 23, group = RoleGroup.Accent),

    @SerialName("ErrorContainer")
    ErrorContainer(code = 24, group = RoleGroup.Accent),

    @SerialName("OnErrorContainer")
    OnErrorContainer(code = 25, group = RoleGroup.Accent),

    @SerialName("Outline")
    Outline(code = 26, group = RoleGroup.OutlineInverse),

    @SerialName("OutlineVariant")
    OutlineVariant(code = 27, group = RoleGroup.OutlineInverse),

    @SerialName("Scrim")
    Scrim(code = 28, group = RoleGroup.OutlineInverse),

    @SerialName("SurfaceBright")
    SurfaceBright(code = 29, group = RoleGroup.Surface),

    @SerialName("SurfaceDim")
    SurfaceDim(code = 30, group = RoleGroup.Surface),

    @SerialName("SurfaceContainer")
    SurfaceContainer(code = 31, group = RoleGroup.Surface),

    @SerialName("SurfaceContainerHigh")
    SurfaceContainerHigh(code = 32, group = RoleGroup.Surface),

    @SerialName("SurfaceContainerHighest")
    SurfaceContainerHighest(code = 33, group = RoleGroup.Surface),

    @SerialName("SurfaceContainerLow")
    SurfaceContainerLow(code = 34, group = RoleGroup.Surface),

    @SerialName("SurfaceContainerLowest")
    SurfaceContainerLowest(code = 35, group = RoleGroup.Surface),

    @SerialName("PrimaryFixed")
    PrimaryFixed(code = 36, group = RoleGroup.Fixed),

    @SerialName("PrimaryFixedDim")
    PrimaryFixedDim(code = 37, group = RoleGroup.Fixed),

    @SerialName("OnPrimaryFixed")
    OnPrimaryFixed(code = 38, group = RoleGroup.Fixed),

    @SerialName("OnPrimaryFixedVariant")
    OnPrimaryFixedVariant(code = 39, group = RoleGroup.Fixed),

    @SerialName("SecondaryFixed")
    SecondaryFixed(code = 40, group = RoleGroup.Fixed),

    @SerialName("SecondaryFixedDim")
    SecondaryFixedDim(code = 41, group = RoleGroup.Fixed),

    @SerialName("OnSecondaryFixed")
    OnSecondaryFixed(code = 42, group = RoleGroup.Fixed),

    @SerialName("OnSecondaryFixedVariant")
    OnSecondaryFixedVariant(code = 43, group = RoleGroup.Fixed),

    @SerialName("TertiaryFixed")
    TertiaryFixed(code = 44, group = RoleGroup.Fixed),

    @SerialName("TertiaryFixedDim")
    TertiaryFixedDim(code = 45, group = RoleGroup.Fixed),

    @SerialName("OnTertiaryFixed")
    OnTertiaryFixed(code = 46, group = RoleGroup.Fixed),

    @SerialName("OnTertiaryFixedVariant")
    OnTertiaryFixedVariant(code = 47, group = RoleGroup.Fixed),
    ;

    /**
     * The role that gets painted on top of this one, when there is one.
     *
     * Roles that are themselves a content color, and the ones nothing is ever drawn on, answer
     * null. The pair drives the contrast readout beside every swatch in the Roles tab.
     *
     * This is a getter rather than a constructor argument because an enum entry cannot name an
     * entry declared after it without reading a null.
     */
    public val onPair: Role?
        get() = when (this) {
            Primary -> OnPrimary
            PrimaryContainer -> OnPrimaryContainer
            Secondary -> OnSecondary
            SecondaryContainer -> OnSecondaryContainer
            Tertiary -> OnTertiary
            TertiaryContainer -> OnTertiaryContainer
            Error -> OnError
            ErrorContainer -> OnErrorContainer
            Background -> OnBackground
            Surface -> OnSurface
            SurfaceVariant -> OnSurfaceVariant
            SurfaceBright -> OnSurface
            SurfaceDim -> OnSurface
            SurfaceContainer -> OnSurface
            SurfaceContainerHigh -> OnSurface
            SurfaceContainerHighest -> OnSurface
            SurfaceContainerLow -> OnSurface
            SurfaceContainerLowest -> OnSurface
            InverseSurface -> InverseOnSurface
            PrimaryFixed -> OnPrimaryFixed
            PrimaryFixedDim -> OnPrimaryFixed
            SecondaryFixed -> OnSecondaryFixed
            SecondaryFixedDim -> OnSecondaryFixed
            TertiaryFixed -> OnTertiaryFixed
            TertiaryFixedDim -> OnTertiaryFixed
            OnPrimary -> null
            OnPrimaryContainer -> null
            InversePrimary -> null
            OnSecondary -> null
            OnSecondaryContainer -> null
            OnTertiary -> null
            OnTertiaryContainer -> null
            OnBackground -> null
            OnSurface -> null
            OnSurfaceVariant -> null
            SurfaceTint -> null
            InverseOnSurface -> null
            OnError -> null
            OnErrorContainer -> null
            Outline -> null
            OutlineVariant -> null
            Scrim -> null
            OnPrimaryFixed -> null
            OnPrimaryFixedVariant -> null
            OnSecondaryFixed -> null
            OnSecondaryFixedVariant -> null
            OnTertiaryFixed -> null
            OnTertiaryFixedVariant -> null
        }
}

/**
 * The section of the Roles tab a role is listed under.
 *
 * @property[code] The number the share codec writes for this group.
 */
@Serializable
public enum class RoleGroup(
    override val code: Int,
) : CodedEnum {
    /**
     * The accent families and their containers, error included.
     */
    @SerialName("Accent")
    Accent(code = 0),

    /**
     * Backgrounds, the surface steps and what reads on them.
     */
    @SerialName("Surface")
    Surface(code = 1),

    /**
     * The fixed accents, the ones that hold their tone across both modes.
     */
    @SerialName("Fixed")
    Fixed(code = 2),

    /**
     * Outlines, the scrim and the inverse roles.
     */
    @SerialName("OutlineInverse")
    OutlineInverse(code = 3),
}

/**
 * A color someone nailed a role to, so the engine stops deriving it.
 *
 * At least one mode has to carry a color, a pin with neither is a pin of nothing. A pin that only
 * sets one mode leaves the other derived, which is the usual way a brand color gets held in light
 * mode while dark mode keeps working.
 *
 * @property[light] The color the role takes in light mode, or null to keep deriving it.
 * @property[dark] The color the role takes in dark mode, or null to keep deriving it.
 */
@Immutable
@Serializable
public data class RolePin(
    @SerialName("light")
    public val light: Argb? = null,
    @SerialName("dark")
    public val dark: Argb? = null,
) {
    init {
        require(light != null || dark != null) { "A role pin needs a light color, a dark color, or both" }
    }
}
