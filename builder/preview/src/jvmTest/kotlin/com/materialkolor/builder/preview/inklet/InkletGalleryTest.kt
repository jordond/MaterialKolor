package com.materialkolor.builder.preview.inklet

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.preview.Chrome
import com.materialkolor.builder.preview.LightSpec
import com.materialkolor.builder.preview.canvas.ComponentsTab
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.GALLERY_CARD
import com.materialkolor.builder.preview.canvas.PreviewPane
import com.materialkolor.builder.preview.on
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Wide enough for four columns and tall enough that every card composes.
 */
private val InkletWhole: Modifier = Modifier
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(1280.dp, 8000.dp)

@OptIn(ExperimentalTestApi::class)
class InkletGalleryTest {
    /**
     * The D40 guard. With the builder's motion frozen the pen's boil clock and the indeterminate
     * progress hold still, so the gallery settles, and no control, right click or long press on a
     * text field opens a popup or a window.
     */
    @Test
    fun gallery_motionFrozen_settlesAndOpensNoPopupOrWindow() =
        runComposeUiTest {
            val composed = mutableSetOf<String>()
            val toolbar = ToolbarProbe()
            val spec = LightSpec.on(Library.Inklet)
            val state = DemoAppState()
            setContent {
                val probe = remember {
                    { where: String -> if (where.startsWith(GALLERY_CARD)) composed += where.removePrefix(GALLERY_CARD) }
                }
                CompositionLocalProvider(
                    LocalMotionFrozen provides true,
                    LocalCompositionProbe provides probe,
                    LocalTextToolbar provides toolbar,
                ) {
                    Chrome {
                        PreviewPane(spec, InkletWhole) { ComponentsTab(spec, state) }
                    }
                }
            }
            waitForIdle()
            composed shouldBe InkletCards.map { card -> card.title }.toSet()

            val pressable = onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes()
            pressable.shouldNotBeEmpty()
            runOnIdle {
                for (node in pressable) {
                    if (SemanticsProperties.Disabled !in node.config) {
                        node.config
                            .getOrNull(SemanticsActions.OnClick)
                            ?.action
                            ?.invoke()
                    }
                }
            }
            waitForIdle()
            onAllNodes(isRoot()).assertCountEquals(1)

            // A word to select, so the field has a context menu and a text toolbar to open.
            val destination = onNode(hasSetTextAction() and hasText("Destination"))
            destination.performTextReplacement("Lisbon")
            destination.performMouseInput { rightClick() }
            waitForIdle()
            onAllNodes(isRoot()).assertCountEquals(1)
            destination.performTouchInput { longClick() }
            waitForIdle()
            onAllNodes(isRoot()).assertCountEquals(1)
            toolbar.shown shouldBe 0
        }
}

/**
 * A text toolbar that counts how often it is asked to show, where the web's would open a popup.
 */
private class ToolbarProbe : TextToolbar {
    var shown: Int = 0
        private set

    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        shown++
    }

    override fun hide() = Unit
}
