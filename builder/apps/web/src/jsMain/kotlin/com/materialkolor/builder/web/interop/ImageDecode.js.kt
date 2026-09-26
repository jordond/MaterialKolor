package com.materialkolor.builder.web.interop

import org.khronos.webgl.Int32Array

internal actual fun Int32Array.copyToIntArray(): IntArray = unsafeCast<IntArray>()
