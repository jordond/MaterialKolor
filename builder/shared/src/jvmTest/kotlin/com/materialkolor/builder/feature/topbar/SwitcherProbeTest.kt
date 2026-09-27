package com.materialkolor.builder.feature.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.materialkolor.builder.ShellExpressive
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.layout.ProvideBuilderLayout
import com.materialkolor.builder.kit.skin.BuilderTheme
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * The off screen switcher the Medium dropdown and the wide row measure for their room skips the
 * frame lines, since reading them in a group that is never placed crashed the web page. Skipping
 * them must not change the width the bar makes room for.
 */
@OptIn(ExperimentalTestApi::class)
class SwitcherProbeTest {
    @Test
    fun probe_isAsWideAsTheSwitcherThatShows_inEveryForm() =
        runDesktopComposeUiTest(width = 1600, height = 800) {
            val forms = listOf(false, true)
            val shown = mutableMapOf<Pair<LibraryChoice, Boolean>, Int>()
            val probed = mutableMapOf<Pair<LibraryChoice, Boolean>, Int>()
            val document = ThemeDocument.Default
            setContent {
                BuilderTheme(
                    expressive = ShellExpressive,
                    result = ThemeResolver().resolve(document),
                    isDark = false,
                    reducedMotion = true,
                ) {
                    ProvideBuilderLayout(modifier = Modifier.fillMaxSize()) {
                        Column {
                            for (segmented in forms) {
                                for (choice in LibraryChoice.entries) {
                                    for (probe in listOf(false, true)) {
                                        val widths = if (probe) probed else shown
                                        LibrarySwitcher(
                                            selected = choice,
                                            expressive = false,
                                            onSwitch = { _, _ -> },
                                            onExpressiveChange = { _, _ -> },
                                            modifier = Modifier.onSizeChanged { size ->
                                                widths[choice to segmented] = size.width
                                            },
                                            segmented = segmented,
                                            expressiveShown = true,
                                            probe = probe,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            waitForIdle()

            shown.size shouldBe LibraryChoice.entries.size * forms.size
            shown.values.min() shouldBeGreaterThan 0
            probed shouldBe shown
        }
}
