package com.materialkolor.builder.feature.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.feature.poster.ContrastStop
import com.materialkolor.builder.feature.poster.PosterContext
import com.materialkolor.builder.feature.poster.rememberThemeResolver
import com.materialkolor.builder.feature.poster.styleName
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.image_menu_presets
import com.materialkolor.builder.generated.resources.image_menu_upload
import com.materialkolor.builder.generated.resources.image_presets_close
import com.materialkolor.builder.generated.resources.image_presets_images
import com.materialkolor.builder.generated.resources.image_presets_images_line
import com.materialkolor.builder.generated.resources.image_presets_starters
import com.materialkolor.builder.generated.resources.image_presets_starters_line
import com.materialkolor.builder.generated.resources.image_presets_title
import com.materialkolor.builder.generated.resources.image_starter_card
import com.materialkolor.builder.generated.resources.image_starter_card_contrast
import com.materialkolor.builder.generated.resources.poster_image
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderMenu
import com.materialkolor.builder.kit.control.BuilderMenuItem
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChip
import com.materialkolor.builder.kit.widget.SchemeChipFootprint
import com.materialkolor.builder.kit.widget.SchemeChipSkeleton
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Tags a starter's scheme chip once it has resolved, for tests to count them.
 */
internal const val STARTER_CHIP_TAG: String = "image-starter-chip"

/**
 * Tags a starter's skeleton while its scheme resolves.
 */
internal const val STARTER_SKELETON_TAG: String = "image-starter-skeleton"

/**
 * The Image button, which opens a menu of the two ways to seed from a picture (F-08, F-09).
 *
 * Upload image opens the platform picker. Browsers only open it inside the click, so the row's click
 * dispatches `OpenImagePicker` and the workspace starts the pick before the click returns (R-B-302).
 * Presets and starters opens [PresetPicker] as `Panel.Presets`, so Back closes it, and a preset or
 * starter chosen there lands behind a crossfade as one undo entry. The picker hands the focus back
 * to this button once it closes.
 */
@Composable
internal fun ImageMenuButton(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
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
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenPanel(Panel.Presets)) }, // b-311d
            icon = IconId.Image,
        ),
    )
    BuilderMenu(expanded = menu, onDismissRequest = { menu = false }, items = items, modifier = modifier) {
        BuilderButton(
            onClick = { menu = true },
            label = stringResource(Res.string.poster_image),
            modifier = Modifier.focusRequester(button),
            icon = IconId.Image,
        )
    }
    // b-311d
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
 * The preset pictures and the starter themes, each a card that chooses it (F-09).
 *
 * A picture sets only the seed, to its strongest color, and the row under the seed actions then
 * offers its other colors. A starter sets the seed, the style and the contrast. Neither touches the
 * target, the overrides, the pins or the accents. Each starter's card shows the scheme it makes of
 * [document], resolved one per frame once the picker opens.
 *
 * @param[visible] Whether the picker is open, while the workspace's panel is `Panel.Presets`.
 * @param[document] The theme a starter's scheme is drawn from, with the starter's seed, style and
 * contrast in place of its own.
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
    val spacing = LocalBuilderTokens.current.spacing
    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.image_presets_title),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        actions = {
            BuilderButton(
                onClick = onDismissRequest,
                label = stringResource(Res.string.image_presets_close),
                emphasis = Emphasis.Subtle,
            )
        },
    ) {
        // b-311d
        // The cards take the focus themselves, so the list needs no Tab stop of its own. It scrolls
        // in the height the title and the buttons leave, so the buttons stay on screen.
        BuilderScrollArea(Modifier.leaveRoomBelow(dialogButtonRoom()), tabStop = false) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraLarge)) {
                PresetGroup(
                    title = stringResource(Res.string.image_presets_images),
                    line = stringResource(Res.string.image_presets_images_line),
                ) {
                    Presets.images.forEach { preset ->
                        PresetCard(stringResource(preset.name), onClick = { onChoose(preset) }) {
                            PresetPicture(preset)
                        }
                    }
                }
                PresetGroup(
                    title = stringResource(Res.string.image_presets_starters),
                    line = stringResource(Res.string.image_presets_starters_line),
                ) {
                    StarterCards(document, isDark, onChoose) // b-311d
                }
            }
        }
    }
}

/**
 * A heading, a line on what choosing from it does, and the cards.
 */
@Composable
private fun PresetGroup(
    title: String,
    line: String,
    cards: @Composable () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        BuilderText(text = title, style = BuilderTextStyle.SectionLabel)
        BuilderText(text = line, emphasis = Emphasis.Secondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            cards()
        }
    }
}

/**
 * One preset as a card, its picture over its [name]. The card reads as a button named by what it says.
 */
@Composable
private fun PresetCard(
    name: String,
    onClick: () -> Unit,
    picture: @Composable () -> Unit,
) {
    val spacing = LocalBuilderTokens.current.spacing
    BuilderCard(onClick = onClick) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
        ) {
            picture()
            BuilderText(text = name, style = BuilderTextStyle.Label)
        }
    }
}

/**
 * The preset's picture in the room a chip takes.
 */
@Composable
private fun PresetPicture(preset: Preset.Image) {
    val radius = LocalBuilderTokens.current.radius
    Image(
        painter = painterResource(preset.drawable),
        // The card already reads out the name.
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(SchemeChipFootprint).clip(RoundedCornerShape(radius.small)),
    )
}

// b-311d

/**
 * A card for each starter, with the scheme the starter makes of [document] drawn as a chip. The
 * schemes resolve one per frame, each a skeleton until its turn, so opening the picker never costs
 * eight in one frame.
 */
@Composable
private fun StarterCards(
    document: ThemeDocument,
    isDark: Boolean,
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
    starters.forEachIndexed { index, starter ->
        val name = starterName(starter)
        PresetCard(name, onClick = { onChoose(starter) }) {
            StarterChip(colors.getOrNull(index), name, onClick = { onChoose(starter) })
        }
    }
}

/**
 * The starter's name and style, and its contrast too when that is not Standard, such as "Ink,
 * TonalSpot, Medium contrast".
 */
@Composable
private fun starterName(starter: Preset.Starter): String {
    val name = stringResource(starter.name)
    val style = stringResource(styleName(starter.style))
    val contrast = ContrastStop.of(starter.contrast).takeIf { stop -> stop != ContrastStop.Standard }
    return if (contrast == null) {
        stringResource(Res.string.image_starter_card, name, style)
    } else {
        stringResource(Res.string.image_starter_card_contrast, name, style, stringResource(contrast.label))
    }
}

/**
 * The starter's scheme as a chip in the room a chip takes, or a skeleton until [colors] resolve.
 *
 * The card around it is the button and already reads out its [name], so the chip takes no focus
 * and reads as nothing. A press on it chooses the starter all the same.
 */
@Composable
private fun StarterChip(
    colors: CandidateColors?,
    name: String,
    onClick: () -> Unit,
) {
    if (colors == null) {
        SchemeChipSkeleton(Modifier.testTag(STARTER_SKELETON_TAG))
        return
    }
    SchemeChip(
        primary = colors.primary,
        secondaryContainer = colors.secondaryContainer,
        tertiaryContainer = colors.tertiaryContainer,
        selected = false,
        onClick = onClick,
        label = name,
        modifier = Modifier
            .testTag(STARTER_CHIP_TAG)
            .focusProperties { canFocus = false }
            .clearAndSetSemantics { },
    )
}
