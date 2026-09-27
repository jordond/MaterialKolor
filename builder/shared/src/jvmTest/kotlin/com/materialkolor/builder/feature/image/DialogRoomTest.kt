package com.materialkolor.builder.feature.image

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.materialkolor.builder.LocalThemeResolver
import com.materialkolor.builder.LocalThemeResult
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * A phone on its side.
 */
private const val PHONE_WIDTH = 740
private const val PHONE_HEIGHT = 360

/**
 * A window tall enough that nothing in either dialog has to give way.
 */
private const val ROOMY_HEIGHT = 1200

@OptIn(ExperimentalTestApi::class)
class DialogRoomTest {
    // Taller than wide, so the picture would take all the height it could.
    private val picture = ImageBitmap(300, 400)

    @Test
    fun eyedropper_onAPhoneOnItsSide_keepsCancelWholeAndOnScreen() {
        assertButtonFits("Cancel") { Eyedropper() }
        runDesktopComposeUiTest(width = PHONE_WIDTH, height = PHONE_HEIGHT) {
            showDialog { Eyedropper() }
            onNodeWithTag(EYEDROPPER_PICTURE_TAG).getUnclippedBoundsInRoot().height shouldBeGreaterThan 0.dp
        }
    }

    @Test
    fun presets_onAPhoneOnItsSide_keepCloseWholeAndOnScreen() {
        assertButtonFits("Close") {
            PresetPicker(
                visible = true,
                document = ThemeDocument.Default,
                isDark = false,
                onChoose = { },
                onDismissRequest = { },
            )
        }
    }

    @Composable
    private fun Eyedropper() {
        val dispatcher = rememberDispatcher<WorkspaceAction> { }
        ImageEyedropper(visible = true, picture = picture, source = SeedSource.Typed, dispatcher = dispatcher)
    }

    /**
     * Checks the button named [label] in [dialog] stands as tall on a phone on its side as in a
     * roomy window, and sits inside the window.
     */
    private fun assertButtonFits(
        label: String,
        dialog: @Composable () -> Unit,
    ) {
        val roomy = buttonBounds(ROOMY_HEIGHT, label, dialog)
        val phone = buttonBounds(PHONE_HEIGHT, label, dialog)
        phone.height shouldBe roomy.height
        phone.bottom shouldBeLessThanOrEqualTo PHONE_HEIGHT.dp
    }

    /**
     * Where the button named [label] sits in [dialog], in a window [height] tall.
     */
    private fun buttonBounds(
        height: Int,
        label: String,
        dialog: @Composable () -> Unit,
    ): DpRect {
        var bounds = DpRect(0.dp, 0.dp, 0.dp, 0.dp)
        runDesktopComposeUiTest(width = PHONE_WIDTH, height = height) {
            showDialog(dialog)
            // B-548 The preset picker's Close is a glyph beside the title, named by its content description.
            bounds = onNode(hasText(label) or hasContentDescription(label)).assertIsDisplayed().getUnclippedBoundsInRoot()
        }
        return bounds
    }

    /**
     * Shows [dialog] in the builder's skin on a touch screen that fills the window.
     */
    private fun ComposeUiTest.showDialog(dialog: @Composable () -> Unit) {
        val resolver = ThemeResolver()
        val result = resolver.resolve(ThemeDocument.Default)
        setContent {
            BuilderTheme(
                expressive = false,
                result = result,
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(coarsePointer = true, modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalThemeResult provides result, LocalThemeResolver provides resolver) {
                        dialog()
                    }
                }
            }
        }
        waitForIdle()
    }
}
