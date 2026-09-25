package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalInputModeManager
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.OnColorThreshold
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.validate.MAX_ACCENTS
import com.materialkolor.builder.engine.audit.rate
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.canvas.RampTarget
import com.materialkolor.builder.feature.picker.PickerTarget
import com.materialkolor.builder.feature.picker.pickButtonFocus
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.accents_add
import com.materialkolor.builder.generated.resources.accents_cap
import com.materialkolor.builder.generated.resources.accents_empty
import com.materialkolor.builder.generated.resources.accents_harmonize
import com.materialkolor.builder.generated.resources.accents_label
import com.materialkolor.builder.generated.resources.accents_mode_dark
import com.materialkolor.builder.generated.resources.accents_mode_light
import com.materialkolor.builder.generated.resources.accents_name
import com.materialkolor.builder.generated.resources.accents_pick
import com.materialkolor.builder.generated.resources.accents_ratios
import com.materialkolor.builder.generated.resources.accents_remove
import com.materialkolor.builder.generated.resources.accents_seed
import com.materialkolor.builder.generated.resources.accents_show_on_ramp
import com.materialkolor.builder.generated.resources.accents_threshold
import com.materialkolor.builder.generated.resources.accents_threshold_aa
import com.materialkolor.builder.generated.resources.accents_threshold_aa_large
import com.materialkolor.builder.generated.resources.accents_threshold_aaa
import com.materialkolor.builder.generated.resources.accents_threshold_for
import com.materialkolor.builder.generated.resources.accents_tone_dark_color
import com.materialkolor.builder.generated.resources.accents_tone_dark_container
import com.materialkolor.builder.generated.resources.accents_tone_light_color
import com.materialkolor.builder.generated.resources.accents_tone_light_container
import com.materialkolor.builder.generated.resources.accents_tone_value
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderHexField
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderSegmented
import com.materialkolor.builder.kit.control.BuilderSlider
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextField
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The contrast rules an on color can be held to, loosest first.
 */
private val Thresholds: List<OnColorThreshold> =
    listOf(OnColorThreshold.AaLarge, OnColorThreshold.AaNormal, OnColorThreshold.Aaa)

/**
 * The tones an extra color's slider covers.
 */
private val AccentToneRange: ClosedFloatingPointRange<Float> = 0f..100f

/**
 * One arrow press moves one tone.
 */
private const val ACCENT_TONE_STEP = 1f

private const val MAX_ACCENT_TONE = 100

/**
 * The document's extra colors (F-17). Each one has its name, its seed with Pick and "Harmonize with
 * seed", the four colors it makes in the modes the preview shows with their contrast, Show on ramp
 * to see its ramp on the Palettes tab, its four tones and the contrast its on colors clear.
 *
 * It lists the document as stored, since the theme the target resolves has no extra colors at all
 * on a target that takes none (D35). There the list stays on screen without input and says why. Add
 * appends the first free `accentN`, a little further round the hue circle from the seed for each
 * one already there, and stays, turned off, once the theme holds as many as an export takes. Add
 * hands a keyboard user's focus to the new color's name field once its row is there, and Remove to
 * the next name field, else the one before, else Add.
 */
