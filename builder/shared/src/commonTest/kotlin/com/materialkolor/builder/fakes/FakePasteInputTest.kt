package com.materialkolor.builder.fakes

import com.materialkolor.builder.core.platform.Paste
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakePasteInputTest {
    private val input = FakePasteInput()

    @Test
    fun everyCollector_seesEveryPaste() =
        runTest(UnconfinedTestDispatcher()) {
            val first = mutableListOf<Paste>()
            val second = mutableListOf<Paste>()
            val jobs = listOf(
                launch { input.pastes.collect { paste -> first += paste } },
                launch { input.pastes.collect { paste -> second += paste } },
            )

            input.paste(Paste.Text("#6750A4"))

            first shouldBe listOf(Paste.Text("#6750A4"))
            second shouldBe listOf(Paste.Text("#6750A4"))
            jobs.forEach { job -> job.cancel() }
        }

    @Test
    fun aPasteBeforeAnyCollector_reachesTheFirstOneOnly() =
        runTest(UnconfinedTestDispatcher()) {
            input.paste(Paste.Text("early"))
            val first = mutableListOf<Paste>()
            val second = mutableListOf<Paste>()

            val jobs = listOf(
                launch { input.pastes.collect { paste -> first += paste } },
                launch { input.pastes.collect { paste -> second += paste } },
            )

            first shouldBe listOf(Paste.Text("early"))
            second.shouldBeEmpty()
            jobs.forEach { job -> job.cancel() }
        }
}
