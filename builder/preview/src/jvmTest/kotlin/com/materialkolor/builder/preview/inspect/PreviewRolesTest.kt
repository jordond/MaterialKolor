package com.materialkolor.builder.preview.inspect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.DarkSpec
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.split.PaneSide
import com.materialkolor.builder.preview.split.SplitPreview
import com.materialkolor.builder.preview.split.SplitState
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Card = listOf(ColorRef.OfRole(Role.SurfaceContainerLow), ColorRef.OfRole(Role.OnSurface))
private val Button = listOf(ColorRef.OfRole(Role.Primary), ColorRef.OfRole(Role.OnPrimary))

@OptIn(ExperimentalTestApi::class)
class PreviewRolesTest {
    @Test
    fun previewRoles_withInspectOff_stillWriteTheSemanticsKey() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(100.dp).testTag("button").previewRoles(*Button.toTypedArray()))
            }

            onNodeWithTag("button").assert(SemanticsMatcher.expectValue(PreviewRoles, Button))
        }

    @Test
    fun inspect_turnedOnAfterLayoutOffAndOnAgain_recordsEveryElementAndHitsTheSmallest() =
        runComposeUiTest {
            val registry = InspectRegistry()
            var inspect: InspectRegistry? by mutableStateOf(null)
            setContent {
                CompositionLocalProvider(LocalInspectRegistry provides inspect) {
                    Box(Modifier.size(200.dp).testTag("card").previewRoles(*Card.toTypedArray())) {
                        Box(Modifier.size(50.dp).testTag("button").previewRoles(*Button.toTypedArray()))
                    }
                }
            }
            waitForIdle()

            inspect = registry
            waitForIdle()
            val card = onNodeWithTag("card").fetchSemanticsNode().boundsInWindow
            val button = onNodeWithTag("button").fetchSemanticsNode().boundsInWindow

            registry.size shouldBe 2
            registry.hit(PaneSide.Start, button.center)?.roles shouldBe Button
            registry.hit(PaneSide.Start, Offset(card.right - 1f, card.bottom - 1f))?.roles shouldBe Card
            registry.hit(PaneSide.Start, Offset(card.right + 10f, card.bottom + 10f)).shouldBeNull()
            registry.hit(PaneSide.End, button.center).shouldBeNull()

            inspect = null
            waitForIdle()
            registry.size shouldBe 0

            val again = InspectRegistry()
            inspect = again
            waitForIdle()
            again.size shouldBe 2
            again.hit(PaneSide.Start, button.center)?.roles shouldBe Button
        }

    @Test
    fun registry_twoElementsOfOneSize_hitsTheOneRecordedLast() {
        val registry = InspectRegistry()
        val bounds = Rect(0f, 0f, 10f, 10f)
        val first = Any()
        val second = Any()

        registry.record(first, InspectEntry(PaneSide.Start, Card, bounds))
        registry.record(second, InspectEntry(PaneSide.Start, Button, bounds))
        registry.hit(PaneSide.Start, bounds.center)?.roles shouldBe Button

        registry.record(first, InspectEntry(PaneSide.Start, Card, bounds))
        registry.hit(PaneSide.Start, bounds.center)?.roles shouldBe Card
    }

    @Test
    fun inspect_inASplit_recordsEachCopyUnderItsSide() =
        runComposeUiTest {
            val registry = InspectRegistry()
            setContent {
                Chrome {
                    CompositionLocalProvider(LocalInspectRegistry provides registry) {
                        SplitPreview(
                            LightSpec,
                            DarkSpec,
                            SplitState(),
                            Modifier.size(400.dp, 300.dp).testTag("split"),
                        ) {
                            Box(Modifier.size(400.dp, 300.dp).previewRoles(*Card.toTypedArray()))
                        }
                    }
                }
            }
            waitForIdle()
            val center = onNodeWithTag("split").fetchSemanticsNode().boundsInWindow.center

            registry.size shouldBe 2
            registry.hit(PaneSide.Start, center)?.side shouldBe PaneSide.Start
            registry.hit(PaneSide.End, center)?.side shouldBe PaneSide.End
        }
}
