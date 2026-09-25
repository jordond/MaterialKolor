package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.close
import com.materialkolor.builder.kit.headless.HeadlessDrawer
import com.materialkolor.builder.kit.layout.DockedPosterBreakpoint
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.PanelEdge
import org.jetbrains.compose.resources.stringResource

/**
 * How a [BuilderSheet] takes the screen.
 */
public enum class SheetPresentation {
    /**
     * A panel along the end edge, 60% of the width, with the page still showing beside it.
     */
    EndPanel,

    /**
     * The whole screen.
     */
    FullScreen,

    ;

    public companion object {
        /**
         * An end panel from 840 dp up, where 60% still leaves room for a form, and the full screen below.
         */
        public fun of(layout: LayoutInfo): SheetPresentation =
            if (layout.widthDp >= DockedPosterBreakpoint) EndPanel else FullScreen
    }
}

/**
 * A modal sheet for a task that needs more room than a dialog, such as export.
 *
 * The caller picks [presentation] from `LocalLayout`, usually with [SheetPresentation.of]. The
 * sheet has a header with its title, an optional [subtitle] and a close button, and an optional
 * [footer] along the bottom under a hairline, all three the same room in from the sheet's edges. It
 * keeps focus inside while it is open, closes on Esc and on the veil, and hands focus back to
 * [returnFocusTo] once it has gone. It slides in from the end edge, or only fades under reduced
 * motion. Material3 has no side sheet, so every skin draws the headless drawer in its own dress.
 *
 * @param[visible] Whether the sheet is open.
 * @param[onDismissRequest] Called when the sheet asks to close.
 * @param[title] The sheet's name.
 * @param[presentation] An end panel or the whole screen.
 * @param[modifier] Applied to the sheet panel.
 * @param[closeLabel] What the close button says to assistive technology.
 * @param[returnFocusTo] The trigger that opened the sheet.
 * @param[subtitle] A quieter line under the title, or null for none.
 * @param[footer] What sits along the bottom, such as the sheet's actions, or null for no footer.
 * @param[content] The sheet's body, between the header and the footer.
 */
@Composable
public fun BuilderSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    presentation: SheetPresentation,
    modifier: Modifier = Modifier,
    closeLabel: String = stringResource(Res.string.close),
    returnFocusTo: FocusRequester? = null,
    subtitle: String? = null, // b-511
    footer: (@Composable () -> Unit)? = null, // b-511
    content: @Composable ColumnScope.() -> Unit,
) {
    HeadlessDrawer(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = title,
        closeLabel = closeLabel,
        edge = PanelEdge.End,
        widthFraction = when (presentation) {
            SheetPresentation.EndPanel -> EndPanelFraction
            SheetPresentation.FullScreen -> 1f
        },
        widthCap = Dp.Infinity,
        style = overlayStyle(LocalSkin.current.library),
        returnFocusTo = returnFocusTo,
        modifier = modifier,
        subtitle = subtitle,
        footer = footer,
        content = content,
    )
}

private const val EndPanelFraction = 0.6f
