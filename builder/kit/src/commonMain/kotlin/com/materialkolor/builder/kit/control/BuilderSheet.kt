package com.materialkolor.builder.kit.control

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import com.materialkolor.builder.kit.generated.resources.Res
import com.materialkolor.builder.kit.generated.resources.close
import com.materialkolor.builder.kit.generated.resources.sheet_detent_full
import com.materialkolor.builder.kit.generated.resources.sheet_detent_half
import com.materialkolor.builder.kit.generated.resources.sheet_detent_peek
import com.materialkolor.builder.kit.headless.BottomSheetDetent
import com.materialkolor.builder.kit.headless.BottomSheetState
import com.materialkolor.builder.kit.headless.HeadlessBottomSheet
import com.materialkolor.builder.kit.headless.HeadlessDrawer
import com.materialkolor.builder.kit.layout.DockedPosterBreakpoint
import com.materialkolor.builder.kit.layout.LayoutInfo
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.PanelEdge
import com.materialkolor.builder.kit.skin.headless.overlayStyle
import org.jetbrains.compose.resources.stringResource

/** How a [BuilderSheet] takes the screen. */
public enum class SheetPresentation {
    /** A panel along the end edge, 60% of the width, with the page still showing beside it. */
    EndPanel,

    /** The whole screen. */
    FullScreen,

    ;

    public companion object {
        /** An end panel from 840 dp up, where 60% still leaves room for a form, and the full screen below. */
        public fun of(layout: LayoutInfo): SheetPresentation =
            if (layout.widthDp >= DockedPosterBreakpoint) EndPanel else FullScreen
    }
}

/**
 * A modal sheet for a task that needs more room than a dialog, such as export.
 *
 * The caller picks [presentation] from `LocalLayout`, usually with [SheetPresentation.of]. The
 * sheet has a header with its title and a close button, keeps focus inside while it is open,
 * closes on Esc and on the veil, and hands focus back to [returnFocusTo] once it has gone. It
 * slides in from the end edge, or only fades under reduced motion. Material3 has no side sheet, so
 * every skin draws the headless drawer in its own dress.
 *
 * @param[visible] Whether the sheet is open.
 * @param[onDismissRequest] Called when the sheet asks to close.
 * @param[title] The sheet's name.
 * @param[presentation] An end panel or the whole screen.
 * @param[modifier] Applied to the sheet panel.
 * @param[closeLabel] What the close button says to assistive technology.
 * @param[returnFocusTo] The trigger that opened the sheet.
 * @param[content] The sheet's body, below the header.
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
        content = content,
    )
}

private const val EndPanelFraction = 0.6f

/** The kit's names for the detents, Peek, Half and Full in English. */
@Composable
private fun detentNames(): (BottomSheetDetent) -> String {
    val peek = stringResource(Res.string.sheet_detent_peek)
    val half = stringResource(Res.string.sheet_detent_half)
    val full = stringResource(Res.string.sheet_detent_full)
    return { detent ->
        when (detent) {
            BottomSheetDetent.Peek -> peek
            BottomSheetDetent.Half -> half
            BottomSheetDetent.Full -> full
        }
    }
}

/**
 * A sheet docked to the bottom of its host that rests at peek, half and full. The poster lives in
 * one at Compact.
 *
 * It fills the host it is given and slides inside it, so put it in a box over the content it
 * covers. It drags anywhere on its surface, and its handle takes focus so the arrows, Page Up,
 * Page Down, Home and End move it too. Tab onto a row below the fold raises the sheet until the
 * row shows, and a scrolling body hands a drag back to the sheet once it reaches its top. Read or
 * drive the detent through [state]. Material3's own bottom sheet only has two states, so every skin
 * draws the headless sheet in its own dress.
 *
 * @param[state] The sheet's detent, from `rememberBottomSheetState`.
 * @param[label] The sheet's name, read on the handle.
 * @param[modifier] Applied to the host the sheet slides inside.
 * @param[detentLabel] What the handle reads as its state at each detent, the kit's name for the
 * detent unless the caller words it.
 * @param[peekHeight] How much of the sheet shows at peek.
 * @param[content] The sheet's body, below the handle.
 */
@Composable
public fun BuilderBottomSheet(
    state: BottomSheetState,
    label: String,
    modifier: Modifier = Modifier,
    detentLabel: (BottomSheetDetent) -> String = detentNames(),
    peekHeight: Dp = OverlayMetrics.sheetPeekHeight,
    content: @Composable ColumnScope.() -> Unit,
) {
    HeadlessBottomSheet(
        state = state,
        label = label,
        detentLabel = detentLabel,
        peekHeight = peekHeight,
        style = overlayStyle(LocalSkin.current.library),
        modifier = modifier,
        content = content,
    )
}
