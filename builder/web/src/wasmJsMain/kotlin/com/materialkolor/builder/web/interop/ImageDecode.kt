@file:OptIn(ExperimentalWasmJsInterop::class, UnsafeWasmMemoryApi::class)

package com.materialkolor.builder.web.interop

import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int32Array
import org.w3c.files.Blob
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * An image scaled down three ways, still in browser memory.
 *
 * Each scale keeps the aspect and never grows an image smaller than its edge.
 *
 * @property[width] The width of [pixels].
 * @property[height] The height of [pixels].
 * @property[pixels] ARGB pixels row by row, the longer side at the pixel edge.
 * @property[thumbnail] RGBA bytes row by row, not premultiplied, the longer side at the thumbnail
 *   edge.
 * @property[detail] RGBA bytes the same way, the longer side at the detail edge.
 * @property[resized] Whether `createImageBitmap` did the scaling. False when it ignored the resize
 *   options and a canvas scaled instead.
 * @property[decodeMillis] How long the full decode took.
 * @property[scaleMillis] How long the three scales took.
 * @property[readMillis] How long reading the pixels back took.
 */
internal external interface ScaledImage : JsAny {
    val width: Int
    val height: Int
    val pixels: Int32Array
    val thumbnailWidth: Int
    val thumbnailHeight: Int
    val thumbnail: ArrayBuffer
    val detailWidth: Int
    val detailHeight: Int
    val detail: ArrayBuffer
    val resized: Boolean
    val decodeMillis: Double
    val scaleMillis: Double
    val readMillis: Double
}

/**
 * Decode [blob] and scale it to [pixelEdge], [thumbnailEdge] and [detailEdge] on the longer side.
 * Null when the browser cannot read it as an image.
 *
 * The browser decodes off the main thread and the full image stays in browser memory until it is
 * closed here. Only the 128 px pixels ever reach Kotlin, and the other two go straight to Skia.
 */
internal suspend fun scaleImage(
    blob: Blob,
    pixelEdge: Int,
    thumbnailEdge: Int,
    detailEdge: Int,
): ScaledImage? = startScaling(blob, pixelEdge, thumbnailEdge, detailEdge).await<ScaledImage?>()

/** Copy these ints into Kotlin in one go, through linear memory rather than one call per element. */
internal fun Int32Array.copyToIntArray(): IntArray {
    val size = length
    if (size == 0) return IntArray(0)
    return withScopedMemoryAllocator { allocator ->
        val start = allocator.allocate(size * Int.SIZE_BYTES)
        copyIntoMemory(this, wasmMemory, start.address.toInt())
        IntArray(size) { index -> (start + index * Int.SIZE_BYTES).loadInt() }
    }
}

// The allocator hands out addresses that are multiples of 8, so the view lines up.
private fun copyIntoMemory(
    source: Int32Array,
    memory: WebAssembly.Memory,
    address: Int,
): Unit = js("new Int32Array(memory.buffer, address, source.length).set(source)")

// The detail comes from the full image, then the two smaller ones from the detail, which is much
// cheaper than scaling the full image three times. Any browser that ignores the resize options
// (the bitmap comes back at full size) gets the canvas instead.
private fun startScaling(
    blob: Blob,
    pixelEdge: Int,
    thumbnailEdge: Int,
    detailEdge: Int,
): Promise<ScaledImage?> =
    js(
        """(async () => {
        const started = performance.now();
        let full;
        try {
            full = await createImageBitmap(blob);
        } catch (e) {
            return null;
        }
        const decoded = performance.now();
        let resized = true;
        const fit = (source, edge) => {
            const scale = Math.min(1, edge / Math.max(source.width, source.height));
            return [Math.max(1, Math.round(source.width * scale)), Math.max(1, Math.round(source.height * scale))];
        };
        const canvasOf = (width, height) => {
            if (typeof OffscreenCanvas === 'function') return new OffscreenCanvas(width, height);
            const canvas = document.createElement('canvas');
            canvas.width = width;
            canvas.height = height;
            return canvas;
        };
        const draw = (source, width, height) => {
            const canvas = canvasOf(width, height);
            const context = canvas.getContext('2d', { willReadFrequently: true });
            context.imageSmoothingEnabled = true;
            context.imageSmoothingQuality = 'high';
            context.drawImage(source, 0, 0, width, height);
            return canvas;
        };
        const scale = async (source, edge) => {
            const [width, height] = fit(source, edge);
            if (width === source.width && height === source.height) return source;
            try {
                const bitmap = await createImageBitmap(source, {
                    resizeWidth: width,
                    resizeHeight: height,
                    resizeQuality: 'medium',
                });
                if (bitmap.width === width && bitmap.height === height) return bitmap;
                bitmap.close();
            } catch (e) {}
            resized = false;
            return draw(source, width, height);
        };
        const read = (source) => {
            const context = draw(source, source.width, source.height).getContext('2d', { willReadFrequently: true });
            return context.getImageData(0, 0, source.width, source.height).data.buffer;
        };
        const release = (source) => {
            if (typeof source.close === 'function') source.close();
        };
        try {
            const detail = await scale(full, detailEdge);
            if (detail !== full) release(full);
            const [thumbnail, small] = await Promise.all([scale(detail, thumbnailEdge), scale(detail, pixelEdge)]);
            const scaled = performance.now();
            const pixels = new Int32Array(read(small));
            for (let index = 0; index < pixels.length; index++) {
                const rgba = pixels[index];
                pixels[index] = (rgba & 0xff00ff00) | ((rgba & 0xff) << 16) | ((rgba >>> 16) & 0xff);
            }
            const image = {
                width: small.width,
                height: small.height,
                pixels,
                thumbnailWidth: thumbnail.width,
                thumbnailHeight: thumbnail.height,
                thumbnail: read(thumbnail),
                detailWidth: detail.width,
                detailHeight: detail.height,
                detail: read(detail),
                resized,
                decodeMillis: decoded - started,
                scaleMillis: scaled - decoded,
                readMillis: performance.now() - scaled,
            };
            new Set([full, detail, thumbnail, small]).forEach(release);
            return image;
        } catch (e) {
            release(full);
            return null;
        }
    })()""",
    )
