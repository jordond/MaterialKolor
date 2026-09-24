package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
 * This is public API that leans on two foundation internals, the handle visibility test in
 * `CoreTextField` and the handle position `TextFieldSelectionManager` hands it. The b-228 e2e test
 * in `text-toolbar.spec.ts` trips when either moves. Drop it when CMP fixes the single-owner
 * listener, together with the in-page overlays.
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

/**
 * A selection container that leaves a finger out where overlays render in the page (D45), for text
 * over several lines, which [withoutSelectionHandles] cannot reach.
 *
 * Foundation's selection container puts up both of its handle popups once a finger has selected in
 * it, and a popup takes the web mirror over for good. So while the last pointer over [content] was a
 * finger or a pen, the content shows without the container, and a mouse brings the container back.
 * Before any pointer comes by, a coarse pointer counts as a finger. Either way the area stays one
 * focus stop, so the keys that scroll it still reach it.
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
    var touch by remember { mutableStateOf(coarse) }
    val watched = modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val type = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()?.type
                if (type == PointerType.Mouse) {
                    touch = false
                } else if (type == PointerType.Touch || type == PointerType.Stylus || type == PointerType.Eraser) {
                    touch = true
                }
            }
        }
    }
    if (enabled && touch) {
        Box(watched.focusable(), propagateMinConstraints = true) { content() }
    } else {
        SelectionContainer(watched, content)
    }
}
