package com.materialkolor.builder.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A slot of the custom theme target.
 *
 * The vocabulary is fixed, it is the one the custom theme sample ships, so the exported theme is
 * the same shape every time and the Custom tab can show one row per slot. Seven color families
 * instead of three, two interaction tones on primary, three surface steps, and a handful of
 * decorative colors that belong to no family.
 *
 * Each entry serializes under the name of the property it generates, so a saved document reads
 * the same way the exported theme does.
 *
 * @property[code] The number the share codec writes for this slot.
 * @property[resolution] Where the slot's color comes from before any override is applied.
 */
@Serializable
public enum class CustomSlot(
    override val code: Int,
    public val resolution: SlotResolution,
) : CodedEnum {
    @SerialName("primary")
    Primary(code = 0, resolution = SlotResolution.FromRole(Role.Primary)),

    @SerialName("onPrimary")
    OnPrimary(code = 1, resolution = SlotResolution.FromRole(Role.OnPrimary)),

    @SerialName("primaryContainer")
    PrimaryContainer(code = 2, resolution = SlotResolution.FromRole(Role.PrimaryContainer)),

    @SerialName("onPrimaryContainer")
    OnPrimaryContainer(code = 3, resolution = SlotResolution.FromRole(Role.OnPrimaryContainer)),

    @SerialName("primaryPressed")
    PrimaryPressed(code = 4, resolution = SlotResolution.FromRamp(TonalRamp.Primary, light = 32, dark = 70)),

    @SerialName("primaryRaised")
    PrimaryRaised(code = 5, resolution = SlotResolution.FromRamp(TonalRamp.Primary, light = 46, dark = 88)),

    @SerialName("secondary")
    Secondary(code = 6, resolution = SlotResolution.FromRole(Role.Secondary)),

    @SerialName("onSecondary")
    OnSecondary(code = 7, resolution = SlotResolution.FromRole(Role.OnSecondary)),

    @SerialName("secondaryContainer")
    SecondaryContainer(code = 8, resolution = SlotResolution.FromRole(Role.SecondaryContainer)),

    @SerialName("onSecondaryContainer")
    OnSecondaryContainer(code = 9, resolution = SlotResolution.FromRole(Role.OnSecondaryContainer)),

    @SerialName("tertiary")
    Tertiary(code = 10, resolution = SlotResolution.FromRole(Role.Tertiary)),

    @SerialName("onTertiary")
    OnTertiary(code = 11, resolution = SlotResolution.FromRole(Role.OnTertiary)),

    @SerialName("tertiaryContainer")
    TertiaryContainer(code = 12, resolution = SlotResolution.FromRole(Role.TertiaryContainer)),

    @SerialName("onTertiaryContainer")
    OnTertiaryContainer(code = 13, resolution = SlotResolution.FromRole(Role.OnTertiaryContainer)),

    @SerialName("error")
    Error(code = 14, resolution = SlotResolution.FromRole(Role.Error)),

    @SerialName("onError")
    OnError(code = 15, resolution = SlotResolution.FromRole(Role.OnError)),

    @SerialName("errorContainer")
    ErrorContainer(code = 16, resolution = SlotResolution.FromRole(Role.ErrorContainer)),

    @SerialName("onErrorContainer")
    OnErrorContainer(code = 17, resolution = SlotResolution.FromRole(Role.OnErrorContainer)),

    @SerialName("love")
    Love(code = 18, resolution = SlotResolution.FromRamp(TonalRamp.Love, light = 40, dark = 80)),

    @SerialName("onLove")
    OnLove(code = 19, resolution = SlotResolution.OnRamp(TonalRamp.Love, light = 40, dark = 80)),

    @SerialName("loveContainer")
    LoveContainer(code = 20, resolution = SlotResolution.FromRamp(TonalRamp.Love, light = 90, dark = 30)),

    @SerialName("onLoveContainer")
    OnLoveContainer(code = 21, resolution = SlotResolution.OnRamp(TonalRamp.Love, light = 90, dark = 30)),

    @SerialName("cold")
    Cold(code = 22, resolution = SlotResolution.FromRamp(TonalRamp.Cold, light = 40, dark = 80)),

    @SerialName("onCold")
    OnCold(code = 23, resolution = SlotResolution.OnRamp(TonalRamp.Cold, light = 40, dark = 80)),

    @SerialName("coldContainer")
    ColdContainer(code = 24, resolution = SlotResolution.FromRamp(TonalRamp.Cold, light = 90, dark = 30)),

    @SerialName("onColdContainer")
    OnColdContainer(code = 25, resolution = SlotResolution.OnRamp(TonalRamp.Cold, light = 90, dark = 30)),

    @SerialName("warm")
    Warm(code = 26, resolution = SlotResolution.FromRamp(TonalRamp.Warm, light = 40, dark = 80)),

    @SerialName("onWarm")
    OnWarm(code = 27, resolution = SlotResolution.OnRamp(TonalRamp.Warm, light = 40, dark = 80)),

    @SerialName("warmContainer")
    WarmContainer(code = 28, resolution = SlotResolution.FromRamp(TonalRamp.Warm, light = 90, dark = 30)),

    @SerialName("onWarmContainer")
    OnWarmContainer(code = 29, resolution = SlotResolution.OnRamp(TonalRamp.Warm, light = 90, dark = 30)),

    @SerialName("surface")
    Surface(code = 30, resolution = SlotResolution.FromRole(Role.Surface)),

    @SerialName("surfaceRaised")
    SurfaceRaised(code = 31, resolution = SlotResolution.FromRamp(TonalRamp.Neutral, light = 100, dark = 12)),

    @SerialName("surfaceSunken")
    SurfaceSunken(code = 32, resolution = SlotResolution.FromRamp(TonalRamp.Neutral, light = 94, dark = 4)),

    @SerialName("surfaceInverse")
    SurfaceInverse(code = 33, resolution = SlotResolution.FromRole(Role.InverseSurface)),

    @SerialName("onSurface")
    OnSurface(code = 34, resolution = SlotResolution.FromRole(Role.OnSurface)),

    @SerialName("onSurfaceInverse")
    OnSurfaceInverse(code = 35, resolution = SlotResolution.FromRole(Role.InverseOnSurface)),

    @SerialName("textStrong")
    TextStrong(code = 36, resolution = SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 10, dark = 90)),

    @SerialName("textMuted")
    TextMuted(code = 37, resolution = SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 40, dark = 70)),

    @SerialName("borderFaint")
    BorderFaint(code = 38, resolution = SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 92, dark = 22)),

    @SerialName("borderSoft")
    BorderSoft(code = 39, resolution = SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 85, dark = 32)),

    @SerialName("borderStrong")
    BorderStrong(code = 40, resolution = SlotResolution.FromRamp(TonalRamp.NeutralVariant, light = 55, dark = 65)),

    @SerialName("drinkCoffee")
    DrinkCoffee(code = 41, resolution = SlotResolution.FromRamp(TonalRamp.Coffee, light = 50, dark = 50)),

    @SerialName("drinkMatcha")
    DrinkMatcha(code = 42, resolution = SlotResolution.FromRamp(TonalRamp.Matcha, light = 50, dark = 50)),

    @SerialName("drinkIced")
    DrinkIced(code = 43, resolution = SlotResolution.FromRamp(TonalRamp.Iced, light = 50, dark = 50)),

    @SerialName("drinkTea")
    DrinkTea(code = 44, resolution = SlotResolution.FromRamp(TonalRamp.Tea, light = 50, dark = 50)),

    @SerialName("drinkChoc")
    DrinkChoc(code = 45, resolution = SlotResolution.FromRamp(TonalRamp.Chocolate, light = 50, dark = 50)),

    @SerialName("scrim")
    Scrim(code = 46, resolution = SlotResolution.FromRole(Role.Scrim)),

    @SerialName("focusRing")
    FocusRing(code = 47, resolution = SlotResolution.FromRamp(TonalRamp.Primary, light = 60, dark = 60)),

    @SerialName("shadow")
    Shadow(code = 48, resolution = SlotResolution.FromRamp(TonalRamp.Neutral, light = 0, dark = 0)),
}

