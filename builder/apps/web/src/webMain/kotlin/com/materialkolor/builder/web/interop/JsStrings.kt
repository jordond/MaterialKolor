@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString

/**
 * Read this JavaScript string as a Kotlin one.
 *
 * On JS the two are the same type, so `toString()` there is a redundant call the compiler warns
 * about, while on wasm it is the copy out of the JavaScript heap.
 */
internal expect fun JsString.toKotlinString(): String
