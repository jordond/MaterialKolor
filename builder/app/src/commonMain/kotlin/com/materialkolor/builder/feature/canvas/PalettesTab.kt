package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.engine.resolve.AccentFamily
import com.materialkolor.builder.engine.resolve.Ramp
import com.materialkolor.builder.engine.resolve.RampSet
import com.materialkolor.builder.engine.resolve.RampStep
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.poster.readoutName
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.tabs_copied_tone
import com.materialkolor.builder.generated.resources.tabs_mark_dark
import com.materialkolor.builder.generated.resources.tabs_mark_light
import com.materialkolor.builder.generated.resources.tabs_palette_error
import com.materialkolor.builder.generated.resources.tabs_palette_neutral
import com.materialkolor.builder.generated.resources.tabs_palette_neutral_variant
import com.materialkolor.builder.generated.resources.tabs_palette_primary
import com.materialkolor.builder.generated.resources.tabs_palette_secondary
import com.materialkolor.builder.generated.resources.tabs_palette_tertiary
import com.materialkolor.builder.generated.resources.tabs_ramp_shown
import com.materialkolor.builder.generated.resources.tabs_same_in_both
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.RampMark
import com.materialkolor.builder.kit.widget.RampStrip
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Tags the ramp Show on ramp picked out, for tests to find it. */
internal const val PICKED_RAMP_TAG: String = "picked-ramp"

/**
 * The Palettes tab, the tonal palettes the roles of [result] are drawn from and one ramp per
 * accent (F-23).
 *
 * Each ramp's stops copy their hex when pressed, and its markers show the tones the roles or the
 * accent's four colors picked. In Split a ramp that is the same in light and dark, as every ramp is
 * under 2021 and an accent's always is, shows once under Same in light and dark with each marker
 * named by its mode. The rest show per mode in the light and dark columns. Light or Dark shows that
 * mode's ramps alone.
 *
 * The ramp [highlight] points at is scrolled into view and outlined with a label naming what sits
 * on it, as long as it was picked in the project [generation] counts. A target the theme no longer
 * has, such as an accent an undo took away, picks out nothing. Focus moves to the ramp's first stop
 * and the label is read out, once for each highlight, so an edit after it leaves the scroll alone.
 * A ramp that is the same in both modes is picked out whichever mode it was picked in.
 *
 * @param[result] The resolved theme the canvas shows.
 * @param[mode] Which modes to show, laid out like the Roles tab.
 * @param[filter] The vision filter the canvas is drawn through, or null for none.
 * @param[highlight] What Show on ramp last asked for, or null.
 * @param[generation] The open project's generation, which [highlight] has to match.
 * @param[dispatcher] Where a pressed stop sends its copy.
 * @param[modifier] Applied to the tab.
 */
@Composable
internal fun PalettesTab(
    result: ThemeResult,
    mode: PreviewMode,
    filter: ColorMatrix?,
    highlight: RampHighlight?,
    generation: Int,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val layout = remember(result, mode) { rampLayout(result, mode) }
    val target = highlight?.takeIf { shown -> shown.generation == generation }?.target
    val picked = remember(result, target) { target?.let { wanted -> result.pick(wanted) } }
    // b-308ba
    val requesters = remember { PickedRequesters() }
    val announcer = LocalAnnouncer.current
    LaunchedEffect(highlight) {
        val name = picked?.name ?: return@LaunchedEffect
        requesters.view.bringIntoView()
        requesters.focus.requestFocus()
        announcer.announce(getString(Res.string.tabs_ramp_shown, name))
    }
    val shared: (@Composable ColumnScope.() -> Unit)? = if (layout.shared.isEmpty()) {
        null
    } else {
        { SharedRamps(layout.shared, result, picked, requesters, dispatcher) }
    }
    DataColumns(
        mode = mode,
        filter = filter,
        modifier = modifier,
        tabStop = false,
        top = shared,
        columns = layout.light.isNotEmpty() || layout.dark.isNotEmpty(),
    ) { isDark ->
        for (entry in layout.mode(isDark)) RampBlock(entry, entry.title(result), picked, requesters, dispatcher)
    }
}

// b-308ba

/** How the tab reaches the picked ramp, to scroll it into view and to focus its first stop. */
@Stable
private class PickedRequesters {
    val view: BringIntoViewRequester = BringIntoViewRequester()
    val focus: FocusRequester = FocusRequester()
}

/** The ramps both modes share, on one panel under Same in light and dark. */
@Composable
private fun SharedRamps(
    entries: List<RampEntry>,
    result: ThemeResult,
    picked: PickedRamp?,
    requesters: PickedRequesters,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small)) {
        BuilderText(
            text = stringResource(Res.string.tabs_same_in_both),
            modifier = Modifier.semantics { heading() },
            style = BuilderTextStyle.Title,
        )
        DataPanel { for (entry in entries) RampBlock(entry, entry.title(result), picked, requesters, dispatcher) }
    }
}

