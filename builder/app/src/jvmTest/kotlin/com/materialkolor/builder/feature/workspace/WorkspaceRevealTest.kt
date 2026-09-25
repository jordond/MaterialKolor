package com.materialkolor.builder.feature.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.transition.RevealStyle
import com.materialkolor.builder.kit.transition.SkinTransition
import com.materialkolor.builder.kit.transition.SkinTransitionHost
import com.materialkolor.builder.kit.transition.rememberSkinTransition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.test.Test

/**
 * How long the checks give a reveal, well inside the 300 ms it waits on a font at most.
 */
private const val SHORT_WAIT_MS = 100L

/**
 * The reveal round a discrete change waits on Fluent's face only when the change lands on Fluent,
 * so a switch to Fluent comes in set in Selawik and nothing else waits on a font.
 */
@OptIn(ExperimentalTestApi::class)
class WorkspaceRevealTest {
    @Test
    fun reveal_aSwitchToFluent_waitsOnTheFluentFaceBeforeTheChange() =
        runComposeUiTest {
            val host = showHost()
            val face = CompletableDeferred<Unit>()
            var waited = 0
            var applied = false

            revealOn(
                host = host,
                change = DocumentChange.SetLibrary(Library.Fluent, expressive = false),
                fluentFace = {
                    waited++
                    face.await()
                },
            ) { applied = true }
            mainClock.advanceTimeBy(SHORT_WAIT_MS)

            waited shouldBe 1
            applied shouldBe false

            face.complete(Unit)
            mainClock.advanceTimeBy(SHORT_WAIT_MS)
            applied shouldBe true
        }

    @Test
    fun reveal_aWholeDocumentInFluent_waitsOnTheFluentFace() =
        runComposeUiTest {
            val host = showHost()
            var waited = 0

            val change = DocumentChange.Replace(ThemeDocument.Default.copy(library = Library.Fluent))
            revealOn(host, change, fluentFace = { waited++ }) {}
            mainClock.advanceTimeBy(SHORT_WAIT_MS)

            waited shouldBe 1
        }

    @Test
    fun reveal_anyOtherChange_neverWaitsOnTheFluentFace() =
        runComposeUiTest {
            val host = showHost()
            val others = listOf(
                DocumentChange.SetLibrary(Library.Material3, expressive = false),
                DocumentChange.SetLibrary(Library.Unstyled, expressive = false),
                DocumentChange.SetSeed(Argb(0xD9653B), SeedSource.Typed),
                DocumentChange.Replace(ThemeDocument.Default.copy(library = Library.Material3)),
            )
            for (change in others) {
                withClue(change) {
                    var waited = 0
                    var applied = false

                    revealOn(
                        host = host,
                        change = change,
                        fluentFace = {
                            waited++
                            awaitCancellation()
                        },
                    ) { applied = true }
                    mainClock.advanceTimeBy(SHORT_WAIT_MS)

                    waited shouldBe 0
                    applied shouldBe true
                }
            }
        }
}

/**
 * A transition over a small host, and a scope to launch its reveals in.
 */
private class RevealHost(
    val transition: SkinTransition,
    val scope: CoroutineScope,
)

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showHost(): RevealHost {
    mainClock.autoAdvance = false
    var host: RevealHost? = null
    setContent {
        BuilderTheme(
            skin = Skin(library = Library.Material3, expressive = false),
            result = ThemeResolver().resolve(ThemeDocument.Default),
            isDark = false,
            reducedMotion = true,
        ) {
            val transition = rememberSkinTransition()
            host = RevealHost(transition, rememberCoroutineScope())
            SkinTransitionHost(transition = transition, modifier = Modifier.size(100.dp)) { Box(Modifier.size(100.dp)) }
        }
    }
    mainClock.advanceTimeByFrame()
    return checkNotNull(host)
}

/**
 * Launches the reveal the workspace plays round [change], with [fluentFace] standing in for Selawik.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.revealOn(
    host: RevealHost,
    change: DocumentChange,
    fluentFace: suspend () -> Unit,
    apply: () -> Unit,
) {
    runOnUiThread {
        host.scope.launch {
            host.transition.reveal(RevealStyle.Crossfade, fontWaitFor(change, fluentFace), apply)
        }
    }
    // The v2 test dispatcher queues the launch, so run it now, up to its first wait on a frame or a delay.
    mainClock.advanceTimeBy(0)
}
