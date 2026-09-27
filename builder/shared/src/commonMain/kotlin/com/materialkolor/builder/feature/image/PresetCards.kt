package com.materialkolor.builder.feature.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.feature.poster.ContrastStop
import com.materialkolor.builder.feature.poster.styleName
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.image_preset_card
import com.materialkolor.builder.generated.resources.image_starter_card
import com.materialkolor.builder.generated.resources.image_starter_card_contrast
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Tags a starter's card once its colors have resolved, for tests to count them.
 */
internal const val STARTER_CARD_TAG: String = "image-starter-card"

/**
 * Tags a starter's card while its colors resolve.
 */
internal const val STARTER_SKELETON_TAG: String = "image-starter-skeleton"

/**
 * How wide a picture stands in the row that scrolls sideways on a phone.
 */
internal val CompactPictureWidth: Dp = 116.dp

/**
 * How far a selected card's ring reaches past the card, the room a grid keeps around its cards.
 */
internal val SelectedRingRoom: Dp = 6.dp

/**
 * The check badge's circle, and the halo of the dialog's surface around it.
 */
private val BadgeSize: Dp = 28.dp
private val BadgeHalo: Dp = 2.dp
private val BadgeGlyph: Dp = 16.dp

/**
 * The seed's dot and the other colors' dots under a picture, on a phone and wider.
 */
private val SeedDot: Dp = 14.dp
private val OtherDot: Dp = 10.dp
private val CompactSeedDot: Dp = 12.dp
private val CompactOtherDot: Dp = 8.dp

/**
 * The starter card's block in its primary, its band of containers and its footer.
 */
private val StarterTop: Dp = 84.dp
private val CompactStarterTop: Dp = 76.dp
private val StarterBand: Dp = 18.dp
private val CompactStarterBand: Dp = 16.dp
private val StarterFooter: Dp = 40.dp

/**
 * How tall the contrast pill in a starter's footer stands.
 */
private val PillHeight: Dp = 22.dp

/**
 * One preset picture as a square photo over its name and the colors pulled from it.
 *
 * The photo is the button, named "Kelp picture", and a [selected] one wears the accent ring and a
 * check. The name and the dots read as nothing, since the photo already speaks for them, and a
 * press on them chooses the picture as well.
 *
 * @param[compact] Whether it stands in the phone's sideways row, at [CompactPictureWidth].
 */
@Composable
internal fun PictureTile(
    preset: Preset.Image,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val name = stringResource(preset.name)
    Column(
        modifier = if (compact) modifier.width(CompactPictureWidth) else modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        BuilderPressable(
            onClick = onClick,
            label = stringResource(Res.string.image_preset_card, name),
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = RoundedCornerShape(tokens.radius.medium),
            selected = selected,
        ) {
            Image(
                painter = painterResource(preset.drawable),
                // The pressable already reads out the picture's name.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (selected) {
                CheckBadge(Modifier.align(Alignment.TopEnd).padding(tokens.spacing.small - BadgeHalo))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { }
                .focusProperties { canFocus = false }
                .clickable(interactionSource = null, indication = null, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BuilderText(
                text = name,
                modifier = Modifier.weight(1f),
                style = BuilderTextStyle.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CandidateDots(preset.candidates, compact)
        }
    }
}

/**
 * A dot for each color pulled from a picture, the first bigger since it becomes the seed. Each has
 * a faint hairline inside its edge, so a pale color still shows on the dialog.
 */
@Composable
private fun CandidateDots(
    candidates: List<Argb>,
    compact: Boolean,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        candidates.forEachIndexed { index, candidate ->
            val size = when {
                index == 0 && compact -> CompactSeedDot
                index == 0 -> SeedDot
                compact -> CompactOtherDot
                else -> OtherDot
            }
            Box(
                Modifier
                    .size(size)
                    .background(candidate.toColor(), CircleShape)
                    .border(tokens.outlineWidth, tokens.border, CircleShape),
            )
        }
    }
}

/**
 * One starter as a card in its own colors. A block in its primary carries its name, a band shows
 * its secondary and tertiary containers, and a footer names its style, and its contrast when that
 * is not Standard.
 *
 * The card is the button, named "Ink, TonalSpot, Medium contrast". Until [colors] resolve it draws
 * in the builder's hairline color with its name in the builder's ink.
 *
 * @param[colors] The colors the starter makes, or null while they resolve.
 * @param[compact] Whether it stands in the phone's sheet, where the blocks are a little shorter.
 */
@Composable
internal fun StarterSwatch(
    starter: Preset.Starter,
    colors: CandidateColors?,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val contrast = ContrastStop.of(starter.contrast).takeIf { stop -> stop != ContrastStop.Standard }
    BuilderPressable(
        onClick = onClick,
        label = starterName(starter),
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (colors == null) STARTER_SKELETON_TAG else STARTER_CARD_TAG),
        shape = RoundedCornerShape(tokens.radius.medium),
        selected = selected,
    ) {
        Column(Modifier.fillMaxWidth().background(tokens.panel)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) CompactStarterTop else StarterTop)
                    .background(colors?.primary ?: tokens.border)
                    .padding(start = spacing.medium, top = spacing.medium, end = spacing.small, bottom = spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                BuilderText(
                    text = stringResource(starter.name),
                    modifier = Modifier.weight(1f),
                    style = BuilderTextStyle.Title,
                    color = colors?.onPrimary ?: tokens.textStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected) CheckBadge()
            }
            Row(Modifier.fillMaxWidth().height(if (compact) CompactStarterBand else StarterBand)) {
                Box(Modifier.weight(1f).fillMaxHeight().background(colors?.secondaryContainer ?: tokens.border))
                Box(Modifier.weight(1f).fillMaxHeight().background(colors?.tertiaryContainer ?: tokens.border))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = StarterFooter)
                    .padding(start = spacing.medium, end = spacing.small),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderText(
                    text = stringResource(styleName(starter.style)),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (contrast != null) ContrastPill(stringResource(contrast.label))
            }
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
 * A starter's contrast as a small pill in its footer, such as "Medium".
 */
@Composable
private fun ContrastPill(text: String) {
    val tokens = LocalBuilderTokens.current
    Box(
        modifier = Modifier
            .height(PillHeight)
            .background(tokens.border, CircleShape)
            .padding(horizontal = tokens.spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        BuilderText(text = text, style = BuilderTextStyle.Label, maxLines = 1)
    }
}

/**
 * The check on the current card, in the accent with a halo of the dialog's surface around it.
 */
@Composable
private fun CheckBadge(modifier: Modifier = Modifier) {
    val tokens = LocalBuilderTokens.current
    Box(
        modifier = modifier
            .size(BadgeSize + BadgeHalo * 2)
            .background(tokens.panelRaised, CircleShape)
            .padding(BadgeHalo)
            .background(tokens.accent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BuilderIcon(IconId.Check, contentDescription = null, tint = tokens.onAccent, size = BadgeGlyph)
    }
}
