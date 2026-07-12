@file:Suppress("unused", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.export

import com.materialkolor.builder.export.model.ExportFile
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

expect class JSZip() {
    fun file(name: String, data: String)
    fun generateAsync(options: JsAny): Promise<JsAny>
}

actual suspend fun exportFiles(list: List<ExportFile>) {
    val zip = JSZip()
    list.forEach { file -> zip.file(file.name, file.content) }
    offerFileForDownload(zip.createBlob(), "materialkolor-theme.zip")
}

internal expect suspend fun JSZip.createBlob(): Blob

private fun offerFileForDownload(blob: Blob, filename: String) {
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
}
