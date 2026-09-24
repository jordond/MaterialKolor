package com.materialkolor.builder.kit.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.control.foldState
import com.materialkolor.builder.kit.control.get
import com.materialkolor.builder.kit.control.ink
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.selectable_text_name
import com.materialkolor.builder.kit.headless.LocalOverlaysInTree
import com.materialkolor.builder.kit.headless.TouchlessSelectionContainer
import com.materialkolor.builder.kit.headless.withoutSelectionHandles
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.kit.token.LocalBuilderType
import org.jetbrains.compose.resources.stringResource

// b-228a

/**
 * Text someone may have to select and copy by hand, such as a link, or a hex the clipboard turned
 * down.
 *
 * One line shows in a read only field with no box of its own, so it reads like the text around it.
 * A mouse and the keys select in it, and a long press selects a word and brings up the page's text
 * toolbar with Copy and Select all. On the web it puts up no selection handles, since a handle would
 * take the page's accessibility mirror over (D45).
 *
 * Text over several lines shows in a selection container, where on the web a finger does not select
 * (D45) and a mouse and the keys still do.
 *
 * @param[text] The text to show.
 * @param[modifier] Applied to the text.
 * @param[style] How the text is set.
 * @param[label] What the text is, the name its field goes by.
 */
@Composable
public fun SelectableText(
    text: String,
    modifier: Modifier = Modifier,
    style: BuilderTextStyle = BuilderTextStyle.Body,
    label: String = stringResource(Res.string.selectable_text_name),
) {
    if (text.any { char -> char == '\n' || char == '\r' }) {
        TouchlessSelectionContainer(modifier) { BuilderText(text = text, style = style) }
        return
    }
    val ink = Emphasis.Primary.ink(LocalBuilderTokens.current)
    val inTree = LocalOverlaysInTree.current
    var shown by remember { mutableStateOf(TextFieldValue(text)) }
    BasicTextField(
        value = shown.copy(text = text),
        onValueChange = { next -> shown = next },
        modifier = modifier.foldState(label, null).semantics { contentDescription = label },
        readOnly = true,
        textStyle = LocalBuilderType.current[style].merge(color = ink),
        singleLine = true,
        decorationBox = { field ->
            Box(Modifier.withoutSelectionHandles(inTree), propagateMinConstraints = true) { field() }
        },
    )
}
