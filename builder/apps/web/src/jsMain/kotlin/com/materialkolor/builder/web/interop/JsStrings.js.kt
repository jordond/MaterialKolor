@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

internal actual fun JsString.toKotlinString(): String = this
