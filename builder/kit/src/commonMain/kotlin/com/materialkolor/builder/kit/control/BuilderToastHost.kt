package com.materialkolor.builder.kit.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.kit.skin.fluent.fluentOverlayStyle
import com.materialkolor.builder.kit.skin.headless.OverlayStyle
import com.materialkolor.builder.kit.skin.headless.customOverlayStyle
import com.materialkolor.builder.kit.skin.headless.overlayFeedback
import com.materialkolor.builder.kit.skin.headless.popoverEnter
import com.materialkolor.builder.kit.skin.headless.unstyledOverlayStyle
import com.materialkolor.builder.kit.skin.material.MaterialToast
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import kotlinx.coroutines.delay

/** How long a toast stays before it goes by itself. */
public enum class ToastDuration(
    internal val millis: Long?,
) {
    /** Four seconds, for a plain confirmation. */
    Short(4_000),

    /** Ten seconds, for a toast with an action such as Undo. */
    Long(10_000),

    /** Until someone acts on it or it is pushed out by newer toasts. */
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

    /** The toasts on screen, oldest first. */
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

    /** Takes [toast] off the screen, if it is still there. */
    public fun dismiss(toast: BuilderToast) {
        shown.remove(toast)
    }

    public companion object {
        /** The most toasts the host stacks at once. */
        public const val MaxToasts: Int = 3
    }
}

/** A toast host state that lives as long as the composition that remembers it. */
@Composable
public fun rememberBuilderToastHostState(): BuilderToastHostState = remember { BuilderToastHostState() }

/**
 * Stacks the toasts of [state] at the bottom centre of the space it is given, newest at the bottom.
 *
 * Each toast is a polite live region, so a screen reader reads it once it has finished what it was
 * saying (AR-06). A toast goes by itself after its duration, and its action closes it. Material3
 * draws each toast as a `Snackbar`, the other skins as a headless toast.
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
    val library = LocalSkin.current.library
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = ToastMaxWidth)
                .padding(tokens.spacing.large),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (toast in state.toasts) {
                key(toast.id) {
                    ToastEntry(toast, state, library)
                }
            }
        }
    }
}

@Composable
private fun ToastEntry(
    toast: BuilderToast,
    state: BuilderToastHostState,
    library: Library,
) {
    val tokens = LocalBuilderTokens.current
    val millis = toast.duration.millis
    if (millis != null) {
        LaunchedEffect(toast) {
            delay(millis)
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
        val live = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        when (library) {
            Library.Material3 -> MaterialToast(toast, onAction, live)
            Library.Unstyled -> HeadlessToast(toast, onAction, unstyledOverlayStyle(tokens), live)
            // fluent-placeholder
            Library.Fluent -> HeadlessToast(toast, onAction, fluentOverlayStyle(tokens), live)
            Library.Custom -> HeadlessToast(toast, onAction, customOverlayStyle(tokens), live)
        }
    }
}

@Composable
private fun HeadlessToast(
    toast: BuilderToast,
    onAction: () -> Unit,
    style: OverlayStyle,
    modifier: Modifier,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(style.shadow, style.popoverShape)
            .clip(style.popoverShape)
            .background(style.toast)
            .then(if (style.toastBorder != null) Modifier.border(style.toastBorder, style.popoverShape) else Modifier)
            .padding(start = tokens.spacing.large, end = tokens.spacing.small)
            .heightIn(min = LocalLayout.current.primaryTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.medium),
    ) {
        BuilderText(
            text = toast.message,
            modifier = Modifier.weight(1f).padding(vertical = tokens.spacing.small),
            style = BuilderTextStyle.Body,
            color = style.toastContent,
        )
        val label = toast.actionLabel
        if (label != null) ToastAction(label, onAction, style)
    }
}

/** The one action of a toast, a text button in the toast's own ink. */
@Composable
private fun ToastAction(
    label: String,
    onClick: () -> Unit,
    style: OverlayStyle,
) {
    val tokens = LocalBuilderTokens.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .heightIn(min = LocalLayout.current.minTouchTarget)
            .overlayFeedback(
                interactionSource = interaction,
                style = style,
                highlight = style.toastContent.copy(alpha = ToastActionHighlightAlpha),
                focus = style.toastContent,
            ).clickable(interaction, null, role = Role.Button, onClick = onClick)
            .padding(horizontal = tokens.spacing.medium),
        contentAlignment = Alignment.Center,
    ) {
        BuilderText(label, style = BuilderTextStyle.Label, color = style.toastContent)
    }
}

private val ToastMaxWidth = 560.dp
private const val ToastActionHighlightAlpha = 0.12f
