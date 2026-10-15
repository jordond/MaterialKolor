package com.materialkolor.builder.feature.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.engine.image.SeedExtractor
import com.materialkolor.builder.engine.resolve.SchemeInputs
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.poster.PosterContext
import com.materialkolor.builder.feature.poster.rememberThemeResolver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.image_add_again
import com.materialkolor.builder.generated.resources.image_candidates
import com.materialkolor.builder.generated.resources.image_chip
import com.materialkolor.builder.generated.resources.image_mostly_gray
import com.materialkolor.builder.generated.resources.image_reading
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderChoiceGroup
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChip
import com.materialkolor.builder.kit.widget.SchemeChipFootprint
import com.materialkolor.builder.kit.widget.SchemeChipSkeleton
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.stringResource

/**
 * The image under the seed actions and the colors it offered.
 *
 * While an image is on its way the row shows its thumbnail, once it has one, beside skeleton chips,
 * whatever the seed came from. Once the seed comes from an image the row shows a chip for each
 * candidate, and a chip click swaps the seed for that candidate. The image itself only shows while
 * it is still in memory, so after a reload the row offers to add it again instead. A seed from a
 * preset picture shows that picture and its chips the same way. Any other seed leaves the row out.
 * The picture opens the image eyedropper.
 *
 * The skeleton takes the row's place, so when the focus was in the row as an image came in, the
 * skeleton takes it over. When the image lands the chips take it back, and when it never does the
 * row it replaced takes it back, but only while the skeleton still has it.
 */
@Composable
internal fun ImageCandidateRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val seeds = LocalImageSeeds.current
    val source = context.document.seedSource
    val arriving = seeds.arriving?.takeIf { arriving -> arriving.lands != source }
    // Read in the frame the row changes over, before what it showed leaves and the focus goes with it.
    val focus = remember { RowFocus() }
    val preset = Presets.imageOf(source)
    val shows = when {
        arriving != null -> RowContent.Arriving
        source is SeedSource.Image -> RowContent.Candidates
        preset != null -> RowContent.Preset
        else -> RowContent.None
    }
    val refocus = remember(shows, arriving?.id) { focus.handOver() }
    val rowModifier = modifier.onFocusChanged { state -> focus.inRow = state.hasFocus }
    when {
        arriving != null -> {
            // A fresh skeleton for each image, so the focus it takes over is its own.
            key(arriving.id) { ArrivingRow(arriving, rowModifier, refocus) }
        }
        source is SeedSource.Image -> {
            val newest = seeds.newest?.takeIf { newest -> newest.source == source }
            CandidateRow(context, source, newest, dispatcher, rowModifier, refocus)
        }
        preset != null -> {
            PresetRow(context, preset, dispatcher, rowModifier, refocus)
        }
    }
}

/**
 * The thumbnail, or a skeleton for it until the image decodes, and a skeleton for each chip. With
 * [refocus] the whole of it takes the focus, which the row it replaced had.
 */
@Composable
private fun ArrivingRow(
    arriving: ArrivingImage,
    modifier: Modifier,
    refocus: Boolean,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val reading = stringResource(Res.string.image_reading)
    val column = remember { FocusRequester() }
    if (refocus) LaunchedEffect(Unit) { column.requestFocus() }
    Column(
        modifier = modifier
            .focusRequester(column)
            .focusable(enabled = refocus)
            .clearAndSetSemantics { contentDescription = reading },
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        val thumbnail = arriving.thumbnail
        if (thumbnail == null) {
            SchemeChipSkeleton()
        } else {
            Thumbnail(thumbnail)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            repeat(SeedExtractor.MAX_CANDIDATES) { SchemeChipSkeleton() }
        }
    }
}

/**
 * The image when it is still in memory, or a button to add it again, over the candidate chips,
 * with a line saying so when the image is mostly gray. With [refocus] the chips take the focus as
 * they come in, since the skeleton they replaced still had it.
 */
