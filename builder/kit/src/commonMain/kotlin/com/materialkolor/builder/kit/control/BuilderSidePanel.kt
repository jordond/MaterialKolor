package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.close
import com.materialkolor.builder.kit.headless.HeadlessDrawer
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.layout.WindowClass
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.PanelEdge
import org.jetbrains.compose.resources.stringResource

/**
 * A modal panel along the start edge, the projects drawer.
 *
 * It is 380 dp wide, or 85% of a narrower screen, and takes the whole screen at Compact. A header
 * holds its title, an optional [subtitle] and a close button, and an optional [footer] sits along
 * the bottom under a hairline. The header, the body and the footer keep the same room from the
 * panel's edges, and the inner corners take the skin's panel rounding. It keeps focus inside while it is open, closes on Esc and on
 * the veil, and hands focus back to [returnFocusTo] once it has gone. It slides in from the start
 * edge, or only fades under reduced motion. Material3 has no side sheet, so it uses Material's colours and shapes over the
 * headless drawer like every other skin.
 *
 * @param[visible] Whether the panel is open.
 * @param[onDismissRequest] Called when the panel asks to close.
 * @param[title] The panel's name.
 * @param[modifier] Applied to the panel.
 * @param[closeLabel] What the close button says to assistive technology.
 * @param[returnFocusTo] The trigger that opened the panel.
 * @param[subtitle] A quieter line under the title, or null for none.
 * @param[footer] What sits along the bottom, such as the panel's actions, or null for no footer.
 * @param[content] The panel's body, between the header and the footer.
 */
@Composable
public fun BuilderSidePanel(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    closeLabel: String = stringResource(Res.string.close),
    returnFocusTo: FocusRequester? = null,
    subtitle: String? = null,
    footer: (@Composable () -> Unit)? = null,
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
        subtitle = subtitle,
        footer = footer,
        content = content,
    )
}
