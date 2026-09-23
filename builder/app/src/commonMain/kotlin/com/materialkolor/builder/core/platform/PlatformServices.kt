package com.materialkolor.builder.core.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.persist.QuarantineReason
import com.materialkolor.builder.domain.persist.RecordCodec
import com.materialkolor.builder.domain.persist.StorageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// These are the only public types in the app besides `BuilderApp` and `InMemoryStoreFactory`,
// because `:builder:web` and the desktop entry implement them. Everything that uses them lives
// behind the graph. ArchitectureTest keeps everything else in app and web internal or private.

/**
 * Everything the builder needs from the platform it runs on.
 *
 * The graph takes this as an included dependency, so each property below is a binding a model can
 * inject on its own.
 */
interface PlatformServices {
    val router: Router
    val stores: StoreFactory
    val clipboard: Clipboard
    val files: FileSaver
    val images: ImageInput
    val pastes: PasteInput
    val environment: Environment
}

/**
 * The address bar and the back button.
 *
 * Panels open as history entries on the same URL, so back closes the top one instead of leaving.
 */
interface Router {
    /** Where the builder was opened, read before the first composition. */
    val initial: Route

    /** Put `/` back in the address bar once a link has been read, without adding an entry. */
    fun replaceHome()

    /** Add a history entry for the overlay [id], so back closes it. */
    fun pushOverlay(id: String)

    /** Drop the entry the top overlay added, when it closes some other way than back. */
    fun popOverlay()

    /** Emits each time back pops an overlay entry. */
    val overlayPops: Flow<Unit>
}

/**
 * One stored record.
 *
 * A record that is missing reads as its default. One that no longer decodes is set aside under its
 * quarantine key and also reads as the default, so user data is never dropped silently.
 *
 * A failed write comes back as a [StoreError] rather than the `Result` that [Clipboard] and
 * [FileSaver] use, because store failures are a closed set the UI branches on (a full quota shows
 * its own banner) while clipboard and file failures are opaque platform errors.
 */
interface Store<T> {
    /** The record now and after every change, including changes from another tab. */
    val data: Flow<T>

    /** The record as it is stored right now. */
    suspend fun get(): T

    /**
     * Replace the record with what [block] makes of it.
     *
     * Returns the reason the write did not land, or null when it did.
     */
    suspend fun update(block: (T) -> T): StoreError?

    // b-214

    /**
     * Remove the record, so it reads as its default again.
     *
     * Returns the reason it could not be removed, or null when it was.
     */
    suspend fun delete(): StoreError?
}

/**
 * Opens [Store]s by key.
 */
interface StoreFactory {
    /** The store for [key], written and read through [codec], holding [default] until it is first written. */
    fun <T> create(
        key: String,
        codec: RecordCodec<T>,
        default: T,
    ): Store<T>

    /** Keys another tab wrote. Keys the builder does not know are left out. */
    val externalChanges: Flow<StorageKey>

    // b-214

    /**
     * Records a store could not read, each reported once after it was moved to its quarantine key.
     *
     * Reports found before anything collects are kept until the first collector comes, and each
     * report reaches exactly one collector. B-302's localStorage store has to keep to this too, since
     * boot reads the stores before the toast host subscribes.
     */
    val quarantined: Flow<Quarantined>
}

/**
 * Why a write to a [Store] did not land.
 */
enum class StoreError {
    /** The storage is full. The repository prunes old histories and tries again. */
    QuotaExceeded,

    /** There is no storage at all, private browsing for example. */
    Unavailable,
}

/**
 * The system clipboard.
 */
interface Clipboard {
    suspend fun writeText(text: String): Result<Unit>
}

/**
 * A file the builder hands to the user.
 *
 * @property[name] The file name, with its extension.
 * @property[bytes] The contents.
 * @property[mime] The media type, `application/zip` for example.
 */
class OutgoingFile(
    val name: String,
    val bytes: ByteArray,
    val mime: String,
)

/**
 * Downloads and the share sheet.
 */
interface FileSaver {
    /** Save [bytes] as [name] through the platform's download or save dialog. */
    suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit>

    /** Whether [shareFiles] can hand files to the share sheet here. */
    val canShareFiles: Boolean

    /** Hand [files] to the share sheet. */
    suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit>
}

/**
 * An image the user picked, dropped or pasted, not read yet.
 */
interface ImageHandle {
    /** The file name, when the platform knows it. */
    val name: String?
}

/**
 * An image read for seed extraction.
 *
 * @property[width] The width of [pixels], at most 128.
 * @property[height] The height of [pixels], at most 128.
 * @property[pixels] ARGB pixels row by row, scaled so the longer side is at most 128 px.
 * @property[thumbnail] The image scaled so the longer side is at most 256 px, for showing back.
 */
class DecodedImage(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
    val thumbnail: ImageBitmap,
)

/**
 * Images coming in from a picker or a drop.
 */
interface ImageInput {
    /** Open the platform picker. Null when the user closes it without choosing. */
    suspend fun pick(): ImageHandle?

    /** Images dropped anywhere on the builder. */
    val drops: Flow<ImageHandle>

    /** Read [handle]. Null when it is not an image the platform can read. */
    suspend fun decode(handle: ImageHandle): DecodedImage?
}

/**
 * Something the user pasted while nothing editable had focus.
 */
sealed interface Paste {
    /** Pasted text, a color or a share link perhaps. */
    data class Text(
        val text: String,
    ) : Paste

    /** Pasted files, usually a screenshot. */
    data class Files(
        val files: List<ImageHandle>,
    ) : Paste
}

/**
 * Pastes the builder handles itself.
 */
interface PasteInput {
    val pastes: Flow<Paste>
}

/**
 * What the builder can learn about, and ask of, the page or window around it.
 */
interface Environment {
    /** Whether the system is in dark mode. */
    val prefersDark: StateFlow<Boolean>

    /** Whether the user asked the system for less motion. */
    val reducedMotion: StateFlow<Boolean>

    /** Whether the main pointer is a finger rather than a mouse. */
    val coarsePointer: StateFlow<Boolean>

    /** Whether [pickScreenColor] can do anything here. */
    val eyeDropperAvailable: Boolean

    /** Let the user pick a color off the screen. Null when they cancel. */
    suspend fun pickScreenColor(): Argb?

    /** Remove the boot splash once the first frame is up. */
    fun hideSplash()

    /** Tint the browser chrome, the `theme-color` meta tag on the web. */
    fun setThemeColor(argb: Argb)

    /** Remember the splash colors so the next boot paints them before any code loads. */
    fun writeSplashColors(
        light: Argb,
        dark: Argb,
    )

    /** An id for this tab, fresh on every load, so autosave can tell its own writes apart. */
    val tabId: String

    /** Ask the platform to keep stored data through storage pressure. True when it agreed. */
    suspend fun requestPersist(): Boolean

    /** Whether writes to [StoreFactory] stores outlive this session. */
    val storageAvailable: Boolean
}

// b-214

/**
 * A record a store could not read, moved to its quarantine key so the user can be told and nothing
 * is lost.
 *
 * @property[key] Where the record was stored.
 * @property[reason] Why it could not be read.
 */
data class Quarantined(
    val key: String,
    val reason: QuarantineReason,
)
