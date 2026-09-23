package com.materialkolor.builder.kit.control

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.materialkolor.builder.kit.token.BuilderType
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType

/**
 * Which part of [BuilderType] a piece of text is set in.
 *
 * The type is the same in every skin, so a style names a role in the builder rather than a size.
 */
public enum class BuilderTextStyle {
    /** The seed hex on the poster. */
    PosterHero,

    /** The MaterialKolor wordmark. */
    Wordmark,

    /** A panel or dialog title. */
    Title,

    /** The small label above a group of controls. */
    SectionLabel,

    /** Explainers and descriptions. */
    Body,

    /** A control label or a button. */
    Label,

    /** A hex, a tone or a ratio shown beside a control. */
    Value,

    /** Generated code. */
    Code,
}

/** The text style [style] names in this type. */
internal operator fun BuilderType.get(style: BuilderTextStyle): TextStyle =
    when (style) {
        BuilderTextStyle.PosterHero -> posterHero
        BuilderTextStyle.Wordmark -> wordmark
        BuilderTextStyle.Title -> title
        BuilderTextStyle.SectionLabel -> sectionLabel
        BuilderTextStyle.Body -> body
        BuilderTextStyle.Label -> label
        BuilderTextStyle.Value -> value
        BuilderTextStyle.Code -> code
    }

/**
 * Text in the builder's own type, inked from the surrounding skin.
 *
 * @param[text] What to show.
 * @param[modifier] Applied to the text layout.
 * @param[style] The part of the builder's type to set it in.
 * @param[emphasis] Picks the ink from the skin's tokens when [color] is left unspecified.
 * @param[color] An explicit ink, for text standing on something other than a panel.
 * @param[textAlign] How lines line up inside the layout.
 * @param[maxLines] The most lines to show before [overflow] applies.
 * @param[overflow] What happens to text that does not fit.
 */
@Composable
public fun BuilderText(
    text: String,
    modifier: Modifier = Modifier,
    style: BuilderTextStyle = BuilderTextStyle.Body,
    emphasis: Emphasis = Emphasis.Primary,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val ink = color.takeOrElse { emphasis.ink(LocalBuilderTokens.current) }
    BasicText(
        text = text,
        modifier = modifier,
        style = LocalBuilderType.current[style].merge(color = ink, textAlign = textAlign),
        overflow = overflow,
        maxLines = maxLines,
    )
}
