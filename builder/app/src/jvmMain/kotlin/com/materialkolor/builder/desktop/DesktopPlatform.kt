package com.materialkolor.builder.desktop

import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.DecodedImage
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.ImageHandle
import com.materialkolor.builder.core.platform.ImageInput
import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.core.platform.OutgoingFile
import com.materialkolor.builder.core.platform.Paste
import com.materialkolor.builder.core.platform.PasteInput
import com.materialkolor.builder.core.platform.PlatformServices
import com.materialkolor.builder.core.platform.Router
import com.materialkolor.builder.core.platform.StoreFactory
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

/**
 * The desktop window's services.
 *
 * Desktop is a development target, so stores live in memory for the session and the clipboard,
 * files and images report that they are not available yet.
 */
internal object DesktopPlatform : PlatformServices {
    override val router: Router = DesktopRouter
    override val stores: StoreFactory = InMemoryStoreFactory()
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

    // b-302a
    override fun canShare(files: List<OutgoingFile>): Boolean = false

    // b-310
    override val canShareLink: Boolean = false

    // b-310
    override suspend fun shareLink(
        url: String,
        title: String,
    ): Result<Unit> = notOnDesktop()
}

private object DesktopImageInput : ImageInput {
    override val drops: Flow<ImageHandle> = emptyFlow()

    // b-302a
    override val dragging: StateFlow<Boolean> = MutableStateFlow(false)

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

    // b-215a
    // A window has no reload, so the tab's project only has to last as long as the window.
    @Volatile
    private var tabProject: String? = null

    override fun readTabProject(): String? = tabProject

    override fun writeTabProject(id: String?) {
        tabProject = id
    }

    // b-301
    // A window is never hidden the way a tab is, and closing it ends the session anyway.
    override val pageHides: Flow<Unit> = emptyFlow()

    // b-221c
    // The desktop build is for development only (D2), so it reads nothing out.
    override fun announce(message: String) = Unit

    // b-314
    override val browser: String =
        "Java ${System.getProperty("java.version")} (${System.getProperty("java.vm.name")}), " +
            "${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}"
}

private fun notOnDesktop(): Result<Unit> = Result.failure(UnsupportedOperationException("Not available on desktop yet"))
