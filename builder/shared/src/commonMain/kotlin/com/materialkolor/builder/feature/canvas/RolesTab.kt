package com.materialkolor.builder.feature.canvas

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.audit.ContrastPair
import com.materialkolor.builder.domain.audit.PairKind
import com.materialkolor.builder.domain.capability.Control
import com.materialkolor.builder.domain.model.AccentPart
import com.materialkolor.builder.domain.model.AccentSlot
import com.materialkolor.builder.domain.model.KeyColor
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RoleGroup
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.audit.rate
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.RampSet
import com.materialkolor.builder.engine.resolve.ThemeResult
import com.materialkolor.builder.feature.poster.readoutName
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.capabilitiesOf
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.tabs_group_accent
import com.materialkolor.builder.generated.resources.tabs_group_accents
import com.materialkolor.builder.generated.resources.tabs_group_fixed
import com.materialkolor.builder.generated.resources.tabs_group_key_colors
import com.materialkolor.builder.generated.resources.tabs_group_outline_inverse
import com.materialkolor.builder.generated.resources.tabs_group_surface
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The narrowest a swatch gets before its grid drops a column. Wide enough for a role name such as
 * onSecondaryContainer and its hex and tone readout on one line at the default text size.
 */
private val SwatchMinWidth: Dp = 152.dp

/**
 * The tone at and above which a swatch with no on-pair is inked black rather than white.
 */
private const val BLACK_INK_TONE = 50.0

/**
 * The Roles tab, every color role of [result] with the tone and contrast it resolved to.
 *
 * Each mode lists the Accent, Surface, Fixed and Outline and inverse roles, then the key colors
 * and the accents, the same groups for every target. In Split it shows light and dark as two
 * labeled columns, stacked on phones, and in Light or Dark that mode alone. The arrow keys move
 * focus around a group's grid, and Tab walks the swatches in order.
 *
 * @param[result] The resolved theme the canvas shows.
 * @param[mode] Which modes to show.
 * @param[filter] The vision filter the canvas is drawn through, or null for none.
 * @param[dispatcher] Where the swatch menus send what they are asked to do.
 * @param[modifier] Applied to the tab.
 */
@Composable
internal fun RolesTab(
    result: ThemeResult,
    mode: PreviewMode,
    filter: ColorMatrix?,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val pins = remember(result) { capabilitiesOf(result.document)[Control.RolePins] }
    DataColumns(mode, filter, modifier, tabStop = false) { isDark ->
        val groups = remember(result, isDark) { swatchGroups(result, isDark) }
        for (group in groups) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                BuilderText(
                    text = stringResource(group.title),
                    modifier = Modifier.semantics { heading() },
                    style = BuilderTextStyle.SectionLabel,
                )
                SwatchGrid {
                    for (swatch in group.swatches) {
                        key(swatch.target) { RolePopover(swatch, pins, dispatcher) }
                    }
                }
            }
        }
    }
}

/**
 * One heading of the Roles tab and its swatches.
 *
 * @property[title] The heading.
 * @property[swatches] The swatches under it, in order.
 */
@Immutable
private class SwatchGroup(
    val title: StringResource,
    val swatches: List<RoleSwatch>,
)

/**
 * The groups one mode of [result] lists, the accents only when the document has any.
 */
private fun swatchGroups(
    result: ThemeResult,
    isDark: Boolean,
): List<SwatchGroup> {
    val roleGroups = RoleGroup.entries.map { group ->
        SwatchGroup(
            title = group.title,
            swatches = Role.entries
                .filter { role -> role.group == group }
                .map { role -> roleSwatch(result, role, isDark) },
        )
    }
    val keyColors = SwatchGroup(
        title = Res.string.tabs_group_key_colors,
        swatches = RampSet.Palettes.map { palette -> keyColorSwatch(result, palette, isDark) },
    )
    val accents = result.accents.families.indices.flatMap { index ->
        AccentPart.entries.map { part -> accentSwatch(result, AccentSlot(index, part), isDark) }
    }
    return buildList {
        addAll(roleGroups)
        add(keyColors)
        if (accents.isNotEmpty()) add(SwatchGroup(Res.string.tabs_group_accents, accents))
    }
}

/**
 * A role, inked with its on-pair and rated against it. An on role is inked with the role it sits
 * on and rated against it too, so both cards of a pair carry the same readout. A role with neither
 * is inked black or white by its tone and has no ratio.
 */
private fun roleSwatch(
    result: ThemeResult,
    role: Role,
    isDark: Boolean,
): RoleSwatch {
    val entry = result.roles[role, isDark]
    val onPair = role.onPair
    val under = Role.entries.firstOrNull { other -> other.onPair == role }
    val ink = when {
        onPair != null -> result.roles[onPair, isDark].argb.toColor()
        under != null -> result.roles[under, isDark].argb.toColor()
        else -> inkFor(entry.tone)
    }
    val pin = result.document.pins[role]
    return RoleSwatch(
        name = ColorRef.OfRole(role).readoutName(result.document),
        argb = entry.argb,
        ink = ink,
        tone = entry.tone,
        contrast = when {
            onPair != null -> result.ratio(ColorRef.OfRole(onPair), ColorRef.OfRole(role), isDark)
            under != null -> result.ratio(ColorRef.OfRole(role), ColorRef.OfRole(under), isDark)
            else -> null
        },
        target = RampTarget.OfRole(role, isDark),
        pinned = (if (isDark) pin?.dark else pin?.light) != null,
    )
}