@Composable
private fun CandidateRow(
    context: PosterContext,
    source: SeedSource.Image,
    newest: NewestImage?,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier,
    refocus: Boolean,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val chips = remember { FocusRequester() }
    if (refocus) LaunchedEffect(Unit) { chips.requestFocus() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        if (newest == null) {
            BuilderButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenImagePicker) },
                label = stringResource(Res.string.image_add_again),
                emphasis = Emphasis.Subtle,
                icon = IconId.Image,
            )
        } else {
            EyedropperThumbnail(newest.thumbnail, newest.detail, source, context.openPanel, dispatcher)
        }
        CandidateChips(context, source.candidates, source, dispatcher, Modifier.focusRequester(chips))
        if (newest?.mostlyGray == true) {
            BuilderText(text = stringResource(Res.string.image_mostly_gray), emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * A preset picture's seed, the picture over its candidate chips. A chip keeps the preset as the seed
 * source. With [refocus] the chips take the focus as they come in.
 */
@Composable
private fun PresetRow(
    context: PosterContext,
    preset: Preset.Image,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier,
    refocus: Boolean,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val source = context.document.seedSource
    val chips = remember { FocusRequester() }
    if (refocus) LaunchedEffect(Unit) { chips.requestFocus() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        // Only ever 256 px, so the eyedropper shows the same picture.
        val picture = imageResource(preset.drawable)
        EyedropperThumbnail(picture, picture, source, context.openPanel, dispatcher)
        CandidateChips(context, preset.candidates, source, dispatcher, Modifier.focusRequester(chips))
    }
}

/**
 * One chip per candidate, drawn the way a style chip is, from the scheme the candidate makes in the
 * document's style. The schemes resolve one per frame, so a new image never costs five in one. A
 * chip keeps [source] as the seed source.
 */
@Composable
private fun CandidateChips(
    context: PosterContext,
    candidates: List<Argb>,
    source: SeedSource,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier,
) {
    val resolver = rememberThemeResolver()
    val isDark = context.visibleModes == PreviewMode.Dark
    val base = context.result.document
    val inputs = remember(candidates, base) {
        candidates.map { candidate -> SchemeInputs.from(base.copy(seed = candidate)) }
    }
    val colors = rememberCandidateColors(candidates, inputs, isDark, resolver)
    val selected = context.document.seed
    val choose = { candidate: Argb, origin: Rect? ->
        val change = DocumentChange.SetSeed(candidate, source)
        dispatcher.dispatch(WorkspaceAction.EditWithReveal(change, origin?.center))
    }
    BuilderChoiceGroup(
        options = candidates,
        selected = selected,
        // The keys only move the focus here, so a pick that ever comes this way has no chip to reveal from.
        onSelect = { candidate -> if (candidate != selected) choose(candidate, null) },
        label = stringResource(Res.string.image_candidates),
        modifier = modifier,
        selectOnFocus = false,
    ) { candidate, isSelected, optionModifier ->
        val placeholder = LocalBuilderTokens.current.border
        val chip = colors.getOrNull(candidates.indexOf(candidate))
        val bounds = remember { ChipBounds() }
        val name = remember(candidate) { ColorNames.nameOf(candidate) }
        SchemeChip(
            primary = chip?.primary ?: placeholder,
            secondaryContainer = chip?.secondaryContainer ?: placeholder,
            tertiaryContainer = chip?.tertiaryContainer ?: placeholder,
            selected = isSelected,
            onClick = { if (candidate != selected) choose(candidate, bounds.rect) },
            label = stringResource(Res.string.image_chip, candidate.toHex(), name),
            modifier = optionModifier.onGloballyPositioned { coordinates -> bounds.rect = coordinates.boundsInRoot() },
        )
    }
}

/**
 * The colors of a chip for each of [inputs], null until its scheme has first resolved. One resolves
 * per frame, starting once the chips are first drawn.
 *
 * New inputs, from a style or contrast edit, keep the colors already there and overwrite them one
 * per frame, picking up after the last chip that resolved. So an edit never blanks the chips, and a
 * drag that changes the inputs every frame still walks through all of them. Other [chips], such as
 * the candidates of an earlier image an undo goes back to, start over from blank chips instead,
 * since the colors kept belong to other chips.
 *
 * @param[chips] What the chips stand for, the candidates of an image or the starters.
 */
@Composable
internal fun rememberCandidateColors(
    chips: List<Any>,
    inputs: List<SchemeInputs>,
    isDark: Boolean,
    resolver: ThemeResolver,
): List<CandidateColors?> {
    val colors = remember(chips) { mutableStateListOf<CandidateColors?>() }
    val next = remember(chips) { NextChip() }
    LaunchedEffect(inputs, isDark, resolver) {
        while (colors.size > inputs.size) colors.removeAt(colors.lastIndex)
        while (colors.size < inputs.size) colors.add(null)
        repeat(inputs.size) { step ->
            if (step > 0) withFrameNanos { }
            val index = next.index % inputs.size
            val scheme = resolver.scheme(inputs[index], isDark)
            colors[index] = CandidateColors(
                primary = Color(scheme.primary),
                onPrimary = Color(scheme.onPrimary),
                secondaryContainer = Color(scheme.secondaryContainer),
                tertiaryContainer = Color(scheme.tertiaryContainer),
            )
            next.index = index + 1
        }
    }
    return colors
}

/**
 * The image as it came in, in the room a chip takes.
 */
@Composable
private fun Thumbnail(bitmap: ImageBitmap) {
    val radius = LocalBuilderTokens.current.radius
    Image(
        bitmap = bitmap,
        // The seed's source line already names the file.
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(SchemeChipFootprint).clip(RoundedCornerShape(radius.small)),
    )
}

/**
 * The colors a candidate chip is drawn in, with [onPrimary] for a starter card's name on its primary.
 */
internal class CandidateColors(
    val primary: Color,
    val onPrimary: Color,
    val secondaryContainer: Color,
    val tertiaryContainer: Color,
)

/**
 * Which chip resolves its colors next, kept across new inputs so no chip waits for long.
 */
private class NextChip {
    var index: Int = 0
}

/**
 * Whether the focus is in the row, in whatever it shows right now.
 */
private class RowFocus {
    var inRow: Boolean = false

    /**
     * Whether the focus was in the row as it changed over to show something else, which starts the
     * next thing it shows afresh. Its own focus events say soon enough whether it has the focus.
     */
    fun handOver(): Boolean = inRow.also { inRow = false }
}

/**
 * What the row shows, whose change hands the focus over.
 */
private enum class RowContent {
    Arriving,
    Candidates,
    Preset,
    None,
}

/**
 * Where a chip sits in the root, which a pick reveals from. Only a pick reads it.
 */
private class ChipBounds {
    var rect: Rect = Rect.Zero
}
