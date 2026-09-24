package com.materialkolor.builder.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Codes 18 to 29 and 41 to 45 are retired. They belonged to the Love, Cold and Warm families and
// the five drink slots, which are now accents. A link shared before they went may still carry one,
// so no slot ever takes those numbers again.

/**
 * A slot of the custom theme target.
 *
 * The vocabulary is fixed and structural. Two interaction tones on primary, three surface steps,
 * two text steps, three border steps and a handful of decorative colors that belong to no family,
 * on top of the roles the scheme already names. Every slot here resolves out of the theme itself,
 * so the exported theme is the same shape every time and the Custom tab can show one row per slot.
 *
 * Color families beyond the scheme's own are not slots. They come from the document's accents,
 * each of which carries its own seed, and they are addressed by [AccentSlot] instead.
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

    @SerialName("scrim")
    Scrim(code = 46, resolution = SlotResolution.FromRole(Role.Scrim)),

    /**
     * The keyboard focus ring. Light mode cuts tone 50, which holds 3 to 1 on every surface step
     * the kit rings on whatever the seed, since a tone fixes the luminance. Tone 60 stood only 2.7
     * to 1 on the sunken step (D46). Dark mode keeps tone 60.
     */
    @SerialName("focusRing")
    FocusRing(code = 47, resolution = SlotResolution.FromRamp(TonalRamp.Primary, light = 50, dark = 60)),

    @SerialName("shadow")
    Shadow(code = 48, resolution = SlotResolution.FromRamp(TonalRamp.Neutral, light = 0, dark = 0)),
}

/**
 * Where a custom slot gets its color.
 *
 * A slot either takes a role the scheme already solved, or cuts a tone off a ramp itself. Material
 * has no name for a pressed state, a surface step or a border step, so those are tones.
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
 * These are the scheme's own palettes and nothing else, because those are the only ramps the
 * theme can hand back for any document. A family with a seed of its own is an accent, and an
 * accent's ramp is built per family rather than named here.
 */
public enum class TonalRamp {
    Primary,
    Secondary,
    Tertiary,
    Error,
    Neutral,
    NeutralVariant,
}

/**
 * The tones someone moved a custom slot to, one per mode.
 *
 * A tone always names the slot it is attached to, never a color that slot is worked out against.
 * On a [SlotResolution.FromRamp] slot it moves the tone the slot is cut at. On a
 * [SlotResolution.OnRamp] slot it moves the on-color's own tone and the contrast derivation is
 * skipped for that one slot, which is how someone sets an on-color by hand. The background tone
 * an on-color reads against stays where its own slot puts it.
 *
 * A null mode keeps the tone the slot's [SlotResolution] already carries, so a document only
 * holds the tones that were actually changed.
 *
 * @property[light] The tone in light mode, or null to keep the slot's own.
 * @property[dark] The tone in dark mode, or null to keep the slot's own.
 */
@Serializable
public data class CustomTone(
    @SerialName("light")
    public val light: Int? = null,
    @SerialName("dark")
    public val dark: Int? = null,
) {
    init {
        require(light == null || light in 0..100) { "A tone is 0 to 100, got light $light" }
        require(dark == null || dark in 0..100) { "A tone is 0 to 100, got dark $dark" }
    }
}
