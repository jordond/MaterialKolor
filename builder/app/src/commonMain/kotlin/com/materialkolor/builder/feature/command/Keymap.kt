package com.materialkolor.builder.feature.command

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint

/**
 * Whether [browser], the way `Environment.browser` names it, runs on an Apple system, where the
 * shortcuts take Cmd rather than Ctrl. An iPad asking for the desktop site calls itself a Macintosh,
 * so it counts too.
 */
internal fun isApple(browser: String): Boolean = APPLE_MARKERS.any { marker -> marker in browser }

private val APPLE_MARKERS = listOf("Macintosh", "Mac OS", "iPhone", "iPad", "iPod", "Darwin")

/** The key a [Chord] is pressed with. */
internal sealed interface ChordKey {
    /** A key found by where it sits, the way US keyboards print [name] on it. */
    data class Physical(
        val key: Key,
        val name: String,
    ) : ChordKey

    /**
     * A key found by the character it types, for `?`, `[` and `]`, which other layouts put on other
     * keys or behind Shift or AltGr. A key press that carries no character, as some platforms send,
     * falls back to where US keyboards put it, [usKey] with Shift when [usShift].
     */
    data class Typed(
        val char: Char,
        val usKey: Key,
        val usShift: Boolean = false,
    ) : ChordKey
}

/**
 * One way to press a shortcut. [primary] is Cmd on Apple systems and Ctrl everywhere else.
 */
@Immutable
internal data class Chord(
    val key: ChordKey,
    val primary: Boolean = false,
    val shift: Boolean = false,
) {
    /** Whether the chord is a key on its own or with Shift, which WCAG 2.1.4 lets someone turn off. */
    val singleKey: Boolean
        get() = !primary

    /** The chord as the cheat sheet and the tooltips write it, "Cmd+K" or "Ctrl+K". */
    fun text(apple: Boolean): String {
        val name = when (key) {
            is ChordKey.Physical -> key.name
            is ChordKey.Typed -> key.char.toString()
        }
        val parts = buildList {
            if (primary) add(if (apple) "Cmd" else "Ctrl")
            if (shift) add("Shift")
            add(name)
        }
        return parts.joinToString("+")
    }

    /** Whether [event] presses this chord, with Cmd as the primary key when [apple]. */
    fun matches(
        event: KeyEvent,
        apple: Boolean,
    ): Boolean {
        val command = if (apple) event.isMetaPressed else event.isCtrlPressed
        val other = if (apple) event.isCtrlPressed else event.isMetaPressed
        return when (key) {
            is ChordKey.Physical -> {
                event.key == key.key &&
                    command == primary &&
                    !other &&
                    !event.isAltPressed &&
                    event.isShiftPressed == shift
            }
            // AltGr reaches a page as Ctrl and Alt together, so only Ctrl without Alt rules a key out.
            is ChordKey.Typed -> {
                val altGr = event.isCtrlPressed && event.isAltPressed
                val typed = if (event.utf16CodePoint == 0) {
                    event.key == key.usKey && event.isShiftPressed == key.usShift && !event.isAltPressed
                } else {
                    event.utf16CodePoint == key.char.code
                }
                typed && !event.isMetaPressed && (!event.isCtrlPressed || altGr)
            }
        }
    }
}

/** How the cheat sheet groups the shortcuts, in the order of the spec's keyboard map. */
internal enum class ShortcutGroup {
    General,
    Theme,
    Preview,
    Project,
}

/**
 * The keyboard map (spec section 6), each shortcut with the chords that press it.
 *
 * Shortcuts marked [inFields] fire while a text field has focus. The rest stay out of the way of
 * typing, and every single key, Space included, can be turned off in the cheat sheet. V, the held B
 * and the keys of the export sheet are not here yet.
 */