@Composable
internal fun AccentsEditor(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val input = LocalInputModeManager.current
    val state = context.capabilities[Control.ExtendedColors]
    if (!state.shown) return
    val accents = context.document.accents
    val focus = remember { AccentFocus() }
    val names = rememberAccentNameMessages()
    val hex = rememberHexMessages()
    val messages = remember(names, hex) { AccentRowMessages(names, hex) }
    val full = accents.size >= MAX_ACCENTS
    val reason = state.explanation
    // The new row only composes after the change lands, so the focus waits for the list to grow.
    // Moving it there also keeps it on the page when the eighth Add turns its own button off.
    LaunchedEffect(accents.size) {
        val added = focus.added
        focus.added = null
        if (added != null && added < accents.size) input.handFocusTo(focus.name(added))
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        InfoLabel(label = stringResource(Res.string.accents_label), topic = InfoTopic.Accents)
        when {
            reason != null -> {
                ReasonLine(reason)
            }
            accents.isEmpty() -> {
                BuilderText(text = stringResource(Res.string.accents_empty), emphasis = Emphasis.Secondary)
            }
        }
        accents.forEachIndexed { index, accent ->
            // Keyed by place, since an extra color has nothing else to tell it by. The row at a removed
            // color's place stays and shows the next one, and so does the focus handed to its name.
            key(index) {
                AccentRow(
                    context = context,
                    dispatcher = dispatcher,
                    index = index,
                    accent = accent,
                    enabled = state.usable,
                    messages = messages,
                    name = focus.name(index),
                    onRemove = {
                        val next = when {
                            index + 1 < accents.size -> focus.name(index)
                            index > 0 -> focus.name(index - 1)
                            else -> focus.add
                        }
                        input.handFocusTo(next)
                    },
                )
            }
        }
        BuilderButton(
            onClick = {
                focus.added = accents.size
                val change = DocumentChange.AddAccent(newAccent(context.document))
                dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Discrete))
            },
            label = stringResource(Res.string.accents_add),
            modifier = Modifier.focusRequester(focus.add),
            icon = IconId.Plus,
            enabled = state.usable && !full,
        )
        if (full) {
            BuilderText(text = stringResource(Res.string.accents_cap, MAX_ACCENTS), emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * One extra color, from its name down to Remove. Each change lands in the undo history, and changes
 * to the same color that follow close on each other fold into one step. A tone drag moves the
 * preview every frame and lets go as one.
 *
 * @param[name] Where the name field takes focus.
 * @param[onRemove] Runs before the color goes, to hand the focus on.
 */
@Composable
private fun AccentRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    index: Int,
    accent: Accent,
    enabled: Boolean,
    messages: AccentRowMessages,
    name: FocusRequester,
    onRemove: () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val update = { changed: Accent ->
        dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.UpdateAccent(index, changed), EditPhase.Discrete))
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderTextField(
            value = accent.name,
            onCommit = { text -> update(accent.copy(name = text)) },
            label = stringResource(Res.string.accents_name, index + 1),
            modifier = Modifier.fillMaxWidth().focusRequester(name),
            error = { draft -> messages.names.messageFor(accentNameProblems(context.document, index, draft)) },
            enabled = enabled,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorSwatch(accent.seed)
            BuilderHexField(
                value = accent.seed,
                onCommit = { argb, _ -> if (argb != accent.seed) update(accent.copy(seed = argb)) },
                label = stringResource(Res.string.accents_seed, accent.name),
                errorMessage = messages.hex::errorOf,
                noteMessage = messages.hex::noteOf,
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
            // b-307
            val pick = remember { FocusRequester() }
            BuilderIconButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenPicker(PickerTarget.Accent(index), pick)) },
                icon = IconId.Eyedropper,
                contentDescription = stringResource(Res.string.accents_pick, accent.name),
                modifier = pickButtonFocus(pick),
                enabled = enabled,
            )
        }
        BuilderSwitch(
            checked = accent.harmonize,
            onCheckedChange = { on -> update(accent.copy(harmonize = on)) },
            label = stringResource(Res.string.accents_harmonize, accent.name),
            enabled = enabled,
        )
        FamilyColors(context.result, index, context.visibleModes)
        BuilderButton(
            onClick = {
                val slot = AccentSlot(index, AccentPart.Color)
                val target = RampTarget.OfAccent(slot, isDark = context.visibleModes == PreviewMode.Dark)
                dispatcher.dispatch(WorkspaceAction.ShowOnRamp(target))
            },
            label = stringResource(Res.string.accents_show_on_ramp, accent.name),
            emphasis = Emphasis.Subtle,
            enabled = enabled,
        )
        AccentTone.entries.forEach { which -> AccentToneSlider(dispatcher, index, accent, which, enabled) }
        ThresholdChoice(accent, enabled, onSelect = { threshold -> update(accent.copy(threshold = threshold)) })
        BuilderButton(
            onClick = {
                onRemove()
                dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.RemoveAccent(index), EditPhase.Discrete))
            },
            label = stringResource(Res.string.accents_remove, accent.name),
            emphasis = Emphasis.Subtle,
            icon = IconId.Trash,
            enabled = enabled,
        )
    }
}

/**
 * The four colors the extra color at [index] makes, in each mode [modes] shows, with the contrast
 * of each on color over its fill. A theme that has no family there, on a target that takes no
 * extra colors, shows nothing.
 */
@Composable
private fun FamilyColors(
    result: ThemeResult,
    index: Int,
    modes: PreviewMode,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val family = result.accents.families.getOrNull(index) ?: return
    val light = stringResource(Res.string.accents_mode_light)
    val dark = stringResource(Res.string.accents_mode_dark)
    modes.shown.forEach { isDark ->
        val colors = family.mode(isDark)
        val onColor = result.ratio(index, AccentPart.OnColor, AccentPart.Color, isDark)
        val onContainer = result.ratio(index, AccentPart.OnContainer, AccentPart.Container, isDark)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            BuilderText(
                text = stringResource(
                    Res.string.accents_ratios,
                    if (isDark) dark else light,
                    ratioText(onColor),
                    ratioText(onContainer),
                ),
                emphasis = Emphasis.Secondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                ColorSwatch(colors.color)
                ColorSwatch(colors.onColor)
                ColorSwatch(colors.container)
                ColorSwatch(colors.onContainer)
            }
        }
    }
}

/**
 * The modes this preview mode shows, as whether each is dark, light first.
 */
private val PreviewMode.shown: List<Boolean>
    get() = when (this) {
        PreviewMode.Light -> listOf(false)
        PreviewMode.Split -> listOf(false, true)
        PreviewMode.Dark -> listOf(true)
    }

/**
 * The contrast of [foreground] as text over [background] in the extra color at [index], as the audit rates it.
 */
