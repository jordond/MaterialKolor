package com.materialkolor.builder.core.platform

import androidx.compose.ui.graphics.ImageBitmap
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.SITE_ORIGIN
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.domain.persist.QuarantineReason
import com.materialkolor.builder.domain.persist.RecordCodec
import com.materialkolor.builder.domain.persist.StorageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

// These are the only public types in shared besides `BuilderApp` and `InMemoryStoreFactory`,
// because `:builder:apps:web` and `:builder:apps:desktop` implement them. Everything that uses them
// lives behind the graph. ArchitectureTest keeps everything else in shared and the apps internal or
// private.

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
    val linkCards: LinkCardSource
    val libraryVersions: LibraryVersionSource
}

/**
 * The cards a share link shows in a chat app, fetched so the share dialog can show one too.
 */
interface LinkCardSource {
    /**
     * The PNG at [url], or null for anything else. There is no network, the answer has a status that
     * is not OK or a type that is not `image/png`, or the fetch threw.
     */
    suspend fun fetch(url: String): ByteArray?
}

/**
 * The versions each library an export depends on has published, so an export names current ones
 * rather than the ones this build was made with.
 */
interface LibraryVersionSource {
    /**
     * The JSON the site serves at `/api/versions`, or null when there is none. There is no network,
     * the answer is not OK or not JSON, or the fetch threw.
     */
    suspend fun fetch(): String?
}

/**
 * The address bar and the back button.
 *
 * Panels open as history entries on the same URL, so back closes the top one instead of leaving.
 */
interface Router {
    /**
     * Where the builder was opened, read before the first composition.
     */
    val initial: Route

    /**
     * Put `/` back in the address bar once a link has been read, without adding an entry.
     */
    fun replaceHome()

    /**
     * Add a history entry for the overlay [id], so back closes it.
     */
    fun pushOverlay(id: String)

    /**
     * Drop the entry the top overlay added, when it closes some other way than back.
     */
    fun popOverlay()

    /**
     * Emits each time back pops an overlay entry.
     */
    val overlayPops: Flow<Unit>
}

/**
 * One stored record.
 *
 * A record that is missing reads as its default. One that no longer decodes is set aside under its
 * quarantine key and also reads as the default, so user data is never dropped silently. One written
 * by a newer build stays where it is, reads as the default and turns down every update.
 *
 * A failed write comes back as a [StoreError] rather than the `Result` that [Clipboard] and
 * [FileSaver] use, because store failures are a closed set the UI branches on (a full quota shows
 * its own banner) while clipboard and file failures are opaque platform errors.
 */
interface Store<T> {
    /**
     * The record now and after every change, including changes from another tab.
     */
    val data: Flow<T>

    /**
     * The record as it is stored right now.
     */
    suspend fun get(): T

    /**
     * Replace the record with what [block] makes of it.
     *
     * Returns the reason the write did not land, or null when it did. An update the store turns down
     * for what the key holds, a record from a newer build or unreadable text it could not move aside,
     * is turned down before [block] runs, so [block] never sees it. Only a write that fails after
     * that, on a full storage for example, has called [block].
     */
    suspend fun update(block: (T) -> T): StoreError?

    /**
     * Whether a newer build wrote the record, so this one reads its default and turns down every
     * update to it.
     *
     * Asking only reads. Nothing is moved aside or reported on [StoreFactory.quarantined], and a
     * record that is missing, readable or unreadable answers false.
     */
    suspend fun fromNewerBuild(): Boolean

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
    /**
     * The store for [key], written and read through [codec], holding [default] until it is first written.
     */
    fun <T> create(
        key: String,
        codec: RecordCodec<T>,
        default: T,
    ): Store<T>

    /**
     * Keys another tab wrote. Keys the builder does not know are left out.
     */
    val externalChanges: Flow<StorageKey>

    /**
     * Records a store could not read, each reported once. Unreadable text has been moved to its
     * quarantine key by then, while a record from a newer build is left in place.
     *
     * Reports found before anything collects are kept until the first collector comes, and each
     * report reaches exactly one collector. The web store keeps to this too, since boot reads the
     * stores before the toast host subscribes.
     */
    val quarantined: Flow<Quarantined>
}

/**
 * Why a write to a [Store] did not land.
 */
enum class StoreError {
    /**
     * The storage is full. The repository prunes old histories and tries again.
     */
    QuotaExceeded,

    /**
     * There is no storage at all, private browsing for example.
     */
    Unavailable,
}

/**
 * The system clipboard.
 */
interface Clipboard {
    /**
     * Put [text] on the clipboard. A failure means it did not land.
     *
     * Browsers only allow this with user activation, so start it from the click with
     * `scope.launch(start = CoroutineStart.UNDISPATCHED)` and make this call the first suspension,
     * with no model hop, `withContext` or `yield` before it.
     */
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
    /**
     * Save [bytes] as [name] through the platform's download or save dialog.
     *
     * Safari only allows this with user activation. Build [bytes] before the click, then start this
     * from the click with `scope.launch(start = CoroutineStart.UNDISPATCHED)` and make it the first
     * suspension, with no model hop, `withContext` or `yield` before it.
     */
    suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit>

