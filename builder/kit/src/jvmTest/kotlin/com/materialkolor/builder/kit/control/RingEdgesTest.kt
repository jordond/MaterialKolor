package com.materialkolor.builder.kit.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.poster.PosterColors
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.shell.PosterSurface
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.CustomActionStyles
import com.materialkolor.builder.kit.skin.headless.FocusRingOffset
import com.materialkolor.builder.kit.skin.headless.SegmentedStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.assertions.withClue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A light seed and a dark one, so the hero's ring is held to 3 to 1 on both kinds of poster.
 */
private val PosterSeeds: List<Argb> = listOf(Argb(0xF2E6A0), Argb(0x1B2A4A))

/**
 * The rings once found cut, shaded or matched at their edges, each checked side by side in every
 * skin. The ring covers at least half the middle of every side at 3 to 1.
 */
@OptIn(ExperimentalTestApi::class)
class RingEdgesTest {
    /**
     * The poster's headline marks focus with a line under it, one side only, so it rings too. On the
     * poster the ring takes the poster's ink, which has to stand 3 to 1 from the seed. The caret is
     * ink as well and sits over the edge of the text, so the ring is judged outside the box.
     */
    @Test
    fun heroField_onALightAndADarkPoster_materialSkins_ringsAllTheWayRound() {
        for (seed in PosterSeeds) {
            withClue(seed.toHex()) {
                forEachSkin { _, skin ->
                    // The poster, and so the hero, only ever stands in the shell's Material skin.
                    if (skin.library != SkinLibrary.Material3) return@forEachSkin
                    val poster = PosterColors.of(seed)
                    val capture = tabOntoRing(skin, ringColors = { listOf(poster.ink.toColor()) }) {
                        PosterSurface(poster) {
                            Box(Modifier.background(LocalBuilderTokens.current.panel).padding(8.dp)) {
                                BuilderHexField(
                                    value = seed,
                                    onCommit = { _, _ -> },
                                    label = "Seed",
                                    errorMessage = { _ -> "" },
                                    noteMessage = { _ -> "" },
                                    modifier = Modifier.width(320.dp),
                                    large = true,
                                )
                            }
                        }
                    }
                    capture.shouldShowRing()
                    capture.shouldCoverEverySide()
                    capture.shouldRingAllTheWayRoundOutside(capture.focused)
                }
            }
        }
    }

    /**
     * The row clips where it scrolls, and its ends keep room for the end tabs' rings.
     */
    @Test
    fun tabs_firstAndLastTab_everySkin_ringOnEverySide() {
        val tabs = listOf("Light", "Dark", "Contrast")
        for (end in listOf(tabs.first(), tabs.last())) {
            withClue(end) {
                forEachSkin { _, skin ->
                    val capture = tabOntoRing(skin) {
                        BuilderTabs(tabs = tabs, selected = end, onSelect = {}, label = { tab -> tab })
                    }
                    capture.shouldShowRing()
                    capture.shouldCoverEverySide()
                }
            }
        }
    }

    /**
     * A tooltip that opens with focus sits clear of the anchor's ring, its shadow included, so the
     * ring's top shows whole under the bubble. The anchor stands low enough in the window for the
     * shadow to fall as far as it does on a page.
     */
    @Test
    fun tooltip_inThePage_everySkin_leavesTheAnchorRingWhole() =
        forEachSkin { _, skin ->
            val overlays = PageOverlays()
            val whole = tabOntoRing(skin) {
                InThePage(overlays) {
                    BuilderTooltip("Copy the hex") {
                        BuilderIconButton(onClick = {}, icon = IconId.Copy, contentDescription = "Copy")
                    }
                }
            }
            whole.shouldShowABubbleAbove(overlays)
            val capture = whole.nearFocused(RingReach)
            capture.shouldShowRing()
            capture.shouldCoverEverySide()
        }

    /**
     * A tooltip with no room above its anchor opens below it, the other branch of its placement, and
     * there too it keeps clear of the anchor's ring, so the ring's bottom shows whole over the bubble.
     */
    @Test
    fun tooltip_atTheTopOfThePage_everySkin_opensBelowAndLeavesTheAnchorRingWhole() =
        forEachSkin { _, skin ->
            val overlays = PageOverlays()
            val whole = tabOntoRing(skin) {
                InThePage(overlays, bubbleBelow = true) {
                    BuilderTooltip("Copy the hex") {
                        BuilderIconButton(onClick = {}, icon = IconId.Copy, contentDescription = "Copy")
                    }
                }
            }
            whole.shouldShowABubbleBelow(overlays)
            val capture = whole.nearFocused(RingReach)
            capture.shouldShowRing()
            capture.shouldCoverEverySide()
        }

