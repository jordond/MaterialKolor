package com.materialkolor.sample.customtheme.ui.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.onTone
import com.materialkolor.ktx.toneColor
import com.materialkolor.palettes.TonalPalette
import com.materialkolor.sample.customtheme.theme.AppColors
import com.materialkolor.sample.customtheme.theme.AppPalettes
import com.materialkolor.sample.customtheme.theme.LocalAppColors
import com.materialkolor.sample.customtheme.ui.component.AppType
import com.materialkolor.sample.customtheme.ui.component.Rule
import com.materialkolor.sample.customtheme.ui.component.Text
import com.materialkolor.sample.customtheme.ui.component.TornShape
import com.materialkolor.sample.customtheme.ui.component.drawRegistrationMark
import com.materialkolor.sample.customtheme.ui.component.halftone
import com.materialkolor.sample.customtheme.ui.component.ink

@Immutable
internal data class Ink(
    val name: String,
    val palette: TonalPalette,
    val tone: Int,
    val color: Color,
)

internal fun AppPalettes.inks(colors: AppColors): List<Ink> =
    listOf(
        Ink("Key ink", scheme.primaryPalette, tones.ink, colors.ink),
        Ink("Fluorescent pink", pink, tones.pink, colors.pink),
        Ink("Blue", blue, tones.blue, colors.blue),
        Ink("Yellow", yellow, tones.yellow, colors.yellow),
        Ink("Stock", stock, tones.paper, colors.paper),
    )

@Composable
internal fun InkRow(ink: Ink) {
    val colors = LocalAppColors.current
    val tones = (RampTones + ink.tone).distinct().sorted()

    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.width(150.dp),
        ) {
            Text(
                text = ink.name.uppercase(),
                style = AppType.Label,
                color = colors.ink,
            )

            Text(
                text = "TONE ${ink.tone}",
                style = AppType.Caption,
                color = colors.inkSoft,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
        ) {
            Row(modifier = Modifier.border(Rule, colors.ink)) {
                for (tone in tones) {
                    ToneTile(
                        palette = ink.palette,
                        tone = tone,
                        isPrinted = tone == ink.tone,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (screen in ScreenShares) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .halftone(color = ink.color, colors = colors, cell = 5.dp) { _, _ -> screen }
                            .border(1.dp, colors.paperEdge),
                    ) {
                        Text(
                            text = "${(screen * 100).toInt()}%",
                            style = AppType.Caption,
                            color = colors.ink,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .background(colors.paper)
                                .padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToneTile(
    palette: TonalPalette,
    tone: Int,
    isPrinted: Boolean,
    modifier: Modifier = Modifier,
) {
    val content = palette.onTone(tone)

    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier
            .height(52.dp)
            .background(palette.toneColor(tone))
            .drawBehind {
                if (isPrinted) {
                    drawRegistrationMark(
                        color = content,
                        center = center.copy(y = size.height * MARK_HEIGHT),
                        radius = size.minDimension * MARK_SHARE,
                    )
                }
            }.padding(4.dp),
    ) {
        Text(
            text = tone.toString(),
            style = AppType.Caption,
            color = content,
        )
    }
}

/**
 * The three spot inks overlapping, the classic overprint diagram.
 */
@Composable
internal fun InkVenn() {
    val colors = LocalAppColors.current
    val circles = listOf(
        colors.pink to Pair(0.dp, 0.dp),
        colors.blue to Pair(64.dp, 0.dp),
        colors.yellow to Pair(32.dp, 54.dp),
    )

    Box(modifier = Modifier.size(width = 184.dp, height = 174.dp)) {
        for ((ink, position) in circles) {
            Box(
                modifier = Modifier
                    .offset(x = position.first, y = position.second)
                    .size(120.dp)
                    .ink(ink, colors, shape = CircleShape),
            )
        }
    }
}

@Composable
internal fun OverprintGrid(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val inks = listOf(
        "Primary" to colors.primary,
        "Pink" to colors.pink,
        "Blue" to colors.blue,
        "Yellow" to colors.yellow,
    )
    val pairs = inks.flatMapIndexed { index, first -> inks.drop(index + 1).map { second -> first to second } }

    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier,
    ) {
        for (row in pairs.chunked(PAIRS_PER_ROW)) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                for ((first, second) in row) {
                    OverprintPair(
                        first = first.second,
                        second = second.second,
                        label = "${first.first} × ${second.first}",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun OverprintPair(
    first: Color,
    second: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        Box(modifier = Modifier.size(width = 84.dp, height = 60.dp)) {
            Box(Modifier.size(56.dp, 44.dp).ink(first, colors))
            Box(
                modifier = Modifier
                    .offset(x = 28.dp, y = 16.dp)
                    .size(56.dp, 44.dp)
                    .ink(second, colors),
            )
        }

        Text(
            text = label.uppercase(),
            style = AppType.Caption,
            color = colors.inkSoft,
        )
    }
}

/**
 * The paper and ink slots as torn scraps, then the role colors as they come from the scheme.
 */
@Composable
internal fun StockRow() {
    val colors = LocalAppColors.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScrapRow(
            scraps = listOf(
                Scrap("Paper", colors.paper, colors.ink),
                Scrap("Shade", colors.paperShade, colors.ink),
                Scrap("Edge", colors.paperEdge, colors.ink),
                Scrap("Ink", colors.ink, colors.paper),
                Scrap("Soft ink", colors.inkSoft, colors.paper),
                Scrap("Highlight", colors.highlight, colors.ink),
            ),
        )

        ScrapRow(
            scraps = listOf(
                Scrap("Primary", colors.primary, colors.onPrimary),
                Scrap("Error", colors.error, colors.onError),
                Scrap("Scrim", colors.scrim, Color.White),
            ),
            firstSeed = STOCK_SCRAPS,
        )
    }
}

@Composable
private fun ScrapRow(
    scraps: List<Scrap>,
    firstSeed: Int = 0,
) {
    val colors = LocalAppColors.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        scraps.forEachIndexed { index, scrap ->
            val shape = TornShape(seed = firstSeed + index + 1, tooth = 4.dp)
            Box(
                contentAlignment = Alignment.BottomStart,
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .background(scrap.color, shape)
                    .border(1.dp, colors.paperEdge, shape)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            ) {
                Text(
                    text = scrap.name.uppercase(),
                    style = AppType.Caption,
                    color = scrap.content,
                    maxLines = 1,
                )
            }
        }
    }
}

private data class Scrap(
    val name: String,
    val color: Color,
    val content: Color,
)

private val RampTones = listOf(5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95, 99)
private val ScreenShares = listOf(1f, 0.7f, 0.5f, 0.3f, 0.15f, 0.06f)

private const val PAIRS_PER_ROW = 3
private const val STOCK_SCRAPS = 6
private const val MARK_SHARE = 0.26f
private const val MARK_HEIGHT = 0.4f
