package com.materialkolor.builder.feature.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.feature.poster.Eyebrow
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.share_card_failed
import com.materialkolor.builder.generated.resources.share_card_failed_body
import com.materialkolor.builder.generated.resources.share_card_heading
import com.materialkolor.builder.generated.resources.share_card_loading
import com.materialkolor.builder.generated.resources.share_card_name
import com.materialkolor.builder.generated.resources.share_card_updating
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderProgress
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * The width over the height of a link card, as the Worker draws it.
 */
private const val CARD_ASPECT = 1200f / 630f

/**
 * The shortest the card shrinks to when the well is short of height.
 */
private val CardFloor: Dp = 120.dp

/**
 * The height a failed box needs for its glyph as well as both lines, below which the glyph goes.
 */
private val FailedGlyphRoom: Dp = 150.dp

/**
 * The failed card's box, for tests.
 */
internal const val FailedCardTag: String = "share-failed-card"

/**
 * How much of the old card shows through while the new one is on its way.
 */
private const val UPDATING_ALPHA = 0.45f

/**
 * How big the glyph in the updating chip is.
 */
private val ChipGlyphSize: Dp = 16.dp

/**
 * How long each dash and each gap of the failed card's outline is.
 */
private val DashLength: Dp = 6.dp

// The well's ground, the text color laid thinly over the dialog, so it sits a step deeper in light and dark.
private const val WELL_TINT = 0.06f

/**
 * The Link preview well, the card chats and posts show for the link, sunk into the dialog, with
 * its caption under it.
 *
 * The card fills the well's width while the height allows. Given less height it shrinks at its own
 * shape, centred over the caption, down to [CardFloor] tall.
 *
 * @param[card] Where the card is.
 */
@Composable
internal fun ShareWell(
    card: CardState,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = modifier
            .background(tokens.textStrong.copy(alpha = WELL_TINT), RoundedCornerShape(tokens.radius.medium))
            .padding(tokens.spacing.large),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        Eyebrow(stringResource(Res.string.share_card_heading))
        CardBox(card, Modifier.weight(1f, fill = false).align(Alignment.CenterHorizontally).cardFit())
    }
}

/**
 * The card box at the card's own shape, drawing [card] as it stands.
 */
@Composable
private fun CardBox(
    card: CardState,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val shape = RoundedCornerShape(tokens.radius.small)
    when (card) {
        is CardState.Shown -> {
            CardImage(card.card, modifier.clip(shape).border(tokens.outlineWidth, tokens.border, shape))
        }
        is CardState.Updating -> {
            Box(modifier.clip(shape).border(tokens.outlineWidth, tokens.border, shape)) {
                CardImage(card.previous, Modifier.fillMaxSize().alpha(UPDATING_ALPHA))
                UpdatingChip(Modifier.align(Alignment.TopStart).padding(tokens.spacing.medium))
            }
        }
        CardState.Loading -> {
            Box(
                modifier = modifier
                    .clip(shape)
                    .background(tokens.panel)
                    .border(tokens.outlineWidth, tokens.border, shape)
                    .padding(tokens.spacing.extraLarge),
                contentAlignment = Alignment.Center,
            ) {
                BuilderProgress(label = stringResource(Res.string.share_card_loading))
            }
        }
        CardState.Failed -> {
            FailedCard(modifier)
        }
    }
}

/**
 * The card itself, fitted into its box and spoken as the Link preview card.
 */
@Composable
private fun CardImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = bitmap,
        contentDescription = stringResource(Res.string.share_card_name),
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

/**
 * The chip over the old card while the new one loads, in inverse colors so it reads on any card.
 */
@Composable
private fun UpdatingChip(modifier: Modifier = Modifier) {
    val tokens = LocalBuilderTokens.current
    val ink = tokens.panel
    Row(
        modifier = modifier
            .background(tokens.textStrong, RoundedCornerShape(tokens.radius.medium))
            .padding(horizontal = tokens.spacing.medium, vertical = tokens.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderIcon(IconId.Progress, contentDescription = null, tint = ink, size = ChipGlyphSize)
        BuilderText(text = stringResource(Res.string.share_card_updating), style = BuilderTextStyle.Label, color = ink)
    }
}

/**
 * A dashed box in the card's place, saying the card did not load and the link still works. The
 * glyph over the words only decorates, so a box shrunk toward [CardFloor] drops it and keeps both
 * lines inside the outline.
 */
@Composable
private fun FailedCard(modifier: Modifier = Modifier) {
    val tokens = LocalBuilderTokens.current
    val outline = tokens.borderStrong
    BoxWithConstraints(
        modifier = modifier
            .testTag(FailedCardTag)
            .dashedOutline(outline, tokens.outlineWidth, tokens.radius.small),
        contentAlignment = Alignment.Center,
    ) {
        val glyph = maxHeight >= FailedGlyphRoom
        Column(
            modifier = Modifier.padding(tokens.spacing.large),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (glyph) BuilderIcon(IconId.Image, contentDescription = null, emphasis = Emphasis.Secondary)
            BuilderText(
                text = stringResource(Res.string.share_card_failed),
                style = BuilderTextStyle.Label,
                textAlign = TextAlign.Center,
            )
            BuilderText(
                text = stringResource(Res.string.share_card_failed_body),
                emphasis = Emphasis.Secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * A dashed outline [width] thick in [color], with corners of [radius].
 */
private fun Modifier.dashedOutline(
    color: Color,
    width: Dp,
    radius: Dp,
): Modifier =
    drawBehind {
        val stroke = width.toPx()
        val dash = DashLength.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
        )
    }

/**
 * Sizes the card at [CARD_ASPECT], as wide as it may be unless the height it may take is shorter,
 * and never shorter than [CardFloor] while the width allows. Asked how short it can be it says
 * [CardFloor], so a body can tell how little height the well needs.
 */
private fun Modifier.cardFit(): Modifier = this then CardFitElement

private data object CardFitElement : ModifierNodeElement<CardFitNode>() {
    override fun create(): CardFitNode = CardFitNode()

    override fun update(node: CardFitNode) = Unit
}

private class CardFitNode :
    Modifier.Node(),
    LayoutModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val byWidth = if (constraints.hasBoundedWidth) heightFor(constraints.maxWidth) else Constraints.Infinity
        val fit = minOf(byWidth, constraints.maxHeight)
        val floor = minOf(CardFloor.roundToPx(), byWidth)
        val height = if (fit == Constraints.Infinity) floor else maxOf(fit, floor)
        val width = (height * CARD_ASPECT).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(width, height))
        return layout(width, height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = minOf(CardFloor.roundToPx(), heightFor(width))

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int = heightFor(width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int = (CardFloor.roundToPx() * CARD_ASPECT).roundToInt()

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        val tallest = if (height == Constraints.Infinity) CardFloor.roundToPx() else height
        return (tallest * CARD_ASPECT).roundToInt()
    }

    private fun heightFor(width: Int): Int =
        if (width == Constraints.Infinity) width else (width / CARD_ASPECT).roundToInt()
}