    /**
     * The sheet clips to its rounded top, and the handle's ring stands in from its sides.
     */
    @Test
    fun bottomSheet_handle_everySkin_ringsOnEverySideInsideTheSheet() =
        forEachSkin { _, skin ->
            val capture = tabOntoRing(skin) {
                Box(Modifier.size(360.dp, 240.dp)) {
                    TestBottomSheet(rememberBottomSheetState(), label = "Poster") { BuilderText("Poster body") }
                }
            }
            capture.shouldShowRing()
            capture.shouldCoverEverySide()
        }

    /**
     * An end option as round as the frame rings outside the frame's own outline, where a whole pixel
     * shows at one pixel per dp as well as at two.
     */
    @Test
    fun segmented_firstAndLastOption_everySkin_ringOnEverySideAtOneAndTwoPixelsPerDp() {
        val options = listOf("Hex", "RGB", "HSL")
        for (density in listOf(1f, 2f)) {
            for (end in listOf(options.first(), options.last())) {
                withClue("$end at $density") {
                    forEachSkin { _, skin ->
                        val capture = tabOntoRing(skin, density = density) {
                            BuilderSegmented(
                                options = options,
                                selected = end,
                                onSelect = {},
                                label = "Format",
                                optionLabel = { option -> option },
                            )
                        }
                        capture.shouldShowRing()
                        capture.shouldCoverEverySide()
                    }
                }
            }
        }
    }

    /**
     * An end option's ring stands the style's end offset off its rounded end alone. Its straight top
     * and bottom run in line with a middle option's ring, and its other end stands where a middle
     * option's does.
     */
    @Test
    fun segmented_endOption_custom_ringsFurtherOffTheRoundedEndOnly() {
        val options = listOf("Hex", "RGB", "HSL")
        val skin = Skin(SkinLibrary.Custom, expressive = false)
        var extra = 0f
        val middle = segmentRing(skin, options, focused = "RGB") { style ->
            extra = style.endRingOffset.value - FocusRingOffset.value
        }
        val first = segmentRing(skin, options, focused = "Hex") { _ -> }
        val last = segmentRing(skin, options, focused = "HSL") { _ -> }
        for ((name, end) in listOf("first" to first, "last" to last)) {
            withClue(name) {
                end.reach().top shouldBe (middle.reach().top plusOrMinus 1f)
                end.reach().bottom shouldBe (middle.reach().bottom plusOrMinus 1f)
            }
        }
        first.reach().left shouldBe (middle.reach().left + extra plusOrMinus 1f)
        first.reach().right shouldBe (middle.reach().right plusOrMinus 1f)
        last.reach().right shouldBe (middle.reach().right + extra plusOrMinus 1f)
        last.reach().left shouldBe (middle.reach().left plusOrMinus 1f)
    }
}

/**
 * Tabs onto [focused], the chosen option of a segmented row of [options] in [skin], in a test of its
 * own, handing [style] the row's segmented style.
 */
@OptIn(ExperimentalTestApi::class)
private fun segmentRing(
    skin: Skin,
    options: List<String>,
    focused: String,
    style: (SegmentedStyle) -> Unit,
): RingCapture {
    var capture: RingCapture? = null
    runComposeUiTest {
        capture = tabOntoRing(skin) {
            style(CustomActionStyles.segmented)
            BuilderSegmented(
                options = options,
                selected = focused,
                onSelect = {},
                label = "Format",
                optionLabel = { option -> option },
            )
        }
    }
    return checkNotNull(capture)
}

/**
 * How far the ring reaches past each side of the focused node, in pixels, at one pixel per dp.
 */
private fun RingCapture.reach(): Rect =
    Rect(
        left = focused.left - ringBounds.left,
        top = focused.top - ringBounds.top,
        right = ringBounds.right - focused.right,
        bottom = ringBounds.bottom - focused.bottom,
    )