internal enum class Shortcut(
    val group: ShortcutGroup,
    val chords: List<Chord>,
    val inFields: Boolean = false,
) {
    Palette(ShortcutGroup.General, listOf(Chord(physical(Key.K, "K"), primary = true)), inFields = true),
    CheatSheet(ShortcutGroup.General, listOf(Chord(ChordKey.Typed('?', Key.Slash, usShift = true)))),
    Shuffle(ShortcutGroup.Theme, listOf(Chord(physical(Key.Spacebar, "Space")))),
    HueLock(ShortcutGroup.Theme, listOf(Chord(physical(Key.L, "L")))),
    StyleLock(ShortcutGroup.Theme, listOf(Chord(physical(Key.L, "L"), shift = true))),
    Library1(ShortcutGroup.Theme, listOf(Chord(physical(Key.One, "1")))),
    Library2(ShortcutGroup.Theme, listOf(Chord(physical(Key.Two, "2")))),
    Library3(ShortcutGroup.Theme, listOf(Chord(physical(Key.Three, "3")))),
    Library4(ShortcutGroup.Theme, listOf(Chord(physical(Key.Four, "4")))),
    Library5(ShortcutGroup.Theme, listOf(Chord(physical(Key.Five, "5")))),
    PreviewMode(ShortcutGroup.Preview, listOf(Chord(physical(Key.D, "D")))),
    Appearance(ShortcutGroup.Preview, listOf(Chord(physical(Key.D, "D"), shift = true))),
    PreviousTab(ShortcutGroup.Preview, listOf(Chord(ChordKey.Typed('[', Key.LeftBracket)))),
    NextTab(ShortcutGroup.Preview, listOf(Chord(ChordKey.Typed(']', Key.RightBracket)))),
    Export(ShortcutGroup.Project, listOf(Chord(physical(Key.E, "E")))),
    CopySeed(ShortcutGroup.Theme, listOf(Chord(physical(Key.C, "C")))),
    CopyAll(ShortcutGroup.Project, listOf(Chord(physical(Key.C, "C"), shift = true))),
    CopyLink(ShortcutGroup.Project, listOf(Chord(physical(Key.S, "S")))),
    Save(ShortcutGroup.Project, listOf(Chord(physical(Key.S, "S"), primary = true)), inFields = true),
    Undo(ShortcutGroup.Theme, listOf(Chord(physical(Key.Z, "Z"), primary = true))),
    Redo(
        ShortcutGroup.Theme,
        listOf(
            Chord(physical(Key.Z, "Z"), primary = true, shift = true),
            Chord(physical(Key.Y, "Y"), primary = true),
        ),
    ),
    // Only Cmd or Ctrl+O fires in a field, never P on its own.
    Projects(
        ShortcutGroup.Project,
        listOf(Chord(physical(Key.P, "P")), Chord(physical(Key.O, "O"), primary = true)),
        inFields = true,
    ),
    NewProject(ShortcutGroup.Project, listOf(Chord(physical(Key.N, "N"), shift = true))),
    Inspect(ShortcutGroup.Preview, listOf(Chord(physical(Key.I, "I")))),
    AddImage(ShortcutGroup.Theme, listOf(Chord(physical(Key.U, "U")))),
    DeviceWidth(ShortcutGroup.Preview, listOf(Chord(physical(Key.W, "W")))),
    Fullscreen(ShortcutGroup.Preview, listOf(Chord(physical(Key.F, "F")))),
    Poster(ShortcutGroup.Preview, listOf(Chord(physical(Key.Backslash, "\\"), primary = true)), inFields = true),
    ;

    /** The chords written out for this platform, "P, Ctrl+O" say. */
    fun text(apple: Boolean): String = chords.joinToString(", ") { chord -> chord.text(apple) }

    /** Whether [chord] of this shortcut fires while a text field has focus. */
    fun firesInFields(chord: Chord): Boolean = inFields && !chord.singleKey

    companion object {
        /**
         * The shortcut [event] presses and the chord it pressed it with, or null. With [singleKeys]
         * off only Cmd or Ctrl chords count.
         */
        fun match(
            event: KeyEvent,
            apple: Boolean,
            singleKeys: Boolean = true,
        ): Pair<Shortcut, Chord>? {
            if (clipboardChord(event, apple)) return null
            for (shortcut in entries) {
                val chord = shortcut.chords.firstOrNull { chord -> chord.matches(event, apple) } ?: continue
                if (chord.singleKey && !singleKeys) return null
                return shortcut to chord
            }
            return null
        }
    }
}

/** Cmd or Ctrl with V, C, X or A, which always stay the browser's and the field's own. */
internal fun clipboardChord(
    event: KeyEvent,
    apple: Boolean,
): Boolean {
    val command = if (apple) event.isMetaPressed else event.isCtrlPressed
    return command && event.key in CLIPBOARD_KEYS
}

private val CLIPBOARD_KEYS = setOf(Key.V, Key.C, Key.X, Key.A)

private fun physical(
    key: Key,
    name: String,
): ChordKey = ChordKey.Physical(key, name)

/**
 * Whether the shortcuts here are written with Cmd, as on Apple systems, or Ctrl. The workspace
 * provides it from the browser, and anything drawn without it writes Ctrl.
 */
internal val LocalAppleKeys: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }
