@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import org.w3c.files.File
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.toList

/**
 * Hear what the user pastes while nothing editable has focus, until the returned function is called.
 *
 * Image files go to [onFiles], and failing those any text that is not blank goes to [onText]. A
 * paste into a text field, a contenteditable element or the text area Compose types into belongs
 * to that field and is left alone. Reading the clipboard this way asks for no permission.
 */
internal fun listenForPastes(
    onText: (String) -> Unit,
    onFiles: (List<File>) -> Unit,
): () -> Unit {
    val listener = addPasteListener(onText) { files -> onFiles(files.toList()) }
    return { removePasteListener(listener) }
}

private fun addPasteListener(
    onText: (String) -> Unit,
    onFiles: (JsArray<File>) -> Unit,
): JsAny =
    js(
        """{
        const editable = (node) =>
            !!node && (node.isContentEditable || node.tagName === 'INPUT' || node.tagName === 'TEXTAREA');
        const listener = (event) => {
            if (editable(event.target) || editable(document.activeElement)) return;
            const data = event.clipboardData;
            if (!data) return;
            const images = Array.from(data.files || []).filter(
                (file) => typeof file.type === 'string' && file.type.startsWith('image/'),
            );
            if (images.length > 0) {
                event.preventDefault();
                onFiles(images);
                return;
            }
            const text = data.getData('text/plain') || '';
            if (text.trim().length > 0) {
                event.preventDefault();
                onText(text);
            }
        };
        document.addEventListener('paste', listener);
        return listener;
    }""",
    )

private fun removePasteListener(listener: JsAny): Unit = js("document.removeEventListener('paste', listener)")
