package com.materialkolor.builder.domain.capability

import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * A control whose state depends on the export target, one row of the per-target control matrix.
 */
public enum class Control {
    /** Every way to set the seed, typing, the picker, the eyedropper, an image and shuffle. */
    SeedEntryPoints,

    /** The primary key color override. */
    PrimaryOverride,

    /** The secondary, tertiary, error, neutral and neutral variant key color overrides. */
    OtherOverrides,

    /** The palette style picker. */
    Style,

    /** The second seed the Cmf style takes for its tertiary. Only shown while the style is Cmf. */
    CmfSecondSeed,

    /** The contrast slider. */
    Contrast,

    /** The spec version picker, whose options come from [EffectiveSpec.offered]. */
    SpecVersion,

    /** The Phone or Watch platform picker. */
    Platform,

    /** Pinning a role to a color of your own. */
    RolePins,

    /** The pure black dark mode switch. */
    AmoledDark,

    /** The motion scheme picker. */
    MotionScheme,

    /** Whether the exported theme animates between color changes. */
    ColorAnimation,

    /** Extra color families on top of the scheme, the document's accents. */
    ExtendedColors,

    /** The table of tones for the Custom target's slots. */
    CustomToneTable,

    /** The Light, Split and Dark preview modes. */
    PreviewModes,

    /** The device width the preview is laid out at. */
    DeviceWidth,

    /** Inspecting which role or slot a preview element is painted with. */
    Inspect,

    /** Exporting every color as a literal hex value. */
    FrozenExport,

    /** Choosing between a multiplatform and an Android only project. */
    KmpOrAndroid,

    /** Whether the dependency snippet goes through a version catalog. */
    VersionCatalog,

    /** The 2025 Dim roles. */
    DimRoles,
}

/**
 * How a [Control] shows up for the current target.
 */
public sealed interface ControlState {
    /**
     * The control is there and works.
     *
     * @property[note] Something worth saying next to the control, or null when it speaks for itself.
     */
    public data class Enabled(
        public val note: Reason? = null,
    ) : ControlState

    /**
     * The control is not shown at all.
     *
     * @property[reason] Why it is left out, for the places that explain an absence, or null when
     * the control simply has no meaning here.
     */
    public data class Hidden(
        public val reason: Reason? = null,
    ) : ControlState

    /**
     * The control is shown but cannot be used, with the reason in its place.
     *
     * @property[reason] Why it cannot be used.
     */
    public data class Disabled(
        public val reason: Reason,
    ) : ControlState
}

/**
 * The state of every [Control] for one target, style and spec.
 *
 * Build one with [of] and read a control with [get]. The column comes from the library and
 * whether it is expressive, the same way [ExportTarget.of] picks it, and the style and effective
 * spec settle the few rows that move with them.
 */
public class Capabilities private constructor(
    private val states: Map<Control, ControlState>,
) {
    /** How [control] shows up. */
    public operator fun get(control: Control): ControlState = states.getValue(control)

    override fun equals(other: Any?): Boolean = other is Capabilities && other.states == states

    override fun hashCode(): Int = states.hashCode()

    override fun toString(): String = "Capabilities($states)"

    public companion object {
        /**
         * The matrix column for [library] and [expressive], with [style] and [effectiveSpec]
         * applied.
         *
         * @param[effectiveSpec] The spec the scheme actually runs, from [EffectiveSpec.of].
         */
        public fun of(
            library: Library,
            expressive: Boolean,
            style: Style,
            effectiveSpec: SpecVersion,
        ): Capabilities {
            val target = ExportTarget.of(library = library, expressive = expressive)
            val states = Control.entries.associateWith { control ->
                stateOf(control = control, target = target, style = style, effectiveSpec = effectiveSpec)
            }
            return Capabilities(states)
        }
    }
}

private val enabled = ControlState.Enabled()

private val hidden = ControlState.Hidden()

