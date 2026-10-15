package com.materialkolor.builder.kit.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.v2.runComposeUiTest
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.kit.skin.Skin
import com.materialkolor.builder.kit.skin.SkinTestTheme

/**
 * Matches a node that plays [role].
 */
internal fun hasRole(role: Role): SemanticsMatcher = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

internal fun hasOverlayPaneTitle(title: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, title)

internal fun hasStateDescription(state: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)

/**
 * Matches a node with no content description at all.
 */
internal fun hasNoContentDescription(): SemanticsMatcher =
    SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription)

/**
 * Matches a node whose content descriptions are exactly [names].
 */
internal fun hasContentDescriptionExactly(vararg names: String): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, names.toList())

/**
 * A kit's worth of state words in English, for the plain functions.
 */
internal val TestStateWords: StateWords = StateWords(
    selected = "Selected",
    notSelected = "Not selected",
    checked = "Checked",
    notChecked = "Not checked",
    on = "On",
    off = "Off",
    expanded = "Expanded",
    collapsed = "Collapsed",
    disabled = "Disabled",
    checkbox = "checkbox",
    switch = "switch",
    radio = "radio",
    tab = "tab",
    slider = "slider",
    progressBar = "progress bar",
    popUpButton = "pop-up button",
    menuItem = "menu item",
    option = "option",
    dialog = "dialog",
)

/**
 * One ink on one ground a control draws, and the least contrast the pair may have.
 */
internal class InkPair(
    val name: String,
    val ink: Color,
    val ground: Color,
    val minimum: Double,
)

/**
 * The WCAG contrast ratio of two opaque colours.
 */
internal fun contrast(
    a: Color,
    b: Color,
): Double {
    val lighter = maxOf(a.luminance(), b.luminance())
    val darker = minOf(a.luminance(), b.luminance())
    return (lighter + 0.05) / (darker + 0.05)
}

/**
 * Every pair that falls short of its minimum, one line each, headed by the [mode] it was seen in.
 */
internal fun Iterable<InkPair>.shortfalls(mode: String): List<String> =
    mapNotNull { pair ->
        val ratio = contrast(pair.ink, pair.ground)
        if (ratio < pair.minimum) "$mode ${pair.name} ${"%.2f".format(ratio)} < ${pair.minimum}" else null
    }

/**
 * What [read] sees inside each of [skins] over [document], in light and in dark, keyed by the
 * skin's name and the mode. Every reading comes from one composition, with no scene drawn.
 */
@OptIn(ExperimentalTestApi::class)
internal fun <T> readInEverySkin(
    document: ThemeDocument = ThemeDocument(seed = Argb(0x6750A4)),
    skins: List<Pair<String, Skin>> = ControlSkins,
    read: @Composable () -> T,
): Map<String, T> {
    val seen = linkedMapOf<String, T>()
    runComposeUiTest {
        setContent {
            val result = remember { ThemeResolver().resolve(document) }
            for ((name, skin) in skins) {
                for (isDark in listOf(false, true)) {
                    SkinTestTheme(skin, result, isDark, reducedMotion = false) {
                        seen["$name ${if (isDark) "dark" else "light"}"] = read()
                    }
                }
            }
        }
        waitForIdle()
    }
    return seen
}

/**
 * Every pair of every reading that falls short of its minimum, each headed by the skin and mode.
 */
internal fun Map<String, List<InkPair>>.shortfalls(): List<String> =
    flatMap { (where, pairs) -> pairs.shortfalls(where) }
