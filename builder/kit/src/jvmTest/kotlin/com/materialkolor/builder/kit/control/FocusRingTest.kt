package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.kit.icon.IconId
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.shouldBeGreaterThan
import kotlin.test.Test

/**
 * A seed and style whose Material3 primary and secondary sit far apart, so the tab ring under the
 * primary focus layer drifts off the secondary focus colour by more than the ring tolerance.
 */
private val FarApartDocument: ThemeDocument = ThemeDocument(seed = Argb(0x0000FF), style = Style.Vibrant)

/** How far the focus layer has to pull the ring for [FarApartDocument] to test anything. */
private const val RingTolerance = 0.02f

/**
 * Every control rings when Tab lands on it, in every skin, with enough of the ring standing out 3 to 1
 * from what it covers (AR-01, S5 F6). Material3 keeps its own focus layers, so only ring pixels count.
 */
@OptIn(ExperimentalTestApi::class)
class FocusRingTest {
    @Test
    fun iconButton_everyEmphasis_ringsOnEverySide() {
        for (emphasis in Emphasis.entries) {
            withClue(emphasis.name) {
                forEachSkin { _, skin ->
                    val capture = tabOntoRing(skin) {
                        BuilderIconButton(
                            onClick = {},
                            icon = IconId.Copy,
                            contentDescription = "Copy",
                            emphasis = emphasis,
                        )
                    }
                    capture.shouldShowRing()
                    capture.shouldRingEverySide()
                }
            }
        }
    }

    @Test
    fun iconButton_inATooltip_ringsOnEverySide() =
        forEachSkin { _, skin ->
            val capture = tabOntoRing(skin) {
                BuilderTooltip("Copy the hex") {
                    BuilderIconButton(onClick = {}, icon = IconId.Copy, contentDescription = "Copy")
                }
            }
            capture.shouldShowRing()
            capture.shouldRingEverySide()
        }

    @Test
    fun checkbox_ringsOnEverySide() =
        forEachSkin { _, skin ->
            val capture = tabOntoRing(skin) {
                BuilderCheckbox(checked = true, onCheckedChange = {}, label = "Show tones")
            }
            capture.shouldShowRing()
            capture.shouldRingEverySide()
        }

    @Test
    fun switch_ringsOnEverySide() =
        forEachSkin { _, skin ->
            val capture = tabOntoRing(skin) {
                BuilderSwitch(checked = false, onCheckedChange = {}, label = "Dark theme")
            }
            capture.shouldShowRing()
            capture.shouldRingEverySide()
        }

    @Test
    fun tabs_selectedTab_ringsOnEverySide() =
        forEachSkin { _, skin ->
            val tabs = listOf("Light", "Dark", "Contrast")
            val capture = tabOntoRing(skin) {
                BuilderTabs(tabs = tabs, selected = "Dark", onSelect = {}, label = { tab -> tab })
            }
            capture.shouldShowRing()
            capture.shouldRingEverySide()
        }

    @Test
    fun tabs_material3OnAFarApartSeed_ringsUnderTheFocusLayer() {
        for ((name, skin) in ControlSkins.filter { (_, skin) -> skin.library == Library.Material3 }) {
            withClue(name) {
                runComposeUiTest {
                    val tabs = listOf("Light", "Dark", "Contrast")
                    val capture = tabOntoRing(skin, document = FarApartDocument) {
                        BuilderTabs(tabs = tabs, selected = "Dark", onSelect = {}, label = { tab -> tab })
                    }
                    capture.layerDrift shouldBeGreaterThan RingTolerance
                    capture.shouldShowRing()
                    capture.shouldRingEverySide()
                }
            }
        }
    }

    @Test
    fun disclosure_collapsed_ringsOnEverySide() =
        forEachSkin { _, skin ->
            val capture = tabOntoRing(skin) {
                BuilderDisclosure(expanded = false, onExpandedChange = {}, title = "Contrast") {
                    BuilderText("Standard")
                }
            }
            capture.shouldShowRing()
            capture.shouldRingEverySide()
        }

    @Test
    fun disclosure_expanded_ringsOnEverySide() =
        forEachSkin { _, skin ->
            val capture = tabOntoRing(skin) {
                BuilderDisclosure(expanded = true, onExpandedChange = {}, title = "Contrast") {
                    BuilderText("Standard")
                }
            }
            capture.shouldShowRing()
            capture.shouldRingEverySide()
        }

    /**
     * The thumb is small and round, so at one pixel per dp most of its ring is blended edge and too
     * few whole pixels are left to count. It is drawn at two instead of lowering the pixel line.
     */
    @Test
    fun slider_ringsTheThumbClearOfTheTrack_andRightStillMovesIt() =
        forEachSkin { _, skin ->
            var value by mutableFloatStateOf(0.5f)
            val capture = tabOntoRing(skin, density = 2f) {
                BuilderSlider(
                    value = value,
                    onValueChange = { next -> value = next },
                    label = "Chroma",
                    modifier = Modifier.width(240.dp),
                )
            }
            capture.shouldShowRing()
            capture.shouldClearTheTrack()
            onNode(isFocused()).performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            value shouldBeGreaterThan 0.5f
        }
}
