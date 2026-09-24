package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.export_animate
import com.materialkolor.builder.generated.resources.export_duration
import com.materialkolor.builder.generated.resources.export_duration_ms
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/** The animation lengths the export sheet offers, in milliseconds. A length stored from elsewhere joins them. */
private val AnimationDurationsMs: List<Int> = listOf(150, 300, 500, 1000)

/**
 * Whether the poster offers the export's color animation for the document's target. It does while
 * the target animates at all (X18) and its export runs dynamic, since a frozen export writes fixed
 * colors with nothing to animate. The option and the "Target options" label over it both ask here,
 * so the label never stands over nothing.
 */
internal fun PosterContext.showsColorAnimation(): Boolean =
    capabilities[Control.ColorAnimation].shown &&
        preferences.exportPrefsFor(exportTarget()).mode == ExportMode.Dynamic

/** The target the document exports to, whose export options this browser remembers. */
private fun PosterContext.exportTarget(): ExportTarget = ExportTarget.of(document.library, document.expressive)

/**
 * Whether the exported theme animates between color changes, and for how long (X18).
 *
 * It sets the export options this browser remembers for the document's target, the ones the export
 * sheet shows, and leaves every other target's alone. They live outside the document, so a change is
 * no undo entry. The length only shows while the animation is on.
 */
@Composable
internal fun ColorAnimationOption(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    if (!context.showsColorAnimation()) return
    val spacing = LocalBuilderTokens.current.spacing
    val state = context.capabilities[Control.ColorAnimation]
    val target = context.exportTarget()
    val prefs = context.preferences.exportPrefsFor(target)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        BuilderSwitch(
            checked = prefs.animate,
            onCheckedChange = { on -> dispatcher.dispatch(WorkspaceAction.SetColorAnimation(target, on)) },
            label = stringResource(Res.string.export_animate),
            enabled = state.usable,
        )
        if (prefs.animate) {
            DurationChoice(
                current = prefs.animationDurationMs,
                enabled = state.usable,
                onSelect = { ms -> dispatcher.dispatch(WorkspaceAction.SetColorAnimationDuration(target, ms)) },
            )
        }
        state.explanation?.let { reason -> ReasonLine(reason) }
    }
}

/** How long the animation runs, the lengths on offer with [current] among them. */
@Composable
private fun DurationChoice(
    current: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val label = stringResource(Res.string.export_duration)
    val durations = (AnimationDurationsMs + current).distinct().sorted()
    val names = durations.associateWith { ms -> stringResource(Res.string.export_duration_ms, ms) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderText(text = label, style = BuilderTextStyle.SectionLabel)
        BuilderSegmented(
            options = durations,
            selected = current,
            onSelect = { ms -> if (ms != current) onSelect(ms) },
            label = label,
            enabled = enabled,
            optionLabel = { ms -> names.getValue(ms) },
        )
    }
}