private fun ThemeResult.ratio(
    index: Int,
    foreground: AccentPart,
    background: AccentPart,
    isDark: Boolean,
): Double {
    val pair = ContrastPair(
        foreground = ColorRef.OfAccent(AccentSlot(index, foreground)),
        background = ColorRef.OfAccent(AccentSlot(index, background)),
        kind = PairKind.Text,
    )
    return rate(pair, isDark).ratio
}

/**
 * One of an extra color's four tones, the mode it is for and the slot it cuts.
 */
private enum class AccentTone(
    val isDark: Boolean,
    val container: Boolean,
    val label: StringResource,
) {
    LightColor(isDark = false, container = false, label = Res.string.accents_tone_light_color),
    LightContainer(isDark = false, container = true, label = Res.string.accents_tone_light_container),
    DarkColor(isDark = true, container = false, label = Res.string.accents_tone_dark_color),
    DarkContainer(isDark = true, container = true, label = Res.string.accents_tone_dark_container),
}

/**
 * The tone [which] names on this extra color.
 */
private fun Accent.tone(which: AccentTone): Int {
    val tones = if (which.isDark) dark else light
    return if (which.container) tones.container else tones.color
}

/**
 * This extra color with the tone [which] names moved to [tone] and the other three kept.
 */
private fun Accent.withTone(
    which: AccentTone,
    tone: Int,
): Accent {
    val tones = if (which.isDark) dark else light
    val moved = if (which.container) tones.copy(container = tone) else tones.copy(color = tone)
    return if (which.isDark) copy(dark = moved) else copy(light = moved)
}

/**
 * One tone of the extra color at [index], with the tone it holds beside it.
 */
@Composable
private fun AccentToneSlider(
    dispatcher: Dispatcher<WorkspaceAction>,
    index: Int,
    accent: Accent,
    which: AccentTone,
    enabled: Boolean,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val drag = remember { PendingAccent() }
    val tone = accent.tone(which)
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderSlider(
            value = tone.toFloat(),
            onValueChange = { moved ->
                val next = accent.withTone(which, moved.roundToInt().coerceIn(0, MAX_ACCENT_TONE))
                drag.accent = next
                dispatcher.dispatch(WorkspaceAction.Edit(DocumentChange.UpdateAccent(index, next), EditPhase.Dragging))
            },
            label = stringResource(which.label, accent.name),
            modifier = Modifier.weight(1f),
            onValueChangeFinished = {
                drag.accent?.let { last ->
                    drag.accent = null
                    val change = DocumentChange.UpdateAccent(index, last)
                    dispatcher.dispatch(WorkspaceAction.Edit(change, EditPhase.Released))
                }
            },
            valueRange = AccentToneRange,
            step = ACCENT_TONE_STEP,
            stateDescription = tone.toString(),
            enabled = enabled,
        )
        BuilderText(text = stringResource(Res.string.accents_tone_value, tone), style = BuilderTextStyle.Value)
    }
}

/**
 * The last extra color a drag reported, which its release lands on. Only the release reads it.
 */
private class PendingAccent {
    var accent: Accent? = null
}

/**
 * The contrast the extra color's on colors have to clear. The group is read out with the color's
 * name, so each row's choice is told apart from the others.
 */
@Composable
private fun ThresholdChoice(
    accent: Accent,
    enabled: Boolean,
    onSelect: (OnColorThreshold) -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val label = stringResource(Res.string.accents_threshold)
    val groupLabel = stringResource(Res.string.accents_threshold_for, accent.name)
    val names = Thresholds.associateWith { threshold -> stringResource(thresholdName(threshold)) }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderText(text = label, style = BuilderTextStyle.SectionLabel)
        BuilderSegmented(
            options = Thresholds,
            selected = accent.threshold,
            onSelect = { threshold -> if (threshold != accent.threshold) onSelect(threshold) },
            label = groupLabel,
            enabled = enabled,
            optionLabel = { threshold -> names.getValue(threshold) },
        )
    }
}

/**
 * What [threshold] is called.
 */
private fun thresholdName(threshold: OnColorThreshold): StringResource =
    when (threshold) {
        OnColorThreshold.AaLarge -> Res.string.accents_threshold_aa_large
        OnColorThreshold.AaNormal -> Res.string.accents_threshold_aa
        OnColorThreshold.Aaa -> Res.string.accents_threshold_aaa
    }

/**
 * Where the focus goes when Add brings in a row or a Remove button takes its row with it.
 */
@Stable
private class AccentFocus {
    private val names = mutableMapOf<Int, FocusRequester>()

    /**
     * The Add button.
     */
    val add = FocusRequester()

    /**
     * The place of the color Add has just asked for, until the list grows and its name takes the focus.
     */
    var added: Int? = null

    /**
     * The name field of the row at [index].
     */
    fun name(index: Int): FocusRequester = names.getOrPut(index) { FocusRequester() }
}

/**
 * What a row's name and seed fields say, resolved once for the whole list.
 */
@Immutable
private class AccentRowMessages(
    val names: AccentNameMessages,
    val hex: HexMessages,
)
