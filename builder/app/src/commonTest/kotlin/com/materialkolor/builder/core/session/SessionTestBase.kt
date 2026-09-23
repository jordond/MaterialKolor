package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.data.ProjectRepository
import com.materialkolor.builder.core.platform.InMemoryStoreFactory
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.link.Route
import com.materialkolor.builder.domain.model.ThemeDocument
import com.materialkolor.builder.domain.persist.ProjectRecord
import com.materialkolor.builder.domain.persist.StorageKeys
import com.materialkolor.builder.fakes.FakeEnvironment
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

/**
 * The storage, the environment and the helpers every [ProjectSession] test shares.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class SessionTestBase {
    private var ids = 0
    internal val stores = InMemoryStoreFactory(now = { 0 })
    internal val environment = FakeEnvironment(tabId = TAB)
    internal val projects = ProjectRepository(stores, tabId = TAB, now = { 0 }, newId = { "p${++ids}" })

    internal fun TestScope.session(): Pair<ProjectSession, PreferencesRepository> {
        val preferences = PreferencesRepository(stores, backgroundScope)
        val session = ProjectSession(
            projects = projects,
            preferences = preferences,
            environment = environment,
            colorsOf = { document -> colorsOf(document) },
            sharedThemeName = { SHARED_THEME },
            scope = backgroundScope,
            now = { testScheduler.currentTime },
        )
        return session to preferences
    }

    /** Boot on a first visit and let the session start watching other tabs. */
    internal suspend fun TestScope.booted(session: ProjectSession): String {
        session.boot(Route.Home)
        runCurrent()
        return session.project.value
            .shouldBeInstanceOf<ProjectRef.Persisted>()
            .id
    }

    internal fun TestScope.settle() {
        advanceTimeBy(AUTOSAVE_DELAY_MILLIS)
        runCurrent()
    }

    /** Save [document] to the project [id] the way another tab would, as its [revision]th save. */
    internal suspend fun TestScope.saveFromAnotherTab(
        id: String,
        document: ThemeDocument,
        revision: Long = 10,
    ): ProjectRecord {
        val theirs = projects
            .load(id)
            .shouldNotBeNull()
            .copy(document = document, revision = revision, writerTab = "other")
        stores.writeFromAnotherTab(StorageKeys.project(id), ProjectRecord.Codec.encode(theirs))
        runCurrent()
        return theirs
    }

    private fun colorsOf(document: ThemeDocument): SessionColors =
        SessionColors(List(4) { document.seed }, splashLight = document.seed, splashDark = DARK_SPLASH)

    internal companion object {
        const val TAB = "this-tab"
        const val SHARED_THEME = "Shared theme"
        val OCEAN = ThemeDocument(seed = Argb(0xFF1565C0.toInt()))
        val FOREST = ThemeDocument(seed = Argb(0xFF2E7D32.toInt()))
        val MEADOW = ThemeDocument(seed = Argb(0xFF7CB342.toInt()))
        val DARK_SPLASH = Argb(0xFF101010.toInt())
    }
}
