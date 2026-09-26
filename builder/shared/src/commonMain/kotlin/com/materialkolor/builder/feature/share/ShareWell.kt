package com.materialkolor.builder.feature.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.feature.poster.Eyebrow
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.share_card_caption
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

/**
 * The width over the height of a link card, as the Worker draws it.
 */
private const val CARD_ASPECT = 1200f / 630f

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

/**
 * The Link preview well, the card chats and posts show for the link, sunk into the dialog, with
 * its caption under it.
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
            .background(tokens.codeBackground, RoundedCornerShape(tokens.radius.medium))
            .padding(tokens.spacing.large),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        Eyebrow(stringResource(Res.string.share_card_heading))
        CardBox(card, Modifier.fillMaxWidth().aspectRatio(CARD_ASPECT))
        BuilderText(text = stringResource(Res.string.share_card_caption), emphasis = Emphasis.Secondary)
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
 * A dashed box in the card's place, saying the card did not load and the link still works.
 */
@Composable
private fun FailedCard(modifier: Modifier = Modifier) {
    val tokens = LocalBuilderTokens.current
    val outline = tokens.borderStrong
    Column(
        modifier = modifier
            .dashedOutline(outline, tokens.outlineWidth, tokens.radius.small)
            .padding(tokens.spacing.large),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BuilderIcon(IconId.Image, contentDescription = null, emphasis = Emphasis.Secondary)
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
