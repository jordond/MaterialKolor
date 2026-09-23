package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.headless.HeadlessDrawer
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.PanelEdge
import com.materialkolor.builder.kit.skin.headless.overlayStyle

/**
 * A modal panel along the start edge, the projects drawer.
 *
 * It is 320 dp wide, or 85% of a narrower screen, and takes the whole screen at Compact. A header
 * holds its title and a close button. It keeps focus inside while it is open, closes on Esc and on
 * the veil, and hands focus back to [returnFocusTo] once it has gone. It slides in from the start
 * edge, or only fades under reduced motion. Material3 has no side sheet, so it wears Material's colours and shapes over the
 * headless drawer like every other skin.
 *
 * @param[visible] Whether the panel is open.
 * @param[onDismissRequest] Called when the panel asks to close.
 * @param[title] The panel's name.
 * @param[modifier] Applied to the panel.
 * @param[closeLabel] What the close button says to assistive technology.
 * @param[returnFocusTo] The trigger that opened the panel.
 * @param[content] The panel's body, below the header.
 */
@Composable
public fun BuilderSidePanel(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    closeLabel: String = "Close",
    returnFocusTo: FocusRequester? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val compact = when (LocalLayout.current.windowClass) {
        WindowClass.Compact -> true
        WindowClass.Medium, WindowClass.Expanded -> false
    }
    HeadlessDrawer(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = title,
        closeLabel = closeLabel,
        edge = PanelEdge.Start,
        widthFraction = if (compact) 1f else OverlayMetrics.sidePanelNarrowFraction,
        widthCap = if (compact) Dp.Infinity else OverlayMetrics.sidePanelWidth,
        style = overlayStyle(LocalSkin.current.library),
        returnFocusTo = returnFocusTo,
        modifier = modifier,
        content = content,
    )
}
