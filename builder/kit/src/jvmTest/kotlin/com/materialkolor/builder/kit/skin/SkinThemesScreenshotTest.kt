package com.materialkolor.builder.kit.skin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.LocalTextStyle
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.color.ContrastLevel
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.RolePin
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.mapping.toColor
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.control.BuilderIcon
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.BuilderIcons
import com.materialkolor.builder.kit.icon.FluentIcons
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.icon.LocalBuilderIcons
import com.materialkolor.builder.kit.icon.LucideIcons
import com.materialkolor.builder.kit.icon.MaterialIcons
import com.materialkolor.builder.kit.motion.BuilderDurations
import com.materialkolor.builder.kit.motion.LocalBuilderMotion
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.skin.custom.LocalBuilderIdentity
import com.materialkolor.builder.kit.token.BuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import io.github.takahirom.roborazzi.captureRoboImage
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.test.assertNotNull

private const val SheetTag = "token-sheet"
private const val SeedHex = "#6750A4"

/**
 * Where B-213's recording job writes the baselines. Nothing is written unless a Roborazzi task
 * turns capture on, and baselines are only ever recorded on the Linux runner.
 */
private const val ScreenshotDir = "src/jvmTest/screenshots/skin-themes"

/** A reduced contrast document with a pinned primary, the worst case for the chrome floor. */
private val Document = ThemeDocument(
    seed = Argb(0x6750A4),
    contrast = ContrastLevel.Reduced,
    pins = mapOf(Role.Primary to RolePin(light = Argb(0xFFEE00), dark = Argb(0xFFEE00))),
)

@OptIn(ExperimentalTestApi::class)
class SkinThemesScreenshotTest {
    @Test
    fun material3_bothModes_renderTheSheetOnFlooredChrome() =
        runComposeUiTest { checkSheets(Skin(Library.Material3, expressive = false), "material3", MaterialIcons, 20.dp) }

    @Test
    fun material3Expressive_bothModes_renderTheSheetOnFlooredChrome() =
        runComposeUiTest { checkSheets(Skin(Library.Material3, expressive = true), "expressive", MaterialIcons, 20.dp) }

    @Test
    fun unstyled_bothModes_renderTheSheetOnFlooredChrome() =
        runComposeUiTest { checkSheets(Skin(Library.Unstyled, expressive = false), "unstyled", LucideIcons, 16.dp) }

    @Test
    fun custom_bothModes_renderTheSheetOnFlooredChrome() =
        runComposeUiTest { checkSheets(Skin(Library.Custom, expressive = false), "custom", LucideIcons, 18.dp) }

    @Test
    fun fluentPlaceholder_bothModes_renderTheSheetOnFlooredChrome() =
        runComposeUiTest { checkSheets(Skin(Library.Fluent, expressive = false), "fluent", FluentIcons, 16.dp) }

    @Test
    fun custom_pinnedDocument_drawsTheChromeSlotsNotTheDocumentSlots() =
        runComposeUiTest {
            var seen: Color? = null
            var expected: Color? = null
            var pinned: Color? = null
            setContent {
                val result = remember { ThemeResolver().resolve(Document) }
                BuilderTheme(Skin(Library.Custom, expressive = false), result, isDark = false, reducedMotion = false) {
                    seen = LocalBuilderIdentity.current[CustomSlot.Primary]
                    expected = result.chromeCustomSlots[CustomSlot.Primary, false].toColor()
                    pinned = result.customSlots[CustomSlot.Primary, false].toColor()
                }
            }

            assertNotNull(seen) shouldBe expected
            seen shouldNotBe pinned
        }

    @Test
    fun material3AndUnstyled_libraryText_wearsTheBrandFace() =
        runComposeUiTest {
            val seen = mutableMapOf<String, FontFamily?>()
            var brand: FontFamily? = null
            setContent {
                val result = remember { ThemeResolver().resolve(Document) }
                for (expressive in listOf(false, true)) {
                    BuilderTheme(Skin(Library.Material3, expressive), result, isDark = false, reducedMotion = false) {
                        brand = LocalBuilderType.current.body.fontFamily
                        val typography = MaterialTheme.typography
                        seen["m3 expressive=$expressive bodyLarge"] = typography.bodyLarge.fontFamily
                        seen["m3 expressive=$expressive labelLarge"] = typography.labelLarge.fontFamily
                        seen["m3 expressive=$expressive titleMedium"] = typography.titleMedium.fontFamily
                    }
                }
                BuilderTheme(
                    Skin(Library.Unstyled, expressive = false),
                    result,
                    isDark = false,
                    reducedMotion = false,
                ) {
                    seen["unstyled text style"] = LocalTextStyle.current.fontFamily
                }
            }

            waitForIdle()
            val face = assertNotNull(brand)
            face shouldNotBe FontFamily.Default
            seen.size shouldBe 7
            seen.filterValues { family -> family != face } shouldBe emptyMap()
        }
}

/** What one sheet saw of its skin. */
private class Seen(
    val skin: Skin,
    val tokens: BuilderTokens,
    val icons: BuilderIcons,
    val durations: BuilderDurations,
)

/** One ink on one ground, and the least contrast the pair may have. */
private class InkPair(
    val name: String,
    val ink: Color,
    val ground: Color,
    val minimum: Double,
)

