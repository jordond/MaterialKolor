package com.materialkolor.builder.kit.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.kit.a11y.Announcer
import com.materialkolor.builder.kit.a11y.LocalAnnouncer
import com.materialkolor.builder.kit.headless.OverlayTopSlot
import com.materialkolor.builder.kit.skin.headless.OverlayMetrics
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import com.materialkolor.builder.kit.skin.material.MaterialToast
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import kotlinx.coroutines.delay

/**
 * How long a toast stays before it goes by itself.
 */
public enum class ToastDuration(
    internal val millis: Long?,
) {
    /**
     * Four seconds, for a plain confirmation.
     */
    Short(4_000),

    /**
     * Ten seconds, for a toast with an action such as Undo.
     */
    Long(10_000),

    /**
     * Until someone acts on it or it is pushed out by newer toasts.
     */
    Indefinite(null),
}

/**
 * One message in a [BuilderToastHost].
 *
 * @property[message] What the toast says.
 * @property[actionLabel] The label of its one action, or null for none.
 * @property[duration] How long it stays.
 */
@Stable
public class BuilderToast internal constructor(
    internal val id: Long,
    public val message: String,
    public val actionLabel: String?,
    internal val onAction: (() -> Unit)?,
    public val duration: ToastDuration,
)

/**
 * The toasts on screen, oldest first, never more than [MaxToasts].
 *
 * A new toast pushes the oldest one out once the stack is full, so a burst of edits never buries the
 * canvas.
 */
@Stable
public class BuilderToastHostState {
    private val shown = mutableStateListOf<BuilderToast>()
    private var nextId = 0L

    /**
     * The toasts on screen, oldest first.
     */
    public val toasts: List<BuilderToast>
        get() = shown

    /**
     * Shows [message] and returns the toast, which [dismiss] takes back.
     *
     * @param[message] What the toast says.
     * @param[actionLabel] The label of an action such as Undo. It needs [onAction] with it.
     * @param[duration] How long the toast stays.
     * @param[onAction] What the action does. The toast closes first.
     */
    public fun show(
        message: String,
        actionLabel: String? = null,
        duration: ToastDuration = if (actionLabel == null) ToastDuration.Short else ToastDuration.Long,
        onAction: (() -> Unit)? = null,
    ): BuilderToast {
        require((actionLabel == null) == (onAction == null)) { "A toast action needs both a label and something to do" }
        val toast = BuilderToast(nextId++, message, actionLabel, onAction, duration)
        shown += toast
        while (shown.size > MaxToasts) shown.removeAt(0)
        return toast
    }

    /**
     * Takes [toast] off the screen, if it is still there.
     */
    public fun dismiss(toast: BuilderToast) {
        shown.remove(toast)
    }

    /**
     * The id of the newest toast read out through an [Announcer], so no toast is read twice.
     */
    private var announcedThrough = -1L

    /**
     * Reads out through [announcer] every toast on screen that has not been read yet.
     */
    internal fun announceNew(announcer: Announcer) {
        for (toast in shown.toList()) {
            if (toast.id <= announcedThrough) continue
            announcedThrough = toast.id
            announcer.announce(toast.announcement)
        }
    }

    public companion object {
        /**
         * The most toasts the host stacks at once.
         */
        public const val MaxToasts: Int = 3
    }
}

/**
 * A toast host state that lives as long as the composition that remembers it.
 */
@Composable
public fun rememberBuilderToastHostState(): BuilderToastHostState = remember { BuilderToastHostState() }

/**
 * Stacks the toasts of [state] at the bottom centre of the space it is given, newest at the bottom.
 *
 * The stack is one polite live region that is always there, even with no toast in it, so a screen
 * reader hears each toast arrive and reads it once it has finished what it was saying. On
 * the web, where controls fold their state into their names, that live region never reaches
 * the page, so each toast is read out once through [LocalAnnouncer] instead, a modal open or not. A
 * toast goes by itself after its duration, and its action closes it. The countdown waits while the
 * pointer rests on a toast or focus is inside it, and picks up with the time it had left (WCAG
 * 2.2.1), so a keyboard user on Undo never loses the toast under them. Each toast is Material's
 * `Snackbar`. Where overlays render in the page the stack is drawn in the overlay host's top slot over the space it is given,
 * so a toast raised from inside a dialog or a sheet shows over its veil rather than under it, and
 * its action joins the dialog's Tab cycle. When a toast goes with focus on its action, focus goes
 * back into the dialog.
 *
 * @param[state] The toasts to show.
 * @param[modifier] Applied to the host, which fills the space it is given without taking any
 * pointer input.
 */
@Composable
public fun BuilderToastHost(
    state: BuilderToastHostState,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val announces = LocalFoldsStateIntoName.current
    if (announces) {
        val announcer = LocalAnnouncer.current
        LaunchedEffect(state, announcer) {
            snapshotFlow { state.toasts.lastOrNull() }.collect { state.announceNew(announcer) }
        }
    }
    OverlayTopSlot(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = OverlayMetrics.toastMaxWidth)
                    .then(if (announces) Modifier else Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    .padding(tokens.spacing.large),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (toast in state.toasts) {
                    key(toast.id) {
                        ToastEntry(toast, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToastEntry(
    toast: BuilderToast,
    state: BuilderToastHostState,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val millis = toast.duration.millis
    if (millis != null) {
        val left = remember { mutableLongStateOf(millis) }
        LaunchedEffect(toast, hovered || focused) {
            if (hovered || focused) return@LaunchedEffect
            while (left.longValue > 0) {
                val step = minOf(left.longValue, ToastTickMillis)
                delay(step)
                left.longValue -= step
            }
            state.dismiss(toast)
        }
    }
    val onAction = {
        state.dismiss(toast)
        toast.onAction?.invoke()
        Unit
    }
    val entered = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = entered, enter = popoverEnter()) {
        val holds = Modifier
            .hoverable(interaction)
            .onFocusChanged { focus -> focused = focus.hasFocus }
        MaterialToast(toast, onAction, holds)
    }
}

/**
 * What an announcer reads out for a toast, its message and then its action.
 */
private val BuilderToast.announcement: String
    get() = listOfNotNull(message, actionLabel).joinToString(", ")

/**
 * How finely the countdown keeps the time a paused toast has left.
 */
private const val ToastTickMillis = 100L
