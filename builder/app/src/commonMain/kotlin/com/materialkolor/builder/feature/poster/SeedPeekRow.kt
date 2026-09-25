package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.engine.color.HctReadout
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.poster_hct
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import dev.stateholder.dispatcher.Dispatcher
import org.jetbrains.compose.resources.stringResource

/**
 * The seed as the sheet leads with it, the hex large with the seed's name and its HCT readout under
 * it, or beside it where there is room, and a round Shuffle at the end. It is what a phone sees of the poster at rest, even on its
 * side (D38).
 *
 * The hex here only reads. Editing it is the hero's job further down the sheet.
 */
@Composable
internal fun SeedPeekRow(
    context: PosterContext,
    dispatcher: Dispatcher<WorkspaceAction>,
    modifier: Modifier = Modifier,
) {
    val seed = context.document.seed
    val tokens = LocalBuilderTokens.current
    val spacing = tokens.spacing
    val hct = remember(seed) { HctReadout.of(seed).rounded() }
    // b-510
    val hex = LocalBuilderType.current.posterHero.merge(
        color = tokens.textStrong,
        fontSize = PeekHexSize,
        lineHeight = PeekHexLine,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The name and the readout go under the hex on an upright phone, and beside it on a phone
        // on its side, whose peek only has room for one line.
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(text = seed.toHex(), style = hex, maxLines = 1, overflow = TextOverflow.Clip)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                BuilderText(
                    text = remember(seed) { ColorNames.nameOf(seed) },
                    style = BuilderTextStyle.Label,
                    maxLines = 1,
                )
                BuilderText(
                    text = stringResource(Res.string.poster_hct, hct.hue, hct.chroma, hct.tone),
                    style = BuilderTextStyle.Value,
                    emphasis = Emphasis.Secondary,
                    maxLines = 1,
                )
            }
        }
        ShuffleIconButton(context, dispatcher, emphasis = Emphasis.Primary)
    }
}

// b-510

/**
 * How big the peek sets the hex, the phone design's size, under the docked hero's 72.
 */
private val PeekHexSize: TextUnit = 36.sp

/**
 * The line the peek's hex takes.
 */
private val PeekHexLine: TextUnit = 40.sp
