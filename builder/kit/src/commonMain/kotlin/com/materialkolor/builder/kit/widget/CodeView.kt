package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.widget_copy
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.token.CodePalette
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import org.jetbrains.compose.resources.stringResource

/** Tags the swatch before a color literal, so a test can count them. */
internal const val CodeSwatchTag: String = "code-swatch"

/** One run of a line, either text or the swatch of the color literal that follows. */
@Immutable
private sealed interface CodePart {
    data class Text(
        val text: AnnotatedString,
    ) : CodePart

    data class Swatch(
        val color: Color,
    ) : CodePart
}

/**
 * Generated code, highlighted from the tokens codegen emits.
 *
 * Each kind of token takes its color from the skin's code palette, and a small swatch sits before
 * every color literal. Lines are numbered, the text can be selected, long lines scroll sideways and
 * only the lines on screen are composed, so a 400 line file scrolls smoothly. Give it a bounded
 * height, since it scrolls its own lines.
 *
 * The copy button sits in the top corner. Selecting text takes a part of the file, the button takes
 * all of it, byte for byte.
 *
 * @param[lines] The file as lines of tokens, as in `GeneratedFile.lines`.
 * @param[onCopy] Called when the copy button is pressed. The caller does the copying.
 * @param[modifier] Applied to the viewer.
 */
@Composable
public fun CodeView(
    lines: List<List<Token>>,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val style = LocalBuilderType.current.code
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val copyLabel = stringResource(Res.string.widget_copy)
    val parts = remember(lines, tokens.codePalette) { lines.map { line -> codeParts(line, tokens.codePalette) } }
    val swatchSize = with(density) { style.fontSize.toDp() }
    val gap = tokens.spacing.extraSmall
    val charWidth = remember(style, density) {
        with(density) {
            measurer
                .measure("0", style)
                .size.width
                .toDp()
        }
    }
    val gutterWidth = charWidth * lines.size.toString().length
    val codeWidth = remember(lines, charWidth, swatchSize, gap) {
        val widest = lines.maxOfOrNull { line ->
            val chars = line.sumOf { token -> token.text.length }
            val swatches = line.count { token -> token.isSwatched }
            charWidth * (chars + 1) + (swatchSize + gap) * swatches
        }
        widest ?: charWidth
    }
    val shape = RoundedCornerShape(tokens.radius.medium)

    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.codeBackground)
            .border(WidgetOutlineWidth, tokens.border, shape),
    ) {
        SelectionContainer {
            Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                LazyColumn(
                    modifier = Modifier
                        .width(gutterWidth + tokens.spacing.medium + codeWidth + tokens.spacing.medium * 2)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(vertical = tokens.spacing.medium),
                ) {
                    itemsIndexed(parts) { index, line ->
                        CodeLine(index + 1, line, gutterWidth, swatchSize)
                    }
                }
            }
        }
        BuilderTooltip(
            text = copyLabel,
            modifier = Modifier.align(Alignment.TopEnd).padding(tokens.spacing.small),
        ) {
            BuilderIconButton(onClick = onCopy, icon = IconId.Copy, contentDescription = copyLabel)
        }
    }
}

@Composable
private fun CodeLine(
    number: Int,
    parts: List<CodePart>,
    gutterWidth: Dp,
    swatchSize: Dp,
) {
    val tokens = LocalBuilderTokens.current
    val style = LocalBuilderType.current.code
    Row(
        modifier = Modifier.padding(horizontal = tokens.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DisableSelection {
            BasicText(
                text = number.toString(),
                modifier = Modifier.width(gutterWidth).clearAndSetSemantics {},
                style = style.merge(color = tokens.textMuted, textAlign = TextAlign.End),
                maxLines = 1,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (part in parts) {
                when (part) {
                    is CodePart.Text -> {
                        BasicText(part.text, style = style.merge(color = tokens.codePalette.plain), softWrap = false)
                    }
                    is CodePart.Swatch -> {
                        Box(
                            modifier = Modifier
                                .testTag(CodeSwatchTag)
                                .padding(end = tokens.spacing.extraSmall)
                                .size(swatchSize)
                                .clip(RoundedCornerShape(tokens.radius.small / 2))
                                .background(part.color)
                                .border(WidgetOutlineWidth, tokens.border, RoundedCornerShape(tokens.radius.small / 2)),
                        )
                    }
                }
            }
        }
    }
}

/** Whether [this] gets a swatch before it. */
private val Token.isSwatched: Boolean
    get() = kind == TokenKind.ColorLiteral && color != null

/**
 * [line] as runs of highlighted text, broken before every color literal to make room for its
 * swatch. A line with no tokens still gets one empty run, so it keeps the height of a line.
 */
private fun codeParts(
    line: List<Token>,
    palette: CodePalette,
): List<CodePart> {
    val parts = mutableListOf<CodePart>()
    var run = AnnotatedString.Builder()

    fun flush() {
        if (run.length > 0) parts += CodePart.Text(run.toAnnotatedString())
        run = AnnotatedString.Builder()
    }
    for (token in line) {
        val argb = token.color
        if (token.kind == TokenKind.ColorLiteral && argb != null) {
            flush()
            parts += CodePart.Swatch(Color(argb))
        }
        run.withStyle(SpanStyle(color = palette[token.kind])) { append(token.text) }
    }
    flush()
    if (parts.none { part -> part is CodePart.Text }) parts += CodePart.Text(AnnotatedString(""))
    return parts
}
