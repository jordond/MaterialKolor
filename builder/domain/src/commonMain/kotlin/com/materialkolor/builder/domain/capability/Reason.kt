package com.materialkolor.builder.domain.capability

/**
 * Why a control is disabled, hidden or carries a note for the current target.
 *
 * The UI owns the wording it actually shows and looks it up by [key], so the text can be translated
 * or reworded without touching the matrix.
 *
 * @property[key] The string resource the UI shows for this reason.
 */
public enum class Reason(
    public val key: String,
) {
    /**
     * Fluent themes one accent ramp from the primary palette. Secondary, tertiary, error and neutral
     * colors never reach it, and its neutrals are fixed greys.
     */
    FluentOneRamp(key = "reason_fluent_one_ramp"),

    /**
     * For Fluent, style only changes the chroma of the accent ramp.
     */
    FluentStyleChroma(key = "reason_fluent_style_chroma"),

    /**
     * Fluent's text colors are fixed and its ramps ignore contrast.
     */
    FluentFixedText(key = "reason_fluent_fixed_text"),

    /**
     * Contrast changes role-based slots. Slots picked by tone keep their tones.
     */
    ToneSlotsKeepTones(key = "reason_tone_slots_keep_tones"),

    /**
     * Pins set Material roles, which Fluent does not use.
     */
    FluentUsesNoRoles(key = "reason_fluent_uses_no_roles"),

    /**
     * The Unstyled adapter has no AMOLED switch yet.
     */
    UnstyledNoAmoled(key = "reason_unstyled_no_amoled"),

    /**
     * Fluent has no place for extra colors in v1.
     */
    FluentNoAccents(key = "reason_fluent_no_accents"),

    /**
     * Unstyled and Fluent need JVM 17 and have no macOS native target. Unstyled needs Android
     * minSdk 23.
     */
    PlatformLimits(key = "reason_platform_limits"),

    /**
     * The 2025 Dim roles stay hidden in v1 until a library module exposes them.
     */
    DimRolesUnexposed(key = "reason_dim_roles_unexposed"),
}
