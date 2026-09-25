package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

private const val SkeletonTag = "skeleton"
private const val ChipTag = "chip"

/**
 * A quarter pulse, where the skeleton reaches the strong border. The pulse starts halfway between
 * its two tones and comes back down the way it went up, so half a pulse lands on the tone it
 * started from.
 */
private const val QuarterPulseMillis = 300L

@OptIn(ExperimentalTestApi::class)
class SchemeChipSkeletonTest {
    @Test
    fun schemeChipSkeleton_everySkin_takesTheRoomOfAChipAndNoFocus() =
        forEachWidgetSkin { _, skin ->
            setContent {
                WidgetHarness(skin) {
                    Row {
                        SchemeChip(
                            primary = Color.Red,
                            secondaryContainer = Color.Green,
                            tertiaryContainer = Color.Blue,
                            selected = true,
                            onClick = {},
                            label = "Chip",
                            modifier = Modifier.testTag(ChipTag),
                        )
                        SchemeChipSkeleton(Modifier.testTag(SkeletonTag))
                    }
                }
            }

            val chip = onNodeWithTag(ChipTag).getUnclippedBoundsInRoot()
            val skeleton = onNodeWithTag(SkeletonTag).getUnclippedBoundsInRoot()
            skeleton.width shouldBe chip.width
            skeleton.height shouldBe chip.height
            skeleton.width shouldBe SchemeChipFootprint
            onAllNodes(isFocusable()).assertCountEquals(1)
        }

    @Test
    fun schemeChipSkeleton_everySkin_pulses() =
        forEachWidgetSkin { _, skin ->
            val (first, later) = centerAcrossAQuarterPulse(skin, reducedMotion = false)

            later shouldNotBe first
        }

    @Test
    fun schemeChipSkeleton_everySkinUnderReducedMotion_holdsStill() =
        forEachWidgetSkin { _, skin ->
            val (first, later) = centerAcrossAQuarterPulse(skin, reducedMotion = true)

            later shouldBe first
        }
}

/**
 * The skeleton's center color on a running clock, first and a quarter pulse later.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.centerAcrossAQuarterPulse(
    skin: Skin,
    reducedMotion: Boolean,
): Pair<Color, Color> {
    mainClock.autoAdvance = false
    setContent { RunningHarness(skin, reducedMotion) { SchemeChipSkeleton(Modifier.testTag(SkeletonTag)) } }
    mainClock.advanceTimeByFrame()
    val first = skeletonCenter()
    mainClock.advanceTimeBy(QuarterPulseMillis)
    return first to skeletonCenter()
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.skeletonCenter(): Color {
    val pixels = onNodeWithTag(SkeletonTag).captureToImage().toPixelMap()
    return pixels[pixels.width / 2, pixels.height / 2]
}

/**
 * A skin over [WidgetDocument] with motion left running, unlike [WidgetHarness].
 */
@Composable
private fun RunningHarness(
    skin: Skin,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val result = remember { ThemeResolver().resolve(WidgetDocument) }
    BuilderTheme(skin, result, isDark = false, reducedMotion = reducedMotion, content = content)
}
