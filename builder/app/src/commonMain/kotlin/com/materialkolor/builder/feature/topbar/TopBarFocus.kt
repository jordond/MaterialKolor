package com.materialkolor.builder.feature.topbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.materialkolor.builder.kit.skin.LocalSkin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The top bar controls that can hold keyboard focus.
 */
internal enum class TopBarControl {
    Library,
    Commands,
    Undo,
    Redo,
    History,
    Share,
    Export,
    More,
}

/**
 * Remembers which top bar control has focus, so a skin switch that rebuilds the controls in
 * another library's components hands focus back to the same one.
 *
 * A switch moves the whole workspace, which takes focus away before the new skin has composed. So
 * a control that loses focus is only forgotten a frame later, unless something has taken or given
 * focus in the meantime. Focus that really went somewhere else is forgotten that way too.
 */
internal class TopBarFocus(
    private val scope: CoroutineScope,
) {
    private val requesters = mutableMapOf<TopBarControl, FocusRequester>()

    /**
     * Counts focus changes, so a late forget can tell whether anything happened since.
     */
    private var changes = 0

    /**
     * The control holding focus, or the one that just lost it. Plain, since nothing draws from it.
     */
    var focused: TopBarControl? = null
        private set

    /**
     * The control holding focus right now, or null.
     */
    var holding: TopBarControl? = null
        private set

    fun requester(control: TopBarControl): FocusRequester = requesters.getOrPut(control) { FocusRequester() }

    fun onFocusChanged(
        control: TopBarControl,
        hasFocus: Boolean,
    ) {
        changes++
        if (hasFocus) {
            focused = control
            holding = control
            return
        }
        if (holding == control) holding = null
        if (focused != control) return
        val lostAt = changes
        scope.launch {
            withFrameNanos {}
            if (changes == lostAt) focused = null
        }
    }

    /**
     * Puts focus back on [control] after it was rebuilt in another form at a new width, the library
     * switcher turning from its segmented row into its dropdown or back. Only a control that
     * had focus a moment ago takes it, so a resize never pulls focus into the top bar.
     */
    fun restoreAfterRefit(control: TopBarControl) {
        if (focused != control) return
        // A control this width does not show has no node to take focus, and that is fine.
        runCatching { requester(control).requestFocus() }
    }
}

/**
 * A [TopBarFocus] that puts focus back after every skin switch. Call it outside the skin's top bar
 * region so it lives across the switch. The library switcher asks for focus back itself when a
 * resize changes its form, through [TopBarFocus.restoreAfterRefit].
 */
@Composable
internal fun rememberTopBarFocus(): TopBarFocus {
    val scope = rememberCoroutineScope()
    val focus = remember(scope) { TopBarFocus(scope) }
    val skin = LocalSkin.current
    val restore = remember(skin) { focus.focused }
    LaunchedEffect(skin) {
        // A flavor switch within one library keeps the controls, and focus with them.
        val control = restore?.takeIf { control -> control != focus.holding } ?: return@LaunchedEffect
        // A control this width does not show has no node to take focus, and that is fine.
        runCatching { focus.requester(control).requestFocus() }
    }
    return focus
}

/**
 * Lets [focus] track this control as [control] and bring focus back to it.
 */
internal fun Modifier.topBarFocus(
    focus: TopBarFocus,
    control: TopBarControl,
): Modifier =
    focusRequester(focus.requester(control))
        .onFocusChanged { state -> focus.onFocusChanged(control, state.hasFocus) }
