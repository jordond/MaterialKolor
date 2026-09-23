package com.materialkolor.builder.desktop

import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.DecodedImage
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.ImageHandle
import com.materialkolor.builder.core.platform.ImageInput
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.core.platform.Store
import com.materialkolor.builder.core.platform.StoreError
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.persist.DecodeOutcome
import com.materialkolor.builder.domain.persist.RecordCodec
import com.materialkolor.builder.domain.persist.StorageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * The desktop window's services.
 *
 * Desktop is a development target, so stores live in memory for the session and the clipboard,
 * files and images report that they are not available yet.
 */
internal object DesktopPlatform : PlatformServices {
    override val router: Router = DesktopRouter
    override val stores: StoreFactory = MemoryStoreFactory()
    override val clipboard: Clipboard = DesktopClipboard
    override val files: FileSaver = DesktopFileSaver
    override val images: ImageInput = DesktopImageInput
    override val pastes: PasteInput = DesktopPasteInput
    override val environment: Environment = DesktopEnvironment
}

/** A window has no address bar, so the builder always opens at home and overlays keep no history. */
private object DesktopRouter : Router {
    override val initial: Route = Route.Home
    override val overlayPops: Flow<Unit> = emptyFlow()

    override fun replaceHome() = Unit

    override fun pushOverlay(id: String) = Unit

    override fun popOverlay() = Unit
}

/**
 * Stores that keep each record as the text its codec writes, so a desktop run exercises the same
 * encode and decode path the browser does.
 */
private class MemoryStoreFactory : StoreFactory {
    private val texts = MutableStateFlow<Map<String, String>>(emptyMap())

    override val externalChanges: Flow<StorageKey> = emptyFlow()

    override fun <T> create(
        key: String,
        codec: RecordCodec<T>,
        default: T,
    ): Store<T> = MemoryStore(key, codec, default, texts)
}

private class MemoryStore<T>(
    private val key: String,
    private val codec: RecordCodec<T>,
    private val default: T,
    private val texts: MutableStateFlow<Map<String, String>>,
) : Store<T> {
    override val data: Flow<T> = texts.map { stored -> stored[key] }.distinctUntilChanged().map(::read)

    override suspend fun get(): T = read(texts.value[key])

    override suspend fun update(block: (T) -> T): StoreError? {
        texts.update { stored -> stored + (key to codec.encode(block(read(stored[key])))) }
        return null
    }

    private fun read(text: String?): T {
        if (text == null) return default
        return when (val outcome = codec.decode(text)) {
            is DecodeOutcome.Ok -> outcome.value
            is DecodeOutcome.Quarantine -> default
        }
    }
}

private object DesktopClipboard : Clipboard {
    override suspend fun writeText(text: String): Result<Unit> = notOnDesktop()
}

private object DesktopFileSaver : FileSaver {
    override val canShareFiles: Boolean = false

    override suspend fun save(
        name: String,
        bytes: ByteArray,
        mime: String,
    ): Result<Unit> = notOnDesktop()

    override suspend fun shareFiles(files: List<OutgoingFile>): Result<Unit> = notOnDesktop()
}

private object DesktopImageInput : ImageInput {
    override val drops: Flow<ImageHandle> = emptyFlow()

    override suspend fun pick(): ImageHandle? = null

    override suspend fun decode(handle: ImageHandle): DecodedImage? = null
}

private object DesktopPasteInput : PasteInput {
    override val pastes: Flow<Paste> = emptyFlow()
}

private object DesktopEnvironment : Environment {
    override val prefersDark: StateFlow<Boolean> = MutableStateFlow(false)
    override val reducedMotion: StateFlow<Boolean> = MutableStateFlow(false)
    override val coarsePointer: StateFlow<Boolean> = MutableStateFlow(false)
    override val eyeDropperAvailable: Boolean = false
    override val tabId: String = UUID.randomUUID().toString()

    // Stores are in memory, so nothing written survives the window.
    override val storageAvailable: Boolean = false

    override suspend fun pickScreenColor(): Argb? = null

    override fun hideSplash() = Unit

    override fun setThemeColor(argb: Argb) = Unit

    override fun writeSplashColors(
        light: Argb,
        dark: Argb,
    ) = Unit

    override suspend fun requestPersist(): Boolean = false
}

private fun notOnDesktop(): Result<Unit> = Result.failure(UnsupportedOperationException("Not available on desktop yet"))
