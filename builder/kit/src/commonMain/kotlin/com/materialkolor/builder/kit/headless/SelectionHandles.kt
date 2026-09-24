package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.offset
import com.materialkolor.builder.kit.layout.LocalLayout

// b-228a

/**
 * Keeps foundation's touch selection handles off a single line field's text where overlays render
 * in the page (D45), and changes nothing anywhere else.
 *
 * The handles are popups, and a popup takes the web mirror over for good (D40). Foundation only
 * puts a handle up while its foot, which sits on the bottom edge of its line, lies inside the part
 * of the text its clipping parents leave visible. So the text is clipped one pixel short of its own
 * bottom and the pixel is given back as layout, which leaves the field its size and hides nothing
 * anyone can see, but puts every handle's foot outside. A long press still selects a word, a drag
 * widens it, and the page's text toolbar still shows over it.
 *
 * It is built on public API but leans on two foundation internals, the handle visibility test in
 * `CoreTextField` and the handle position `TextFieldSelectionManager` hands it. The b-228 e2e test
 * in `text-toolbar.spec.ts` trips when either moves. Drop it when CMP fixes the single-owner
 * listener, together with the in-page overlays.
 *
 * The clip costs the text its bottom pixel, so while the field has focus the caret and the
 * selection highlight each lose their bottom pixel too.
 *
 * Goes on the box that holds a field's inner text, never on the field as a whole, since the clip
 * has to meet the text's own bottom edge. A multi-line field is out of its reach, since a handle on
 * an upper line has its foot inside the text.
 *
 * @param[enabled] Whether the handles stay off. Overlays in the page by default, and a composable
 *   passes `LocalOverlaysInTree` so a desktop test can turn it on.
 */
internal fun Modifier.withoutSelectionHandles(enabled: Boolean = overlaysInTree): Modifier {
    if (!enabled) return this
    return layout { measurable, constraints ->
        val clipped = measurable.measure(constraints.offset(vertical = -1))
        layout(clipped.width, constraints.constrainHeight(clipped.height + 1)) { clipped.place(0, 0) }
    }.clipToBounds()
        .layout { measurable, constraints ->
            val text = measurable.measure(constraints.offset(vertical = 1))
            layout(text.width, constraints.constrainHeight(text.height - 1)) { text.place(0, 0) }
        }
}

// b-228b

/**
 * Holds the inner text of a single line field the kit does not draw, such as a preview's Material3
 * sample field, so foundation puts up no touch selection handles on it where overlays render in the
 * page (D45). Anywhere else it only holds the text.
 *
 * Build the field from its parts, a foundation field inside a decoration box, and hand this the
 * inner text the decoration box is given. It goes on nothing bigger, and a multi-line field is out
 * of its reach, since a handle on an upper line has its foot inside the text. The caret and the
 * selection highlight each lose their bottom pixel.
 *
 * It leans on two foundation internals, and the b-228 e2e tests in `text-toolbar.spec.ts` trip
 * when either moves. Drop it when CMP fixes the single-owner listener, together with the in-page
 * overlays.
 *
 * @param[innerTextField] The inner text a decoration box is given.
 */
@Composable
public fun InnerTextWithoutHandles(innerTextField: @Composable () -> Unit) {
    val inTree = LocalOverlaysInTree.current
    Box(Modifier.withoutSelectionHandles(inTree), propagateMinConstraints = true) { innerTextField() }
}

/**
 * A selection container that leaves a finger out where overlays render in the page (D45), for text
 * over several lines, which [withoutSelectionHandles] cannot reach.
 *
 * Foundation's selection container puts up both of its handle popups once a finger has selected in
 * it, and a popup takes the web mirror over for good. So while the last pointer over [content] was a
 * finger or a pen, the content shows without the container, and a mouse or a key brings the
 * container back. Before any pointer comes by, a coarse pointer counts as a finger. Either way the
 * area stays one focus stop, so the keys that scroll it still reach it.
 *
 * The swap puts the focus target on another node, so focus held when the input changes is asked for
 * again on the new one. A key brings the container back with focus kept, and its copy keys copy what
 * a mouse selected. Foundation's container has no keys that select, so a keyboard alone copies the
 * whole text with the caller's copy button.
 *
 * A finger loses selecting the text with a long press, and still scrolls. Drop this with
 * [withoutSelectionHandles].
 *
 * @param[modifier] Applied to the container, or to the box that stands in for it, ahead of its
 *   focus target.
 * @param[enabled] Whether a finger is left out, by default where overlays render in the page.
 * @param[content] The text.
 */
@Composable
internal fun TouchlessSelectionContainer(
    modifier: Modifier = Modifier,
    enabled: Boolean = LocalOverlaysInTree.current,
    content: @Composable () -> Unit,
) {
    val coarse = LocalLayout.current.coarsePointer
    val input = remember { LastInput(coarse) }
    val requester = remember { FocusRequester() }
    val touchless = enabled && input.touch
    // Read while composing the swap, before the old focus target leaves, so a focus that moved on by
    // itself, as with Tab, is not pulled back.
    val refocus = remember(touchless) { input.focused }
    LaunchedEffect(touchless) {
        if (refocus) requester.requestFocus()
    }
    val watched = modifier
        .focusRequester(requester)
        .onFocusChanged { state -> input.focused = state.hasFocus }
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) input.touch = false
            false
        }.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val type = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()?.type
                    if (type == PointerType.Mouse) {
                        input.touch = false
                    } else if (type == PointerType.Touch || type == PointerType.Stylus || type == PointerType.Eraser) {
                        input.touch = true
                    }
                }
            }
        }
    if (touchless) {
        Box(watched.focusable(), propagateMinConstraints = true) { content() }
    } else {
        SelectionContainer(watched, content)
    }
}

/**
 * What [TouchlessSelectionContainer] knows of the input that last came its way, whether it was a
 * finger or a pen, and whether its area holds focus. The focus is a plain field, since only the
 * swap reads it.
 */
@Stable
private class LastInput(
    touch: Boolean,
) {
    var touch: Boolean by mutableStateOf(touch)
    var focused: Boolean = false
}
