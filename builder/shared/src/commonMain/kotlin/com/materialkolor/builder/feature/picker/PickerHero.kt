package com.materialkolor.builder.feature.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ColorNames
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.picker_eyedropper
import com.materialkolor.builder.generated.resources.picker_go_back
import com.materialkolor.builder.generated.resources.picker_go_back_seed
import com.materialkolor.builder.generated.resources.picker_was
import com.materialkolor.builder.generated.resources.picker_was_seed
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderPressable
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.BuilderType
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import com.materialkolor.builder.kit.token.readableInk
import org.jetbrains.compose.resources.stringResource

/**
 * The band across the top of the color picker, filled edge to edge with the color it shows.
 *
 * On it sit the target's title in small capitals, the hex large in the poster's display type and the
 * color's name under it, all following every drag, with Pick from screen as a round button at the
 * top end. Everything on the band is in the ink that reads on the color. At the end sits Was, a strip
 * in the color the target had when the picker opened, which goes back to it and keeps the picker
 * open. It says "From the seed" when the target had no color of its own.
 *
 * The band is the color itself in both modes, only the panel under it follows dark mode.
 *
 * @param[title] What the picker is called. It shows in capitals, and the pane keeps it as written.
 * @param[color] The color the picker shows.
 * @param[was] The color the target had at open.
 * @param[wasFromSeed] Whether that color came from the seed, the target storing none of its own.
 * @param[onGoBack] Called when Was is pressed.
 * @param[onPickScreen] Starts Pick from screen, or null where there is no eyedropper.
 * @param[compact] Whether the band tops a phone's full screen sheet, which sets it smaller.
 */
