package com.materialkolor.builder.feature.share

import androidx.lifecycle.ViewModel
import com.materialkolor.builder.core.platform.Clipboard
import com.materialkolor.builder.core.platform.FileSaver
import com.materialkolor.builder.core.session.BootNotice
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.di.AppScope
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.share_invalid
import com.materialkolor.builder.generated.resources.share_newer_version
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import org.jetbrains.compose.resources.getString

/**
 * Share links, out and in (F-32).
 *
 * A link carries every field of the document, target included, and the project name. The package
 * name and the preview mode stay behind, since neither is part of the document. Going out, the link
 * lands on the clipboard, or in the share sheet on a touch screen that has one. Coming in,
 * [openShared] opens the theme a pasted or opened link carries.
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
) : ViewModel() {
    /** Whether a share goes to the share sheet here rather than the clipboard. */
    val sharesToSheet: Boolean
        get() = files.canShareLink

    /** The link to [document] called [projectName], or null when the document cannot be put in one. */
    fun link(
        document: ThemeDocument,
        projectName: String,
    ): String? = runCatching { ShareCodec.encode(document, projectName) }.map(::shareUrl).getOrNull()

    /** Put [url] on the clipboard. Copied only when the clipboard really took it. */
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
     * routing (B-315) calls this.
     *
     * @return Why the code could not be opened, [BootNotice.NewerVersion] or [BootNotice.InvalidLink],
     *   or null when it was.
     */
    suspend fun openShared(code: String): BootNotice? = session.openShared(code)
}

/**
 * How a copy or a share went.
 */
internal enum class ShareOutcome {
    /** The link is on the clipboard. */
    Copied,

    /** The clipboard would not take the link, so it has to be copied by hand. */
    CopyFailed,

    /** The share sheet took the link, or the user closed it. */
    Shared,

    /** The share sheet would not open, so the link has to be copied by hand. */
    ShareFailed,
}

/** Where every share link points. */
internal const val SHARE_LINK_BASE: String = "https://materialkolor.com/t/"

/** The share link for [code]. */
internal fun shareUrl(code: String): String = SHARE_LINK_BASE + code

/**
 * What to tell the user when [ShareController.openShared] could not open a code, for a toast. A
 * code from a newer builder asks for a reload, and anything else is a link that does not read.
 */
internal suspend fun sharedNoticeText(notice: BootNotice): String =
    when (notice) {
        BootNotice.NewerVersion -> getString(Res.string.share_newer_version)
        BootNotice.InvalidLink, BootNotice.UnknownPath -> getString(Res.string.share_invalid)
    }
