package com.materialkolor.builder.kit.token

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.codegen.TokenKind

/**
 * The neutral layer every builder widget reads.
 *
 * Each skin fills this from its own theme, so one widget implementation looks at home in all of
 * them. Material3 fills it from a `ColorScheme`, Unstyled from its token table, Fluent from
 * `Colors`, and Custom from the builder's own identity record.
 *
 * @property[canvas] The workspace behind the panels.
 * @property[panel] A panel or card sitting on the canvas.
 * @property[panelRaised] A panel that needs to read as lifted, such as a menu or a popover.
 * @property[border] The hairline between regions.
 * @property[borderStrong] The border of something interactive or selected.
 * @property[textStrong] Body and heading ink.
 * @property[textMuted] Secondary ink for hints, units and disabled rows.
 * @property[accent] The skin's accent, used for selection and primary actions.
 * @property[onAccent] Ink that reads on [accent].
 * @property[focus] The keyboard focus ring.
 * @property[codeBackground] The code viewer and code block background.
 * @property[codePalette] One colour per codegen token kind.
 * @property[success] Passing contrast, a completed export.
 * @property[warning] A pair that only just passes, a capability the target downgrades.
 * @property[danger] A failing pair, a destructive action.
 * @property[radius] Corner radii, small through large.
 * @property[spacing] The spacing scale.
 */
@Immutable
public data class BuilderTokens(
    public val canvas: Color,
    public val panel: Color,
    public val panelRaised: Color,
    public val border: Color,
    public val borderStrong: Color,
    public val textStrong: Color,
    public val textMuted: Color,
    public val accent: Color,
    public val onAccent: Color,
    public val focus: Color,
    public val codeBackground: Color,
    public val codePalette: CodePalette,
    public val success: Color,
    public val warning: Color,
    public val danger: Color,
    public val radius: BuilderRadii = BuilderRadii(),
    public val spacing: BuilderSpacing = BuilderSpacing(),
)

/**
 * One colour per codegen token kind, with a fallback for any kind the skin did not name.
 *
 * The fallback means a new token kind in codegen cannot break a skin, it just renders as plain
 * text until someone gives it a colour.
 *
 * @property[plain] The colour used for [TokenKind.Plain] and for any kind [colors] leaves out.
 */
@Immutable
public class CodePalette(
    colors: Map<TokenKind, Color>,
    public val plain: Color,
) {
    private val colors: Map<TokenKind, Color> = colors.toMap()

    public operator fun get(kind: TokenKind): Color = colors[kind] ?: plain

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodePalette) return false
        return plain == other.plain && colors == other.colors
    }

    override fun hashCode(): Int = 31 * plain.hashCode() + colors.hashCode()

    override fun toString(): String = "CodePalette(plain=$plain, colors=$colors)"
}

/**
 * Corner radii. Design D puts the poster panel and the canvas frame on [large].
 *
 * @property[small] Chips, fields and small buttons.
 * @property[medium] Cards and rows inside a panel.
 * @property[large] The panel and canvas frames themselves.
 */
@Immutable
public data class BuilderRadii(
    public val small: Dp = 8.dp,
    public val medium: Dp = 16.dp,
    public val large: Dp = 28.dp,
)

/**
 * The spacing scale, a 4 dp grid.
 *
 * @property[extraSmall] The gap inside a chip or between an icon and its label.
 * @property[small] The gap between controls in a row.
 * @property[medium] The margin around a panel's content, and the panel's own margin in design D.
 * @property[large] The gap between groups of controls.
 * @property[extraLarge] The gap between a heading and what it heads.
 * @property[section] The gap between sections of a panel.
 */
@Immutable
public data class BuilderSpacing(
    public val extraSmall: Dp = 4.dp,
    public val small: Dp = 8.dp,
    public val medium: Dp = 12.dp,
    public val large: Dp = 16.dp,
    public val extraLarge: Dp = 24.dp,
    public val section: Dp = 32.dp,
)

/**
 * The neutral tokens of the surrounding skin.
 *
 * Static for the same reason as `LocalSkin`. The poster surface re-provides it so widgets standing
 * on the seed read the seed's own tones instead of the chrome's.
 */
public val LocalBuilderTokens: ProvidableCompositionLocal<BuilderTokens> = staticCompositionLocalOf {
    error("No BuilderTokens provided")
}