private fun stateOf(
    control: Control,
    target: ExportTarget,
    style: Style,
    effectiveSpec: SpecVersion,
): ControlState =
    when (control) {
        Control.SeedEntryPoints -> enabled
        // Fluent takes the primary override too, it moves the accent ramp.
        Control.PrimaryOverride -> enabled
        Control.OtherOverrides -> fluentApart(target, fluent = ControlState.Disabled(Reason.FluentOneRamp))
        Control.Style -> fluentApart(target, fluent = ControlState.Enabled(Reason.FluentStyleChroma))
        Control.CmfSecondSeed -> cmfSecondSeed(target, style)
        Control.Contrast -> contrast(target)
        Control.SpecVersion -> enabled
        Control.Platform -> if (effectiveSpec == SpecVersion.Spec2021) hidden else enabled
        Control.RolePins -> rolePins(target)
        Control.AmoledDark -> amoledDark(target)
        Control.MotionScheme -> motionScheme(target)
        Control.ColorAnimation -> colorAnimation(target)
        Control.ExtendedColors -> fluentApart(target, fluent = ControlState.Disabled(Reason.FluentNoAccents))
        Control.CustomToneTable -> customToneTable(target)
        Control.PreviewModes -> enabled
        Control.DeviceWidth -> enabled
        Control.Inspect -> enabled
        Control.FrozenExport -> enabled
        Control.KmpOrAndroid -> kmpOrAndroid(target)
        Control.VersionCatalog -> enabled
        Control.DimRoles -> ControlState.Hidden(Reason.DimRolesUnexposed)
    }

/** A row where Fluent is the only target that differs and every other target is plainly enabled. */
private fun fluentApart(
    target: ExportTarget,
    fluent: ControlState,
): ControlState =
    when (target) {
        ExportTarget.Material3,
        ExportTarget.Material3Expressive,
        ExportTarget.Unstyled,
        ExportTarget.Custom,
        -> enabled
        ExportTarget.Fluent -> fluent
    }

private fun cmfSecondSeed(
    target: ExportTarget,
    style: Style,
): ControlState =
    if (style != Style.Cmf) {
        hidden
    } else {
        fluentApart(target, fluent = ControlState.Disabled(Reason.FluentOneRamp))
    }

private fun contrast(target: ExportTarget): ControlState =
    when (target) {
        ExportTarget.Material3,
        ExportTarget.Material3Expressive,
        ExportTarget.Unstyled,
        -> enabled
        ExportTarget.Fluent -> ControlState.Disabled(Reason.FluentFixedText)
        ExportTarget.Custom -> ControlState.Enabled(Reason.ToneSlotsKeepTones)
    }

private fun rolePins(target: ExportTarget): ControlState =
    when (target) {
        ExportTarget.Material3,
        ExportTarget.Material3Expressive,
        ExportTarget.Custom,
        -> enabled
        // v-02 B-112 may disable this with a new reason once the explicit-scheme export is checked.
        ExportTarget.Unstyled -> enabled
        ExportTarget.Fluent -> ControlState.Disabled(Reason.FluentUsesNoRoles)
    }

private fun amoledDark(target: ExportTarget): ControlState =
    when (target) {
        ExportTarget.Material3,
        ExportTarget.Material3Expressive,
        ExportTarget.Custom,
        -> enabled
        ExportTarget.Unstyled -> ControlState.Disabled(Reason.UnstyledNoAmoled)
        ExportTarget.Fluent -> hidden
    }

private fun motionScheme(target: ExportTarget): ControlState =
    when (target) {
        ExportTarget.Material3Expressive -> enabled
        ExportTarget.Material3,
        ExportTarget.Unstyled,
        ExportTarget.Fluent,
        ExportTarget.Custom,
        -> hidden
    }

private fun colorAnimation(target: ExportTarget): ControlState =
    when (target) {
        ExportTarget.Material3,
        ExportTarget.Material3Expressive,
        ExportTarget.Unstyled,
        -> enabled
        // u2-animate B-112 enables this once the Fluent export can call animateFluentColors.
        ExportTarget.Fluent -> ControlState.Hidden(Reason.FluentNoAnimation)
        // Hidden until F-56 gives the Custom export somewhere to animate.
        ExportTarget.Custom -> hidden
    }

private fun customToneTable(target: ExportTarget): ControlState =
    when (target) {
        ExportTarget.Custom -> enabled
        ExportTarget.Material3,
        ExportTarget.Material3Expressive,
        ExportTarget.Unstyled,
        ExportTarget.Fluent,
        -> hidden
    }

private fun kmpOrAndroid(target: ExportTarget): ControlState =
    when (target) {
        ExportTarget.Material3,
        ExportTarget.Material3Expressive,
        ExportTarget.Custom,
        -> enabled
        ExportTarget.Unstyled,
        ExportTarget.Fluent,
        -> ControlState.Enabled(Reason.PlatformLimits)
    }