    /**
     * Whether the share sheet takes the kinds of file an export makes here, so Share is worth offering.
     */
    val canShareFiles: Boolean

    /**
     * Whether the share sheet takes [files] here, answered without suspending.
     *
     * The click asks this first and then calls either [shareFiles] or [save]. It never falls back to
     * [save] after a share fails, because by then the click is spent in Safari.
     */
    fun canShare(files: List<OutgoingFile>): Boolean

    /**
     * Hand [files] to the share sheet.
     *
     * Every browser needs user activation for this, so it starts the way [save] does, with the bytes
     * built before the click and this call as the first suspension of an undispatched launch. A share
     * sheet the user dismisses counts as success, with no toast and no fallback to [save].
     */
    suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit>

    /**
     * Whether a share link goes to the share sheet here rather than the clipboard, on a touch
     * screen whose browser has one. Read it in the click, before anything suspends.
     */
    val canShareLink: Boolean

    /**
     * Hand the link [url] called [title] to the share sheet.
     *
     * It needs user activation the way [shareFiles] does, so start it from the click with
     * `scope.launch(start = CoroutineStart.UNDISPATCHED)` and make it the first suspension. A share
     * sheet the user dismisses counts as success.
     */
    suspend fun shareLink(
        url: String,
        title: String,
    ): Result<Unit>
}

/**
 * An image the user picked, dropped or pasted, not read yet.
 */
interface ImageHandle {
    /**
     * The file name, when the platform knows it.
     */
    val name: String?
}

/**
 * An image read for seed extraction.
 *
 * @property[width] The width of [pixels], at most 128.
 * @property[height] The height of [pixels], at most 128.
 * @property[pixels] ARGB pixels row by row, scaled so the longer side is at most 128 px.
 * @property[thumbnail] The image scaled so the longer side is at most 256 px, for showing back.
 * @property[detail] The image scaled so the longer side is at most 1024 px, for the image eyedropper.
 */
class DecodedImage(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
    val thumbnail: ImageBitmap,
    val detail: ImageBitmap,
)

/**
 * Images coming in from a picker or a drop.
 */
interface ImageInput {
    /**
     * Open the platform picker. Null when the user closes it without choosing, or when there is no
     * click to open it in.
     *
     * Browsers only open it with user activation, so start it from the click with
     * `scope.launch(start = CoroutineStart.UNDISPATCHED)` and make this call the first suspension,
     * with no model hop, `withContext` or `yield` before it.
     */
    suspend fun pick(): ImageHandle?

    /**
     * Files dropped anywhere on the builder, images or not, so [decode] can turn down the rest.
     */
    val drops: Flow<ImageHandle>

    /**
     * Read [handle]. Null when it is not an image the platform can read, or one too big to read safely.
     */
    suspend fun decode(handle: ImageHandle): DecodedImage?

    /**
     * Whether files are being dragged over the builder right now, for the drop overlay.
     */
    val dragging: StateFlow<Boolean>
}

/**
 * Something the user pasted while nothing editable had focus.
 */
sealed interface Paste {
    /**
     * Pasted text, a color or a share link perhaps.
     */
    data class Text(
        val text: String,
    ) : Paste

    /**
     * Pasted files, usually a screenshot. Files that are not images come too, for [ImageInput.decode] to turn down.
     */
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
    /**
     * Whether the system is in dark mode.
     */
    val prefersDark: StateFlow<Boolean>

    /**
     * Whether the user asked the system for less motion.
     */
    val reducedMotion: StateFlow<Boolean>

    /**
     * Whether the main pointer is a finger rather than a mouse.
     */
    val coarsePointer: StateFlow<Boolean>

    /**
     * The kind of device this is, so a new project frames its preview at the width the user holds.
     * The web tells a phone, a tablet and a computer apart by the user agent and the screen, and the
     * desktop build is always [DeviceWidth.Desktop]. A saved project keeps the width it was saved with.
     */
    val defaultDeviceWidth: DeviceWidth

    /**
     * Whether [pickScreenColor] can do anything here.
     */
    val eyeDropperAvailable: Boolean

    /**
     * Let the user pick a color off the screen. Null when they cancel.
     *
     * Browsers only open the eyedropper with user activation, so start it from the click with
     * `scope.launch(start = CoroutineStart.UNDISPATCHED)` and make this call the first suspension,
     * with no model hop, `withContext` or `yield` before it.
     */
    suspend fun pickScreenColor(): Argb?

    /**
     * Remove the boot splash once the first frame is up.
     */
    fun hideSplash()

    /**
     * Tint the browser chrome, the `theme-color` meta tag on the web.
     */
    fun setThemeColor(argb: Argb)

