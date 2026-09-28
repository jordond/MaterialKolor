@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import com.materialkolor.builder.core.platform.StoreError
import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsBoolean
import kotlin.js.Promise

// localStorage throws when the page may not use it, blocked cookies in Chromium for example, and
// when it is full. Every call here catches, so the rest of the shell never sees a JS exception.

/**
 * The text under [key], or null when there is none or storage is blocked.
 */
internal fun localStorageRead(key: String): String? = readItem(key)

/**
 * Put [value] under [key]. Returns why it did not land, or null when it did.
 */
internal fun localStorageWrite(
    key: String,
    value: String,
): StoreError? = writeOutcome(writeItem(key, value))

/**
 * Remove [key]. Returns why it could not be removed, or null when it was.
 */
internal fun localStorageRemove(key: String): StoreError? = writeOutcome(removeItem(key))

/**
 * Whether this page may write to localStorage at all. A full storage still counts.
 */
internal fun localStorageWorks(): Boolean {
    val outcome = writeOutcome(writeItem(PROBE_KEY, "1"))
    removeItem(PROBE_KEY)
    return outcome != StoreError.Unavailable
}

/**
 * Call [listener] each time another tab of this origin changes localStorage, with the key it
 * changed, or null when it cleared everything.
 */
internal fun onLocalStorageChange(listener: (String?) -> Unit): Unit = listenForStorage(listener)

/**
 * Ask the browser to keep this origin's storage through storage pressure. True when it agreed.
 */
internal suspend fun requestPersistentStorage(): Boolean = persist().await<JsBoolean>().toBoolean()

private fun writeOutcome(code: Int): StoreError? =
    when (code) {
        WRITE_DONE -> null
        WRITE_FULL -> StoreError.QuotaExceeded
        else -> StoreError.Unavailable
    }

private const val WRITE_DONE = 0
private const val WRITE_FULL = 1
private const val PROBE_KEY = "mk:probe"

private fun readItem(key: String): String? =
    js(
        """{
        try { return window.localStorage.getItem(key); } catch (e) { return null; }
    }""",
    )

// 0 when it landed, 1 when storage is full, 2 when storage is blocked.
private fun writeItem(
    key: String,
    value: String,
): Int =
    js(
        """{
        try {
            window.localStorage.setItem(key, value);
            return 0;
        } catch (e) {
            const name = e && e.name;
            const code = e && e.code;
            const full = name === 'QuotaExceededError' || name === 'NS_ERROR_DOM_QUOTA_REACHED' ||
                code === 22 || code === 1014;
            return full ? 1 : 2;
        }
    }""",
    )

private fun removeItem(key: String): Int =
    js(
        """{
        try {
            window.localStorage.removeItem(key);
            return 0;
        } catch (e) {
            return 2;
        }
    }""",
    )

private fun listenForStorage(listener: (String?) -> Unit): Unit =
    js(
        """{
        window.addEventListener('storage', (event) => {
            let area = null;
            try { area = window.localStorage; } catch (e) { return; }
            if (event.storageArea === area) listener(event.key);
        });
    }""",
    )

private fun persist(): Promise<JsBoolean> =
    js(
        """{
        if (!navigator.storage || !navigator.storage.persist) return Promise.resolve(false);
        return navigator.storage.persist().catch(() => false);
    }""",
    )
