@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("unused", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.materialkolor.builder.export

import kotlinx.coroutines.await
import org.w3c.files.Blob
import kotlin.js.Promise

@JsModule("jszip")
actual external class JSZip actual constructor() {
    actual fun file(name: String, data: String)
    actual fun generateAsync(options: JsAny): Promise<JsAny>
}

internal actual suspend fun JSZip.createBlob(): Blob {
    return generateAsync(createParams()).await()
}

private fun createParams(): JsAny = js("({ type: 'blob' })")
