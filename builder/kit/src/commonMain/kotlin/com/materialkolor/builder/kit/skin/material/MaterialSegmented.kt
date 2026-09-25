package com.materialkolor.builder.kit.skin.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.composeunstyled.collectIsFocusVisibleAsState
import com.materialkolor.builder.kit.a11y.LocalWebKeyboard
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.ControlState
import com.materialkolor.builder.kit.control.FittedLabel
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.HeadlessSegmented
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.headless.RadioGroupFocus
import com.materialkolor.builder.kit.headless.radioGroupOption
import com.materialkolor.builder.kit.headless.rememberRadioGroupFocus
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.ActionColors
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.FocusRingWidth
import com.materialkolor.builder.kit.skin.headless.SegmentedStyle
import com.materialkolor.builder.kit.skin.headless.SelectableStyle
import com.materialkolor.builder.kit.skin.headless.controlPress
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.skin.headless.controlTouchTarget
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * Material's single choice segmented row, with the radio group's roving focus and arrow keys laid
 * over it, since Material's row moves neither. The expressive flavour draws a row of connected toggle
 * buttons instead ([ExpressiveSegmented]).
 *
 * Material raises the chosen button over its neighbours, and the web mirror reads the page in that
 * order, so the chosen option would come last. On the web each button sits in a box of its own,
 * which keeps the options in the order they are written (S5 row 8).
 */