/**
 * The key color [palette] is built around. It has no on-pair, so it is inked by its tone.
 */
private fun keyColorSwatch(
    result: ThemeResult,
    palette: KeyColor,
    isDark: Boolean,
): RoleSwatch {
    val argb = result.ramps[palette, isDark].keyColor
    val tone = HctReadout.of(argb).tone
    return RoleSwatch(
        name = palette.swatchName,
        argb = argb,
        ink = inkFor(tone),
        tone = tone,
        contrast = null,
        target = RampTarget.OfKeyColor(palette, isDark),
    )
}

/**
 * One color of an accent, inked with its partner and rated against it, the on color over the fill.
 */
private fun accentSwatch(
    result: ThemeResult,
    slot: AccentSlot,
    isDark: Boolean,
): RoleSwatch {
    val family = result.accents.families[slot.index]
    val argb = family[slot.part, isDark]
    val partner = AccentSlot(slot.index, slot.part.partner)
    val onFill = slot.part == AccentPart.Color || slot.part == AccentPart.Container
    return RoleSwatch(
        name = ColorRef.OfAccent(slot).readoutName(result.document),
        argb = argb,
        ink = family[partner.part, isDark].toColor(),
        tone = HctReadout.of(argb).tone,
        contrast = if (onFill) {
            result.ratio(ColorRef.OfAccent(partner), ColorRef.OfAccent(slot), isDark)
        } else {
            result.ratio(ColorRef.OfAccent(slot), ColorRef.OfAccent(partner), isDark)
        },
        target = RampTarget.OfAccent(slot, isDark),
    )
}

/**
 * The ratio of [foreground] as text over [background], rated the way the audit rates it.
 */
private fun ThemeResult.ratio(
    foreground: ColorRef,
    background: ColorRef,
    isDark: Boolean,
): Double = rate(ContrastPair(foreground, background, PairKind.Text), isDark).ratio

/**
 * What the key color of this palette is called in code, as in primaryPaletteKeyColor.
 */
internal val KeyColor.swatchName: String
    get() = name.replaceFirstChar { char -> char.lowercaseChar() } + "PaletteKeyColor"

/**
 * Black on a light color, white on a dark one.
 */
private fun inkFor(tone: Double): Color = if (tone >= BLACK_INK_TONE) Color.Black else Color.White

/**
 * The part an accent part is paired with, the fill for an on color and the on color for a fill.
 */
private val AccentPart.partner: AccentPart
    get() = when (this) {
        AccentPart.Color -> AccentPart.OnColor
        AccentPart.OnColor -> AccentPart.Color
        AccentPart.Container -> AccentPart.OnContainer
        AccentPart.OnContainer -> AccentPart.Container
    }

/**
 * What the Roles tab calls a role group.
 */
private val RoleGroup.title: StringResource
    get() = when (this) {
        RoleGroup.Accent -> Res.string.tabs_group_accent
        RoleGroup.Surface -> Res.string.tabs_group_surface
        RoleGroup.Fixed -> Res.string.tabs_group_fixed
        RoleGroup.OutlineInverse -> Res.string.tabs_group_outline_inverse
    }

/**
 * Swatches in as many equal columns as fit at [SwatchMinWidth], row by row.
 *
 * The arrow keys move focus to the nearest swatch that way and stop at the grid's edge. Tab and
 * Shift Tab still leave it.
 */
@Composable
private fun SwatchGrid(content: @Composable () -> Unit) {
    val focusManager = LocalFocusManager.current
    val gap = LocalBuilderTokens.current.spacing.small
    Layout(
        content = content,
        modifier = Modifier
            .fillMaxWidth()
            .focusProperties { onExit = { if (requestedFocusDirection.isArrow) cancelFocusChange() } }
            .focusGroup()
            .onKeyEvent { event ->
                val direction = event.arrowDirection()
                if (direction == null || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                focusManager.moveFocus(direction)
                true
            },
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val minCell = SwatchMinWidth.roundToPx()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else minCell * measurables.size
        val columns = ((width + gapPx) / (minCell + gapPx)).coerceAtLeast(1)
        val cell = ((width - gapPx * (columns - 1)) / columns).coerceAtLeast(0)
        val rows = measurables.map { measurable -> measurable.measure(Constraints.fixedWidth(cell)) }.chunked(columns)
        val heights = rows.map { row -> row.maxOf { placeable -> placeable.height } }
        val height = heights.sum() + gapPx * (rows.size - 1).coerceAtLeast(0)
        layout(width, height) {
            var y = 0
            rows.forEachIndexed { index, row ->
                row.forEachIndexed { column, placeable -> placeable.placeRelative(column * (cell + gapPx), y) }
                y += heights[index] + gapPx
            }
        }
    }
}

/**
 * The focus direction an arrow key asks for, or null for any other key.
 */
private fun KeyEvent.arrowDirection(): FocusDirection? =
    when (key) {
        Key.DirectionLeft -> FocusDirection.Left
        Key.DirectionRight -> FocusDirection.Right
        Key.DirectionUp -> FocusDirection.Up
        Key.DirectionDown -> FocusDirection.Down
        else -> null
    }

private val FocusDirection.isArrow: Boolean
    get() = this == FocusDirection.Left ||
        this == FocusDirection.Right ||
        this == FocusDirection.Up ||
        this == FocusDirection.Down
