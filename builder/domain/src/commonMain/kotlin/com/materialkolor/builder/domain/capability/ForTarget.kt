package com.materialkolor.builder.domain.capability

import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ExportTarget

/**
 * This document as [target] sees it, with every field the capability table turns off for that
 * target put back to its default.
 *
 * Every export and every preview reads this rather than the raw document, so a setting left over
 * from another target never leaks into what [target] writes or shows. The stored document keeps
 * everything, so moving back to the other target brings the setting back.
 *
 * The fields follow from [Capabilities], read with the document's own style and effective spec. A
 * control that is disabled or hidden for [target] clears its field, so a new reason or a new cell
 * in the table carries over without a change here. Today that means
 * - [Reason.FluentOneRamp] keeps only the primary key color and drops the Cmf second seed for Fluent,
 * - [Reason.FluentFixedText] puts Fluent back on standard contrast,
 * - [Reason.FluentUsesNoRoles] drops the pins for Fluent,
 * - [Reason.UnstyledNoAmoled] turns AMOLED off for Unstyled,
 * - [Reason.FluentNoAccents] drops the accents for Fluent,
 * - the hidden rows turn AMOLED off for Fluent, drop the custom tones for every target but
 *   Custom, and put the motion scheme back to its default for every target but Material 3
 *   Expressive,
 * - the style and spec rows drop the Cmf second seed when the style is not Cmf, and put the
 *   platform back to its default when the scheme runs the 2021 spec. Neither changes a color there.
 *
 * [Reason.FluentStyleChroma], [Reason.ToneSlotsKeepTones] and [Reason.PlatformLimits] are notes on
 * controls that stay on, so they clear nothing. [Reason.DimRolesUnexposed] hides the Dim roles, which
 * the document has no field for. The seed has no empty value, and the preview, export and animation
 * rows are view state or export options that live outside the document.
 *
 * The library and the expressive flag are left as they are, so the result still names the target
 * the document was saved with.
 */
public fun ThemeDocument.forTarget(target: ExportTarget): ThemeDocument {
    val capabilities = Capabilities.of(
        library = target.library,
        expressive = target == ExportTarget.Material3Expressive,
        style = style,
        effectiveSpec = EffectiveSpec.of(style = style, requested = spec),
    )

    return Control.entries
        .filter { control -> capabilities[control] !is ControlState.Enabled }
        .fold(this) { document, control -> document.without(control) }
}

/**
 * The library whose column of the table [ExportTarget.of] maps onto this target.
 */
private val ExportTarget.library: Library
    get() = when (this) {
        ExportTarget.Material3,
        ExportTarget.Material3Expressive,
        -> Library.Material3
        ExportTarget.Unstyled -> Library.Unstyled
        ExportTarget.Fluent -> Library.Fluent
        ExportTarget.Custom -> Library.Custom
    }

/**
 * This document with the field [control] sets put back to its default, or as it is when there is none.
 */
private fun ThemeDocument.without(control: Control): ThemeDocument =
    when (control) {
        Control.PrimaryOverride -> copy(keyColors = keyColors.copy(primary = null))
        Control.OtherOverrides -> copy(keyColors = KeyColors(primary = keyColors.primary))
        Control.Style -> copy(style = ThemeDocument.Default.style)
        Control.CmfSecondSeed -> copy(cmfTertiarySeed = null)
        Control.Contrast -> copy(contrast = ThemeDocument.Default.contrast)
        Control.SpecVersion -> copy(spec = ThemeDocument.Default.spec)
        Control.Platform -> copy(platform = ThemeDocument.Default.platform)
        Control.RolePins -> copy(pins = emptyMap())
        Control.AmoledDark -> copy(amoled = false)
        Control.MotionScheme -> copy(motionScheme = ThemeDocument.Default.motionScheme)
        Control.ExtendedColors -> copy(accents = emptyList())
        Control.CustomToneTable -> copy(customTones = emptyMap())
        Control.SeedEntryPoints,
        Control.ColorAnimation,
        Control.PreviewModes,
        Control.DeviceWidth,
        Control.Inspect,
        Control.FrozenExport,
        Control.KmpOrAndroid,
        Control.VersionCatalog,
        Control.DimRoles,
        -> this
    }
