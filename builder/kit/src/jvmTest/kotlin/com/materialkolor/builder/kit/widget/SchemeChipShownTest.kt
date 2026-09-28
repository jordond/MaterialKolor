package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.materialkolor.builder.kit.control.hasContentDescriptionExactly
import com.materialkolor.builder.kit.control.hasRole
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private const val PressableChipTag = "pressable-chip"
private const val ShownChipTag = "shown-chip"
private const val CardTag = "card"

@OptIn(ExperimentalTestApi::class)
class SchemeChipShownTest {
    @Test
    fun shownSchemeChip_everySkin_takesTheRoomOfAChipAndLeavesThePressAndTheNameToItsCard() =
        forEachWidgetSkin { _, skin ->
            var presses = 0
            setContent {
                WidgetHarness(skin) {
                    Row {
                        SchemeChip(
                            primary = Color.Red,
                            secondaryContainer = Color.Green,
                            tertiaryContainer = Color.Blue,
                            selected = false,
                            onClick = {},
                            label = "Tonal spot",
                            modifier = Modifier.testTag(PressableChipTag),
                        )
                        Box(
                            Modifier
                                .testTag(CardTag)
                                .clickable(interactionSource = null, indication = null) { presses += 1 }
                                .semantics { contentDescription = "Vibrant starter" },
                        ) {
                            SchemeChip(Color.Red, Color.Green, Color.Blue, Modifier.testTag(ShownChipTag))
                        }
                    }
                }
            }

            val pressable = onNodeWithTag(PressableChipTag).getUnclippedBoundsInRoot()
            val shownChip = onNodeWithTag(ShownChipTag, useUnmergedTree = true)
            val shown = shownChip.getUnclippedBoundsInRoot()
            shown.width shouldBe pressable.width
            shown.height shouldBe pressable.height
            shown.width shouldBe SchemeChipFootprint

            // A background or a clip notes its shape, which nothing reads out, and the tag is the test's.
            val unheard = setOf(SemanticsProperties.Shape, SemanticsProperties.TestTag)
            val heard = generateSequence(listOf(shownChip.fetchSemanticsNode())) { level ->
                level.flatMap { node -> node.children }.ifEmpty { null }
            }.flatten()
                .flatMap { node -> node.config.map { entry -> entry.key } }
                .filterNot { key -> key in unheard }
                .toList()
            heard.shouldBeEmpty()
            onAllNodes(isFocusable()).assertCountEquals(2)
            onAllNodes(hasRole(Role.RadioButton)).assertCountEquals(1)
            onNodeWithTag(CardTag).assert(hasContentDescriptionExactly("Vibrant starter"))

            shownChip.performClick()
            waitForIdle()
            presses shouldBe 1
        }
}
