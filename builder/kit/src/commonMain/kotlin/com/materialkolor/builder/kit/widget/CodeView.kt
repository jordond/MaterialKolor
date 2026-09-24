package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.codegen.dsl.Token
import com.materialkolor.builder.codegen.dsl.TokenKind
import com.materialkolor.builder.kit.control.BuilderIconButton
import com.materialkolor.builder.kit.control.BuilderTooltip
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.code_view_name
import com.materialkolor.builder.kit.generated.resources.widget_copy
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.skin.headless.controlRing
import com.materialkolor.builder.kit.token.CodePalette
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Tags the swatch before a color literal, so a test can count them. */
internal const val CodeSwatchTag: String = "code-swatch"

/** Tags the code area, the one focus stop of the lines, so a test can focus it and press keys. */
internal const val CodeScrollTag: String = "code-scroll"

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
 * height, since it scrolls its own lines. The code area is one focus stop, the selection's own, and
 * wears the focus ring outside its frame while it has focus. There the arrows, Page Up and Page Down
 * scroll it without a pointer, and the copy keys copy what is selected.
 *
 * The copy button sits in the top corner. Selecting text takes a part of the file, the button takes
 * all of it, byte for byte.
 *
 * On the web the list of lines goes by [label], since the page reads it as a list and a list with
 * no name is announced as nothing but a list (S5 row 33).
 *
 * @param[lines] The file as lines of tokens, as in `GeneratedFile.lines`.
 * @param[onCopy] Called when the copy button is pressed. The caller does the copying.
 * @param[modifier] Applied to the viewer.
 * @param[label] What the code is, such as the file's name, read out as the name of its lines.
 */
@Composable
public fun CodeView(
    lines: List<List<Token>>,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.code_view_name),
) {
    val listName = if (LocalFoldsStateIntoName.current) Modifier.semantics { contentDescription = label } else Modifier
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
    val look = remember(style, tokens, gutterWidth, swatchSize) {
        CodeLineLook(
            gutter = style.merge(color = tokens.textMuted, textAlign = TextAlign.End),
            code = style.merge(color = tokens.codePalette.plain),
            gutterWidth = gutterWidth,
            swatchSize = swatchSize,
            swatchShape = RoundedCornerShape(tokens.radius.small / 2),
        )
    }
    val listState = rememberLazyListState()
    val sideways = rememberScrollState()
    val codeFocus = remember { CodeFocus() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .controlRing(codeFocus.interactions, shape)
            .clip(shape)
            .background(tokens.codeBackground)
            .border(tokens.outlineWidth, tokens.border, shape),
    ) {
        // The selection is its own focus target and asks for focus when a drag starts, so it is the
        // one stop. The scroll keys and the focus flag go on its modifier, ahead of that target.
        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .testTag(CodeScrollTag)
                .semantics {
                    focused = codeFocus.focused
                    requestFocus { codeFocus.requester.requestFocus(FocusDirection.Enter) }
                }.focusRequester(codeFocus.requester)
                .onFocusChanged { state -> codeFocus.update(state.isFocused) }
                .onKeyEvent { event -> scope.scrollOnKey(event, listState, sideways) },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(sideways),
            ) {
                LazyColumn(
                    modifier = listName
                        .width(gutterWidth + tokens.spacing.medium + codeWidth + tokens.spacing.medium * 2)
                        .fillMaxHeight(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = tokens.spacing.medium),
                ) {
                    itemsIndexed(parts) { index, line ->
                        CodeLine(index + 1, line, look)
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

/**
 * The focus of the code area, which the selection's own focus target holds. That target sends no
 * focus interactions and reports no focus, so this turns its focus changes into the interactions
 * [controlRing] listens for, and gives the area a focused state and a focus request to report.
 */
@Stable
private class CodeFocus {
    val interactions: MutableInteractionSource = MutableInteractionSource()
    val requester: FocusRequester = FocusRequester()
    var focused: Boolean by mutableStateOf(false)
        private set
    private var held: FocusInteraction.Focus? = null

    fun update(isFocused: Boolean) {
        focused = isFocused
        val focus = held
        if (isFocused && focus == null) {
            val next = FocusInteraction.Focus()
            held = next
            interactions.tryEmit(next)
        } else if (!isFocused && focus != null) {
            held = null
            interactions.tryEmit(FocusInteraction.Unfocus(focus))
        }
    }
}

/**
 * What every line of one [CodeView] is drawn with, worked out once so a line scrolling into view
 * does not merge styles or build shapes again.
 */
@Immutable
private data class CodeLineLook(
    val gutter: TextStyle,
    val code: TextStyle,
    val gutterWidth: Dp,
    val swatchSize: Dp,
    val swatchShape: Shape,
)

@Composable
private fun CodeLine(
    number: Int,
    parts: List<CodePart>,
    look: CodeLineLook,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = Modifier.padding(horizontal = tokens.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DisableSelection {
            BasicText(
                text = number.toString(),
                modifier = Modifier.width(look.gutterWidth).clearAndSetSemantics {},
                style = look.gutter,
                maxLines = 1,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (part in parts) {
                when (part) {
                    is CodePart.Text -> {
                        BasicText(part.text, style = look.code, softWrap = false)
                    }
                    is CodePart.Swatch -> {
                        Box(
                            modifier = Modifier
                                .testTag(CodeSwatchTag)
                                .padding(end = tokens.spacing.extraSmall)
                                .size(look.swatchSize)
                                .clip(look.swatchShape)
                                .background(part.color)
                                .border(tokens.outlineWidth, tokens.border, look.swatchShape),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Scrolls the code on the keys a reader expects, since a lazy list scrolls for a pointer but not for
 * a keyboard. The arrows move a line, Page Up and Page Down move a screen less one line, and Home
 * and End jump to the ends of the file. A key with a modifier held is left for the selection.
 */
private fun CoroutineScope.scrollOnKey(
    event: KeyEvent,
    list: LazyListState,
    sideways: ScrollState,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    if (event.isShiftPressed || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
    val info = list.layoutInfo
    val first = info.visibleItemsInfo.firstOrNull() ?: return false
    val line = first.size.toFloat()
    val page = (info.viewportSize.height - line).coerceAtLeast(line)
    when (event.key) {
        Key.DirectionDown -> launch { list.scrollBy(line) }
        Key.DirectionUp -> launch { list.scrollBy(-line) }
        Key.PageDown -> launch { list.scrollBy(page) }
        Key.PageUp -> launch { list.scrollBy(-page) }
        Key.DirectionRight -> launch { sideways.scrollBy(line) }
        Key.DirectionLeft -> launch { sideways.scrollBy(-line) }
        Key.MoveHome -> launch { list.scrollToItem(0) }
        Key.MoveEnd -> launch { list.scrollToItem((info.totalItemsCount - 1).coerceAtLeast(0)) }
        else -> return false
    }
    return true
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
