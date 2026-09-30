package com.materialkolor.builder.kit.widget

import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.snapshots.Snapshot
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanePicturesTest {
    @BeforeTest
    fun setUp() {
        KeptPictures.clear()
    }

    @AfterTest
    fun tearDown() {
        KeptPictures.clear()
    }

    @Test
    fun follow_buildsTheWholeHuesPictureOnItsBuilder_andKeepsIt() =
        runTest {
            val pictures = PlanePictures(StandardTestDispatcher(testScheduler))
            val following = launch { pictures.follow { 40.4 } }
            pictures.shown.shouldBeNull()

            runCurrent()

            pictures.shown?.hue shouldBe 40
            KeptPictures.take(40) shouldBeSameInstanceAs pictures.shown
            following.cancel()
        }

    @Test
    fun follow_aHueThatMovesOnBeforeItsPictureIsBuilt_neverGetsOne() =
        runTest {
            val builder = HeldBuilder()
            val pictures = PlanePictures(builder)
            val hue = mutableDoubleStateOf(40.0)
            val following = launch { pictures.follow { hue.doubleValue } }
            runCurrent()

            hue.doubleValue = 80.0
            Snapshot.sendApplyNotifications()
            runCurrent()
            builder.runHeld()
            runCurrent()

            pictures.shown?.hue shouldBe 80
            KeptPictures.take(40).shouldBeNull()
            following.cancel()
        }

    @Test
    fun follow_aKeptHue_showsItsPictureWithoutBuilding() =
        runTest {
            val kept = planePicture(120)
            KeptPictures.keep(kept)
            val pictures = PlanePictures(HeldBuilder())
            val following = launch { pictures.follow { 120.0 } }

            runCurrent()

            pictures.shown shouldBeSameInstanceAs kept
            following.cancel()
        }
}

/**
 * Holds every build handed to it until the test runs them, on the test's own thread, so a test
 * chooses what the main thread does while a picture is building.
 */
private class HeldBuilder : CoroutineDispatcher() {
    private val held = ArrayDeque<Runnable>()

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        held.addLast(block)
    }

    /**
     * Runs every build held so far, oldest first.
     */
    fun runHeld() {
        while (held.isNotEmpty()) held.removeFirst().run()
    }
}
