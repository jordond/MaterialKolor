package com.materialkolor.builder.feature.share

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.Environment
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.platform.LinkCardSource
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.core.session.ProjectRef
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.derived
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.link.shareCardLink
import com.materialkolor.builder.domain.link.shareLink
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.banners_newer_version
import com.materialkolor.builder.generated.resources.share_invalid
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.getString
import kotlin.time.Duration.Companion.seconds

/**
 * Share links, out and in.
 *
 * A link carries every field of the document, target included, and the project name, built the
 * same way as the link every export points back with ([shareLink]). The package name and the
 * preview mode stay behind, since neither is part of the document. Going out, the link lands on the
 * clipboard, or in the share sheet on a touch screen that has one. Coming in, [openShared] opens the
 * theme a pasted or opened link carries.
 *
 * Browsers only copy and share inside a click, so [copy] and [share] make the platform call their
 * first suspension. Build the link before the click with [link], ask [sharesToSheet] in the click,
 * and start the call there with `scope.launch(start = CoroutineStart.UNDISPATCHED)`.
 */
@Inject
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
@ViewModelKey
internal class ShareController(
    private val session: ProjectSession,
    private val clipboard: Clipboard,
    private val files: FileSaver,
    private val environment: Environment,
    private val linkCards: LinkCardSource,
) : ViewModel() {
    /**
     * Whether a share goes to the share sheet here rather than the clipboard.
     */
    val sharesToSheet: Boolean
        get() = files.canShareLink

    /**
     * The link to [document] called [projectName], or null when the document cannot be put in one.
     */
    fun link(
        document: ThemeDocument,
        projectName: String,
    ): String? = shareLink(document, projectName, environment.siteOrigin)

    /**
     * The link to the card for [document] called [projectName], or null when there is no link.
     */
    fun cardLink(
        document: ThemeDocument,
        projectName: String,
    ): String? = shareCardLink(document, projectName, environment.siteOrigin)

    /**
     * The card at [url], or null when it did not come within [CARD_TIMEOUT] or is not an image.
     */
    suspend fun card(url: String): ImageBitmap? = withTimeoutOrNull(CARD_TIMEOUT) { linkCards.fetch(url) }?.let(::decodeCard)

    /**
     * Whether the open project came from a link and is not saved yet.
     */
    val transient: StateFlow<Boolean> = session.state.derived { state -> state.project is ProjectRef.Transient }

    /**
     * Save the open project from a link to the drawer. A saved project stays as it is.
     */
    fun saveToProjects() {
        viewModelScope.launch { session.saveTransient().join() }
    }

    /**
     * Rename the open project to [name], trimmed. True when a saved project took the new name, false
     * for a project from a link or a rename that did not land.
     */
    suspend fun rename(name: String): Boolean {
        val project = session.state.value.project as? ProjectRef.Persisted ?: return false
        return session.rename(project.id, name.trim()) == null
    }

    /**
     * Put [url] on the clipboard. Copied only when the clipboard really took it.
     */
    suspend fun copy(url: String): ShareOutcome =
        if (clipboard.writeText(url).isSuccess) ShareOutcome.Copied else ShareOutcome.CopyFailed

    /**
     * Hand [url] called [title] to the share sheet, or copy it where there is no sheet. The choice is
     * made before anything suspends, and a failed share never falls back to the clipboard, since the
     * click is spent by then.
     */
    suspend fun share(
        url: String,
        title: String,
    ): ShareOutcome {
        if (!files.canShareLink) return copy(url)
        return if (files.shareLink(url, title).isSuccess) ShareOutcome.Shared else ShareOutcome.ShareFailed
    }

    /**
     * Open the theme a share [code] carries, or the saved project that already holds it. The paste
     * routing and the command palette call this.
     *
     * @return Why the code could not be opened, [BootNotice.NewerVersion] or [BootNotice.InvalidLink],
     *   or null when it was.
     */
    suspend fun openShared(code: String): BootNotice? = session.openShared(code)
}

/**
 * How long a card has to come before the dialog gives up on it.
 */
private val CARD_TIMEOUT = 10.seconds

/**
 * How a copy or a share went.
 */
internal enum class ShareOutcome {
    /**
     * The link is on the clipboard.
     */
    Copied,

    /**
     * The clipboard would not take the link, so it has to be copied by hand.
     */
    CopyFailed,

    /**
     * The share sheet took the link, or the user closed it.
     */
    Shared,

    /**
     * The share sheet would not open, so the link has to be copied by hand.
     */
    ShareFailed,
}

/**
 * What to tell the user when [ShareController.openShared] could not open a code, for a toast. A
 * code from a newer builder asks for a reload, and anything else is a link that does not read.
 */
internal suspend fun sharedNoticeText(notice: BootNotice): String =
    when (notice) {
        BootNotice.NewerVersion -> getString(Res.string.banners_newer_version)
        BootNotice.InvalidLink, BootNotice.UnknownPath -> getString(Res.string.share_invalid)
    }
