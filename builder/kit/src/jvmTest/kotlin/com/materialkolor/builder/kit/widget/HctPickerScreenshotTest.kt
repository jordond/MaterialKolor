package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

private const val PickerSheetTag = "hct-picker"

/** Where the recording job writes the baselines. Nothing is written unless a Roborazzi task turns capture on. */
private const val PickerScreenshotDir = "src/jvmTest/screenshots/hct-picker"

/** A saturated green, so the hue and tone tracks both show stretches sRGB cannot reach. */
private val PickerVividSeed: Argb = Argb(0x00C853)

@OptIn(ExperimentalTestApi::class)
class HctPickerScreenshotTest {
    @Test
    fun hctPicker_everySkinBothModes_rendersTracksAndFormatSwitch() =
        pickerForEachSkin { name, skin ->
            var isDark by mutableStateOf(false)
            setContent { PickerHarness(skin, isDark) { PickerSheet() } }

            for (dark in listOf(false, true)) {
                isDark = dark
                waitForIdle()
                val mode = if (dark) "dark" else "light"
                onNodeWithTag(PickerSheetTag).captureRoboImage("$PickerScreenshotDir/$name-$mode.png")
            }
        }
}

/** The picker on a panel, the way the side panel hosts it. */
@Composable
private fun PickerSheet() {
    val tokens = LocalBuilderTokens.current
    Box(
        Modifier
            .testTag(PickerSheetTag)
            .background(tokens.canvas)
            .padding(tokens.spacing.medium),
    ) {
        HctPicker(
            value = PickerVividSeed,
            onChange = { _, _ -> },
            modifier = Modifier
                .width(PickerWidth)
                .background(tokens.panel, RoundedCornerShape(tokens.radius.large))
                .padding(tokens.spacing.large),
        )
    }
}
