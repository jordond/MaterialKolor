package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.canvas_mode_dark
import com.materialkolor.builder.generated.resources.canvas_mode_light
import com.materialkolor.builder.generated.resources.tabs_key_color
import com.materialkolor.builder.generated.resources.tabs_tone_tag
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * A marker on a ramp, what picked a tone there and in which mode.
 *
 * @property[name] The role or accent color, in lower camel case.
 * @property[tone] The tone it picked.
 * @property[isDark] The mode it picked it in, or null on a ramp that shows one mode only.
 */
@Immutable
internal data class ModeMark(
    val name: String,
    val tone: Double,
    val isDark: Boolean?,
)

/**
 * What landed on one tone of a ramp.
 *
 * @property[tone] The tone, rounded as the stops are.
 * @property[names] The roles or accent colors there, in the order the ramp marks them.
 */
@Immutable
private data class ToneTag(
    val tone: Int,
    val names: List<String>,
)

/**
 * These marks gathered by rounded tone, from the darkest up, as the ramp runs.
 */
private fun List<ModeMark>.toneTags(): List<ToneTag> =
    groupBy { mark -> mark.tone.roundToInt() }
        .entries
        .sortedBy { (tone, _) -> tone }
        .map { (tone, marks) -> ToneTag(tone, marks.map { mark -> mark.name }) }

/**
 * The ramp's heading with the tag of its key color beside it.
 */
@Composable
internal fun RampTitle(
    title: String,
    keyTone: Double,
    lit: Int?,
    onLit: (Int?) -> Unit,
) {
    val tone = keyTone.roundToInt()
    Row(
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(
            text = title,
            modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            style = BuilderTextStyle.GroupLabel,
        )
        TagChip(
            tag = ToneTag(tone, listOf(stringResource(Res.string.tabs_key_color))),
            lit = lit == tone,
            onLit = onLit,
            emphasis = Emphasis.Primary,
        )
    }
}

/**
 * The tags under a ramp, one row of them for a ramp that shows one mode, and a light row over a
 * dark one, each led by its mode's icon and named for it, for a ramp both modes share.
 */
@Composable
internal fun ModeTags(
    marks: List<ModeMark>,
    lit: Int?,
    onLit: (Int?) -> Unit,
) {
    if (marks.isEmpty()) return
    if (marks.all { mark -> mark.isDark == null }) {
        TagRow(marks.toneTags(), lit, onLit)
        return
    }
    val spacing = LocalBuilderTokens.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        for (isDark in listOf(false, true)) {
            val mode = stringResource(if (isDark) Res.string.canvas_mode_dark else Res.string.canvas_mode_light)
            Row(
                modifier = Modifier.semantics { contentDescription = mode },
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                BuilderIcon(
                    id = if (isDark) IconId.Moon else IconId.Sun,
                    contentDescription = null,
                    modifier = Modifier.padding(top = spacing.extraSmall),
                    emphasis = Emphasis.Secondary,
                )
                TagRow(marks.filter { mark -> mark.isDark == isDark }.toneTags(), lit, onLit, Modifier.weight(1f))
            }
        }
    }
}

/**
 * [tags] in a row that wraps onto as many lines as the card needs.
 */
@Composable
private fun TagRow(
    tags: List<ToneTag>,
    lit: Int?,
    onLit: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        for (tag in tags) TagChip(tag, lit = lit == tag.tone, onLit = onLit)
    }
}

/**
 * One tag, as in "40 · primary, surfaceTint", outlined in the accent while [lit]. The pointer
 * resting on it lights its tone through [onLit].
 */
@Composable
private fun TagChip(
    tag: ToneTag,
    lit: Boolean,
    onLit: (Int?) -> Unit,
    emphasis: Emphasis = Emphasis.Secondary,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small)
    BuilderText(
        text = stringResource(Res.string.tabs_tone_tag, tag.tone, tag.names.joinToString(", ")),
        modifier = Modifier
            .pointerInput(tag.tone, onLit) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> onLit(tag.tone)
                            PointerEventType.Exit -> onLit(null)
                            else -> Unit
                        }
                    }
                }
            }.border(
                width = if (lit) tokens.highlightWidth else tokens.outlineWidth,
                color = if (lit) tokens.accent else tokens.border,
                shape = shape,
            ).padding(horizontal = tokens.spacing.small, vertical = tokens.spacing.extraSmall),
        style = BuilderTextStyle.Value,
        emphasis = if (lit) Emphasis.Primary else emphasis,
    )
}
