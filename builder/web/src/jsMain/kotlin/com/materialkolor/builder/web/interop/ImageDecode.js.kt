package com.materialkolor.builder.web.interop

import org.khronos.webgl.Int32Array

// An IntArray is an Int32Array at runtime on Kotlin/JS, and this one is fresh from the decode, so
// nothing else holds it.
internal actual fun Int32Array.copyToIntArray(): IntArray = unsafeCast<IntArray>()
