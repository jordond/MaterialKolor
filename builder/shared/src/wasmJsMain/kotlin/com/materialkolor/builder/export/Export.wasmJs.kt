package com.materialkolor.builder.export

import com.materialkolor.builder.export.model.ExportFile
import kotlinx.browser.document
import kotlinx.coroutines.await
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import kotlin.js.Promise

@JsModule("jszip")
external class JSZip {

    fun file(name: String, data: String)
    fun generateAsync(options: JsAny = definedExternally): Promise<JsAny>
}

actual suspend fun exportFiles(list: List<ExportFile>) {
    val blob = createZipBlob(list)
    offerFileForDownload(blob, "theme.zip")
}

private suspend fun createZipBlob(files: List<ExportFile>): Blob {
    val zip = JSZip()

    files.forEach { file ->
        zip.file(file.name, file.content)
    }

    return zip.generateAsync(createParams()).await()
}

private fun createParams(): JsAny = js("({ type: 'blob' })")

private fun offerFileForDownload(blob: Blob, filename: String) {
    val url = URL.createObjectURL(blob)
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
}
