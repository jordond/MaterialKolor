package com.materialkolor.builder.feature.command

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.feature.topbar.LocalSwitcherForm
import com.materialkolor.builder.feature.topbar.SwitcherFormState
import com.materialkolor.builder.feature.workspace.Panel
import com.materialkolor.builder.feature.workspace.WorkspaceAction
import com.materialkolor.builder.feature.workspace.WorkspaceModel
import com.materialkolor.builder.kit.headless.LocalOverlayKeys
import dev.stateholder.dispatcher.Dispatcher
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * What the shortcuts keep outside the workspace's own model, whether they write Cmd or Ctrl and
 * the single-key switch (WCAG 2.1.4), which lives in the browser's preferences as
 * `singleKeyShortcuts`.
 */
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class ShortcutsModel(
    private val preferences: PreferencesRepository,
    private val environment: Environment,
) : ViewModel() {
    /**
     * Whether this runs on an Apple system, where the shortcuts take Cmd.
     */
    val apple: Boolean = isApple(environment.browser)

    /**
     * Emits when the page goes out of sight, which lets go of a held B whose release never comes.
     */
    val pageHides: Flow<Unit> = environment.pageHides

    /**
     * The form the top bar's library switcher shows, which the registry reads.
     */
    val switcherForm: SwitcherFormState = SwitcherFormState()

    /**
     * Turn the single-key shortcuts on or off, in every tab of this browser.
     */
    fun setSingleKeys(on: Boolean) {
        viewModelScope.launch { preferences.update { prefs -> prefs.copy(singleKeyShortcuts = on) } }
    }

    /**
     * Tell the platform whether the page's focus holder has focus.
     */
    fun holderFocusChanged(focused: Boolean) = environment.holderFocusChanged(focused)
}

/**
 * The page's focus holder and what the shortcuts need to know about focus.
 *
 * The holder is the page root. It takes focus so keys reach the shortcuts when no control has it,
 * after boot, when focus rests nowhere and on a press no control claims. It is never a Tab stop and
 * draws no ring, since it can only take focus while it asks for it.
 *
 * Text fields below it are counted as their input sessions open and close, which also covers an
 * input method composing, so single keys stay out of the way of typing.
 */
@Stable
internal class ShortcutFocus {
    /**
     * The page root.
     */
    val holder: FocusRequester = FocusRequester()

    internal var armed by mutableStateOf(false)
    internal var holderFocused: Boolean = false
    internal var pageHasFocus by mutableStateOf(false)

    /**
     * How many text input sessions are open below the holder.
     */
    internal var textSessions: Int = 0

    /**
     * How many text input sessions have opened below the holder so far.
     */
    internal var sessionStarts: Int = 0

    /**
     * Whether single keys are on, kept here for the keys a panel takes itself.
     */
    internal var singleKeys: Boolean = true

    /**
     * The page's handling of a key pressed inside an overlay, which the page root never hears.
     */
    internal var overlayKey: (KeyEvent) -> Boolean = { false }

    private var fromKeys = false

    /**
     * The panel a shortcut opened last, which hands focus back to the holder.
     */
    var keyOpened: Panel? by mutableStateOf(null)
        private set

    /**
     * Whether a text field below the holder is taking input.
     */
    val typing: Boolean
        get() = textSessions > 0

    /**
     * Give the holder focus. An overlay's focus trap turns it down, so an open overlay keeps focus.
     */
    fun focusHolder() {
        armed = true
        holder.requestFocus()
    }

    /**
     * Called with every panel that opens, so a panel a shortcut opened gives focus to the holder.
     */
    fun noteOpen(panel: Panel) {
        keyOpened = if (fromKeys) panel else null
    }

    /**
     * Runs [block] as a shortcut, so a panel it opens counts as opened from the keyboard.
     */
    fun runFromKeys(block: () -> Unit) {
        fromKeys = true
        try {
            block()
        } finally {
            fromKeys = false
        }
    }

    /**
     * Where [panel] hands focus once it closes, the holder when a shortcut opened it, else [otherwise].
     */
    fun returnFocusFor(
        panel: Panel,
        otherwise: FocusRequester?,
    ): FocusRequester? = if (keyOpened == panel) holder else otherwise
}

/**
 * Tells the input modes the keyboard is in use, as Tab does, so a control that hands focus on after
 * a shortcut does so the way it does for a Tab user. Every shortcut calls it first.
 */
internal fun InputModeManager.useKeyboard() {
    requestInputMode(InputMode.Keyboard)
}