/**
 * Where a custom slot gets its color.
 *
 * A slot either takes a role the scheme already solved, or cuts a tone off a ramp itself. Material
 * has no name for a pressed state, a border step or a category color, so those are tones.
 */
public sealed interface SlotResolution {
    /**
     * The slot is a scheme role, the same color the Roles tab shows.
     *
     * @property[role] The role the slot takes.
     */
    public data class FromRole(
        public val role: Role,
    ) : SlotResolution

    /**
     * The slot is a tone cut off a ramp, once for light and once for dark.
     *
     * @property[ramp] The palette the tone is cut from.
     * @property[light] The tone in light mode.
     * @property[dark] The tone in dark mode.
     */
    public data class FromRamp(
        public val ramp: TonalRamp,
        public val light: Int,
        public val dark: Int,
    ) : SlotResolution

    /**
     * The slot is the color that reads on a tone of a ramp, the content color of [FromRamp].
     *
     * @property[ramp] The palette the background tone is cut from.
     * @property[light] The background tone in light mode.
     * @property[dark] The background tone in dark mode.
     */
    public data class OnRamp(
        public val ramp: TonalRamp,
        public val light: Int,
        public val dark: Int,
    ) : SlotResolution
}

/**
 * A palette a custom slot can cut a tone from.
 *
 * The first six are the scheme's own palettes. The rest are the families the custom target owns,
 * each built from its own seed and harmonized with the theme seed.
 */
public enum class TonalRamp {
    Primary,
    Secondary,
    Tertiary,
    Error,
    Neutral,
    NeutralVariant,
    Love,
    Cold,
    Warm,
    Coffee,
    Matcha,
    Iced,
    Tea,
    Chocolate,
}

/**
 * The tones someone moved a custom slot to, one per mode.
 *
 * A null mode keeps the tone the slot's [SlotResolution] already carries, so a document only
 * holds the tones that were actually changed.
 *
 * @property[light] The tone in light mode, or null to keep the slot's own.
 * @property[dark] The tone in dark mode, or null to keep the slot's own.
 */
@Serializable
public data class CustomTone(
    public val light: Int? = null,
    public val dark: Int? = null,
) {
    init {
        require(light == null || light in 0..100) { "A tone is 0 to 100, got light $light" }
        require(dark == null || dark in 0..100) { "A tone is 0 to 100, got dark $dark" }
    }
}
