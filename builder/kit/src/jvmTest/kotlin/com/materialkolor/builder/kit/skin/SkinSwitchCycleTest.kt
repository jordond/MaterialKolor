package com.materialkolor.builder.kit.skin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderToggleButton
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The four libraries in the order the cycle wears them, Fluent between the others.
 */
private val Cycle: List<Library> = listOf(Library.Material3, Library.Unstyled, Library.Fluent, Library.Custom)

/**
 * How many times the cycle runs through all four.
 */
private const val Rounds = 20

/**
 * Counts how many of the builders it hands out are remembered and not yet forgotten.
 */
private class LiveBuilders {
    var count: Int = 0
        private set

    fun track(): RememberObserver =
        object : RememberObserver {
            override fun onRemembered() {
                count++
            }

            override fun onForgotten() {
                count--
            }

            override fun onAbandoned() = Unit
        }
}

@OptIn(ExperimentalTestApi::class)
class SkinSwitchCycleTest {
    @Test
    fun everyLibrary_cycledTwentyTimesWithFluentAmongThem_keepsOneLiveBuilderAndItsState() =
        runComposeUiTest {
            val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4)))
            val live = LiveBuilders()
            var skin by mutableStateOf(Skin(Cycle.first(), expressive = false))
            var shown by mutableStateOf(true)
            setContent {
                if (shown) {
                    BuilderTheme(skin, result, isDark = false, reducedMotion = false) {
                        remember { live.track() }
                        var inspect by remember { mutableStateOf(false) }
                        val notes = remember { mutableStateOf("Lisbon") }
                        ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                            Column {
                                BuilderButton({}, "Share")
                                BuilderToggleButton(inspect, { on -> inspect = on }, "Inspect")
                                LazyColumn {
                                    item {
                                        BasicTextField(
                                            value = notes.value,
                                            onValueChange = { text -> notes.value = text },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            onNodeWithText("Inspect").performClick()

            repeat(Rounds) { round ->
                for (library in Cycle) {
                    skin = Skin(library, expressive = false)
                    waitForIdle()
                    withClue("round $round, $library") {
                        live.count shouldBe 1
                        onAllNodesWithText("Share").assertCountEquals(1)
                        onNodeWithText("Inspect").assertIsOn()
                    }
                }
            }

            shown = false
            waitForIdle()
            live.count shouldBe 0
        }
}
