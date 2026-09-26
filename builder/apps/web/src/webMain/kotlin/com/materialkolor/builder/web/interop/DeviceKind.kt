@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import com.materialkolor.builder.domain.persist.DeviceWidth
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Whether the page runs on a phone, a tablet or a computer, read once when the page loads.
 *
 * A browser that says it is mobile, or a user agent that names a phone, is a phone. One that names a
 * tablet or Android without "Mobile" is a tablet, and so is an iPad that asks for the desktop site
 * and reports a Mac with a touch screen. Any other touch screen is a phone when its shorter side is
 * under [PHONE_SHORT_SIDE] and a tablet otherwise. Everything else is a computer.
 */
internal fun pageDeviceWidth(): DeviceWidth {
    val agent = userAgent()
    val coarse = pageMatches("(pointer: coarse)")
    return when {
        userAgentSaysMobile() || PhoneAgent.containsMatchIn(agent) -> DeviceWidth.Phone
        TabletAgent.containsMatchIn(agent) -> DeviceWidth.Tablet
        "Macintosh" in agent && touchPoints() > 1 -> DeviceWidth.Tablet
        coarse && screenShortSide() in 1 until PHONE_SHORT_SIDE -> DeviceWidth.Phone
        coarse -> DeviceWidth.Tablet
        else -> DeviceWidth.Desktop
    }
}

private val PhoneAgent = Regex("iPhone|iPod|Android.+Mobile|Windows Phone|IEMobile|BlackBerry|Opera Mini", RegexOption.IGNORE_CASE)

private val TabletAgent = Regex("iPad|Android|Tablet|Silk|Kindle|PlayBook", RegexOption.IGNORE_CASE)

// The compact window class ends at 600 dp, so a touch screen narrower than that is held like a phone.
private const val PHONE_SHORT_SIDE = 600

// Chromium only, so false elsewhere means nothing and the user agent decides.
private fun userAgentSaysMobile(): Boolean = js("!!(navigator.userAgentData && navigator.userAgentData.mobile)")

private fun pageMatches(query: String): Boolean = js("!!(window.matchMedia && window.matchMedia(query).matches)")

private fun touchPoints(): Int = js("navigator.maxTouchPoints || 0")

private fun screenShortSide(): Int = js("Math.min(screen.width || 0, screen.height || 0)")