/**
 * Counts the text input sessions [content] opens for [focus], and says whether the shortcuts are
 * written with Cmd, from [model]. Every overlay opened in [content] hands the page the keys it
 * keeps while an overlay is open, and the library switcher reports its form to the registry.
 */
@Composable
internal fun ShortcutScope(
    focus: ShortcutFocus,
    model: ShortcutsModel = metroViewModel(),
    content: @Composable () -> Unit,
) {
    val overlayKeys = remember(focus) { { event: KeyEvent -> focus.overlayKey(event) } }
    val interceptor = remember(focus) {
        PlatformTextInputInterceptor { request, nextHandler ->
            focus.textSessions++
            focus.sessionStarts++
            try {
                nextHandler.startInputMethod(request)
            } finally {
                focus.textSessions--
            }
        }
    }
    InterceptPlatformTextInput(interceptor) {
        CompositionLocalProvider(
            LocalAppleKeys provides model.apple,
            LocalShortcutFocus provides focus,
            LocalOverlayKeys provides overlayKeys,
            LocalSwitcherForm provides model.switcherForm,
            content = content,
        )
    }
}

/**
 * The page's [ShortcutFocus], for the panels that take keys of their own, or null outside the page.
 */
internal val LocalShortcutFocus: ProvidableCompositionLocal<ShortcutFocus?> = staticCompositionLocalOf { null }

/**
 * The keys a panel takes itself, since the page under it never hears a key pressed inside it. Put
 * [PanelShortcuts.modifier] on the panel and set [PanelShortcuts.onShortcut].
 *
 * The page's rules hold here too. Nothing fires while a text field in the panel takes input, no
 * single key fires with single keys off, and Cmd or Ctrl with V, C, X or A is always left alone. A
 * panel drawn outside the page takes no keys.
 */
@Composable
internal fun rememberPanelShortcuts(): PanelShortcuts {
    val focus = LocalShortcutFocus.current
    val apple = LocalAppleKeys.current
    val inputModes = LocalInputModeManager.current
    return remember(focus, apple, inputModes) { PanelShortcuts(focus, apple, inputModes) }
}

/**
 * What [rememberPanelShortcuts] gives a panel.
 */
@Stable
internal class PanelShortcuts(
    private val focus: ShortcutFocus?,
    private val apple: Boolean,
    private val inputModes: InputModeManager,
) {
    /**
     * Runs the shortcut pressed and says whether it did anything. Set it as the panel composes.
     */
    var onShortcut: (Shortcut) -> Boolean = { false }

    /**
     * Goes on the panel, around everything that takes focus in it.
     */
    val modifier: Modifier = if (focus == null) Modifier else Modifier.onKeyEvent(::onKey)

    private fun onKey(event: KeyEvent): Boolean {
        val focus = focus ?: return false
        if (event.type != KeyEventType.KeyDown) return false
        val (shortcut, chord) = Shortcut.match(event, apple, focus.singleKeys) ?: return false
        if (focus.typing && !shortcut.firesInFields(chord)) return false
        inputModes.useKeyboard()
        return onShortcut(shortcut)
    }
}

/**
 * The page root's modifier, the focus holder with the keyboard map on it.
 *
 * Keys arrive as they bubble up from the focused control, so a control that takes a key, a button
 * taking Space say, keeps it. Undo and Redo wait the same way, so a field keeps its own text undo.
 * Only Cmd or Ctrl with K, S, O and \, which fire in fields, are taken on the way down. Single keys
 * and Space never fire while a text field below takes input, and Cmd or Ctrl with V, C, X or A is
 * always left alone. Esc closes a panel that has not taken the keyboard yet, or else leaves Inspect,
 * or else moves focus out of a field to the holder, once overlays and Inspect have had their turn.
 *
 * A shortcut whose command cannot run toasts the reason once per press, all but Undo and Redo, which
 * stay quiet with nothing to undo or redo as they do in any editor.
 *
 * V opens the dock's Vision menu, and B shows the canvas in grayscale for as long as it is held. The
 * key's repeats change nothing, and B lets go when it comes up, when the holder or the window loses
 * focus and when the page goes out of sight, since the release may never reach the page then.
 *
 * Inside an overlay, which the page root never hears, Cmd or Ctrl+S still saves and Cmd or Ctrl+O
 * does nothing while a panel or the Vision menu is open, so neither reaches the browser. An overlay
 * on its way out that still holds focus hands both to the page as they are.
 */
