package com.materialkolor.builder.kit.skin.fluent

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.icon.FluentIcons
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.icon.LucideIcons
import com.materialkolor.builder.kit.motion.BuilderDurations
import com.materialkolor.builder.kit.motion.BuilderMotion
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.skin.BuilderTheme
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.fluent.toFluentColors
import io.github.composefluent.FluentTheme
import io.github.composefluent.animation.FluentDuration
import io.github.composefluent.animation.FluentEasing
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.io.File
import kotlin.test.Test

private val Result = ThemeResolver().resolve(ThemeDocument(seed = Argb(0x6750A4)))

private val FluentSkin = Skin(Library.Fluent, expressive = false)

/** The ids `fluent-icons-core` has no glyph for, which borrow Lucide's. */
private val LucideFallbacks: Set<IconId> = setOf(
    IconId.Undo,
    IconId.Redo,
    IconId.Export,
    IconId.Command,
    IconId.Shuffle,
    IconId.Eyedropper,
    IconId.Upload,
    IconId.Lock,
    IconId.Unlock,
    IconId.Sun,
    IconId.Moon,
    IconId.Split,
    IconId.Phone,
    IconId.Tablet,
    IconId.Desktop,
    IconId.Inspect,
    IconId.Collapse,
    IconId.Expand,
    IconId.Keyboard,
    IconId.Help,
)

/** Every kit file B-403 and B-403b own, the ones a Fluent swap can reach. */
private val FluentOwned: List<String> = listOf(
    "skin/fluent/FluentSkinTheme.kt",
    "skin/fluent/FluentActions.kt",
    "skin/fluent/FluentInputs.kt",
    "skin/fluent/FluentOverlays.kt",
    "skin/fluent/FluentSegmented.kt",
    "skin/fluent/FluentToggles.kt",
    "skin/fluent/FluentTabs.kt",
    "skin/fluent/FluentDisclosure.kt",
    "icon/FluentIcons.kt",
    "control/BuilderButton.kt",
    "control/BuilderIconButton.kt",
    "control/BuilderToggleButton.kt",
    "control/BuilderSegmented.kt",
    "control/BuilderSwitch.kt",
    "control/BuilderCheckbox.kt",
    "control/BuilderSlider.kt",
    "control/BuilderTextField.kt",
    "control/BuilderDisclosure.kt",
    "control/BuilderTabs.kt",
)

/**
 * What opens a window of its own or runs a loop that ignores reduced motion. Overlays draw in the
 * page (D40), and the progress bar keeps its own loop, so none of these belong in a Fluent file.
 */
private val WindowsAndLoops: List<Regex> = listOf(
    Regex("""androidx\.compose\.ui\.window\.Popup"""),
    Regex("""androidx\.compose\.ui\.window\.Dialog"""),
    Regex("""io\.github\.composefluent\.component\.Popup"""),
    Regex("""\bComboBox\("""),
    Regex("""\bMenuFlyout\("""),
    Regex("""\bContentDialog\("""),
    Regex("""\bTooltipBox\("""),
    Regex("""\bProgressBar\("""),
    Regex("""\bProgressRing\("""),
)

/** Where the kit's common sources sit, from the module directory Gradle runs the tests in. */
private val KitSources = File("src/commonMain/kotlin/com/materialkolor/builder/kit")

@OptIn(ExperimentalTestApi::class)
class FluentSkinThemeTest {
    @Test
    fun motion_takesFluentStepsAndCurvesAndKeepsTheBuilderDurations() =
        runComposeUiTest {
            var seen: BuilderMotion? = null
            setContent {
                BuilderTheme(FluentSkin, Result, isDark = false, reducedMotion = false) {
                    seen = LocalBuilderMotion.current
                }
            }
            waitForIdle()
            val motion = requireNotNull(seen)

            val quick = FluentDuration.QuickDuration
            val short = FluentDuration.ShortDuration
            val medium = FluentDuration.MediumDuration
            motion.durations shouldBe BuilderDurations()
            motion.reveal<Float>() shouldBe fluent(417, FluentEasing.FastInvokeEasing)
            motion.panelEnter<Float>() shouldBe fluent(medium, FluentEasing.FastInvokeEasing)
            motion.panelExit<Float>() shouldBe fluent(short, FluentEasing.FastDismissEasing)
            motion.popover<Float>() shouldBe fluent(short, FluentEasing.FastInvokeEasing)
            motion.spatial<Float>() shouldBe fluent(short, FluentEasing.PointToPointEasing)
            motion.slide<Float>() shouldBe fluent(medium, FluentEasing.PointToPointEasing)
            motion.effects<Float>() shouldBe fluent(quick, FluentEasing.FastInvokeEasing)
            motion.press<Float>() shouldBe fluent(quick, FluentEasing.FastInvokeEasing)
            motion.crossfade<Float>() shouldBe fluent(short, FluentEasing.FadeInFadeOutEasing)
        }

    private fun fluent(
        millis: Int,
        easing: Easing,
    ): FiniteAnimationSpec<Float> = tween(millis, easing = easing)

    @Test
    fun tokens_bothModes_comeSolidOutOfTheFluentColoursTheThemeWears() =
        runComposeUiTest {
            val seen = mutableMapOf<Boolean, Pair<BuilderTokens, Color>>()
            setContent {
                for (dark in listOf(false, true)) {
                    BuilderTheme(FluentSkin, Result, isDark = dark, reducedMotion = false) {
                        seen[dark] = LocalBuilderTokens.current to FluentTheme.colors.fillAccent.default
                    }
                }
            }
            waitForIdle()

            for (dark in listOf(false, true)) {
                withClue("dark = $dark") {
                    val scheme = Result.chrome(dark)
                    val colors = scheme.toFluentColors()
                    val (tokens, themeAccent) = requireNotNull(seen[dark])
                    tokens shouldBe fluentTokens(colors, scheme, dark)
                    themeAccent shouldBe tokens.accent
                    tokens.accent shouldBe colors.fillAccent.default
                    tokens.canvas shouldBe colors.background.solid.base
                    tokens.onAccent shouldBe colors.text.onAccent.primary
                    tokens.danger shouldBe colors.system.critical
                    tokens.scrim shouldBe colors.background.smoke.default
                    val inks = listOf(
                        tokens.canvas,
                        tokens.panel,
                        tokens.panelRaised,
                        tokens.border,
                        tokens.borderStrong,
                        tokens.textStrong,
                        tokens.textMuted,
                        tokens.accent,
                        tokens.onAccent,
                        tokens.focus,
                        tokens.codeBackground,
                        tokens.danger,
                    )
                    inks.filter { ink -> ink.alpha != 1f }.shouldBeEmpty()
                }
            }
        }

    @Test
    fun icons_everyId_drawsTheFluentGlyphOrBorrowsLucidesWhereCoreHasNone() {
        for (id in IconId.entries) {
            withClue(id) {
                if (id in LucideFallbacks) {
                    FluentIcons[id] shouldBe LucideIcons[id]
                } else {
                    FluentIcons[id].name shouldStartWith "Regular."
                }
            }
        }
    }

    @Test
    fun sources_fluentOwnedFiles_openNoWindowAndRunNoLoopOfTheirOwn() {
        require(KitSources.isDirectory) { "Kit sources not found from ${File("").absolutePath}" }
        val found = FluentOwned.flatMap { path ->
            val text = KitSources.resolve(path).readText()
            WindowsAndLoops.filter { banned -> banned.containsMatchIn(text) }.map { banned -> "$path: $banned" }
        }
        found.shouldBeEmpty()
    }
}
