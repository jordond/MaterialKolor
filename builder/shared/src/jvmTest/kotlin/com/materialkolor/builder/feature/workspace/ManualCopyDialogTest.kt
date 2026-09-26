package com.materialkolor.builder.feature.workspace

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinLibrary
import kotlin.test.Test

private const val FILE = "val primary = Color(0xFF6750A4)\nval onPrimary = Color(0xFFFFFFFF)"

private const val HEX = "#6750A4"

private const val SELECT_HINT = "Select the text below"

/**
 * The manual copy dialog's hint. A finger on the web cannot select text over several
 * lines, so there the hint sends it to the zip button or back to Copy, and everywhere else it says
 * to select the text.
 */
@OptIn(ExperimentalTestApi::class)
class ManualCopyDialogTest {
    @Test
    fun file_onATouchScreen_sendsAFingerToTheZipButton() =
        runComposeUiTest {
            show(FILE, coarsePointer = true, saveLabel = "Download zip")

            onNode(hasText("use Download zip to get the files", substring = true)).assertExists()
            onNode(hasText(SELECT_HINT, substring = true)).assertDoesNotExist()
        }

    @Test
    fun severalLines_onATouchScreenWithNoZip_sendAFingerBackToCopy() =
        runComposeUiTest {
            show(FILE, coarsePointer = true, saveLabel = null)

            onNode(hasText("try Copy again", substring = true)).assertExists()
            onNode(hasText(SELECT_HINT, substring = true)).assertDoesNotExist()
        }

    @Test
    fun oneLine_onATouchScreen_saysToSelectIt() =
        runComposeUiTest {
            show(HEX, coarsePointer = true, saveLabel = null)

            onNode(hasText(SELECT_HINT, substring = true)).assertExists()
        }

    @Test
    fun file_withAMouse_saysToSelectIt() =
        runComposeUiTest {
            show(FILE, coarsePointer = false, saveLabel = "Download zip")

            onNode(hasText(SELECT_HINT, substring = true)).assertExists()
        }

    private fun ComposeUiTest.show(
        text: String,
        coarsePointer: Boolean,
        saveLabel: String?,
    ) {
        setContent {
            BuilderTheme(
                skin = Skin(library = SkinLibrary.Material3, expressive = false),
                result = ThemeResolver().resolve(ThemeDocument.Default),
                isDark = false,
                reducedMotion = true,
            ) {
                ProvideBuilderLayout(coarsePointer = coarsePointer, modifier = Modifier.fillMaxSize()) {
                    ManualCopyDialog(visible = true, text = text, onDismissRequest = {}, saveLabel = saveLabel)
                }
            }
        }
        waitForIdle()
    }
}
