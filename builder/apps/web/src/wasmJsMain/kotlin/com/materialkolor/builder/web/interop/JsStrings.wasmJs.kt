@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlin.js.ExperimentalWasmJsInterop

internal actual fun JsString.toKotlinString(): String = toString()
