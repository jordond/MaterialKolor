package com.materialkolor.builder.feature.share

import androidx.compose.ui.graphics.ImageBitmap
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ShareCardTest {
    private val cards = mutableMapOf<String, ImageBitmap>()
    private val loaded = mutableListOf<String>()

    @Test
    fun show_theFirstLink_loadsAtOnce() =
        runTest {
            cards[FIRST] = ImageBitmap(1, 1)
            val loader = loader()

            loader.show(FIRST)
            loader.state.value shouldBe CardState.Loading
            runCurrent()

            loaded shouldBe listOf(FIRST)
            loader.state.value shouldBe CardState.Shown(cards.getValue(FIRST))
        }

    @Test
    fun show_aNewLinkWhileACardIsUp_keepsItUpAndLoadsAfterThePause() =
        runTest {
            cards[FIRST] = ImageBitmap(1, 1)
            cards[SECOND] = ImageBitmap(2, 2)
            val loader = loader()
            loader.show(FIRST)
            runCurrent()

            loader.show(SECOND)
            loader.state.value shouldBe CardState.Updating(cards.getValue(FIRST))
            advanceTimeBy(PAUSE - 1.milliseconds)
            runCurrent()
            loaded shouldBe listOf(FIRST)
            advanceTimeBy(1.milliseconds)
            runCurrent()

            loaded shouldBe listOf(FIRST, SECOND)
            loader.state.value shouldBe CardState.Shown(cards.getValue(SECOND))
        }

    @Test
    fun show_twoQuickChanges_loadOnlyTheLatest() =
        runTest {
            cards[FIRST] = ImageBitmap(1, 1)
            cards[THIRD] = ImageBitmap(3, 3)
            val loader = loader()
            loader.show(FIRST)
            runCurrent()

            loader.show(SECOND)
            advanceTimeBy(PAUSE / 2)
            loader.show(THIRD)
            loader.show(THIRD)
            advanceTimeBy(PAUSE)
            runCurrent()

            loaded shouldBe listOf(FIRST, THIRD)
            loader.state.value shouldBe CardState.Shown(cards.getValue(THIRD))
        }

    @Test
    fun show_aLinkWithNoCard_isFailedEvenWithACardUp() =
        runTest {
            cards[FIRST] = ImageBitmap(1, 1)
            val loader = loader()
            loader.show(FIRST)
            runCurrent()

            loader.show(SECOND)
            advanceTimeBy(PAUSE)
            runCurrent()

            loader.state.value shouldBe CardState.Failed
        }

    @Test
    fun decodeCard_bytesThatAreNotAnImage_isNull() {
        decodeCard("<!doctype html><title>MaterialKolor</title>".encodeToByteArray()).shouldBeNull()
    }

    private fun TestScope.loader(): ShareCardLoader =
        ShareCardLoader(
            scope = backgroundScope,
            load = { url ->
                loaded += url
                cards[url]
            },
            pause = PAUSE,
        )

    private companion object {
        const val FIRST = "https://materialkolor.com/og/first.png"
        const val SECOND = "https://materialkolor.com/og/second.png"
        const val THIRD = "https://materialkolor.com/og/third.png"
        val PAUSE = 500.milliseconds
    }
}