@Composable
internal fun rememberShortcuts(
    state: WorkspaceModel.State,
    dispatcher: Dispatcher<WorkspaceAction>,
    focus: ShortcutFocus,
    model: ShortcutsModel = metroViewModel(),
): Modifier {
    val commands = actionRegistry(state, dispatcher, shortcuts = model)
    val latestCommands by rememberUpdatedState(commands)
    val latestState by rememberUpdatedState(state)
    val inputModes = LocalInputModeManager.current
    val scope = rememberCoroutineScope()
    ClaimFocusWhenNowhere(focus, state.panel, state.document.library, state.visionMenuOpen)
    val singleKeys = state.preferences.singleKeyShortcuts
    SideEffect { focus.singleKeys = singleKeys }
    val grayscale = remember { HeldKey() }

    fun letGoOfGrayscale() {
        if (!grayscale.down) return
        grayscale.down = false
        dispatcher.dispatch(WorkspaceAction.HoldGrayscale(held = false))
    }

    LaunchedEffect(model) { model.pageHides.collect { letGoOfGrayscale() } }
    val window = LocalWindowInfo.current
    LaunchedEffect(window) {
        snapshotFlow { window.isWindowFocused }.collect { focused -> if (!focused) letGoOfGrayscale() }
    }

    // B works the dock straight away, with no command behind it and no focus to claim after.
    fun dockKey(shortcut: Shortcut) {
        if (shortcut != Shortcut.Grayscale || grayscale.down) return
        grayscale.down = true
        dispatcher.dispatch(WorkspaceAction.HoldGrayscale(held = true))
    }

    // After a shortcut, focus that the shortcut left nowhere goes to the holder. The Vision menu
    // keeps the focus it took.
    fun claimIfNowhere() {
        scope.launch {
            repeat(SETTLE_FRAMES) { withFrameNanos { } }
            val open = latestState.panel != null || latestState.visionMenuOpen
            if (!focus.pageHasFocus && !open) focus.focusHolder()
        }
    }

    fun escape(): Boolean {
        inputModes.useKeyboard()
        return when {
            // In the frames between the key that opened a panel and the panel taking focus.
            latestState.panel != null -> {
                dispatcher.dispatch(WorkspaceAction.ClosePanel)
                true
            }
            latestState.inspect -> {
                dispatcher.dispatch(WorkspaceAction.SetInspect(false))
                true
            }
            focus.typing -> {
                focus.focusHolder()
                true
            }
            else -> {
                false
            }
        }
    }

    // Runs the command [shortcut] presses, or toasts why it cannot run.
    fun runCommand(shortcut: Shortcut): Boolean {
        val command = latestCommands.firstOrNull { command -> command.shortcut == shortcut } ?: return false
        inputModes.useKeyboard()
        when (val commandState = command.state) {
            CommandState.Enabled -> {
                focus.runFromKeys(command.run)
                claimIfNowhere()
            }
            is CommandState.Disabled -> {
                val reason = commandState.reason
                if (shortcut !in QUIET_WHEN_DISABLED) dispatcher.dispatch(WorkspaceAction.ShowToast(reason))
            }
        }
        return true
    }

    // The chords that fire in fields go first, since a field may map one of them to an edit of its
    // own, as Ctrl+K deletes to the end of the line on macOS. The rest wait for the focused control.
    fun onKey(
        event: KeyEvent,
        preview: Boolean,
    ): Boolean {
        // B lets go on its way down, whatever holds focus by then, and leaves the key to it.
        if (event.type == KeyEventType.KeyUp && event.key == Key.B && preview) letGoOfGrayscale()
        if (event.type != KeyEventType.KeyDown) return false
        if (event.key == Key.Escape && !event.anyModifier()) return !preview && escape()
        val singleKeys = latestState.preferences.singleKeyShortcuts
        val (shortcut, chord) = Shortcut.match(event, model.apple, singleKeys) ?: return false
        if (preview != shortcut.firesInFields(chord)) return false
        if (focus.typing && !shortcut.firesInFields(chord)) return false
        if (!shortcut.inRegistry) {
            inputModes.useKeyboard()
            dockKey(shortcut)
            return true
        }
        return runCommand(shortcut)
    }

    fun onOverlayKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val (shortcut, chord) = Shortcut.match(event, model.apple) ?: return false
        if (chord.singleKey || shortcut !in OVERLAY_SHORTCUTS) return false
        val covered = latestState.panel != null || latestState.visionMenuOpen
        if (covered && shortcut == Shortcut.Projects) return true
        return runCommand(shortcut)
    }
    SideEffect { focus.overlayKey = ::onOverlayKey }

    return remember(focus) {
        Modifier
            .onPreviewKeyEvent { event -> onKey(event, preview = true) }
            .onKeyEvent { event -> onKey(event, preview = false) }
            .pointerInput(focus) { claimUnclaimedPresses(focus, scope) { latestState.inspect } }
            .onFocusChanged { focusState ->
                focus.pageHasFocus = focusState.hasFocus
                if (focus.holderFocused != focusState.isFocused) model.holderFocusChanged(focusState.isFocused)
                focus.holderFocused = focusState.isFocused
                if (!focusState.isFocused) focus.armed = false
                if (!focusState.isFocused) letGoOfGrayscale()
            }.focusProperties { canFocus = focus.armed }
            .focusRequester(focus.holder)
            .focusTarget()
    }
}