/**
 * Every ink the builder draws as text or as a mark, on every ground it draws it on.
 *
 * Text keeps to WCAG AA at 4.5, the accent is a mark and only needs 3. Danger counts as text because
 * `Emphasis.Danger` labels a destructive action.
 */
private fun BuilderTokens.inkPairs(): List<InkPair> =
    listOf(
        InkPair("textStrong on panel", textStrong, panel, 4.5),
        InkPair("textMuted on panel", textMuted, panel, 4.5),
        InkPair("accent on panel", accent, panel, 3.0),
        InkPair("onAccent on accent", onAccent, accent, 4.5),
        InkPair("danger on panel", danger, panel, 4.5),
        InkPair("textStrong on canvas", textStrong, canvas, 4.5),
        InkPair("textMuted on canvas", textMuted, canvas, 4.5),
        InkPair("textStrong on panelRaised", textStrong, panelRaised, 4.5),
        InkPair("textMuted on panelRaised", textMuted, panelRaised, 4.5),
    ) +
        (TokenKind.entries - TokenKind.Plain).map { kind ->
            InkPair("$kind on codeBackground", codePalette[kind], codeBackground, 4.5)
        }

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.checkSheets(
    skin: Skin,
    name: String,
    icons: BuilderIcons,
    iconSize: Dp,
) {
    val unreadable = mutableListOf<String>()
    var isDark by mutableStateOf(false)
    var seen: Seen? = null
    setContent {
        val result = remember { ThemeResolver().resolve(Document) }
        CompositionLocalProvider(LocalMotionFrozen provides true) {
            BuilderTheme(skin, result, isDark, reducedMotion = false) {
                seen = Seen(
                    skin = LocalSkin.current,
                    tokens = LocalBuilderTokens.current,
                    icons = LocalBuilderIcons.current,
                    durations = LocalBuilderMotion.current.durations,
                )
                TokenSheet()
            }
        }
    }

    for (dark in listOf(false, true)) {
        isDark = dark
        waitForIdle()
        val sheet = assertNotNull(seen)
        sheet.skin shouldBe skin
        sheet.icons shouldBe icons
        sheet.durations shouldBe BuilderDurations()
        sheet.tokens.iconSize shouldBe iconSize
        val mode = if (dark) "dark" else "light"
        for (pair in sheet.tokens.inkPairs()) {
            val ratio = contrast(pair.ink, pair.ground)
            if (ratio < pair.minimum) unreadable += "$mode ${pair.name} ${"%.2f".format(ratio)} < ${pair.minimum}"
        }
        onNodeWithTag(SheetTag).assertExists()
        onNodeWithText(SeedHex).assertExists()
        onNodeWithContentDescription(IconId.Undo.name).assertExists()
        onNodeWithContentDescription(IconId.ExternalLink.name).assertExists()
        onNodeWithTag(SheetTag).captureRoboImage("$ScreenshotDir/$name-$mode.png")
    }
    unreadable.shouldBeEmpty()
}

/** The WCAG contrast ratio of two opaque colours. */
private fun contrast(
    a: Color,
    b: Color,
): Double {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05) / (darker + 0.05)
}

/** Every token, every text style and every icon of the surrounding skin on one panel. */
@Composable
private fun TokenSheet() {
    val tokens = LocalBuilderTokens.current
    Column(
        modifier = Modifier
            .testTag(SheetTag)
            .background(tokens.canvas)
            .padding(tokens.spacing.medium),
    ) {
        Column(
            modifier = Modifier
                .width(480.dp)
                .background(tokens.panel, RoundedCornerShape(tokens.radius.large))
                .padding(tokens.spacing.large),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
        ) {
            BuilderText(SeedHex, style = BuilderTextStyle.PosterHero)
            BuilderText("MaterialKolor", style = BuilderTextStyle.Wordmark)
            BuilderText("Title", style = BuilderTextStyle.Title)
            BuilderText("SECTION LABEL", style = BuilderTextStyle.SectionLabel)
            BuilderText("Body text explains a control.", style = BuilderTextStyle.Body)
            Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium)) {
                for (emphasis in Emphasis.entries) {
                    BuilderText(emphasis.name, style = BuilderTextStyle.Label, emphasis = emphasis)
                }
            }
            BuilderText("T40 4.5:1", style = BuilderTextStyle.Value)
            Box(Modifier.background(tokens.codeBackground).padding(tokens.spacing.small)) {
                BuilderText("val seed = Color(0xFF6750A4)", style = BuilderTextStyle.Code)
            }
            Swatches(
                listOf(
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
                    tokens.success,
                    tokens.warning,
                    tokens.danger,
                ),
            )
            Swatches(TokenKind.entries.map { kind -> tokens.codePalette[kind] })
            for (row in IconId.entries.chunked(IconsPerRow)) {
                Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small)) {
                    for (id in row) {
                        BuilderIcon(id, contentDescription = id.name)
                    }
                }
            }
        }
    }
}

private const val IconsPerRow = 14

@Composable
private fun Swatches(colors: List<Color>) {
    val tokens = LocalBuilderTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall)) {
        for (color in colors) {
            Box(Modifier.size(tokens.spacing.large).background(color, RoundedCornerShape(tokens.radius.small)))
        }
    }
}
