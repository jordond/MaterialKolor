package com.materialkolor.builder.kit.motion

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class BuilderMotionTest {

    @Test
    fun builderDurations_default_matchTheMotionPrinciples() {
        val durations = BuilderDurations()

        durations.quick shouldBe 120
        durations.standard shouldBe 220
        durations.slow shouldBe 380
        durations.reveal shouldBe 420
        durations.panelEnter shouldBe 300
        durations.panelExit shouldBe 200
        durations.popover shouldBe 160
        durations.press shouldBe 100
        durations.reducedCrossfade shouldBe 150
    }

    @Test
    fun builderDurations_outsideTheSpecRanges_areRejected() {
        assertFailsWith<IllegalArgumentException> { BuilderDurations(reveal = 300) }
        assertFailsWith<IllegalArgumentException> { BuilderDurations(panelEnter = 500) }
        assertFailsWith<IllegalArgumentException> { BuilderDurations(popover = 400) }
        assertFailsWith<IllegalArgumentException> { BuilderDurations(reducedCrossfade = 400) }
    }

    @Test
    fun tweenMotion_eachSpec_runsForItsOwnDuration() {
        val motion = tweenBuilderMotion()

        durationOf(motion.effects<Float>()) shouldBe 120
        durationOf(motion.spatial<Float>()) shouldBe 220
        durationOf(motion.slide<Float>()) shouldBe 380
        durationOf(motion.reveal<Float>()) shouldBe 420
        durationOf(motion.panelEnter<Float>()) shouldBe 300
        durationOf(motion.panelExit<Float>()) shouldBe 200
        durationOf(motion.popover<Float>()) shouldBe 160
        durationOf(motion.press<Float>()) shouldBe 100
        durationOf(motion.crossfade<Float>()) shouldBe 220
    }

    @Test
    fun tweenMotion_theRevealAndPanelArrival_useEmphasizedDecelerate() {
        val motion = tweenBuilderMotion()

        easingOf(motion.reveal<Float>()) shouldBe BuilderEasing.EmphasizedDecelerate
        easingOf(motion.panelEnter<Float>()) shouldBe BuilderEasing.EmphasizedDecelerate
        easingOf(motion.popover<Float>()) shouldBe BuilderEasing.EmphasizedDecelerate
        easingOf(motion.panelExit<Float>()) shouldBe BuilderEasing.EmphasizedAccelerate
    }

    @Test
    fun tweenMotion_press_shrinksToNinetySeven() {
        tweenBuilderMotion().pressScale shouldBe PressScale
        PressScale shouldBe 0.97f
    }

    @Test
    fun reducedMotion_everythingSpatial_snapsAndTheRestCrossfades() {
        val motion = reducedBuilderMotion()

        assertIs<SnapSpec<Float>>(motion.spatial<Float>())
        assertIs<SnapSpec<Float>>(motion.slide<Float>())
        assertIs<SnapSpec<Float>>(motion.press<Float>())
        durationOf(motion.reveal<Float>()) shouldBe 150
        durationOf(motion.crossfade<Float>()) shouldBe 150
        durationOf(motion.panelEnter<Float>()) shouldBe 150
        motion.pressScale shouldBe 1f
    }

    private fun durationOf(spec: FiniteAnimationSpec<Float>): Int = assertIs<TweenSpec<Float>>(spec).durationMillis

    private fun easingOf(spec: FiniteAnimationSpec<Float>): Easing = assertIs<TweenSpec<Float>>(spec).easing
}
