@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise

/**
 * Put [text] on the system clipboard. Returns why the browser refused, or null when it landed.
 *
 * The browser call happens before the first suspension, so a caller that starts this undispatched
 * from a click makes the write inside the click, which Safari insists on.
 */
internal suspend fun writeClipboardText(text: String): String? =
    startClipboardWrite(text).await<JsString?>()?.toKotlinString()

// Without the async clipboard API (an insecure origin, say) the text goes through a hidden text
// area and `execCommand('copy')`, which only works inside the click, so it runs right away.
private fun startClipboardWrite(text: String): Promise<JsString?> =
    js(
        """{
        const reason = (error) => String((error && (error.name || error.message)) || error || 'Refused');
        if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
            try {
                return navigator.clipboard.writeText(text).then(() => null, reason);
            } catch (error) {
                return Promise.resolve(reason(error));
            }
        }
        const active = document.activeElement;
        const area = document.createElement('textarea');
        area.value = text;
        area.setAttribute('readonly', '');
        area.style.cssText = 'position:fixed;top:0;left:0;width:1px;height:1px;opacity:0';
        document.body.appendChild(area);
        area.select();
        let copied = false;
        try {
            copied = document.execCommand('copy');
        } catch (error) {}
        area.remove();
        if (active && typeof active.focus === 'function') active.focus();
        return Promise.resolve(copied ? null : 'NotAllowedError');
    }""",
    )
