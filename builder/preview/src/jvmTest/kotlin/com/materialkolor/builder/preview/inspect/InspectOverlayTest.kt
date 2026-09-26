package com.materialkolor.builder.preview.inspect

import com.materialkolor.builder.kit.skin.SkinLibrary
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.PreviewResult
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A declared element for the card to pin to.
 */
private const val ELEMENT = "element"

/**
 * A clickable element beside it, where the card lands.
 */
private const val UNDER = "under"

/**
 * How far into the card's top start corner the click lands, clear of its actions.
 */
private const val INSET = 4f

/**
 * Actions that do nothing, for tests that only look at the card.
 */
private val NoActions: InspectActions =
    InspectActions(
        pinEnabled = true,
        onPin = { _, _, _ -> },
        onShowOnRamp = { _, _ -> },
        onJumpToKeyColor = { _ -> },
        onLeave = {},
    )

@OptIn(ExperimentalTestApi::class)
class InspectOverlayTest {
    @Test
    fun pressOnTheCard_underTheCustomSkin_neverReachesThePreviewUnderIt() = pressOnTheCardStaysOnIt(SkinLibrary.Custom)

    @Test
    fun pressOnTheCard_underTheMaterialSkin_neverReachesThePreviewUnderIt() =
        pressOnTheCardStaysOnIt(SkinLibrary.Material3)

    /**
     * Pin the card in the [library] skin to a small element with a clickable one beside it, where the
     * card lands, then click the card over the clickable one. It never hears of the click.
     */
    private fun pressOnTheCardStaysOnIt(library: SkinLibrary) =
        runComposeUiTest {
            var clicks = 0
            setContent {
                Chrome(Skin(library, expressive = false)) {
                    ProvideBuilderLayout(modifier = Modifier.size(600.dp, 400.dp)) {
                        InspectOverlay(
                            on = true,
                            result = PreviewResult,
                            shown = PreviewMode.Light,
                            split = SplitState(),
                            actions = NoActions,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Row {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .testTag(ELEMENT)
                                        .previewRoles(ColorRef.OfRole(Role.Primary), ColorRef.OfRole(Role.OnPrimary)),
                                )
                                Box(Modifier.size(300.dp).testTag(UNDER).clickable { clicks++ })
                            }
                        }
                    }
                }
            }
            waitForIdle()

            onNodeWithTag(ELEMENT).performClick()
            waitForIdle()
            clicks shouldBe 0
            val card = onNodeWithTag(INSPECT_CARD_TAG).getBoundsInRoot()
            val under = onNodeWithTag(UNDER).getBoundsInRoot()
            (card.left in under.left..under.right && card.top in under.top..under.bottom) shouldBe true

            onNodeWithTag(INSPECT_CARD_TAG).performTouchInput { click(Offset(INSET, INSET)) }
            waitForIdle()
            clicks shouldBe 0
            onNodeWithTag(INSPECT_CARD_TAG).assertExists()
        }
}
