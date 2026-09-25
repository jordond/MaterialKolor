package com.materialkolor.builder.kit.token

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.materialkolor.builder.kit.generated.resources.BricolageGrotesque_Variable
import com.materialkolor.builder.kit.generated.resources.JetBrainsMono_Variable
import com.materialkolor.builder.kit.generated.resources.Res
import org.jetbrains.compose.resources.Font

/**
 * The builder's own type, the one thing that does not change with the skin.
 *
 * Bricolage Grotesque carries the poster, the wordmark and the chrome. JetBrains Mono carries
 * every value a user might copy, so a hex, a tone or a line of generated code always looks like
 * something you can select.
 *
 * @property[posterHero] The seed hex on the poster, the largest thing on screen.
 * @property[wordmark] The MaterialKolor wordmark in the top bar.
 * @property[title] Panel and dialog titles.
 * @property[sectionLabel] The small label above a group of controls.
 * @property[body] Explainers and descriptions.
 * @property[label] Control labels and buttons.
 * @property[value] A hex, a tone or a contrast ratio shown beside a control.
 * @property[code] The code viewer and any code block.
 */
@Immutable
public data class BuilderType(
    public val posterHero: TextStyle,
    public val wordmark: TextStyle,
    public val title: TextStyle,
    public val sectionLabel: TextStyle,
    public val body: TextStyle,
    public val label: TextStyle,
    public val value: TextStyle,
    public val code: TextStyle,
)

/**
 * The brand face, a Latin subset of Bricolage Grotesque with its weight axis live.
 *
 * Optical size and width are pinned in the subset, see `builder/tools/fonts/subset.sh`. The
 * upstream build defaults its weight axis to 800, so every face here names its weight and the
 * family never falls back to the default instance.
 */
@Composable
public fun brandFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.BricolageGrotesque_Variable, FontWeight.Light),
        Font(Res.font.BricolageGrotesque_Variable, FontWeight.Normal),
        Font(Res.font.BricolageGrotesque_Variable, FontWeight.Medium),
        Font(Res.font.BricolageGrotesque_Variable, FontWeight.SemiBold),
        Font(Res.font.BricolageGrotesque_Variable, FontWeight.Bold),
        Font(Res.font.BricolageGrotesque_Variable, FontWeight.ExtraBold),
    )

/**
 * The mono face, a Latin subset of JetBrains Mono with its weight axis live.
 */
@Composable
public fun monoFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.JetBrainsMono_Variable, FontWeight.Light),
        Font(Res.font.JetBrainsMono_Variable, FontWeight.Normal),
        Font(Res.font.JetBrainsMono_Variable, FontWeight.Medium),
        Font(Res.font.JetBrainsMono_Variable, FontWeight.SemiBold),
        Font(Res.font.JetBrainsMono_Variable, FontWeight.Bold),
    )

/**
 * Builds the builder's type from the two bundled faces.
 *
 * Call this once, at the theme root, and hand the result to [LocalBuilderType].
 */
@Composable
public fun rememberBuilderType(): BuilderType {
    val brand = brandFontFamily()
    val mono = monoFontFamily()
    return remember(brand, mono) { builderType(brand, mono) }
}

/**
 * The type scale, split out so a test can build it without a composition.
 */
internal fun builderType(
    brand: FontFamily,
    mono: FontFamily,
): BuilderType =
    BuilderType(
        posterHero = TextStyle(
            fontFamily = brand,
            fontWeight = FontWeight.Bold,
            fontSize = 72.sp,
            lineHeight = 76.sp,
            letterSpacing = (-1.5).sp,
        ),
        wordmark = TextStyle(
            fontFamily = brand,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.2).sp,
        ),
        title = TextStyle(
            fontFamily = brand,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        ),
        sectionLabel = TextStyle(
            fontFamily = brand,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.8.sp,
        ),
        body = TextStyle(
            fontFamily = brand,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        label = TextStyle(
            fontFamily = brand,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        value = TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        code = TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        ),
    )

/**
 * The builder's type for the surrounding tree. Every skin provides the same value.
 */
public val LocalBuilderType: ProvidableCompositionLocal<BuilderType> = staticCompositionLocalOf {
    error("No BuilderType provided")
}