/** Where a ramp comes from, one of the scheme's six palettes or an accent. */
@Immutable
private sealed interface RampSource {
    data class OfPalette(
        val palette: KeyColor,
    ) : RampSource

    data class OfAccent(
        val index: Int,
    ) : RampSource
}

/**
 * A marker before its name is put into words.
 *
 * @property[name] The role or accent color, in lower camel case.
 * @property[tone] The tone it picked.
 * @property[isDark] The mode it picked it in, or null on a ramp that shows one mode only.
 */
@Immutable
private data class ModeMark(
    val name: String,
    val tone: Double,
    val isDark: Boolean?,
)

/**
 * One ramp as the tab shows it.
 *
 * @property[source] Which palette or accent it is.
 * @property[isDark] The mode it belongs to, or null when it stands for both.
 * @property[steps] Its stops, darkest first.
 * @property[keyTone] The tone of its key color.
 * @property[marks] What picked a tone from it.
 * @property[ramp] The engine's ramp when this is a scheme palette in one mode, drawn with its own
 * markers, or null.
 */
@Immutable
private data class RampEntry(
    val source: RampSource,
    val isDark: Boolean?,
    val steps: List<RampStep>,
    val keyTone: Double,
    val marks: List<ModeMark>,
    val ramp: Ramp? = null,
)

/**
 * The ramps the tab lays out.
 *
 * @property[shared] The ramps shown once for both modes, above the columns.
 * @property[light] The ramps of the light column.
 * @property[dark] The ramps of the dark column.
 */
@Immutable
private class RampLayout(
    val shared: List<RampEntry>,
    val light: List<RampEntry>,
    val dark: List<RampEntry>,
) {
    fun mode(isDark: Boolean): List<RampEntry> = if (isDark) dark else light
}

/**
 * What Show on ramp picked out, found on the theme.
 *
 * @property[source] The ramp it sits on.
 * @property[isDark] The mode it was picked in.
 * @property[name] What sits there, as the Roles tab names it.
 * @property[bothModes] Whether the ramp is the same in light and dark, as an accent's always is and
 * every ramp is under 2021.
 */
@Immutable
private data class PickedRamp(
    val source: RampSource,
    val isDark: Boolean,
    val name: String,
    val bothModes: Boolean, // b-308ba
) {
    /**
     * Whether [entry] is the ramp this picks out. A shared ramp stands for either mode, and so does
     * one that is the same in both, so it still shows in the mode it was not picked in.
     */
    fun matches(entry: RampEntry): Boolean =
        entry.source == source && (bothModes || (entry.isDark ?: isDark) == isDark) // b-308ba
}

/** The ramps of [result] for [mode], shared ones pulled out in Split. */
private fun rampLayout(
    result: ThemeResult,
    mode: PreviewMode,
): RampLayout {
    val split = mode == PreviewMode.Split
    val shared = mutableListOf<RampEntry>()
    val light = mutableListOf<RampEntry>()
    val dark = mutableListOf<RampEntry>()
    for (palette in RampSet.Palettes) {
        val source = RampSource.OfPalette(palette)
        val lightRamp = result.ramps[palette, false]
        val darkRamp = result.ramps[palette, true]
        if (split && lightRamp.steps == darkRamp.steps) {
            val marks = lightRamp.marks(isDark = false) + darkRamp.marks(isDark = true)
            shared += RampEntry(source, null, lightRamp.steps, lightRamp.keyTone, marks)
        } else {
            if (mode != PreviewMode.Dark) light += paletteEntry(source, lightRamp, isDark = false)
            if (mode != PreviewMode.Light) dark += paletteEntry(source, darkRamp, isDark = true)
        }
    }
    result.accents.families.forEachIndexed { index, family ->
        val source = RampSource.OfAccent(index)
        val keyTone = family.palette.keyColor.tone
        when (mode) {
            PreviewMode.Split -> {
                val marks = family.marks(isDark = false, named = true) + family.marks(isDark = true, named = true)
                shared += RampEntry(source, null, family.steps, keyTone, marks)
            }
            PreviewMode.Light -> {
                light += RampEntry(source, false, family.steps, keyTone, family.marks(isDark = false, named = false))
            }
            PreviewMode.Dark -> {
                dark += RampEntry(source, true, family.steps, keyTone, family.marks(isDark = true, named = false))
            }
        }
    }
    return RampLayout(shared, light, dark)
}

private fun paletteEntry(
    source: RampSource,
    ramp: Ramp,
    isDark: Boolean,
): RampEntry = RampEntry(source, isDark, ramp.steps, ramp.keyTone, marks = emptyList(), ramp = ramp)

/** The roles that picked from this ramp, named for the mode [isDark] picks. */
private fun Ramp.marks(isDark: Boolean): List<ModeMark> =
    markers.map { marker -> ModeMark(marker.role.name.lowerFirst(), marker.tone, isDark) }

/** The four colors of the family in the mode [isDark] picks, named for it when [named]. */
private fun AccentFamily.marks(
    isDark: Boolean,
    named: Boolean,
): List<ModeMark> =
    AccentPart.entries.map { part ->
        ModeMark(part.name.lowerFirst(), HctReadout.of(this[part, isDark]).tone, isDark.takeIf { named })
    }

