package com.materialkolor.builder.feature.export

import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.OutgoingFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * What Copy file, Copy all and the poster's copy buttons do when clicked. Writes [text] to
 * [clipboard], then hands [onResult] what the clipboard said.
 *
 * Browsers only let a page write the clipboard inside the click, so the write is the first
 * suspension and it starts before this returns (R-B-302). Call it straight from `onClick`, or from
 * a dispatcher that runs its block in the click, with no hop through the model before it.
 */
internal fun CoroutineScope.launchCopy(
    clipboard: Clipboard,
    text: String,
    onResult: suspend (Result<Unit>) -> Unit,
) {
    launch(start = CoroutineStart.UNDISPATCHED) {
        onResult(clipboard.writeText(text))
    }
}

/**
 * What Download zip and Share files do when clicked. Hands [zip] to the share sheet when [share],
 * or saves it otherwise, then hands [onResult] how it went.
 *
 * Which one was settled before the click, so a share that fails never turns into a download. Like
 * [launchCopy], the save or the share is the first suspension and it starts before this returns
 * (R-B-302).
 */
internal fun CoroutineScope.launchZip(
    files: FileSaver,
    zip: OutgoingFile,
    share: Boolean,
    onResult: suspend (Result<Unit>) -> Unit,
) {
    launch(start = CoroutineStart.UNDISPATCHED) {
        val result = if (share) files.shareFiles(listOf(zip)) else files.save(zip.name, zip.bytes, zip.mime)
        onResult(result)
    }
}
