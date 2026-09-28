package com.materialkolor.builder.web.platform

import androidx.compose.ui.graphics.asComposeImageBitmap
import com.materialkolor.builder.core.platform.DecodedImage
import com.materialkolor.builder.core.platform.ImageHandle
import com.materialkolor.builder.core.platform.ImageInput
import com.materialkolor.builder.web.interop.ScaledImage
import com.materialkolor.builder.web.interop.copyToIntArray
import com.materialkolor.builder.web.interop.listenForFileDrops
import com.materialkolor.builder.web.interop.pickImageFile
import com.materialkolor.builder.web.interop.scaleImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.webext.installPixelsFromArrayBuffer
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.khronos.webgl.ArrayBuffer
import org.w3c.files.File

/**
 * Images from the file picker and from files dropped on the page, read in the browser.
 *
 * The file itself never enters Kotlin memory. The browser decodes and scales it, the 128 px pixels
 * are copied across, and the 256 px thumbnail and the 1024 px detail go from the browser to Skia.
 */
internal object WebImageInput : ImageInput {
    private val dropped = MutableSharedFlow<ImageHandle>(extraBufferCapacity = DROP_BUFFER)
    private val dragOver = MutableStateFlow(false)

    override val drops: Flow<ImageHandle> = dropped.asSharedFlow()
    override val dragging: StateFlow<Boolean> = dragOver.asStateFlow()

    init {
        listenForFileDrops(
            onDragging = { active -> dragOver.value = active },
            onDrop = { file -> dropped.tryEmit(BrowserImage(file)) },
        )
    }

    override suspend fun pick(): ImageHandle? = pickImageFile()?.let(::BrowserImage)

    override suspend fun decode(handle: ImageHandle): DecodedImage? {
        val file = (handle as? BrowserImage)?.file ?: return null
        return scale(file)?.toDecodedImage()
    }
}

internal class BrowserImage(
    val file: File,
) : ImageHandle {
    override val name: String? = file.name.ifEmpty { null }
}

internal suspend fun scale(file: File): ScaledImage? =
    scaleImage(file, PIXEL_EDGE, THUMBNAIL_EDGE, DETAIL_EDGE, MAX_BYTES, MAX_PIXELS)

internal suspend fun ScaledImage.toDecodedImage(): DecodedImage? {
    val thumbnailBitmap = thumbnail.toSkiaBitmap(thumbnailWidth, thumbnailHeight) ?: return null
    val detailBitmap = detail.toSkiaBitmap(detailWidth, detailHeight)
    if (detailBitmap == null) {
        thumbnailBitmap.close()
        return null
    }

    return DecodedImage(
        width = width,
        height = height,
        pixels = pixels.copyToIntArray(),
        thumbnail = thumbnailBitmap.asComposeImageBitmap(),
        detail = detailBitmap.asComposeImageBitmap(),
    )
}

@OptIn(ExperimentalSkikoApi::class)
private suspend fun ArrayBuffer.toSkiaBitmap(
    width: Int,
    height: Int,
): Bitmap? {
    val bitmap = Bitmap()
    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
    if (!bitmap.installPixelsFromArrayBuffer(info, this, info.minRowBytes)) {
        bitmap.close()
        return null
    }
    bitmap.setImmutable()
    return bitmap
}

private const val PIXEL_EDGE = 128
private const val THUMBNAIL_EDGE = 256
private const val DETAIL_EDGE = 1024
private const val MAX_PIXELS = 50_000_000
private const val MAX_BYTES = 50 * 1024 * 1024
private const val DROP_BUFFER = 16
