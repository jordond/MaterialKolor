package com.materialkolor.builder.feature.poster

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.KeyColors
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.SpecVersion
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.feature.workspace.FineTuneSection
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.kit.control.BuilderInsetSheetHost
import com.materialkolor.builder.kit.shell.InversePosterSurface
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val Plain = ThemeDocument(seed = Argb(0x6750A4))

private val Teal = Argb(0x00897B)

@OptIn(ExperimentalTestApi::class)
class FineTuneSheetTest {
    @Test
    fun button_nothingSet_saysTheColorsComeFromTheSeed() =
        runComposeUiTest {
            showSection(PosterHarness(Plain)) { context, dispatcher -> FineTuneButton(context, dispatcher) }

            onNodeWithContentDescription("Fine-tune, Colors from seed · 2021 spec").assertExists()
        }

    @Test
    fun button_summary_countsKeyColorsPinsAndExtraColors() =
        runComposeUiTest {
            val pins = mapOf(Role.OnPrimary to RolePin(light = Argb(0xFFFFFF)))
            val document = Plain.copy(
                keyColors = KeyColors(primary = Teal),
                pins = pins,
                spec = SpecVersion.Spec2025,
                accents = listOf(Accent(name = "brand", seed = Teal)),
            )
            showSection(PosterHarness(document)) { context, dispatcher -> FineTuneButton(context, dispatcher) }

            onNodeWithContentDescription("Fine-tune, 1 key color set, 1 pinned role · 2025 spec · 1 extra color")
                .assertExists()
        }

    @Test
    fun button_summary_namesTheSpecTheStyleReallyRuns() =
        runComposeUiTest {
            val document = Plain.copy(style = Style.Rainbow, spec = SpecVersion.Spec2025)
            showSection(PosterHarness(document)) { context, dispatcher -> FineTuneButton(context, dispatcher) }

            onNodeWithContentDescription("Fine-tune, Colors from seed · 2021 spec").assertExists()
        }

    @Test
    fun button_press_opensTheSheetAtItsTop() =
        runComposeUiTest {
            val harness = PosterHarness(Plain)
            showSection(harness) { context, dispatcher -> FineTuneButton(context, dispatcher) }

            onNodeWithContentDescription("Fine-tune", substring = true).performClick()
            waitForIdle()

            harness.actions shouldBe listOf(WorkspaceAction.OpenFineTune())
            harness.fineTune shouldBe FineTuneSection.Locks
            harness.undoEntries() shouldBe 0
        }

    @Test
    fun sheet_openedAtASection_scrollsItToTheTop() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val harness = PosterHarness(Plain)
            harness.fineTune = FineTuneSection.TargetOptions
            showSection(harness) { context, dispatcher ->
                BuilderInsetSheetHost(
                    sheet = { InversePosterSurface { FineTuneSheet(context, dispatcher) } },
                    modifier = Modifier.fillMaxSize(),
                ) {}
            }
            waitForIdle()

            onNodeWithText("Target options").assertIsDisplayed()
            onNodeWithText("Shuffle keeps").assertIsNotDisplayed()
        }

    @Test
    fun sheet_openedAtTheLocks_leavesTheStyleLockToTheStyle() =
        runDesktopComposeUiTest(width = WIDTH, height = HEIGHT) {
            val harness = PosterHarness(Plain)
            harness.fineTune = FineTuneSection.Locks
            showSection(harness) { context, dispatcher ->
                BuilderInsetSheetHost(
                    sheet = { InversePosterSurface { FineTuneSheet(context, dispatcher) } },
                    modifier = Modifier.fillMaxSize(),
                ) {}
            }
            waitForIdle()

            onNodeWithText("Shuffle keeps").assertIsDisplayed()
            onNodeWithContentDescription("Keep the hue when shuffling").assertIsDisplayed()
            onNodeWithContentDescription("Keep the seed when shuffling").assertIsDisplayed()
            onNodeWithContentDescription("Keep the style when shuffling").assertDoesNotExist()
        }

    private companion object {
        const val WIDTH = 400
        const val HEIGHT = 800
    }
}