/**
 * Where [target] sits on this theme, or null when the theme has no such color any more. A role is
 * found through the markers of its mode's ramps.
 */
private fun ThemeResult.pick(target: RampTarget): PickedRamp? =
    when (target) {
        is RampTarget.OfRole -> {
            val ramp = ramps.mode(target.isDark).firstOrNull { ramp ->
                ramp.markers.any { marker -> marker.role == target.role }
            }
            ramp?.let { found ->
                PickedRamp(
                    source = RampSource.OfPalette(found.palette),
                    isDark = target.isDark,
                    name = target.role.name.lowerFirst(),
                    bothModes = sameInBothModes(found.palette),
                )
            }
        }
        is RampTarget.OfKeyColor -> {
            PickedRamp(
                source = RampSource.OfPalette(target.palette),
                isDark = target.isDark,
                name = target.palette.swatchName,
                bothModes = sameInBothModes(target.palette),
            )
        }
        is RampTarget.OfAccent -> {
            accents.families.getOrNull(target.slot.index)?.let {
                PickedRamp(
                    source = RampSource.OfAccent(target.slot.index),
                    isDark = target.isDark,
                    name = ColorRef.OfAccent(target.slot).readoutName(document),
                    bothModes = true,
                )
            }
        }
    }

// b-308ba

/** Whether the ramp of [palette] has the same stops in light and dark, as every ramp has under 2021. */
private fun ThemeResult.sameInBothModes(palette: KeyColor): Boolean =
    ramps[palette, false].steps == ramps[palette, true].steps

/**
 * One ramp under its [title]. The picked ramp wears an outline and a label saying what sits on it,
 * and takes [requesters], so the tab can scroll to it and focus its first stop.
 *
 * It takes nothing of the theme but its own ramp, so a change that leaves the ramp alone, such as
 * another accent or a custom tone, does not compose it again.
 */
@Composable
private fun RampBlock(
    entry: RampEntry,
    title: String, // pf-1
    picked: PickedRamp?,
    requesters: PickedRequesters,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    LocalTileProbe.current?.invoke(title) // pf-1
    val tokens = LocalBuilderTokens.current
    val shown = picked?.takeIf { pick -> pick.matches(entry) }
    val labels = entry.steps.associate { step ->
        step.tone to stringResource(Res.string.tabs_copied_tone, title, step.tone)
    }
    val onCopyTone = { step: RampStep ->
        dispatcher.dispatch(WorkspaceAction.CopyText(step.argb.toHex(), labels.getValue(step.tone)))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (shown != null) Modifier.bringIntoViewRequester(requesters.view) else Modifier) // b-308ba
            .then(if (shown != null) Modifier.testTag(PICKED_RAMP_TAG) else Modifier)
            .border(
                width = tokens.highlightWidth, // b-308ba
                color = if (shown != null) tokens.accent else Color.Transparent,
                shape = RoundedCornerShape(tokens.radius.small),
            ).padding(tokens.spacing.small),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        BuilderText(title, Modifier.semantics { heading() }, style = BuilderTextStyle.SectionLabel)
        if (shown != null) BuilderText(stringResource(Res.string.tabs_ramp_shown, shown.name))
        val ramp = entry.ramp
        // b-308ba
        val strip = if (shown != null) Modifier.focusRequester(requesters.focus) else Modifier
        if (ramp != null) {
            RampStrip(ramp, onCopyTone, strip)
        } else {
            RampStrip(entry.steps, entry.marks.map { mark -> mark.inWords() }, entry.keyTone, onCopyTone, strip)
        }
    }
}

/** The marker as the strip labels it, with its mode when it has one. */
@Composable
private fun ModeMark.inWords(): RampMark {
    val label = when (isDark) {
        null -> name
        false -> stringResource(Res.string.tabs_mark_light, name)
        true -> stringResource(Res.string.tabs_mark_dark, name)
    }
    return RampMark(label, tone)
}

/** What the ramp is called, the palette's name or the accent's own. */
@Composable
private fun RampEntry.title(result: ThemeResult): String =
    when (source) {
        is RampSource.OfPalette -> {
            stringResource(source.palette.title)
        }
        is RampSource.OfAccent -> {
            val family = result.accents.families[source.index]
            family.accent.name
        }
    }

/** What the Palettes tab calls a palette. */
private val KeyColor.title: StringResource
    get() = when (this) {
        KeyColor.Primary -> Res.string.tabs_palette_primary
        KeyColor.Secondary -> Res.string.tabs_palette_secondary
        KeyColor.Tertiary -> Res.string.tabs_palette_tertiary
        KeyColor.Neutral -> Res.string.tabs_palette_neutral
        KeyColor.NeutralVariant -> Res.string.tabs_palette_neutral_variant
        KeyColor.Error -> Res.string.tabs_palette_error
    }

private fun String.lowerFirst(): String = replaceFirstChar { char -> char.lowercaseChar() }
