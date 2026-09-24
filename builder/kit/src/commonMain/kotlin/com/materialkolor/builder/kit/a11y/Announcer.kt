package com.materialkolor.builder.kit.a11y

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Reads a short message out to assistive technology without moving focus.
 *
 * On the web the Compose `liveRegion` never reaches the page, and an open modal clears it with the
 * rest of the page (D40), so the kit hands what it has to say to the app's own live region through
 * this instead. A toast is the kit's one caller.
 */
public fun interface Announcer {
    /** Reads [message] out once, after whatever is being read now. */
    public fun announce(message: String)
}

/**
 * The announcer the kit reads messages out through.
 *
 * It says nothing by default. The web app provides one that writes into its live region.
 */
public val LocalAnnouncer: ProvidableCompositionLocal<Announcer> = staticCompositionLocalOf { SilentAnnouncer }

/** An announcer that says nothing, for where the Compose semantics already reach assistive technology. */
private val SilentAnnouncer: Announcer = Announcer { }
