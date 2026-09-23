package com.materialkolor.builder.core.session

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.PreviewMode
import com.materialkolor.builder.domain.persist.ProjectRecord
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BootResolverTest {
    private val ocean = record("ocean", ThemeDocument(seed = Argb(0xFF1565C0.toInt())))
    private val forest = record("forest", ThemeDocument(seed = Argb(0xFF2E7D32.toInt())))
    private val oceanTwin = record("ocean-twin", ocean.document.copy(style = Style.TonalSpot))

    @Test
    fun resolve_linkToTheTabsOwnProject_reopensIt() {
        val plan = resolve(Route.Theme(codeOf(ocean)), tab = "ocean-twin", projects = listOf(ocean, oceanTwin))

        plan shouldBe BootPlan(BootStart.Reopen("ocean-twin"))
    }

    @Test
    fun resolve_linkWithoutATabProject_reopensTheLocalProjectThatMatches() {
        val plan = resolve(Route.Theme(codeOf(ocean)), tab = null, projects = listOf(forest, ocean))

        plan shouldBe BootPlan(BootStart.Reopen("ocean"))
    }

    @Test
    fun resolve_linkWhileTheTabHadAnotherProject_reopensTheLocalProjectThatMatches() {
        val plan = resolve(Route.Theme(codeOf(ocean)), tab = "forest", projects = listOf(forest, ocean))

        plan shouldBe BootPlan(BootStart.Reopen("ocean"))
    }

    @Test
    fun resolve_linkToARenamedProject_stillMatchesIt() {
        val code = ShareCodec.encode(ocean.document, projectName = "Deep sea")

        resolve(Route.Theme(code), projects = listOf(ocean)) shouldBe BootPlan(BootStart.Reopen("ocean"))
    }

    @Test
    fun resolve_linkNothingMatches_opensItUnsaved() {
        val code = ShareCodec.encode(ocean.document, projectName = "Ocean")

        val plan = resolve(Route.Theme(code), tab = "forest", projects = listOf(forest))

        plan shouldBe BootPlan(BootStart.Shared(code, ocean.document, projectName = "Ocean"))
    }

    @Test
    fun resolve_homeWithATabProject_reopensIt() {
        val plan = resolve(Route.Home, tab = "forest", last = "ocean", projects = listOf(ocean, forest))

        plan shouldBe BootPlan(BootStart.Reopen("forest"))
    }

    @Test
    fun resolve_homeWithoutATabProject_reopensTheLastProject() {
        val plan = resolve(Route.Home, tab = null, last = "ocean", projects = listOf(forest, ocean))

        plan shouldBe BootPlan(BootStart.Reopen("ocean"))
    }

    @Test
    fun resolve_homeWhoseProjectsWereDeleted_startsANewProject() {
        val plan = resolve(Route.Home, tab = "gone", last = "gone too", projects = listOf(forest))

        plan shouldBe BootPlan(BootStart.New)
    }

    @Test
    fun resolve_homeOnAFirstVisit_startsANewProject() {
        resolve(Route.Home) shouldBe BootPlan(BootStart.New)
    }

    @Test
    fun resolve_legacyQuery_opensLikeALinkWithDarkPreviewAndPackageName() {
        val query = "color_seed=FF1565C0&dark_mode=true&package_name=com.example.ocean"

        val plan = resolve(Route.Legacy(query), projects = listOf(forest))

        val document = ThemeDocument(seed = Argb(0xFF1565C0.toInt()))
        plan shouldBe BootPlan(
            start = BootStart.Shared(ShareCodec.encode(document), document, projectName = null),
            previewMode = PreviewMode.Dark,
            packageName = "com.example.ocean",
        )
    }

    @Test
    fun resolve_legacyQueryForASavedTheme_reopensItAndKeepsTheLightPreview() {
        val query = "color_seed=FF1565C0&dark_mode=false"

        resolve(Route.Legacy(query), projects = listOf(ocean)) shouldBe BootPlan(BootStart.Reopen("ocean"))
    }

    @Test
    fun resolve_badLink_opensHomeWithInvalidLink() {
        val plan = resolve(Route.Theme("not-a-code"), tab = null, last = "forest", projects = listOf(forest))

        plan shouldBe BootPlan(BootStart.Reopen("forest"), notice = BootNotice.InvalidLink)
    }

    @Test
    fun resolve_linkFromANewerBuilder_opensHomeWithNewerVersion() {
        // "Ag" is the single byte 2, a format version this builder does not know.
        val plan = resolve(Route.Theme("Ag"), last = "forest", projects = listOf(forest))

        plan shouldBe BootPlan(BootStart.Reopen("forest"), notice = BootNotice.NewerVersion)
    }

    @Test
    fun resolve_unknownPath_opensHomeWithUnknownPath() {
        val plan = resolve(Route.Unknown("/settings"), tab = "forest", projects = listOf(forest))

        plan shouldBe BootPlan(BootStart.Reopen("forest"), notice = BootNotice.UnknownPath)
    }

    @Test
    fun readsRecords_eachRoute_onlyForLinks() {
        BootResolver.readsRecords(Route.Home) shouldBe false
        BootResolver.readsRecords(Route.Unknown("/settings")) shouldBe false
        BootResolver.readsRecords(Route.Theme(codeOf(ocean))) shouldBe true
        BootResolver.readsRecords(Route.Legacy("color_seed=FF1565C0")) shouldBe true
    }

    @Test
    fun resolve_linkAgainstTheIndexOnly_opensItUnsaved() {
        val code = codeOf(ocean)

        val plan = BootResolver.resolve(Route.Theme(code), null, null, SavedProjects.Listed(listOf("ocean")))

        plan shouldBe BootPlan(BootStart.Shared(code, ocean.document, projectName = "ocean"))
    }

    /** Resolve the way boot does, with records for a link and the index alone otherwise. */
    private fun resolve(
        route: Route,
        tab: String? = null,
        last: String? = null,
        projects: List<ProjectRecord> = emptyList(),
    ): BootPlan {
        val saved = if (BootResolver.readsRecords(route)) {
            SavedProjects.Read(projects)
        } else {
            SavedProjects.Listed(projects.map { record -> record.id })
        }
        return BootResolver.resolve(route, tab, last, saved)
    }

    private fun codeOf(record: ProjectRecord): String = ShareCodec.encode(record.document, record.name)

    private fun record(
        id: String,
        document: ThemeDocument,
    ): ProjectRecord = ProjectRecord(id, name = id, document = document, revision = 1, writerTab = "tab")
}
