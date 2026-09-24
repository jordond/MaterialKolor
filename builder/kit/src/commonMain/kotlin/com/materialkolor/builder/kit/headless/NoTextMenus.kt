package com.materialkolor.builder.kit.headless

import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * Swallows a secondary press before a text field sees it, since foundation's text context menu is
 * a popup. Nothing else in the builder answers one.
 */
internal suspend fun PointerInputScope.swallowSecondaryPresses() {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.buttons.isSecondaryPressed) {
                event.changes.forEach { change -> if (change.changedToDownIgnoreConsumed()) change.consume() }
            }
        }
    }
}

/** A text context menu that shows nothing, standing in for foundation's popup. */
internal object NoTextContextMenu : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) = Unit
}

/**
 * A text selection toolbar that shows nothing, standing in for the web's popup in a pane host with no
 * host above it to pass the page's own toolbar through.
 */
internal object NoTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit

    override fun hide() = Unit
}
