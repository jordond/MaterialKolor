package com.materialkolor.builder.preview.inspect

import androidx.compose.runtime.Immutable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.materialkolor.builder.preview.generated.resources.Res
import com.materialkolor.builder.preview.generated.resources.inspect_pin_key
import org.jetbrains.compose.resources.StringResource

// b-315b

/**
 * A key Inspect listens for, pressed with Shift or without and with no other modifier held.
 *
 * @property[key] The key itself.
 * @property[shift] Whether Shift has to be held with it.
 * @property[name] What the card calls the whole chord, such as Shift+Enter.
 */
@Immutable
internal class InspectKey(
    val key: Key,
    val shift: Boolean,
    val name: StringResource,
) {
    /**
     * Whether [event] is this key, going down or up, with Shift held as it needs and nothing else.
     */
    fun matches(event: KeyEvent): Boolean =
        event.key == key &&
            event.isShiftPressed == shift &&
            !event.isCtrlPressed &&
            !event.isAltPressed &&
            !event.isMetaPressed
}

/**
 * The key that pins the card of the element keyboard focus is on and moves focus to the card's first
 * enabled action. Shift+Enter for now, which the owner may change here along with [InspectKey.name].
 */
internal val PinKey: InspectKey = InspectKey(Key.Enter, shift = true, name = Res.string.inspect_pin_key)
