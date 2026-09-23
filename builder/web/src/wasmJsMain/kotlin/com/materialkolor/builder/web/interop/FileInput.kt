@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import kotlinx.coroutines.await
import org.w3c.files.File
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise

/**
 * Open the browser's file picker for one image. Null when the user closes it without choosing.
 *
 * The picker opens before the first suspension, so a caller that starts this undispatched from a
 * click opens it inside the click, which every browser insists on.
 */
internal suspend fun pickImageFile(): File? = openImagePicker().await<File?>()

// A picker closed without a choice fires `cancel` on the input, in every browser the builder
// supports.
private fun openImagePicker(): Promise<File?> =
    js(
        """new Promise((resolve) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.style.display = 'none';
        const finish = (file) => {
            input.remove();
            resolve(file);
        };
        input.addEventListener('change', () => finish((input.files && input.files[0]) || null), { once: true });
        input.addEventListener('cancel', () => finish(null), { once: true });
        document.body.appendChild(input);
        input.click();
    })""",
    )
