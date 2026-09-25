package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onNodeWithText
import com.materialkolor.builder.kit.a11y.Announcer
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.skin.Skin
import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ToastAnnouncerTest {
    @Test
    fun toastUnderAModal_folding_isAnnouncedOnceWithoutALiveRegion() =
        hostEachWay { skin, inTree ->
            val heard = mutableListOf<String>()
            val toasts = BuilderToastHostState()
            hostToastsUnderADialog(skin, inTree, folds = true, toasts) { message -> heard += message }
            toasts.show("Deleted Sunset", "Undo", ToastDuration.Indefinite) {}
            waitForIdle()
            onNodeWithText("Deleted Sunset").assertExists()
            toasts.show("Copied", duration = ToastDuration.Indefinite)
            waitForIdle()
            heard shouldBe listOf("Deleted Sunset, Undo", "Copied")
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).assertCountEquals(0)
        }

    @Test
    fun toast_notFolding_keepsTheLiveRegionAndAnnouncesNothing() =
        hostEachWay { skin, inTree ->
            val heard = mutableListOf<String>()
            val toasts = BuilderToastHostState()
            hostToastsUnderADialog(skin, inTree, folds = false, toasts) { message -> heard += message }
            toasts.show("Copied", duration = ToastDuration.Indefinite)
            waitForIdle()
            heard shouldBe emptyList()
            onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty() shouldBe true
        }
}

/**
 * A toast host and an open dialog over it, with [folds] as the D37 flag and [announcer] to hear the toasts.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.hostToastsUnderADialog(
    skin: Skin,
    inTree: Boolean,
    folds: Boolean,
    toasts: BuilderToastHostState,
    announcer: Announcer,
) {
    setContent {
        HostOverlays(skin, inTree) {
            CompositionLocalProvider(
                LocalFoldsStateIntoName provides folds,
                LocalAnnouncer provides announcer,
            ) { HostToastsAndDialog(toasts) }
        }
    }
    waitForIdle()
}

@Composable
private fun HostToastsAndDialog(toasts: BuilderToastHostState) {
    Box(Modifier.fillMaxSize()) {
        BuilderToastHost(toasts)
        BuilderDialog(visible = true, onDismissRequest = {}, title = "Export") { BuilderText("Kotlin") }
    }
}
