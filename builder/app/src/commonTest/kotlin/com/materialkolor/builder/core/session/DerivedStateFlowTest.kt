package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.model.ThemeDocument
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * The document the session derives from [ShownDocument], the way [ProjectSession.document] reads it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DerivedStateFlowTest {
    private val shown = MutableStateFlow(ShownDocument(ThemeDocument.Default, generation = 0))
    private val document = shown.derived { shown -> shown.document }

    @Test
    fun derived_rightAfterAnUpdate_agreesWithShown() {
        shown.value = ShownDocument(AMOLED, generation = 1)

        document.value shouldBe shown.value.document
        document.replayCache shouldBe listOf(AMOLED)
    }

    @Test
    fun derived_generationAloneChanges_emitsNoNewDocument() =
        runTest {
            val seen = mutableListOf<ThemeDocument>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { document.toList(seen) }

            shown.update { shown -> shown.copy(generation = shown.generation + 1) }
            runCurrent()
            seen shouldBe listOf(ThemeDocument.Default)

            shown.value = ShownDocument(AMOLED, generation = 2)
            runCurrent()
            seen shouldBe listOf(ThemeDocument.Default, AMOLED)
        }

    @Test
    fun derived_collected_startsWithTheCurrentDocument() =
        runTest {
            shown.value = ShownDocument(AMOLED, generation = 1)

            document.first() shouldBe AMOLED
        }

    private companion object {
        val AMOLED = ThemeDocument.Default.copy(amoled = true)
    }
}
