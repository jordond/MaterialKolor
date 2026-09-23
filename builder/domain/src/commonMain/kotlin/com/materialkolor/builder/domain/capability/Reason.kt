package com.materialkolor.builder.domain.capability

/**
 * Why a control is disabled, hidden or carries a note for the current target.
 *
 * Each entry is one of the reasons R1 to R10 in the product spec, and its KDoc is the spec's
 * sentence. The UI owns the wording it actually shows and looks it up by [key], so the text can be
 * translated or reworded without touching the matrix.
 *
 * @property[key] The string resource the UI shows for this reason.
 */
public enum class Reason(
    public val key: String,
) {
    /**
     * R1. Fluent themes one accent ramp from the primary palette. Secondary, tertiary, error and
     * neutral colors never reach it, and its neutrals are fixed greys.
     */
    FluentOneRamp(key = "reason_fluent_one_ramp"),

    /** R2. For Fluent, style only changes the chroma of the accent ramp. */
    FluentStyleChroma(key = "reason_fluent_style_chroma"),

    /** R3. Fluent's text colors are fixed and its ramps ignore contrast. */
    FluentFixedText(key = "reason_fluent_fixed_text"),

    /** R4. Contrast changes role-based slots. Slots picked by tone keep their tones. */
    ToneSlotsKeepTones(key = "reason_tone_slots_keep_tones"),

    /** R5. Pins set Material roles, which Fluent does not use. */
    FluentUsesNoRoles(key = "reason_fluent_uses_no_roles"),

    /**
     * R6. The Unstyled adapter has no AMOLED switch yet. It arrives once the explicit-scheme
     * export is verified (V-02, F-56).
     */
    UnstyledNoAmoled(key = "reason_unstyled_no_amoled"),

    /** R8. Fluent has no place for extra accents in v1. */
    FluentNoAccents(key = "reason_fluent_no_accents"),

    /**
     * R9. Unstyled and Fluent need JVM 17 and have no macOS native target. Unstyled needs Android
     * minSdk 23.
     */
    PlatformLimits(key = "reason_platform_limits"),

    /** R10. The 2025 Dim roles stay hidden in v1 until a library module exposes them (OQ-6). */
    DimRolesUnexposed(key = "reason_dim_roles_unexposed"),
}
