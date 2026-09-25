package com.materialkolor.builder.kit.skin

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.TextStyle
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import kotlin.test.Test

/**
 * D44. A library switch moves the builder in the same frame it re-styles a text field in a lazy
 * list, as when the Trips app is on screen. Without `BuilderTheme` turning the text field min-size
 * optimisation off, the scene dies on dispose.
 */
@OptIn(ExperimentalTestApi::class)
class SkinSwitchTextFieldTest {
    @Test
    fun librarySwitch_restylingATextFieldInALazyList_disposesCleanly() =
        runComposeUiTest {
            val result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4)))
            var skin by mutableStateOf(Skin(Library.Fluent, expressive = false))
            setContent {
                BuilderTheme(skin, result, isDark = false, reducedMotion = false) {
                    val notes = remember { mutableStateOf("Lisbon") }
                    LazyColumn {
                        item {
                            BasicTextField(
                                value = notes.value,
                                onValueChange = { text -> notes.value = text },
                                textStyle = TextStyle(color = InkOf.getValue(skin.library)),
                            )
                        }
                    }
                }
            }
            waitForIdle()
            for (library in listOf(Library.Material3, Library.Custom, Library.Unstyled)) {
                skin = Skin(library, expressive = false)
                waitForIdle()
            }
        }
}

/**
 * A different ink per library, so every switch re-styles the field.
 */
private val InkOf: Map<Library, Color> = mapOf(
    Library.Material3 to Color.Red,
    Library.Fluent to Color.Blue,
    Library.Custom to Color.Green,
    Library.Unstyled to Color.Magenta,
)
