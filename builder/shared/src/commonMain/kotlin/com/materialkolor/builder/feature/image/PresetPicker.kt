package com.materialkolor.builder.feature.image

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.feature.poster.PosterContext
import com.materialkolor.builder.feature.poster.PosterIconButton
import com.materialkolor.builder.feature.poster.rememberThemeResolver
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.image_menu_presets
import com.materialkolor.builder.generated.resources.image_menu_upload
import com.materialkolor.builder.generated.resources.image_presets_images
import com.materialkolor.builder.generated.resources.image_presets_images_line
import com.materialkolor.builder.generated.resources.image_presets_line
import com.materialkolor.builder.generated.resources.image_presets_starters
import com.materialkolor.builder.generated.resources.image_presets_starters_line
import com.materialkolor.builder.generated.resources.image_presets_title
import com.materialkolor.builder.generated.resources.poster_image
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSheet
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.SheetPresentation
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * The widest the picker's dialog grows, room for five pictures and four starters a row.
 */
private val PickerMaxWidth: Dp = 760.dp

/**
 * The body width under which the starters drop to two a row and each group's line goes under its
 * heading.
 */
private val WideBody: Dp = 600.dp

/**
 * The Image button, which opens a menu of the two ways to seed from a picture.
 *
 * Upload image opens the platform picker. Browsers only open it inside the click, so the row's click
 * dispatches `OpenImagePicker` and the workspace starts the pick before the click returns.
 * Presets and starters opens [PresetPicker] as `Panel.Presets`, so Back closes it, and a preset or
 * starter chosen there lands behind a crossfade as one undo entry. The picker hands the focus back
 * to this button once it closes.
 *
 * @param[glyphOnly] Whether the button draws its glyph alone, where the row is short of room. It
 * reads out the same.
 */
@Composable
internal fun ImageMenuButton(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
    glyphOnly: Boolean = false,
) {
    var menu by remember { mutableStateOf(false) }
    val close = { dispatcher.dispatch(WorkspaceAction.ClosePanel) }
    val button = remember { FocusRequester() }
    val items = listOf(
        BuilderMenuItem(
            label = stringResource(Res.string.image_menu_upload),
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenImagePicker) },
            icon = IconId.Upload,
        ),
        BuilderMenuItem(
            label = stringResource(Res.string.image_menu_presets),
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Presets)) },
            icon = IconId.Image,
        ),
    )
    BuilderMenu(expanded = menu, onDismissRequest = { menu = false }, items = items, modifier = modifier) {
        if (glyphOnly) {
            PosterIconButton(
                icon = IconId.Image,
                description = stringResource(Res.string.poster_image),
                onClick = { menu = true },
                emphasis = Emphasis.Secondary,
                buttonModifier = Modifier.focusRequester(button),
            )
        } else {
            BuilderButton(
                onClick = { menu = true },
                label = stringResource(Res.string.poster_image),
                modifier = Modifier.focusRequester(button),
                icon = IconId.Image,
            )
        }
    }
    PresetPicker(
        visible = context.openPanel == Panel.Presets,
        document = context.result.document,
        isDark = context.visibleModes == PreviewMode.Dark,
        onChoose = { preset ->
            close()
            // The picker is closing over wherever the card sat, so the theme crossfades in place.
            dispatcher.dispatch(WorkspaceAction.EditWithReveal(preset.change(context.document), origin = null))
        },
        onDismissRequest = close,
        returnFocusTo = button,
    )
}

/**
 * The preset pictures and the starter themes, each a card that chooses it.
 *
 * A picture sets only the seed, to its strongest color, and the row under the seed actions then
 * offers its other colors. A starter sets the seed, the style and the contrast. Neither touches the
 * target, the overrides, the pins or the accents. Each starter's card shows the scheme it makes of
 * [document], resolved one per frame once the picker opens, and the card [document] still stands on
 * reads as selected.
 *
 * It is a dialog closed from the button beside its title, and on a phone a sheet over the whole
 * screen. The cards are the only Tab stops in its body.
 *
 * @param[visible] Whether the picker is open, while the workspace's panel is `Panel.Presets`.
 * @param[document] The theme a starter's scheme is drawn from, with the starter's seed, style and
 * contrast in place of its own. It also says which card is current.
 * @param[isDark] Whether the starters' schemes are drawn dark.
 * @param[onChoose] Called with the preset a card chooses. The picker stays open until told otherwise.
 * @param[returnFocusTo] The button that opened the picker, which gets the focus back once it closes.
 */
@Composable
internal fun PresetPicker(
    visible: Boolean,
    document: ThemeDocument,
    isDark: Boolean,
    onChoose: (Preset) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    val title = stringResource(Res.string.image_presets_title)
    val line = stringResource(Res.string.image_presets_line)
    if (LocalLayout.current.windowClass == WindowClass.Compact) {
        BuilderSheet(
            visible = visible,
            onDismissRequest = onDismissRequest,
            title = title,
            presentation = SheetPresentation.FullScreen,
            modifier = modifier,
            returnFocusTo = returnFocusTo,
            subtitle = line,
        ) {
            PresetBody(document, isDark, onChoose, compact = true, modifier = Modifier.weight(1f))
        }
        return
    }
    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = title,
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        closeButton = true,
        maxWidth = PickerMaxWidth,
    ) {
        BuilderText(
            text = line,
            modifier = Modifier.padding(bottom = LocalBuilderTokens.current.spacing.large),
            emphasis = Emphasis.Secondary,
        )
        PresetBody(document, isDark, onChoose, compact = false)
    }
}