@Composable
internal fun <T> MaterialSegmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    optionIcon: (T) -> IconId?,
    selectOnFocus: Boolean,
    optionLabel: (T) -> String,
    compact: Boolean = false, // b-510
) {
    val selectedIndex = options.indexOf(selected)
    val focus = rememberRadioGroupFocus(options.size, selectedIndex)
    if (LocalSkin.current.expressive) {
        ExpressiveSegmented(
            options = options,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            label = label,
            modifier = modifier,
            enabled = enabled,
            optionIcon = optionIcon,
            selectOnFocus = selectOnFocus,
            optionLabel = optionLabel,
            focus = focus,
            compact = compact,
        )
        return
    }
    // b-510
    if (compact) {
        HeadlessSegmented(
            options = options,
            selected = selected,
            onSelect = onSelect,
            label = label,
            style = materialTrackStyle(),
            modifier = modifier,
            enabled = enabled,
            optionIcon = optionIcon,
            selectOnFocus = selectOnFocus,
            optionLabel = optionLabel,
            compact = true,
        )
        return
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val folds = LocalFoldsStateIntoName.current
    val writingOrder = LocalWebKeyboard.current
    MaterialTarget {
        SingleChoiceSegmentedButtonRow(modifier.semantics { roleLessName(label, folds) }) {
            options.forEachIndexed { index, value ->
                key(index) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    val glyph = optionIcon(value)
                    val isSelected = index == selectedIndex
                    InWritingOrder(keep = writingOrder) {
                        SegmentedButton(
                            selected = isSelected,
                            onClick = { onSelect(value) },
                            shape = shape,
                            modifier = Modifier
                                .radioGroupOption(focus, index, selectedIndex, rtl, selectOnFocus) { target ->
                                    onSelect(options[target])
                                }.foldState(
                                    name = optionLabel(value),
                                    state = ControlState.Selected(isSelected),
                                    enabled = enabled,
                                    role = FoldedRole.Radio,
                                ).materialFeedback(interactionSource, shape),
                            enabled = enabled,
                            interactionSource = interactionSource,
                            icon = {
                                SegmentedButtonDefaults.Icon(
                                    active = isSelected,
                                    activeContent = {
                                        BuilderIcon(
                                            IconId.Check,
                                            contentDescription = null,
                                            tint = LocalContentColor.current,
                                        )
                                    },
                                    inactiveContent = glyph?.let { id ->
                                        { BuilderIcon(id, contentDescription = null, tint = LocalContentColor.current) }
                                    },
                                )
                            },
                        ) {
                            BuilderText(
                                optionLabel(value),
                                style = BuilderTextStyle.Label,
                                color = LocalContentColor.current,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The expressive row, Material's connected toggle buttons laid out by hand. Material's own button
 * group folds whatever does not fit into an overflow menu, a popup the web may not open (D40), and
 * nothing turns that off, so the row takes only the group's connected shapes.
 *
 * Each option still reads as a radio button with the radio group's roving focus over it, where the
 * toggle button on its own would read as a checkbox. The chosen option wears the check as well as
 * the fill and the rounder shape, so the choice never rests on colour alone. The row draws the
 * focused option's ring over every option ([RowRing]).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class) // b-510
@Composable
private fun <T> ExpressiveSegmented(
    options: List<T>,
    selectedIndex: Int,
    onSelect: (T) -> Unit,
    label: String,
    modifier: Modifier,
    enabled: Boolean,
    optionIcon: (T) -> IconId?,
    selectOnFocus: Boolean,
    optionLabel: (T) -> String,
    focus: RadioGroupFocus,
    compact: Boolean = false, // b-510
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val folds = LocalFoldsStateIntoName.current
    val writingOrder = LocalWebKeyboard.current
    val ring = remember { RowRing() }
    val ringColor = LocalBuilderTokens.current.focus
    val touchTarget = LocalLayout.current.primaryTouchTarget
    MaterialTarget {
        Row(
            modifier = modifier
                .semantics { roleLessName(label, folds) }
                .selectableGroup()
                .width(IntrinsicSize.Min)
                .onPlaced { coordinates -> ring.row = coordinates }
                .drawWithContent {
                    drawContent()
                    with(ring) { drawRing(ringColor) }
                },
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, value ->
                key(index) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val shapes = connectedShapes(index, options.size)
                    val isSelected = index == selectedIndex
                    val name = optionLabel(value)
                    val ringed = ring.rememberOption(
                        index = index,
                        shows = interactionSource.collectIsFocusVisibleAsState(),
                        shape = rememberUpdatedState(if (isSelected) shapes.checkedShape else shapes.shape),
                    )
                    InWritingOrder(keep = writingOrder) {
                        ToggleButton(
                            checked = isSelected,
                            onCheckedChange = { onSelect(value) },
                            modifier = Modifier
                                .weight(1f)
                                .radioGroupOption(focus, index, selectedIndex, rtl, selectOnFocus) { target ->
                                    onSelect(options[target])
                                }.semantics {
                                    role = Role.RadioButton
                                    selected = isSelected
                                }.foldState(
                                    name = name,
                                    state = ControlState.Selected(isSelected),
                                    enabled = enabled,
                                    role = FoldedRole.Radio,
                                ).controlTouchTarget(touchTarget)
                                .onGloballyPositioned { coordinates -> ringed.place(ring.row, coordinates) }
                                .controlPress(interactionSource),
                            enabled = enabled,
                            shapes = shapes,
                            // b-510
                            contentPadding = if (compact) {
                                CompactPadding
                            } else {
                                ButtonDefaults.contentPaddingFor(
                                    ButtonDefaults.MinHeight,
                                )
                            },
                            interactionSource = interactionSource,
                        ) {
                            if (compact) {
                                FittedLabel(name, LocalContentColor.current) // b-510
                            } else {
                                ExpressiveLabel(name, if (isSelected) IconId.Check else optionIcon(value))
                            }
                        }
                    }
                }
            }
        }
    }
}

// b-510

/**
 * An expressive option's glyph, if any, and its label.
 */
@Composable
private fun ExpressiveLabel(
    name: String,
    glyph: IconId?,
) {
    if (glyph != null) {
        BuilderIcon(glyph, contentDescription = null, tint = LocalContentColor.current)
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    }
    // A label is as wide as its whole line even where the row asks for its least.
    BuilderText(
        name,
        modifier = Modifier.width(IntrinsicSize.Max),
        style = BuilderTextStyle.Label,
        color = LocalContentColor.current,
        maxLines = 1,
    )
}

/**
 * The little room round a compact row's label.
 */
private val CompactPadding: PaddingValues = PaddingValues(horizontal = 4.dp)

/**
 * Material's compact row as a track, the design's look for the poster's contrast levels. The track
 * takes the raised fill and the chosen option the accent pill, with nothing round the others.
 */
@Composable
private fun materialTrackStyle(): SegmentedStyle {
    val tokens = LocalBuilderTokens.current
    val pill = RoundedCornerShape(percent = 50)
    val none = Color.Transparent
    return SegmentedStyle(
        shape = pill,
        inset = tokens.spacing.extraSmall,
        borderWidth = 0.dp,
        colors = ActionColors(tokens.panelRaised, tokens.textStrong, none),
        option = SelectableStyle(
            shape = pill,
            height = ButtonDefaults.MinHeight - tokens.spacing.extraSmall * 2,
            horizontalPadding = tokens.spacing.extraSmall,
            gap = tokens.spacing.small,
            borderWidth = 0.dp,
            off = ActionColors(none, tokens.textStrong, none),
            on = ActionColors(tokens.accent, tokens.onAccent, none),
        ),
        endRingOffset = tokens.spacing.extraSmall,
    )
}

/**
 * The connected shapes for the option at [index] of [count], by where it sits in the row.
 */
@Composable
private fun connectedShapes(
    index: Int,
    count: Int,
): ToggleButtonShapes {
    val full = ButtonGroupDefaults.connectedButtonCheckedShape
    return when {
        count == 1 -> ToggleButtonShapes(shape = full, pressedShape = full, checkedShape = full)
        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
        index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
    }
}

/**
 * The focus ring of the expressive row, drawn by the row after every option.
 *
 * A ring stands off its option and reaches past the gap into the neighbours, and a neighbour drawn
 * after it covers that stretch. Raising the focused option over them does not help, because siblings
 * are read in the order they are drawn, so the web would read the focused option last (S5 row 8).
 * Drawing the ring from the row keeps every option where it is written.
 */
@Stable
private class RowRing {
    /**
     * Where the row sits, which each option's ring is measured from.
     */
    var row: LayoutCoordinates? = null

    private val options: SnapshotStateMap<Int, RingedOption> = mutableStateMapOf()

    /**
     * Keeps the option at [index] on the row's list while it is shown.
     */
    @Composable
    fun rememberOption(
        index: Int,
        shows: State<Boolean>,
        shape: State<Shape>,
    ): RingedOption {
        val option = remember(shows, shape) { RingedOption(shows, shape) }
        DisposableEffect(index, option) {
            options[index] = option
            onDispose { if (options[index] === option) options.remove(index) }
        }
        return option
    }

    /**
     * Draws the ring round whichever option shows one, the way [controlRing] draws it on its own.
     */
    fun DrawScope.drawRing(color: Color) {
        val option = options.values.firstOrNull { option -> option.shows.value } ?: return
        val bounds = option.bounds ?: return
        val outline = option.shape.value.createOutline(bounds.size, layoutDirection, this)
        val offset = FocusRingOffset.toPx()
        val width = FocusRingWidth.toPx()
        val path = Path().apply { fillType = PathFillType.EvenOdd }
        when (outline) {
            is Outline.Rectangle -> {
                path.addRect(outline.rect.inflate(offset + width))
                path.addRect(outline.rect.inflate(offset))
            }
            is Outline.Rounded -> {
                path.addRoundRect(outline.roundRect.grow(offset + width))
                path.addRoundRect(outline.roundRect.grow(offset))
            }
            // Material's connected shapes are all rounded, and the kit's own ring skips a free path too.
            is Outline.Generic -> {
                return
            }
        }
        translate(bounds.left, bounds.top) { drawPath(path, color) }
    }
}

/**
 * One option of a [RowRing], whether its ring [shows] and the [shape] it goes round.
 */
private class RingedOption(
    val shows: State<Boolean>,
    val shape: State<Shape>,
) {
    /**
     * Where the option sits in the row, or null before it is placed.
     */
    var bounds: Rect? by mutableStateOf(null)
        private set

    /**
     * Measures the option at [coordinates] from the [row] it sits in.
     */
    fun place(
        row: LayoutCoordinates?,
        coordinates: LayoutCoordinates,
    ) {
        if (row == null || !row.isAttached) return
        bounds = row.localBoundingBoxOf(coordinates, clipBounds = false)
    }
}

/**
 * This round rectangle grown by [amount] on every side, each corner as much rounder.
 */
private fun RoundRect.grow(amount: Float): RoundRect =
    RoundRect(
        left = left - amount,
        top = top - amount,
        right = right + amount,
        bottom = bottom + amount,
        topLeftCornerRadius = topLeftCornerRadius.grow(amount),
        topRightCornerRadius = topRightCornerRadius.grow(amount),
        bottomRightCornerRadius = bottomRightCornerRadius.grow(amount),
        bottomLeftCornerRadius = bottomLeftCornerRadius.grow(amount),
    )

private fun CornerRadius.grow(amount: Float): CornerRadius =
    CornerRadius((x + amount).coerceAtLeast(0f), (y + amount).coerceAtLeast(0f))

/**
 * Where [keep] is set, holds one button in a box of its own that takes the button's share of the
 * row. Material places the chosen button above the others, and siblings are read in that order, so
 * a box per button leaves only one child to each and the boxes in the order they are written.
 */
@Composable
private fun <S : RowScope> S.InWritingOrder(
    keep: Boolean,
    button: @Composable S.() -> Unit,
) {
    if (!keep) {
        button()
        return
    }
    Box(Modifier.weight(1f), propagateMinConstraints = true) { this@InWritingOrder.button() }
}
