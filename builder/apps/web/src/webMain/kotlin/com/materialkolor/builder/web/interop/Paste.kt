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
 * Pasted files go to [onFiles], images or not, so a file the builder cannot read still reaches
 * `decode` and gets its error. Failing those, any text that is not blank goes to [onText]. A paste
 * into a text field, a contenteditable element or the text area Compose types into belongs to that
 * field and is left alone. Reading the clipboard this way asks for no permission.
 *
 * Cmd or Ctrl+V on the canvas lands on a hidden text area of Compose's own, which it focuses for a
 * moment so the browser has somewhere to send the paste. Compose marks it `aria-hidden`, which no
 * field it types into has, so its pastes are heard here.
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
        const clipTarget = (node) => node.tagName === 'TEXTAREA' && node.getAttribute('aria-hidden') === 'true';
        const editable = (node) =>
            !!node &&
            !clipTarget(node) &&
            (node.isContentEditable || node.tagName === 'INPUT' || node.tagName === 'TEXTAREA');
        // Compose keeps its canvas and its inputs in a shadow root, and from outside it the event and
        // the focus both point at the root's host.
        const focused = () => {
            let node = document.activeElement;
            while (node && node.shadowRoot && node.shadowRoot.activeElement) node = node.shadowRoot.activeElement;
            return node;
        };
        const listener = (event) => {
            const target = (typeof event.composedPath === 'function' && event.composedPath()[0]) || event.target;
            if (editable(target) || editable(focused())) return;
            const data = event.clipboardData;
            if (!data) return;
            const files = Array.from(data.files || []);
            if (files.length > 0) {
                event.preventDefault();
                onFiles(files);
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
