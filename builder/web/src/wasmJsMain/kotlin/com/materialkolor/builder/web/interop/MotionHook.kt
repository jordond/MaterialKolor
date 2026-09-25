@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

// b-503a
// `?motion=frozen` makes every animation deterministic for the browser tests' screenshots
// (architecture 7, MO-10). It only holds when a test drives the browser, so a shared link that
// carries it never freezes a person's own visit.

/**
 * Whether a test drives the browser, as `navigator.webdriver` says.
 */
internal fun browserIsAutomated(): Boolean = js("navigator.webdriver === true")

/**
 * Whether [query], the address's query with or without its leading `?`, asks for frozen motion on a
 * browser a test drives, which [automated] says.
 */
internal fun motionFrozen(
    query: String,
    automated: Boolean,
): Boolean = automated && query.removePrefix("?").split('&').any { parameter -> parameter == "motion=frozen" }
