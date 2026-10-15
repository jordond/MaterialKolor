package com.materialkolor.builder.kit.skin.custom

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.HostOverlays
import com.materialkolor.builder.kit.control.forEachSkin
import com.materialkolor.builder.kit.control.hasOverlayPaneTitle
import com.materialkolor.builder.kit.headless.LocalOverlayHost
import com.materialkolor.builder.kit.headless.OverlayHostState
import com.materialkolor.builder.kit.headless.OverlayKind
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class CustomPaneThemeTest {
    @Test
    fun paneDialog_inTree_wearsThePaneSlotsInAHostOfItsOwnClippedToThePane() =
        forEachSkin { _, skin ->
            val pane = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x006A6A)))
            val paneAccent = pane.customSlots[CustomSlot.Primary, false].toColor()
            lateinit var chrome: OverlayHostState
            var paneHost: OverlayHostState? = null
            var dialogAccent: Color? = null
            setContent {
                HostOverlays(skin, inTree = true) {
                    chrome = checkNotNull(LocalOverlayHost.current)
                    Column {
                        Box(Modifier.testTag("chrome").size(40.dp).focusable())
                        Box(Modifier.testTag("pane").size(width = 400.dp, height = 320.dp)) {
                            CustomPaneTheme(pane.customSlots, isDark = false, reducedMotion = false) {
                                paneHost = LocalOverlayHost.current
                                Box(Modifier.fillMaxSize())
                                BuilderDialog(visible = true, onDismissRequest = {}, title = "Rename") {
                                    dialogAccent = LocalBuilderTokens.current.accent
                                    BuilderText("Sunset")
                                }
                            }
                        }
                    }
                }
            }
            waitForIdle()
            val own = checkNotNull(paneHost)
            own shouldNotBe chrome
            own.layers.map { it.kind } shouldBe listOf(OverlayKind.Modal)
            chrome.layers.size shouldBe 0
            dialogAccent shouldBe paneAccent
            onNodeWithTag("chrome").assertExists()
            val paneBounds = onNodeWithTag("pane").getBoundsInRoot()
            val dialog = onNode(hasOverlayPaneTitle("Rename")).assertExists().getBoundsInRoot()
            (dialog.left >= paneBounds.left && dialog.right <= paneBounds.right) shouldBe true
            (dialog.top >= paneBounds.top && dialog.bottom <= paneBounds.bottom) shouldBe true
        }
}
