package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.materialkolor.builder.kit.headless.HeadlessDropdown
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.SkinLibrary
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.material.materialPopoverStyle
import com.materialkolor.builder.kit.token.LocalBuilderTokens

/**
 * A popover that opens under [anchor], holds whatever it is given and stays open while it is used.
 * It hangs 8 dp under the anchor with its end lined up with the anchor's end, and moves along to
 * stay inside the window.
 *
 * A [BuilderMenu] closes as soon as a row is chosen. This one never closes on a pick, so a list
 * inside can be picked from again and again with the popover still up. Esc and a click outside call
 * [onDismissRequest]. Focus starts on [initialFocus], or else on the first thing inside that takes
 * it. Down and Up move through the rows, and Home and End go to the first and the last. Once the
 * popover has gone, focus goes back to [returnFocusTo], or else to the anchor, unless something
 * inside already moved it elsewhere. Where overlays render in the page it opens in the overlay
 * host, and anywhere else in a focusable popup.
 *
 * Material3 draws it in Material's container with its large corner and a menu's shadow, and the other
 * skins in their own popover style. Each keeps 8 dp of room inside its edge.
 *
 * @param[expanded] Whether the popover is open.
 * @param[onDismissRequest] Called when the popover asks to close. It must set [expanded] to false,
 * since the popover stays open until it does.
 * @param[modifier] Applied to the box holding [anchor].
 * @param[initialFocus] What takes focus as the popover opens, such as the current row of a list.
 * @param[returnFocusTo] What gets focus back once the popover has gone, when that is not the anchor.
 * @param[anchor] What the popover opens from, usually a button that sets [expanded].
 * @param[content] What the popover holds, top to bottom.
 */
@Composable
public fun BuilderPopover(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    initialFocus: FocusRequester? = null,
    returnFocusTo: FocusRequester? = null,
    anchor: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalBuilderTokens.current
    val style = when (LocalSkin.current.library) {
        SkinLibrary.Material3 -> materialPopoverStyle()
        SkinLibrary.Custom -> customOverlayStyle(tokens)
    }
    val trigger = remember { FocusRequester() }
    Box(modifier.focusRequester(trigger)) {
        anchor()
        HeadlessDropdown(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            style = style,
            initialFocus = initialFocus,
            returnFocusTo = returnFocusTo ?: trigger,
            popover = true,
        ) { _ -> content() }
    }
}
