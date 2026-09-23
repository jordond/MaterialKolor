@file:OptIn(ExperimentalWasmJsInterop::class)

package com.materialkolor.builder.web.interop

import org.w3c.files.File
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Watch the window for files dragged onto the page, for the life of the page.
 *
 * [onDragging] hears true when files first come over the page and false when they leave or land.
 * [onDrop] hears each dropped image file in turn. Dropped files that are not images are ignored,
 * and so is any drag that carries no files, text for example.
 *
 * Every file drag is taken over, whether or not it holds an image, because the browser would
 * otherwise open the dropped file in place of the builder.
 */
internal fun listenForFileDrops(
    onDragging: (Boolean) -> Unit,
    onDrop: (File) -> Unit,
): Unit =
    js(
        """{
        let depth = 0;
        const carriesFiles = (event) =>
            !!event.dataTransfer && Array.from(event.dataTransfer.types || []).includes('Files');
        window.addEventListener('dragenter', (event) => {
            if (!carriesFiles(event)) return;
            event.preventDefault();
            depth += 1;
            if (depth === 1) onDragging(true);
        });
        window.addEventListener('dragover', (event) => {
            if (!carriesFiles(event)) return;
            event.preventDefault();
            event.dataTransfer.dropEffect = 'copy';
        });
        window.addEventListener('dragleave', (event) => {
            if (!carriesFiles(event) || depth === 0) return;
            depth -= 1;
            if (depth === 0) onDragging(false);
        });
        window.addEventListener('drop', (event) => {
            if (!carriesFiles(event)) return;
            event.preventDefault();
            if (depth > 0) onDragging(false);
            depth = 0;
            Array.from(event.dataTransfer.files || [])
                .filter((file) => typeof file.type === 'string' && file.type.startsWith('image/'))
                .forEach((file) => onDrop(file));
        });
    }""",
    )
