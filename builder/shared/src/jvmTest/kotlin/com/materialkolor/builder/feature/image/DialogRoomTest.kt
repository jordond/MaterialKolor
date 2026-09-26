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
import androidx.compose.ui.test.onNodeWithText
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
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import dev.stateholder.dispatcher.rememberDispatcher
import io.kotest.assertions.withClue
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

/**
 * The Material dialog, and the headless one Custom draws and Material draws in the page on the web.
 */
private val Libraries = listOf(SkinLibrary.Material3, SkinLibrary.Custom)

@OptIn(ExperimentalTestApi::class)
class DialogRoomTest {
    // Taller than wide, so the picture would take all the height it could.
    private val picture = ImageBitmap(300, 400)

    @Test
    fun eyedropper_onAPhoneOnItsSide_keepsCancelWholeAndOnScreen() {
        Libraries.forEach { library ->
            assertButtonFits(library, "Cancel") { Eyedropper() }
            runDesktopComposeUiTest(width = PHONE_WIDTH, height = PHONE_HEIGHT) {
                showDialog(library) { Eyedropper() }
                withClue(library) {
                    onNodeWithTag(EYEDROPPER_PICTURE_TAG).getUnclippedBoundsInRoot().height shouldBeGreaterThan 0.dp
                }
            }
        }
    }

    @Test
    fun presets_onAPhoneOnItsSide_keepCloseWholeAndOnScreen() {
        Libraries.forEach { library ->
            assertButtonFits(library, "Close") {
                PresetPicker(
                    visible = true,
                    document = ThemeDocument.Default,
                    isDark = false,
                    onChoose = { },
                    onDismissRequest = { },
                )
            }
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
        library: SkinLibrary,
        label: String,
        dialog: @Composable () -> Unit,
    ) {
        val roomy = buttonBounds(library, ROOMY_HEIGHT, label, dialog)
        val phone = buttonBounds(library, PHONE_HEIGHT, label, dialog)
        withClue(library) {
            phone.height shouldBe roomy.height
            phone.bottom shouldBeLessThanOrEqualTo PHONE_HEIGHT.dp
        }
    }

    /**
     * Where the button named [label] sits in [dialog], in a window [height] tall.
     */
    private fun buttonBounds(
        library: SkinLibrary,
        height: Int,
        label: String,
        dialog: @Composable () -> Unit,
    ): DpRect {
        var bounds = DpRect(0.dp, 0.dp, 0.dp, 0.dp)
        runDesktopComposeUiTest(width = PHONE_WIDTH, height = height) {
            showDialog(library, dialog)
            bounds = onNodeWithText(label).assertIsDisplayed().getUnclippedBoundsInRoot()
        }
        return bounds
    }

    /**
     * Shows [dialog] in [library]'s skin on a touch screen that fills the window.
     */
    private fun ComposeUiTest.showDialog(
        library: SkinLibrary,
        dialog: @Composable () -> Unit,
    ) {
        val resolver = ThemeResolver()
        val result = resolver.resolve(ThemeDocument.Default)
        setContent {
            BuilderTheme(
                skin = Skin(library = library, expressive = false),
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
