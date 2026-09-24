package com.materialkolor.builder.feature.poster

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.capability.Reason
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.reason_dim_roles_unexposed
import com.materialkolor.builder.generated.resources.reason_fluent_fixed_text
import com.materialkolor.builder.generated.resources.reason_fluent_no_accents
import com.materialkolor.builder.generated.resources.reason_fluent_one_ramp
import com.materialkolor.builder.generated.resources.reason_fluent_style_chroma
import com.materialkolor.builder.generated.resources.reason_fluent_uses_no_roles
import com.materialkolor.builder.generated.resources.reason_platform_limits
import com.materialkolor.builder.generated.resources.reason_tone_slots_keep_tones
import com.materialkolor.builder.generated.resources.reason_unstyled_no_amoled
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The words for [reason], the string named by its key. */
internal fun reasonText(reason: Reason): StringResource =
    when (reason) {
        Reason.FluentOneRamp -> Res.string.reason_fluent_one_ramp
        Reason.FluentStyleChroma -> Res.string.reason_fluent_style_chroma
        Reason.FluentFixedText -> Res.string.reason_fluent_fixed_text
        Reason.ToneSlotsKeepTones -> Res.string.reason_tone_slots_keep_tones
        Reason.FluentUsesNoRoles -> Res.string.reason_fluent_uses_no_roles
        Reason.UnstyledNoAmoled -> Res.string.reason_unstyled_no_amoled
        Reason.FluentNoAccents -> Res.string.reason_fluent_no_accents
        Reason.PlatformLimits -> Res.string.reason_platform_limits
        Reason.DimRolesUnexposed -> Res.string.reason_dim_roles_unexposed
    }

/** Whether the control takes input. */
internal val ControlState.usable: Boolean
    get() = this is ControlState.Enabled

/**
 * What the poster says about the control, the note beside a working one or why it is off, or
 * null when there is nothing to say.
 */
internal val ControlState.explanation: Reason?
    get() = when (this) {
        is ControlState.Enabled -> note
        is ControlState.Hidden -> reason
        is ControlState.Disabled -> reason
    }

/** [reason] in words, in the place of the control it explains. */
@Composable
internal fun ReasonLine(
    reason: Reason,
    modifier: Modifier = Modifier,
) {
    BuilderText(text = stringResource(reasonText(reason)), modifier = modifier, emphasis = Emphasis.Secondary)
}
