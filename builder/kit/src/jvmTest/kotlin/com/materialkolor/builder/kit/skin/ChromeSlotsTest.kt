package com.materialkolor.builder.kit.skin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.CustomSlotColors
import com.materialkolor.builder.engine.resolve.ThemeResolver
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ChromeSlotsTest {
    private val resolver = ThemeResolver()
    private val document = ThemeDocument(seed = Argb(0x6750A4), library = Library.Custom)

    @Test
    fun chromeSlots_aNewResultOnTheSameChromeSchemes_keepsTheSlotsItHad() =
        runComposeUiTest {
            var result by mutableStateOf(resolver.resolve(document))
            val seen = mutableListOf<CustomSlotColors>()
            setContent { seen += rememberChromeSlots(result) }
            waitForIdle()

            // Contrast below the standard level leaves the chrome where it was.
            result = resolver.resolve(document.copy(contrast = ContrastLevel.Reduced))
            waitForIdle()

            seen shouldHaveSize 2
            seen.last() shouldBeSameInstanceAs seen.first()
        }

    @Test
    fun chromeSlots_aNewChromeScheme_readsTheSlotsAgain() =
        runComposeUiTest {
            var result by mutableStateOf(resolver.resolve(document))
            val seen = mutableListOf<CustomSlotColors>()
            setContent { seen += rememberChromeSlots(result) }
            waitForIdle()

            result = resolver.resolve(document.copy(seed = Argb(0x1E88E5)))
            waitForIdle()

            seen shouldHaveSize 2
            seen.last() shouldNotBeSameInstanceAs seen.first()
        }
}
