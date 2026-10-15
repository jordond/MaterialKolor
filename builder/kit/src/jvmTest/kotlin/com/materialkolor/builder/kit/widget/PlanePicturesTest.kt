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
import kotlinx.coroutines.test.TestScope
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
            whileFollowing(pictures, builder, hue = { hue.doubleValue }) {
                runCurrent()
                builder.holding shouldBe true

                hue.doubleValue = 80.0
                Snapshot.sendApplyNotifications()
                settle(builder)

                pictures.shown?.hue shouldBe 80
                KeptPictures.take(40).shouldBeNull()
            }
        }

    @Test
    fun follow_aKeptHue_showsItsPictureWithoutBuilding() =
        runTest {
            val kept = planePicture(120)
            KeptPictures.keep(kept)
            val builder = HeldBuilder()
            val pictures = PlanePictures(builder)
            whileFollowing(pictures, builder, hue = { 120.0 }) {
                runCurrent()

                builder.holding shouldBe false
                pictures.shown shouldBeSameInstanceAs kept
            }
        }
}

/**
 * Holds every build handed to it until the test runs them, on the test's own thread, so a test
 * chooses what the main thread does while a picture is building.
 */
private class HeldBuilder : CoroutineDispatcher() {
    private val held = ArrayDeque<Runnable>()

    /**
     * Whether a build is waiting for [runHeld].
     */
    val holding: Boolean
        get() = held.isNotEmpty()

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

/**
 * Runs [check] while [pictures] follows [hue]. Afterwards, passed or not, it stops following and
 * runs what [builder] still holds, so a cancelled build does not keep the test waiting on it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private inline fun TestScope.whileFollowing(
    pictures: PlanePictures,
    builder: HeldBuilder,
    noinline hue: () -> Double,
    check: () -> Unit,
) {
    val following = launch { pictures.follow(hue) }
    try {
        check()
    } finally {
        following.cancel()
        settle(builder)
    }
}

/**
 * Runs the main thread's work and every build [builder] holds, turn about, until neither has any.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.settle(builder: HeldBuilder) {
    runCurrent()
    while (builder.holding) {
        builder.runHeld()
        runCurrent()
    }
}
