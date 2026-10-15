package com.materialkolor.builder.preview.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.Theme
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.MotionSchemeChoice
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.PaneLibraries
import com.materialkolor.builder.preview.PaneLibrary
import com.materialkolor.builder.preview.PreviewResult
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.unstyled.MaterialKolorTokens
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.math.abs
import kotlin.test.Test
import androidx.compose.material3.LocalContentColor as MaterialContentColor
import com.composeunstyled.LocalContentColor as UnstyledContentColor

/**
 * What a pane and the chrome around it said their primary and content colors were.
 */
private class Seen(
    var chromePrimary: Color? = null,
    var panePrimary: Color? = null,
    var materialContent: Color? = null,
    var unstyledContent: Color? = null,
)

@OptIn(ExperimentalTestApi::class)
class PreviewPaneTest {
    @Test
    fun pane_underARedChrome_wearsItsOwnPrimaryAndContentColor() {
        for (pane in PaneLibraries.filter { pane -> pane.library != Library.Fluent }) {
            withClue(pane) {
                runComposeUiTest {
                    val seen = Seen()
                    val spec = LightSpec.on(pane)
                    setContent {
                        Chrome(pane.expressive) {
                            seen.chromePrimary = MaterialTheme.colorScheme.primary
                            PreviewPane(spec) {
                                seen.panePrimary = panePrimaryOf(pane.library)
                                seen.materialContent = MaterialContentColor.current
                                seen.unstyledContent = UnstyledContentColor.current
                            }
                        }
                    }
                    waitForIdle()

                    val expectedPrimary = if (pane.library == Library.Custom) {
                        spec.result.customSlots[CustomSlot.Primary, false].toColor()
                    } else {
                        spec.result.roles[Role.Primary, false]
                            .argb
                            .toColor()
                    }
                    val expectedContent = if (pane.library == Library.Custom) {
                        spec.result.customSlots[CustomSlot.OnSurface, false].toColor()
                    } else {
                        spec.result.roles[Role.OnBackground, false]
                            .argb
                            .toColor()
                    }
                    seen.panePrimary shouldBe expectedPrimary
                    seen.chromePrimary shouldNotBe expectedPrimary
                    seen.materialContent shouldBe expectedContent
                    seen.unstyledContent shouldBe expectedContent
                }
            }
        }
    }

    @Test
    fun pane_withAPinAndAmoled_showsBothInMaterialAndUnstyled() {
        val pin = Argb(0x00C853)
        val document = ThemeDocument(
            seed = Argb(0x1E88E5),
            amoled = true,
            pins = mapOf(Role.Primary to RolePin(light = pin, dark = pin)),
        )
        val dark = PaneSpec(ThemeResolver().resolve(document), isDark = true, label = "Dark")
        val panes = listOf(
            PaneLibrary(Library.Material3),
            PaneLibrary(Library.Material3, expressive = true),
            PaneLibrary(Library.Unstyled),
        )
        for (pane in panes) {
            withClue(pane) {
                runComposeUiTest {
                    var primary: Color? = null
                    var background: Color? = null
                    setContent {
                        Chrome(pane.expressive) {
                            PreviewPane(dark.on(pane)) {
                                if (pane.library == Library.Unstyled) {
                                    val colors = Theme[MaterialKolorTokens.colors]
                                    primary = colors[MaterialKolorTokens.primary]
                                    background = colors[MaterialKolorTokens.background]
                                } else {
                                    primary = MaterialTheme.colorScheme.primary
                                    background = MaterialTheme.colorScheme.background
                                }
                            }
                        }
                    }
                    waitForIdle()

                    primary shouldBe pin.toColor()
                    background shouldBe Color.Black
                }
            }
        }
    }

    @Test
    fun materialPane_underTheBrandChrome_setsTextInTheLibraryDefaultNotTheBrandFace() {
        for (expressive in listOf(false, true)) {
            withClue("expressive = $expressive") {
                runComposeUiTest {
                    var chrome: TextStyle? = null
                    var pane: TextStyle? = null
                    setContent {
                        Chrome(expressive) {
                            chrome = MaterialTheme.typography.bodyLarge
                            PreviewPane(LightSpec.on(Library.Material3, expressive)) {
                                pane =
                                    MaterialTheme.typography.bodyLarge
                            }
                        }
                    }
                    waitForIdle()

                    pane?.fontFamily shouldNotBe chrome?.fontFamily
                    pane shouldBe Typography().bodyLarge
                }
            }
        }
    }

    @Test
    fun expressivePane_movesWithTheDocumentsMotionScheme() {
        val expected = mapOf(
            MotionSchemeChoice.Standard to MotionScheme.standard(),
            MotionSchemeChoice.Expressive to MotionScheme.expressive(),
        )
        for ((choice, motion) in expected) {
            withClue(choice) {
                runComposeUiTest {
                    val result = ThemeResolver().resolve(
                        ThemeDocument(seed = Argb(0x1E88E5), expressive = true, motionScheme = choice),
                    )
                    var seen: MotionScheme? = null
                    setContent {
                        Chrome(expressive = true) {
                            PreviewPane(PaneSpec(result, isDark = false, label = "Light")) {
                                seen = MaterialTheme.motionScheme
                            }
                        }
                    }
                    waitForIdle()

                    seen shouldBe motion
                }
            }
        }
    }

    @Test
    fun plainClickable_underEveryPaneInsideTheShellChrome_composesAndClicks() {
        for (pane in PaneLibraries) {
            withClue(pane) {
                runComposeUiTest {
                    var clicks = 0
                    setContent {
                        Chrome(expressive = true) {
                            PreviewPane(LightSpec.on(pane)) {
                                Box(Modifier.size(40.dp).testTag("tap").clickable { clicks++ })
                            }
                        }
                    }

                    onNodeWithTag("tap").performClick()
                    waitForIdle()

                    clicks shouldBe 1
                }
            }
        }
    }

    @Test
    fun filter_grayscale_drawsThePaneThroughIt() =
        runComposeUiTest {
            val grayscale = ColorMatrix().apply { setToSaturation(0f) }
            setContent {
                Chrome {
                    PreviewPane(
                        spec = PaneSpec(PreviewResult, isDark = false, label = "Gray", filter = grayscale),
                        modifier = Modifier.size(40.dp).testTag("pane"),
                    ) {
                        Box(Modifier.fillMaxSize().background(Color.Red))
                    }
                }
            }

            val pixels = onNodeWithTag("pane").captureToImage().toPixelMap()
            val pixel = pixels[pixels.width / 2, pixels.height / 2]

            withClue(pixel) {
                (abs(pixel.red - pixel.green) < 0.02f && abs(pixel.green - pixel.blue) < 0.02f) shouldBe true
                (pixel.red < 0.9f) shouldBe true
            }
        }
}

/**
 * The primary color a pane in [library] hands out.
 */
@Composable
private fun panePrimaryOf(library: Library): Color? =
    when (library) {
        Library.Material3, Library.Inklet -> MaterialTheme.colorScheme.primary
        Library.Unstyled -> Theme[MaterialKolorTokens.colors][MaterialKolorTokens.primary]
        Library.Fluent -> null
        Library.Custom -> LocalPreviewIdentity.current[CustomSlot.Primary]
    }