    /**
     * Remember [splash] so the next boot paints it before any code loads.
     */
    fun writeSplash(splash: BootSplash)

    /**
     * An id for this tab, fresh on every load, so autosave can tell its own writes apart.
     */
    val tabId: String

    /**
     * Ask the platform to keep stored data through storage pressure. True when it agreed.
     */
    suspend fun requestPersist(): Boolean

    /**
     * Whether writes to [StoreFactory] stores outlive this session.
     */
    val storageAvailable: Boolean

    /**
     * The project this tab had open, kept through a reload and gone with the tab. Null when there is none.
     */
    fun readTabProject(): String?

    /**
     * Remember [id] as the project this tab has open, or forget it with null.
     */
    fun writeTabProject(id: String?)

    /**
     * Emits when the page goes out of sight, hidden behind another tab, closed or put in the back and
     * forward cache. Never emits off the web.
     *
     * The session can save before the page is gone only when its collector runs undispatched. On wasm
     * `Dispatchers.Main` resumes a collector in a later task, and after `pagehide` that task may never
     * run, so the wiring collects on `Dispatchers.Unconfined`.
     */
    val pageHides: Flow<Unit>

    /**
     * Read [message] out to a screen reader once, without moving focus. The web writes it into the
     * page's own live region, since the Compose semantics mirror never reads a live region out.
     * Elsewhere it does nothing.
     */
    fun announce(message: String)

    /**
     * The browser and the system it runs on, the way a bug report names them. The web gives the
     * user agent, the desktop build the JVM and the operating system.
     */
    val browser: String

    /**
     * Load the page again at [path], so a newer build can read what this one cannot. The web goes
     * there with `location.assign`. The desktop build has no page to load, so it does nothing.
     */
    fun reload(path: String)

    /**
     * Whether [reload] loads anything here. The web does, and the desktop build has no page to load,
     * so a banner there leaves its Reload out.
     */
    val canReload: Boolean

    /**
     * Where share links and the links in exports open. The web gives the page's own origin, so a
     * link made on staging opens on staging. The desktop build has no page and links to [SITE_ORIGIN].
     */
    val siteOrigin: String
        get() = SITE_ORIGIN

    /**
     * Leave the timing mark [name], one of [TimingMarks], for the perf run to read. The web calls
     * `performance.mark`, and its first frame mark also loads the analytics beacon. Elsewhere it
     * does nothing.
     */
    fun mark(name: String) = Unit
}

/**
 * The names [Environment.mark] leaves, in the `mk:` namespace the rest of the page uses. The perf run
 * in `builder/e2e/perf` times the budgets against them, and the web loads analytics after
 * [FIRST_FRAME].
 */
object TimingMarks {
    /**
     * The first frame is up and the splash is on its way out.
     */
    const val FIRST_FRAME: String = "mk:first-frame"

    /**
     * The root starts to resolve a theme result.
     */
    const val RESOLVE_START: String = "mk:resolve-start"

    /**
     * The root has resolved a theme result, so the time since [RESOLVE_START] is what it took.
     */
    const val RESOLVE: String = "mk:resolve"

    /**
     * An image the user brought in has its thumbnail.
     */
    const val THUMBNAIL: String = "mk:thumbnail"

    /**
     * An image the user brought in has its seed candidates.
     */
    const val EXTRACT: String = "mk:extract"
}

/**
 * What the next boot's splash paints before any code loads, the chrome, the poster and whether the
 * chrome is dark.
 *
 * @property[light] The chrome surface in light mode.
 * @property[dark] The chrome surface in dark mode.
 * @property[seed] The open theme's seed, for the poster and its hex.
 * @property[appearance] Whether the chrome is always light, always dark or follows the system.
 */
data class BootSplash(
    val light: Argb,
    val dark: Argb,
    val seed: Argb,
    val appearance: Appearance,
) {
    /**
     * The text `boot.js` reads from `mk:splash`, plain JSON such as
     * `{"light":-1,"dark":-16777216,"seed":-2529989,"appearance":"system"}`.
     *
     * Each color is a signed 32 bit ARGB integer, so an opaque one is negative and `value & 0xFFFFFF`
     * is its RGB. The appearance is `light`, `dark` or `system`.
     */
    fun toJson(): String {
        val mode = when (appearance) {
            Appearance.System -> "system"
            Appearance.Light -> "light"
            Appearance.Dark -> "dark"
        }
        return """{"light":${light.value},"dark":${dark.value},"seed":${seed.value},"appearance":"$mode"}"""
    }
}

/**
 * A record a store could not read, so the user can be told and nothing is lost. Unreadable text is
 * moved to its quarantine key, and a record from a newer build is left where it is.
 *
 * @property[key] Where the record was stored.
 * @property[reason] Why it could not be read.
 */
data class Quarantined(
    val key: String,
    val reason: QuarantineReason,
)
