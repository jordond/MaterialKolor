package com.materialkolor.builder.feature.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.materialkolor.builder.kit.motion.rememberLoopPhase
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.widget.SchemeChip
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/**
 * How much room a scheme chip takes with its ring and focus outline, which the thumbnail and the
 * skeleton chips match so nothing moves when the real chips come in. The kit keeps the chip's own
 * sizes to itself.
 */
private val ChipSize: Dp = 58.dp

/** How long one pulse of a skeleton takes. */
private const val SHIMMER_PERIOD_MILLIS = 1_200

/**
 * The image under the seed actions and the colors it offered (F-08).
 *
 * While an image is on its way the row shows its thumbnail, once it has one, beside skeleton chips,
 * whatever the seed came from. Once the seed comes from an image the row shows a chip for each
 * candidate, and a chip click swaps the seed for that candidate. The image itself only shows while
 * it is still in memory, so after a reload the row offers to add it again instead. Any other seed
 * leaves the row out.
 */
@Composable
internal fun ImageCandidateRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val seeds = LocalImageSeeds.current
    val arriving = seeds.arriving
    val source = context.document.seedSource
    when {
        arriving != null && arriving.lands != source -> {
            ArrivingRow(arriving, modifier)
        }
        source is SeedSource.Image -> {
            val newest = seeds.newest?.takeIf { newest -> newest.source == source }
            CandidateRow(context, source, newest, dispatcher, modifier)
        }
    }
}

/** The thumbnail, or a skeleton for it until the image decodes, and a skeleton for each chip. */
@Composable
private fun ArrivingRow(
    arriving: ArrivingImage,
    modifier: Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val reading = stringResource(Res.string.image_reading)
    val phase = rememberLoopPhase(SHIMMER_PERIOD_MILLIS)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = reading },
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        val thumbnail = arriving.thumbnail
        if (thumbnail == null) {
            Skeleton(RoundedCornerShape(tokens.radius.small), phase)
        } else {
            Thumbnail(thumbnail)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
            repeat(SeedExtractor.MAX_CANDIDATES) { Skeleton(CircleShape, phase) }
        }
    }
}

/**
 * The image when it is still in memory, or a button to add it again, over the candidate chips,
 * with a line saying so when the image is mostly gray.
 */
@Composable
private fun CandidateRow(
    context: PosterContext,
    source: SeedSource.Image,
    newest: NewestImage?,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        if (newest == null) {
            BuilderButton(
                onClick = { dispatcher.dispatch(WorkspaceAction.OpenImagePicker) },
                label = stringResource(Res.string.image_add_again),
                emphasis = Emphasis.Subtle,
                icon = IconId.Image,
            )
        } else {
            Thumbnail(newest.thumbnail)
        }
        CandidateChips(context, source, dispatcher)
        if (newest?.mostlyGray == true) {
            BuilderText(text = stringResource(Res.string.image_mostly_gray), emphasis = Emphasis.Secondary)
        }
    }
}

/**
 * One chip per candidate, drawn the way a style chip is, from the scheme the candidate makes in the
 * document's style. The schemes resolve one per frame, so a new image never costs five in one.
 */
@Composable
private fun CandidateChips(
    context: PosterContext,
    source: SeedSource.Image,
    dispatcher: Dispatcher<WorkspaceAction>,
) {
    val resolver = rememberThemeResolver()
    val isDark = context.visibleModes == PreviewMode.Dark
    val base = context.result.document
    val inputs = remember(source.candidates, base) {
        source.candidates.map { candidate -> SchemeInputs.from(base.copy(seed = candidate)) }
    }
    val colors = rememberCandidateColors(inputs, isDark, resolver)
    val selected = context.document.seed
    val choose = { candidate: Argb, origin: Rect? ->
        val change = DocumentChange.SetSeed(candidate, source)
        dispatcher.dispatch(WorkspaceAction.EditWithReveal(change, origin?.center))
    }
    BuilderChoiceGroup(
        options = source.candidates,
        selected = selected,
        // The keys only move the focus here, so a pick that ever comes this way has no chip to reveal from.
        onSelect = { candidate -> if (candidate != selected) choose(candidate, null) },
        label = stringResource(Res.string.image_candidates),
        selectOnFocus = false,
    ) { candidate, isSelected, optionModifier ->
        val placeholder = LocalBuilderTokens.current.border
        val chip = colors.getOrNull(source.candidates.indexOf(candidate))
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
 * The colors of a chip for each of [inputs], null until its scheme has resolved. One resolves per
 * frame, starting once the chips are first drawn.
 */
@Composable
private fun rememberCandidateColors(
    inputs: List<SchemeInputs>,
    isDark: Boolean,
    resolver: ThemeResolver,
): List<CandidateColors?> {
    val colors = remember(inputs, isDark, resolver) {
        List<CandidateColors?>(inputs.size) { null }.toMutableStateList()
    }
    LaunchedEffect(colors) {
        inputs.forEachIndexed { index, input ->
            if (index > 0) withFrameNanos { }
            val scheme = resolver.scheme(input, isDark)
            colors[index] = CandidateColors(
                primary = Color(scheme.primary),
                secondaryContainer = Color(scheme.secondaryContainer),
                tertiaryContainer = Color(scheme.tertiaryContainer),
            )
        }
    }
    return colors
}

/** The image as it came in, at the size of a chip. */
@Composable
private fun Thumbnail(bitmap: ImageBitmap) {
    val radius = LocalBuilderTokens.current.radius
    Image(
        bitmap = bitmap,
        // The seed's source line already names the file.
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(ChipSize).clip(RoundedCornerShape(radius.small)),
    )
}

/** A chip sized placeholder that pulses between two border tones, redrawn without recomposing. */
@Composable
private fun Skeleton(
    shape: Shape,
    phase: State<Float>,
) {
    val tokens = LocalBuilderTokens.current
    Box(
        Modifier.size(ChipSize).clip(shape).drawBehind {
            val pulse = 1f - abs(phase.value * 2f - 1f)
            drawRect(lerp(tokens.border, tokens.borderStrong, pulse))
        },
    )
}

/** The three colors a candidate chip is drawn in. */
private class CandidateColors(
    val primary: Color,
    val secondaryContainer: Color,
    val tertiaryContainer: Color,
)

/** Where a chip sits in the root, which a pick reveals from. Only a pick reads it. */
private class ChipBounds {
    var rect: Rect = Rect.Zero
}
