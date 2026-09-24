package com.materialkolor.builder.feature.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.poster.PosterContext
import com.materialkolor.builder.feature.poster.styleName
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
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChipFootprint
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** How much of the window's height the picker's list may take before it scrolls. */
private const val PICKER_HEIGHT_FRACTION = 0.6f

/**
 * The Image button, which opens a menu of the two ways to seed from a picture (F-08, F-09).
 *
 * Upload image opens the platform picker. Browsers only open it inside the click, so the row's click
 * dispatches `OpenImagePicker` and the workspace starts the pick before the click returns (R-B-302).
 * Presets and starters opens [PresetPicker], and a preset or starter chosen there lands behind a
 * crossfade as one undo entry. The picker hands the focus back to this button once it closes.
 */
@Composable
internal fun ImageMenuButton(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    val button = remember { FocusRequester() }
    val items = listOf(
        BuilderMenuItem(
            label = stringResource(Res.string.image_menu_upload),
            onClick = { dispatcher.dispatch(WorkspaceAction.OpenImagePicker) },
            icon = IconId.Upload,
        ),
        BuilderMenuItem(
            label = stringResource(Res.string.image_menu_presets),
            onClick = { picker = true },
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
    PresetPicker(
        visible = picker,
        onChoose = { preset ->
            picker = false
            // The picker is closing over wherever the card sat, so the theme crossfades in place.
            dispatcher.dispatch(WorkspaceAction.EditWithReveal(preset.change(context.document), origin = null))
        },
        onDismissRequest = { picker = false },
        returnFocusTo = button,
    )
}

/**
 * The preset pictures and the starter themes, each a card that chooses it (F-09).
 *
 * A picture sets only the seed, to its strongest color, and the row under the seed actions then
 * offers its other colors. A starter sets the seed, the style and the contrast. Neither touches the
 * target, the overrides, the pins or the accents.
 *
 * @param[onChoose] Called with the preset a card chooses. The picker stays open until told otherwise.
 * @param[returnFocusTo] The button that opened the picker, which gets the focus back once it closes.
 */
@Composable
internal fun PresetPicker(
    visible: Boolean,
    onChoose: (Preset) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val layout = LocalLayout.current
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
        // The cards take the focus themselves, so the list needs no Tab stop of its own.
        BuilderScrollArea(Modifier.heightIn(max = layout.heightDp * PICKER_HEIGHT_FRACTION), tabStop = false) {
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
                    Presets.starters.forEach { starter ->
                        val style = stringResource(styleName(starter.style))
                        val name = stringResource(Res.string.image_starter_card, stringResource(starter.name), style)
                        PresetCard(name, onClick = { onChoose(starter) }) {
                            StarterSwatch(starter)
                        }
                    }
                }
            }
        }
    }
}

/** A heading, a line on what choosing from it does, and the cards. */
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

/** One preset as a card, its picture over its [name]. The card reads as a button named by what it says. */
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

/** The preset's picture in the room a chip takes. */
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

/** The starter's seed as a swatch in the room a chip takes. */
@Composable
private fun StarterSwatch(starter: Preset.Starter) {
    val radius = LocalBuilderTokens.current.radius
    Box(
        Modifier
            .size(SchemeChipFootprint)
            .clip(RoundedCornerShape(radius.small))
            .background(starter.seed.toColor()),
    )
}
