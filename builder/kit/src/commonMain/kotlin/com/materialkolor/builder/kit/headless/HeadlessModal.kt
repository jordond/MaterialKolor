package com.materialkolor.builder.kit.headless

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.materialkolor.builder.kit.control.FoldedRole
import com.materialkolor.builder.kit.control.LocalFoldsStateIntoName
import com.materialkolor.builder.kit.control.roleLessName
import com.materialkolor.builder.kit.control.stateName
import com.materialkolor.builder.kit.skin.headless.isOverlayShown
import com.materialkolor.builder.kit.skin.headless.rememberOverlayVisibility
import com.materialkolor.builder.kit.skin.headless.scrimEnter
import com.materialkolor.builder.kit.skin.headless.scrimExit

/**
 * A modal layer over the page, the ground every dialog, side panel and sheet stands on.
 *
 * It lives in a focusable layer of its own, a dialog window or a modal layer of the overlay host,
 * so Tab stays inside it until it closes (AR-09). In the host the page under it also leaves the
 * semantics tree while it is open (AR-11). Esc and a click on the veil both ask to close it. Focus
 * moves to the first thing inside that can take it, or to the panel itself when nothing can, and
 * once the layer is gone it goes back to [returnFocusTo]. The layer stays up while [content]
 * animates out, and [content] animates itself through `animateEnterExit`.
 *
 * @param[visible] Whether the layer is wanted.
 * @param[onDismissRequest] Called on Esc and on a click on the veil.
 * @param[scrim] The veil drawn behind [content].
 * @param[contentAlignment] Where [content] sits in the layer.
 * @param[returnFocusTo] The trigger that opened the layer, focused again once it is gone.
 * @param[content] The panel.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HeadlessModal(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    scrim: Color,
    contentAlignment: Alignment,
    returnFocusTo: FocusRequester?,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val state = rememberOverlayVisibility(visible)
    val shown = state.isOverlayShown(visible)
    ReturnFocusWhenGone(shown, returnFocusTo, currentOverlayHost())
    if (!shown) return
    val dismiss by rememberUpdatedState(onDismissRequest)
    val host = inTreeOverlayHost()
    if (host != null) {
        val layer = remember { OverlayLayer(OverlayKind.Modal) }
        OverlayPortal(host, layer, open = visible) {
            ModalLayer(state, { dismiss() }, scrim, contentAlignment, content)
        }
        return
    }
    // b-315d
    // A dialog window has no layer of the host around it to hand the page's keys on.
    val keys = LocalOverlayKeys.current
    Dialog(
        onDismissRequest = { dismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            scrimColor = Color.Transparent,
            animateTransition = false,
        ),
    ) {
        ModalLayer(state, { dismiss() }, scrim, contentAlignment, content, keys = keys)
    }
}

/** The veil and the panel over it, the same in a dialog window and in the overlay host. */
@Composable
private fun ModalLayer(
    state: MutableTransitionState<Boolean>,
    dismiss: () -> Unit,
    scrim: Color,
    contentAlignment: Alignment,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
    keys: ((KeyEvent) -> Boolean)? = null, // b-315d
) {
    AnimatedVisibility(
        visibleState = state,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        val focus = remember { OverlayFocus() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .overlayKeys { keys } // b-315d
                .onKeyEvent { event ->
                    val escape = event.type == KeyEventType.KeyDown && event.key == Key.Escape
                    if (escape) dismiss()
                    escape
                },
            contentAlignment = contentAlignment,
        ) {
            Box(
                modifier = Modifier
                    .animateEnterExit(enter = scrimEnter(), exit = scrimExit())
                    .fillMaxSize()
                    .background(scrim)
                    .pointerInput(Unit) { detectTapGestures { dismiss() } },
            )
            Box(focus.modifier) {
                content()
            }
        }
        LaunchedEffect(Unit) { focus.enter() }
    }
}

/**
 * Focuses [target] once an overlay that was shown has gone.
 *
 * It waits for the layer to leave rather than for the request to close, because a node in the page
 * cannot take focus while a focusable layer still sits over it. In the overlay [host] it leaves
 * focus alone when it has already moved on to the page or another layer, the way a Rename row hands
 * it to a name field.
 */
@Composable
internal fun ReturnFocusWhenGone(
    shown: Boolean,
    target: FocusRequester?,
    host: OverlayHostState? = null,
) {
    var wasShown by remember { mutableStateOf(false) }
    LaunchedEffect(shown) {
        if (shown) {
            wasShown = true
        } else if (wasShown) {
            wasShown = false
            if (host?.holdsFocus() != true) target?.requestFocus()
        }
    }
}

/**
 * Names a modal pane, a dialog, a sheet or a side panel, by its [title], which is its pane title.
 *
 * The web mirror in CMP 1.12.1 carries neither the pane title nor a dialog role, so there the pane
 * also reads its title with the dialog's role word as its text, "Delete theme?, dialog" (D37, D40),
 * and [modalTitle] keeps the title the pane shows from being read twice. Drop both once CMP mirrors
 * `dialog` and `paneTitle`, and recheck that on the CMP 1.13 bump.
 */
@Composable
internal fun Modifier.modalPane(title: String): Modifier {
    val folds = LocalFoldsStateIntoName.current
    val spoken = stateName(title, state = null, role = FoldedRole.Dialog)
    return semantics {
        paneTitle = title
        if (folds) roleLessName(spoken, asText = true)
    }
}

/**
 * Keeps the title a modal pane shows out of the web mirror, where [modalPane] reads it with the
 * role word already. Elsewhere it adds nothing.
 */
@Composable
internal fun Modifier.modalTitle(): Modifier = if (LocalFoldsStateIntoName.current) clearAndSetSemantics { } else this
