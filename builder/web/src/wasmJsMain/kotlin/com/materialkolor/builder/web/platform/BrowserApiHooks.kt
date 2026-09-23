package com.materialkolor.builder.web.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import com.materialkolor.builder.core.platform.DecodedImage
import com.materialkolor.builder.core.platform.ImageHandle
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.web.interop.addE2eGestureButton
import com.materialkolor.builder.web.interop.e2eHooksWanted
import com.materialkolor.builder.web.interop.exposeE2eHook
import com.materialkolor.builder.web.interop.pageMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.files.File
import kotlin.math.roundToInt

// Hooks the Playwright specs in `builder/e2e` drive the clipboard, downloads, sharing, images,
// pastes and the eyedropper through, while the app has no UI that reaches them. They exist only
// when the spec asks for them, like the hooks in E2eHooks.kt.
//
// The browser only lets a page copy, download, share, pick a file or open the eyedropper inside a
// click. A spec arms one of those with `gesture`, then clicks `#mk-e2e-gesture`. Its handler starts
// the call on an unconfined scope, so it runs inside the click the way a builder button will.

private val hookScope = CoroutineScope(Dispatchers.Unconfined)

/** Hang the browser API hooks on the page when a spec opened it. */
internal fun PlatformServices.exposeBrowserApisToE2e() {
    if (!e2eHooksWanted()) return
    val outcomes = mutableMapOf<String, String>()
    val inputs = mutableListOf<String>()
    var latest: ImageHandle? = null
    var image: DecodedImage? = null
    var decoded = "None"
    var armed = ""

    hookScope.launch {
        images.drops.collect { handle ->
            latest = handle
            inputs += "drop ${handle.name}"
        }
    }
    hookScope.launch {
        pastes.pastes.collect { paste ->
            when (paste) {
                is Paste.Text -> {
                    inputs += "text ${paste.text}"
                }
                is Paste.Files -> {
                    latest = paste.files.lastOrNull()
                    inputs += "files ${paste.files.joinToString(",") { handle -> handle.name.orEmpty() }}"
                }
            }
        }
    }

    fun start(
        action: String,
        argument: String,
    ) {
        outcomes[action] = "Pending"
        hookScope.launch {
            outcomes[action] = when (action) {
                "copy" -> clipboard.writeText(argument).describe()
                "save" -> files.save(argument, everyByte(), argument.mimeOf()).describe()
                "share" -> files.shareFiles(sharedFiles(argument)).describe()
                "pick" -> images.pick().let { handle ->
                    if (handle != null) latest = handle
                    handle?.name ?: "None"
                }
                "screenColor" -> environment.pickScreenColor()?.toHex() ?: "None"
                else -> "Unknown $action"
            }
        }
    }

    addE2eGestureButton {
        val action = armed.substringBefore(':')
        val argument = armed.substringAfter(':', missingDelimiterValue = "")
        armed = ""
        if (action.isNotEmpty()) start(action, argument)
    }
    exposeE2eAction("gesture") { action -> armed = action }
    exposeE2eAction("copyWithoutGesture") { text -> start("copy", text) }
    exposeE2eHook("outcome") { action -> outcomes[action].orEmpty() }
    exposeE2eHook("canShareFiles") { files.canShareFiles.toString() }
    exposeE2eHook("dragging") { WebImageInput.dragging.value.toString() }
    exposeE2eHook("inputs") { inputs.joinToString("\n") }
    exposeE2eHook("decodeLatest") {
        val handle = latest
        if (handle == null) {
            decoded = "None"
        } else {
            decoded = "Pending"
            hookScope.launch {
                val started = pageMillis()
                val result = images.decode(handle)
                image = result
                decoded = result?.describe(pageMillis() - started) ?: "None"
            }
        }
        decoded
    }
    exposeE2eHook("decoded") { decoded }
    exposeE2eHook("profileLatest") {
        val file = (latest as? BrowserImage)?.file
        if (file == null) {
            decoded = "None"
        } else {
            decoded = "Pending"
            hookScope.launch { decoded = profile(file) }
        }
        decoded
    }
    exposeE2eHook("pixel") { at ->
        val current = image ?: return@exposeE2eHook ""
        val (x, y) = at.split(",").map(String::toInt)
        current.pixels[y * current.width + x].toHexArgb()
    }
    exposeE2eHook("thumbnailPixel") { at -> image?.thumbnail?.pixelAt(at).orEmpty() }
    exposeE2eHook("detailPixel") { at -> image?.detail?.pixelAt(at).orEmpty() }
}

// Each stage timed apart for spike S7, the browser's decode, scale and read, then the copy into
// Kotlin and Skia.
private suspend fun profile(file: File): String {
    val started = pageMillis()
    val scaled = scale(file) ?: return "None"
    val copyStarted = pageMillis()
    scaled.toDecodedImage() ?: return "None"
    val finished = pageMillis()
    return "{" +
        "\"resized\":${scaled.resized}," +
        "\"decode\":${scaled.decodeMillis.tenths()}," +
        "\"scale\":${scaled.scaleMillis.tenths()}," +
        "\"read\":${scaled.readMillis.tenths()}," +
        "\"copy\":${(finished - copyStarted).tenths()}," +
        "\"total\":${(finished - started).tenths()}" +
        "}"
}

private fun sharedFiles(secondMime: String): List<OutgoingFile> =
    listOf(
        OutgoingFile("palette.txt", "MaterialKolor".encodeToByteArray(), "text/plain"),
        OutgoingFile("theme.txt", "val seed = 0xFF1A73E8".encodeToByteArray(), secondMime.ifEmpty { "text/plain" }),
    )

private fun String.mimeOf(): String = if (endsWith(".txt")) "text/plain" else "application/octet-stream"

private fun everyByte(): ByteArray = ByteArray(BYTE_VALUES) { value -> value.toByte() }

private fun Result<Unit>.describe(): String =
    fold(onSuccess = { "Done" }, onFailure = { error -> "Failed ${error.message}" })

private fun DecodedImage.describe(millis: Double): String =
    "{" +
        "\"width\":$width,\"height\":$height,\"pixels\":${pixels.size}," +
        "\"thumbnail\":[${thumbnail.width},${thumbnail.height}]," +
        "\"detail\":[${detail.width},${detail.height}]," +
        "\"millis\":${millis.tenths()}" +
        "}"

private fun ImageBitmap.pixelAt(at: String): String {
    val (x, y) = at.split(",").map(String::toInt)
    return toPixelMap(startX = x, startY = y, width = 1, height = 1)[0, 0].toArgb().toHexArgb()
}

private fun Int.toHexArgb(): String =
    "#" + toUInt().toString(radix = 16).padStart(length = 8, padChar = '0').uppercase()

private fun Double.tenths(): Double = (this * 10).roundToInt() / 10.0

/** A hook that does something and has nothing to say back. */
private fun exposeE2eAction(
    name: String,
    action: (String) -> Unit,
) = exposeE2eHook(name) { argument ->
    action(argument)
    ""
}

private const val BYTE_VALUES = 256
