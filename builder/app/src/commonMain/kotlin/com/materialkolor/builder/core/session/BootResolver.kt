package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.link.DecodeResult
import com.materialkolor.builder.domain.link.LegacyPreviewMode
import com.materialkolor.builder.domain.link.LegacyQuery
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.ProjectRecord

/**
 * Picks the project the builder opens on, from the address and what this browser has saved, before
 * the first frame.
 *
 * | Address | Result |
 * |---|---|
 * | `/t/C` | the tab's own project when its document encodes to `C`, else any saved project that does, else `C` unsaved |
 * | `/` | the tab's own project, else the last open one, else a new project from the defaults |
 * | a legacy query | as `/t/C`, plus the preview mode and package name the old link carried |
 * | `/t/` with a code that does not read | as `/`, with [BootNotice.InvalidLink] or [BootNotice.NewerVersion] |
 * | any other path | as `/`, with [BootNotice.UnknownPath] |
 *
 * Matching a link encodes every saved project, which is fine at the few dozen projects a browser
 * holds. Documents are compared by their code without the project name, so a project renamed after
 * it was shared still matches its link.
 */
internal object BootResolver {
    /**
     * Where to start for [route].
     *
     * @param[tabProjectId] The project this tab had open before a reload, or null in a new tab.
     * @param[lastProjectId] The project open last in any tab, from the preferences.
     * @param[projects] Every project the drawer lists that could be read, in drawer order.
     */
    fun resolve(
        route: Route,
        tabProjectId: String?,
        lastProjectId: String?,
        projects: List<ProjectRecord>,
    ): BootPlan =
        when (route) {
            Route.Home -> {
                BootPlan(home(tabProjectId, lastProjectId, projects))
            }
            is Route.Unknown -> {
                BootPlan(home(tabProjectId, lastProjectId, projects), notice = BootNotice.UnknownPath)
            }
            is Route.Theme -> {
                when (val result = ShareCodec.decode(route.code)) {
                    is DecodeResult.Ok -> {
                        BootPlan(shared(route.code, result.document, result.projectName, tabProjectId, projects))
                    }
                    DecodeResult.UnknownVersion -> {
                        BootPlan(home(tabProjectId, lastProjectId, projects), notice = BootNotice.NewerVersion)
                    }
                    DecodeResult.Corrupt -> {
                        BootPlan(home(tabProjectId, lastProjectId, projects), notice = BootNotice.InvalidLink)
                    }
                }
            }
            is Route.Legacy -> {
                val legacy = LegacyQuery.parse(route.query)
                val code = ShareCodec.encode(legacy.document)
                BootPlan(
                    start = shared(code, legacy.document, projectName = null, tabProjectId, projects),
                    previewMode = if (legacy.previewMode == LegacyPreviewMode.Dark) PreviewMode.Dark else null,
                    packageName = legacy.packageName,
                )
            }
        }

    /**
     * The saved project whose document encodes to the same code as [document], trying
     * [preferredId] first, or null when none does.
     */
    fun matching(
        document: ThemeDocument,
        projects: List<ProjectRecord>,
        preferredId: String?,
    ): ProjectRecord? {
        val code = codeOf(document) ?: return null
        val preferred = projects.filter { record -> record.id == preferredId }
        return (preferred + projects).firstOrNull { record -> codeOf(record.document) == code }
    }

    private fun home(
        tabProjectId: String?,
        lastProjectId: String?,
        projects: List<ProjectRecord>,
    ): BootStart {
        val listed = projects.mapTo(mutableSetOf()) { record -> record.id }
        val id = listOfNotNull(tabProjectId, lastProjectId).firstOrNull { candidate -> candidate in listed }
        return if (id != null) BootStart.Reopen(id) else BootStart.New
    }

    private fun shared(
        code: String,
        document: ThemeDocument,
        projectName: String?,
        tabProjectId: String?,
        projects: List<ProjectRecord>,
    ): BootStart {
        val local = matching(document, projects, preferredId = tabProjectId)
        return if (local != null) BootStart.Reopen(local.id) else BootStart.Shared(code, document, projectName)
    }

    /** The share code of [document] without a project name, or null when it has too much to share. */
    private fun codeOf(document: ThemeDocument): String? =
        try {
            ShareCodec.encode(document)
        } catch (_: IllegalArgumentException) {
            null
        }
}

/**
 * What boot settled on.
 *
 * @property[start] The project to open.
 * @property[notice] Why the address did not open what it asked for, or null when it did.
 * @property[previewMode] The preview mode an old link asked for, or null to keep the project's own.
 * @property[packageName] The package an old link exported to, for the Material 3 export options.
 */
internal data class BootPlan(
    val start: BootStart,
    val notice: BootNotice? = null,
    val previewMode: PreviewMode? = null,
    val packageName: String? = null,
)

/**
 * The project boot opens.
 */
internal sealed interface BootStart {
    /** The saved project [id]. */
    data class Reopen(
        val id: String,
    ) : BootStart

    /**
     * A theme from a link, shown unsaved until the first edit.
     *
     * @property[code] The share code it came from.
     * @property[document] The theme the code carries.
     * @property[projectName] The name it was shared under, or null when the link has none.
     */
    data class Shared(
        val code: String,
        val document: ThemeDocument,
        val projectName: String?,
    ) : BootStart

    /** A new project from the defaults, named after the default seed. */
    data object New : BootStart
}

/**
 * Why the builder opened something other than what the address asked for.
 */
internal enum class BootNotice {
    /** The link could not be read. The UI offers "Open my last theme" and "Start from defaults". */
    InvalidLink,

    /** The link was written by a newer builder. */
    NewerVersion,

    /** The builder has no page at that path. */
    UnknownPath,
}
