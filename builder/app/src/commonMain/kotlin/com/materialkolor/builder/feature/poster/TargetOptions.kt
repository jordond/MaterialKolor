package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.capability.ControlState
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.extras_amoled
import com.materialkolor.builder.generated.resources.extras_motion_expressive
import com.materialkolor.builder.generated.resources.extras_motion_label
import com.materialkolor.builder.generated.resources.extras_motion_standard
import com.materialkolor.builder.generated.resources.extras_targets_label
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The options only some export targets have (F-18), AMOLED dark, the motion scheme and the export's
 * color animation. Each shows only for the targets that have it, and color animation only while the
 * target's export runs dynamic. The label over them goes too when none of them shows, and it asks
 * [showsColorAnimation] the same as the option does, so it never stands over nothing.
 */
@Composable
internal fun TargetOptions(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val amoled = context.capabilities[Control.AmoledDark]
    val motion = context.capabilities[Control.MotionScheme]
    val animation = context.showsColorAnimation()
    if (!amoled.shown && !motion.shown && !animation) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        InfoLabel(label = stringResource(Res.string.extras_targets_label), topic = InfoTopic.Targets)
        if (amoled.shown) AmoledOption(context, dispatcher, amoled)
        if (motion.shown) MotionOption(context, dispatcher, motion)
        ColorAnimationOption(context, dispatcher)
    }
}

/** The switch that drops dark mode's surfaces to pure black. */
@Composable
private fun AmoledOption(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    state: ControlState,
) {
    BuilderSwitch(
        checked = context.document.amoled,
        onCheckedChange = { on ->
            dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.SetAmoled(on), EditPhase.Discrete))
        },
        label = stringResource(Res.string.extras_amoled),
        enabled = state.usable,
    )
    state.explanation?.let { reason -> ReasonLine(reason) }
}

/** Standard or Expressive, the motion scheme the export carries and the preview moves with. */
@Composable
private fun MotionOption(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    state: ControlState,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val label = stringResource(Res.string.extras_motion_label)
    val names = MotionSchemeChoice.entries.associateWith { choice -> stringResource(motionName(choice)) }
    val current = context.document.motionScheme
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderText(text = label, style = BuilderTextStyle.SectionLabel)
        BuilderSegmented(
            options = MotionSchemeChoice.entries,
            selected = current,
            onSelect = { choice ->
                if (choice != current) {
                    val change = DocumentChange.SetMotionScheme(choice)
                    dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
                }
            },
            label = label,
            enabled = state.usable,
            optionLabel = { choice -> names.getValue(choice) },
        )
        state.explanation?.let { reason -> ReasonLine(reason) }
    }
}

/** What [choice] is called. */
private fun motionName(choice: MotionSchemeChoice): StringResource =
    when (choice) {
        MotionSchemeChoice.Standard -> Res.string.extras_motion_standard
        MotionSchemeChoice.Expressive -> Res.string.extras_motion_expressive
    }