@Composable
internal fun PickerHero(
    title: String,
    color: Argb,
    was: Argb,
    wasFromSeed: Boolean,
    onGoBack: () -> Unit,
    onPickScreen: (() -> Unit)?,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val type = LocalBuilderType.current
    val metrics = if (compact) CompactHero else WideHero
    val styles = remember(type, metrics) { HeroStyles.of(type, metrics) }
    val fill = color.toColor()
    val ink = readableInk(fill)
    val hex = color.toHex()
    val name = remember(color) { ColorNames.nameOf(color) }
    val padding = if (compact) {
        PaddingValues(spacing.large)
    } else {
        PaddingValues(start = spacing.extraLarge, top = spacing.extraLarge, end = spacing.large, bottom = spacing.extraLarge)
    }
    Row(modifier.fillMaxWidth().height(metrics.height)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().background(fill).padding(padding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The pane is named by the title as written, so the capitals are for the eye only.
                BasicText(
                    text = title.uppercase(),
                    modifier = Modifier.weight(1f).clearAndSetSemantics { },
                    style = styles.title.copy(color = ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onPickScreen != null) EyedropperButton(onPickScreen, ink)
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                BasicText(text = hex, style = styles.hex.copy(color = ink), maxLines = 1, softWrap = false)
                BasicText(
                    text = name,
                    style = styles.name.copy(color = ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        WasButton(
            was = was,
            fromSeed = wasFromSeed,
            onGoBack = onGoBack,
            line = ink.copy(alpha = HairlineAlpha),
            styles = styles,
            compact = compact,
            modifier = Modifier.width(metrics.wasWidth).fillMaxHeight(),
        )
    }
}

/**
 * Pick from screen, a round button on the band in a soft wash of its ink.
 */
@Composable
private fun EyedropperButton(
    onClick: () -> Unit,
    ink: Color,
) {
    BuilderPressable(
        onClick = onClick,
        label = stringResource(Res.string.picker_eyedropper),
        modifier = Modifier.size(EyedropperSize).background(ink.copy(alpha = WashAlpha), CircleShape),
        shape = CircleShape,
    ) { BuilderIcon(IconId.Eyedropper, contentDescription = null, tint = ink) }
}

/**
 * Was, one button in the color the target had at open, with an undo glyph over "WAS" and the hex or
 * "From the seed". Its ink reads on that color, and a hairline in the band's ink marks its start.
 */
@Composable
private fun WasButton(
    was: Argb,
    fromSeed: Boolean,
    onGoBack: () -> Unit,
    line: Color,
    styles: HeroStyles,
    compact: Boolean,
    modifier: Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val fill = was.toColor()
    val ink = readableInk(fill)
    val hex = was.toHex()
    val label = if (fromSeed) {
        stringResource(Res.string.picker_go_back_seed)
    } else {
        stringResource(Res.string.picker_go_back, hex)
    }
    val detail = if (fromSeed) stringResource(Res.string.picker_was_seed) else hex
    val padding = if (compact) {
        PaddingValues(horizontal = spacing.medium, vertical = spacing.large)
    } else {
        PaddingValues(horizontal = spacing.large, vertical = spacing.extraLarge)
    }
    Box(modifier.background(fill)) {
        BuilderPressable(
            onClick = onGoBack,
            label = label,
            modifier = Modifier.fillMaxSize(),
            shape = RectangleShape,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                BuilderIcon(IconId.Undo, contentDescription = null, tint = ink)
                Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                    BasicText(
                        text = stringResource(Res.string.picker_was).uppercase(),
                        style = styles.wasLabel.copy(color = ink),
                        maxLines = 1,
                    )
                    BasicText(text = detail, style = styles.wasDetail.copy(color = ink))
                }
            }
        }
        Box(Modifier.align(Alignment.CenterStart).width(HairlineWidth).fillMaxHeight().background(line))
    }
}

/**
 * How big the band is set, in a dialog or at the top of a phone's sheet.
 *
 * @property[height] The band's height.
 * @property[hexSize] The hex's type size.
 * @property[wasWidth] How wide Was is.
 * @property[compact] Whether this is the phone's band, which sets the small type a step smaller.
 */
@Immutable
private data class HeroMetrics(
    val height: Dp,
    val hexSize: TextUnit,
    val wasWidth: Dp,
    val compact: Boolean,
)

private val WideHero = HeroMetrics(height = 184.dp, hexSize = 80.sp, wasWidth = 136.dp, compact = false)
private val CompactHero = HeroMetrics(height = 172.dp, hexSize = 52.sp, wasWidth = 92.dp, compact = true)

/**
 * The band's type, over the builder's own faces.
 */
@Immutable
private data class HeroStyles(
    val title: TextStyle,
    val hex: TextStyle,
    val name: TextStyle,
    val wasLabel: TextStyle,
    val wasDetail: TextStyle,
) {
    companion object {
        fun of(
            type: BuilderType,
            metrics: HeroMetrics,
        ): HeroStyles {
            val compact = metrics.compact
            val capitals = TextStyle(fontWeight = FontWeight.Bold, letterSpacing = if (compact) 1.4.sp else 1.6.sp)
            return HeroStyles(
                title = type.value.merge(capitals).merge(
                    TextStyle(fontSize = if (compact) 12.sp else 13.sp, lineHeight = if (compact) 18.sp else 20.sp),
                ),
                hex = type.posterHero.merge(
                    TextStyle(
                        fontSize = metrics.hexSize,
                        lineHeight = metrics.hexSize,
                        letterSpacing = if (compact) (-2).sp else (-3).sp,
                    ),
                ),
                name = type.title.merge(
                    TextStyle(
                        fontSize = if (compact) 17.sp else 20.sp,
                        lineHeight = if (compact) 22.sp else 26.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                ),
                wasLabel = type.value.merge(capitals).merge(
                    TextStyle(fontSize = if (compact) 11.sp else 12.sp, lineHeight = if (compact) 14.sp else 16.sp),
                ),
                wasDetail = type.value.merge(
                    TextStyle(fontSize = if (compact) 12.sp else 14.sp, lineHeight = if (compact) 16.sp else 18.sp),
                ),
            )
        }
    }
}

/**
 * The round Pick from screen button.
 */
private val EyedropperSize: Dp = 48.dp

/**
 * The line between the band and Was.
 */
private val HairlineWidth: Dp = 1.dp

/**
 * How strong the band's ink is in the hairline, and in the wash behind Pick from screen.
 */
private const val HairlineAlpha: Float = 0.28f
private const val WashAlpha: Float = 0.14f
