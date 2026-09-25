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
 * Null when the browser cannot read it as an image, when it is over [maxBytes] or when its header
 * says it has more than [maxPixels] pixels.
 *
 * The browser decodes off the main thread and the full image stays in browser memory until it is
 * closed here. Only the 128 px pixels ever reach Kotlin, and the other two go straight to Skia. The
 * caps come first because a full decode takes four bytes a pixel before anything is scaled.
 */
internal suspend fun scaleImage(
    blob: Blob,
    pixelEdge: Int,
    thumbnailEdge: Int,
    detailEdge: Int,
    maxBytes: Int,
    maxPixels: Int,
): ScaledImage? = startScaling(blob, pixelEdge, thumbnailEdge, detailEdge, maxBytes, maxPixels).await<ScaledImage?>()

/**
 * Copy these ints into Kotlin in one go, through linear memory rather than one call per element.
 */
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
//
// Every bitmap is closed and every canvas shrunk to nothing on the way out, whichever way that is,
// since Safari holds their memory until the next collection otherwise. The header read knows PNG,
// JPEG, GIF, WebP and BMP. Anything else, HEIC for one, has only the byte cap.
private fun startScaling(
    blob: Blob,
    pixelEdge: Int,
    thumbnailEdge: Int,
    detailEdge: Int,
    maxBytes: Int,
    maxPixels: Int,
): Promise<ScaledImage?> =
    js(
        """(async () => {
        const headerPixels = async () => {
            let bytes;
            try {
                bytes = new Uint8Array(await blob.slice(0, 262144).arrayBuffer());
            } catch (e) {
                return 0;
            }
            const view = new DataView(bytes.buffer);
            const has = (offset, length) => offset + length <= bytes.length;
            const ascii = (offset, text) =>
                has(offset, text.length) && Array.from(text).every((c, i) => bytes[offset + i] === c.charCodeAt(0));
            if (bytes[0] === 0x89 && ascii(1, 'PNG') && ascii(12, 'IHDR') && has(16, 8)) {
                return view.getUint32(16) * view.getUint32(20);
            }
            if (ascii(0, 'GIF8') && has(6, 4)) return view.getUint16(6, true) * view.getUint16(8, true);
            if (ascii(0, 'BM') && has(18, 8)) return Math.abs(view.getInt32(18, true) * view.getInt32(22, true));
            if (ascii(0, 'RIFF') && ascii(8, 'WEBP')) {
                const triple = (offset) => bytes[offset] | (bytes[offset + 1] << 8) | (bytes[offset + 2] << 16);
                if (ascii(12, 'VP8X') && has(24, 6)) return (triple(24) + 1) * (triple(27) + 1);
                if (ascii(12, 'VP8L') && has(21, 4)) {
                    const bits = view.getUint32(21, true);
                    return ((bits & 0x3fff) + 1) * (((bits >>> 14) & 0x3fff) + 1);
                }
                if (ascii(12, 'VP8 ') && has(26, 4)) {
                    return (view.getUint16(26, true) & 0x3fff) * (view.getUint16(28, true) & 0x3fff);
                }
                return 0;
            }
            // JPEG keeps its size in the first start of frame segment, after EXIF and the rest.
            if (bytes[0] === 0xff && bytes[1] === 0xd8) {
                // Every C0 to CF marker starts a frame except C4, C8 and CC.
                const frame = (marker) => marker >= 0xc0 && marker <= 0xcf && ![0xc4, 0xc8, 0xcc].includes(marker);
                let offset = 2;
                while (has(offset, 9) && bytes[offset] === 0xff) {
                    const marker = bytes[offset + 1];
                    if (marker === 0xff) {
                        offset += 1;
                    } else if (frame(marker)) {
                        return view.getUint16(offset + 5) * view.getUint16(offset + 7);
                    } else {
                        offset += 2 + view.getUint16(offset + 2);
                    }
                }
            }
            return 0;
        };
        if (blob.size > maxBytes || (await headerPixels()) > maxPixels) return null;

        const held = [];
        const hold = (source) => {
            held.push(source);
            return source;
        };
        const release = (source) => {
            if (typeof source.close === 'function') {
                source.close();
            } else {
                source.width = 0;
                source.height = 0;
            }
        };
        const started = performance.now();
        let full;
        try {
            full = hold(await createImageBitmap(blob));
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
            const canvas = hold(canvasOf(width, height));
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
                const bitmap = hold(
                    await createImageBitmap(source, {
                        resizeWidth: width,
                        resizeHeight: height,
                        resizeQuality: 'medium',
                    }),
                );
                if (bitmap.width === width && bitmap.height === height) return bitmap;
                bitmap.close();
            } catch (e) {}
            resized = false;
            return draw(source, width, height);
        };
        const read = (source) => {
            const canvas = draw(source, source.width, source.height);
            const context = canvas.getContext('2d', { willReadFrequently: true });
            const buffer = context.getImageData(0, 0, source.width, source.height).data.buffer;
            release(canvas);
            return buffer;
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
            return image;
        } catch (e) {
            return null;
        } finally {
            held.forEach(release);
        }
    })()""",
    )