/**
 * The page's shortcuts that an overlay hands on, since each would otherwise reach the browser.
 */
private val OVERLAY_SHORTCUTS = setOf(Shortcut.Save, Shortcut.Projects)

/**
 * Hands focus to the holder after boot, after a panel closed and after a library switch, when
 * nothing on the page has it by then. Two frames go by first, so a panel's own hand-off lands
 * before this looks, and so does a top bar control taking focus back after a switch.
 *
 * A switch that did not come through the switcher's focused option can leave focus nowhere, so this
 * keys on [library] the way it keys on [panel].
 *
 * Focus that leaves the page while no panel is open is left alone, since on the desktop a menu's
 * popup takes it the same way, and a menu in the page's own overlay host keeps it by its trap. The
 * Vision menu keeps it the same way when a palette row opens it.
 */
@Composable
private fun ClaimFocusWhenNowhere(
    focus: ShortcutFocus,
    panel: Panel?,
    library: Library,
    visionMenuOpen: Boolean,
) {
    val latestPanel by rememberUpdatedState(panel)
    val menuOpen by rememberUpdatedState(visionMenuOpen)
    val lastPanel = remember { mutableStateOf<Panel?>(null) }
    LaunchedEffect(panel) {
        val closed = lastPanel.value != null && panel == null
        val booted = lastPanel.value == null && panel == null
        lastPanel.value = panel
        if (!closed && !booted) return@LaunchedEffect
        repeat(SETTLE_FRAMES) { withFrameNanos { } }
        if (!focus.pageHasFocus && !menuOpen) focus.focusHolder()
    }
    val lastLibrary = remember { mutableStateOf(library) }
    LaunchedEffect(library) {
        if (lastLibrary.value == library) return@LaunchedEffect
        lastLibrary.value = library
        repeat(SETTLE_FRAMES) { withFrameNanos { } }
        if (!focus.pageHasFocus && latestPanel == null && !menuOpen) focus.focusHolder()
    }
    LaunchedEffect(focus.armed) {
        if (!focus.armed) return@LaunchedEffect
        withFrameNanos { }
        if (!focus.holderFocused) focus.armed = false
    }
}

private const val SETTLE_FRAMES = 2

/**
 * Whether a held key is down, known at once rather than a recomposition later.
 */
private class HeldKey {
    var down: Boolean = false
}

/**
 * The shortcuts that say nothing when their command cannot run, since an empty history is no news.
 */
private val QUIET_WHEN_DISABLED = setOf(Shortcut.Undo, Shortcut.Redo)

/**
 * Gives the holder focus after a press that no control consumed, so a click on the bare canvas
 * leaves a text field. A press that opened a text field keeps it, and so does one while [inspecting],
 * since Inspect pins on a press.
 *
 * The field the press left closes its input session a few frames later, and on the desktop focus
 * can go with it, so for a few frames more the holder takes focus back whenever the page lost it.
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.claimUnclaimedPresses(
    focus: ShortcutFocus,
    scope: CoroutineScope,
    inspecting: () -> Boolean,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        val sessionsBefore = focus.sessionStarts
        var claimed = down.isConsumed
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            if (event.changes.any { change -> change.isConsumed }) claimed = true
            if (event.changes.none { change -> change.pressed }) break
        }
        if (claimed || inspecting()) return@awaitEachGesture
        scope.launch {
            repeat(SETTLE_FRAMES) { withFrameNanos { } }
            if (focus.sessionStarts != sessionsBefore) return@launch
            focus.focusHolder()
            repeat(RECLAIM_FRAMES) {
                withFrameNanos { }
                if (focus.sessionStarts != sessionsBefore) return@launch
                if (!focus.pageHasFocus) focus.focusHolder()
            }
        }
    }
}

private const val RECLAIM_FRAMES = 8

private fun KeyEvent.anyModifier(): Boolean = isCtrlPressed || isMetaPressed || isAltPressed || isShiftPressed
