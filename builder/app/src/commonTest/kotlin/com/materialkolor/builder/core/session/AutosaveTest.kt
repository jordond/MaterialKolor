package com.materialkolor.builder.core.session

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutosaveTest {
    private val written = mutableListOf<String>()
    private var lands = true

    @Test
    fun schedule_burstOfValues_writesTheLastOnceAfterTheQuiet() =
        runTest {
            val autosave = Autosave<String>(backgroundScope) { value -> record(value) }

            autosave.schedule("a")
            advanceTimeBy(300)
            autosave.schedule("b")
            advanceTimeBy(AUTOSAVE_DELAY_MILLIS - 1)
            runCurrent()
            written shouldBe emptyList()

            advanceTimeBy(1)
            runCurrent()
            written shouldBe listOf("b")
            advanceTimeBy(AUTOSAVE_DELAY_MILLIS * 4)
            written shouldBe listOf("b")
        }

    @Test
    fun flush_withAValueWaiting_writesItAtOnceAndOnlyOnce() =
        runTest {
            val autosave = Autosave<String>(backgroundScope) { value -> record(value) }

            autosave.schedule("a")
            autosave.flush()

            written shouldBe listOf("a")
            advanceTimeBy(AUTOSAVE_DELAY_MILLIS * 2)
            written shouldBe listOf("a")
            autosave.hasPending shouldBe false
        }

    @Test
    fun flush_afterAWriteThatDidNotLand_triesTheValueAgain() =
        runTest {
            val autosave = Autosave<String>(backgroundScope) { value -> record(value) }
            lands = false
            autosave.schedule("a")
            advanceTimeBy(AUTOSAVE_DELAY_MILLIS)
            runCurrent()
            autosave.hasPending shouldBe true

            lands = true
            autosave.flush()

            written shouldBe listOf("a", "a")
            autosave.hasPending shouldBe false
        }

    private fun record(value: String): Boolean {
        written += value
        return lands
    }
}