/**
 * The two groups, in a scroll area of their own. The cards take the focus themselves, so the area
 * needs no Tab stop.
 *
 * It reaches [SelectedRingRoom] past each side and pads its groups back in by as much, so the ring
 * round a selected card at the edge is never cut off and the groups still line up with the title.
 *
 * @param[compact] Whether it fills a phone's sheet, where the pictures scroll sideways in one row.
 */
@Composable
private fun PresetBody(
    document: ThemeDocument,
    isDark: Boolean,
    onChoose: (Preset) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    // In the dialog the area is only as tall as the groups, so no empty band sits under them.
    BuilderScrollArea(modifier.bleedSideways(SelectedRingRoom), tabStop = false, fitContent = !compact) {
        BoxWithConstraints(Modifier.padding(SelectedRingRoom)) {
            val wide = !compact && maxWidth >= WideBody
            Column(verticalArrangement = Arrangement.spacedBy(spacing.section)) {
                PresetGroup(
                    title = stringResource(Res.string.image_presets_images),
                    line = stringResource(Res.string.image_presets_images_line),
                    inline = wide,
                ) {
                    Pictures(document, compact, onChoose)
                }
                PresetGroup(
                    title = stringResource(Res.string.image_presets_starters),
                    line = stringResource(Res.string.image_presets_starters_line),
                    inline = wide,
                ) {
                    Starters(document, isDark, columns = if (wide) 4 else 2, compact, onChoose)
                }
            }
        }
    }
}

/**
 * A heading, a line on what choosing from it does, and the cards.
 *
 * @param[inline] Whether the line sits beside the heading on its baseline, where there is room,
 * rather than under it.
 */
@Composable
private fun PresetGroup(
    title: String,
    line: String,
    inline: Boolean,
    cards: @Composable () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        if (inline) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                BuilderText(text = title, modifier = Modifier.alignByBaseline(), style = BuilderTextStyle.GroupLabel)
                BuilderText(
                    text = line,
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    emphasis = Emphasis.Secondary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                BuilderText(text = title, style = BuilderTextStyle.GroupLabel)
                BuilderText(text = line, emphasis = Emphasis.Secondary)
            }
        }
        cards()
    }
}

/**
 * The pictures, five a row, or on a phone one row that scrolls sideways. A tile the focus lands on
 * scrolls into view by itself, as anything focusable in a scrolling row does.
 */
@Composable
private fun Pictures(
    document: ThemeDocument,
    compact: Boolean,
    onChoose: (Preset) -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val tile: @Composable (Preset.Image) -> Unit = { preset ->
        PictureTile(
            preset = preset,
            selected = preset.isCurrent(document),
            compact = compact,
            onClick = { onChoose(preset) },
        )
    }
    if (compact) {
        Row(
            modifier = Modifier
                .bleedSideways(SelectedRingRoom)
                .horizontalScroll(rememberScrollState())
                .padding(SelectedRingRoom),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Presets.images.forEach { preset -> tile(preset) }
        }
    } else {
        PresetGrid(Presets.images, columns = Presets.images.size, cell = tile)
    }
}

/**
 * A card for each starter, with the scheme the starter makes of [document]. The schemes resolve one
 * per frame, each card a skeleton until its turn, so opening the picker never costs eight in one
 * frame.
 */
@Composable
private fun Starters(
    document: ThemeDocument,
    isDark: Boolean,
    columns: Int,
    compact: Boolean,
    onChoose: (Preset) -> Unit,
) {
    val starters = Presets.starters
    val resolver = rememberThemeResolver()
    val inputs = remember(document) {
        starters.map { starter ->
            SchemeInputs.from(document.copy(seed = starter.seed, style = starter.style, contrast = starter.contrast))
        }
    }
    val colors = rememberCandidateColors(starters, inputs, isDark, resolver)
    PresetGrid(starters, columns) { starter ->
        StarterSwatch(
            starter = starter,
            colors = colors.getOrNull(starters.indexOf(starter)),
            selected = starter.isCurrent(document),
            compact = compact,
            onClick = { onChoose(starter) },
        )
    }
}

/**
 * [items] in rows of [columns] even cells, so a short last row keeps the width of the rest.
 */
@Composable
private fun <T> PresetGrid(
    items: List<T>,
    columns: Int,
    cell: @Composable (item: T) -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                row.forEach { item -> Box(Modifier.weight(1f)) { cell(item) } }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Lays the content out [extra] wider on each side than it is offered and centres it over its own
 * room, so what it pads back in lines up with its neighbours while what reaches past still draws.
 */
private fun Modifier.bleedSideways(extra: Dp): Modifier =
    layout { measurable, constraints ->
        val px = extra.roundToPx()
        val wider = if (constraints.hasBoundedWidth) {
            constraints.copy(minWidth = constraints.minWidth + px * 2, maxWidth = constraints.maxWidth + px * 2)
        } else {
            constraints
        }
        val placeable = measurable.measure(wider)
        val width = constraints.constrainWidth(placeable.width - px * 2)
        layout(width, placeable.height) { placeable.place(-px, 0) }
    }
