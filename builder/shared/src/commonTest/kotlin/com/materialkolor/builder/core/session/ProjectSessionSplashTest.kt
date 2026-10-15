package com.materialkolor.builder.core.session

import com.materialkolor.builder.core.platform.BootSplash
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.model.DEFAULT_SEED
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.persist.Appearance
import com.materialkolor.builder.domain.persist.MotionOverride
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// What the session leaves for the next boot's splash, the chrome, the seed and the appearance.
@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSessionSplashTest : SessionTestBase() {
    @Test
    fun boot_writesTheSeedAndTheStoredAppearance() =
        runTest {
            val (session, preferences) = session()
            preferences.update { prefs -> prefs.copy(appearance = Appearance.Dark) }
            runCurrent()

            booted(session)

            environment.splashes.last() shouldBe BootSplash(DEFAULT_SEED, DARK_SPLASH, DEFAULT_SEED, Appearance.Dark)
        }

    @Test
    fun edit_writesTheNewSeed() =
        runTest {
            val (session) = session()
            booted(session)

            session.edit(DocumentChange.SetSeed(OCEAN.seed, SeedSource.Picked), EditPhase.Discrete)

            environment.splashes.last() shouldBe BootSplash(OCEAN.seed, DARK_SPLASH, OCEAN.seed, Appearance.System)
        }

    @Test
    fun appearanceChange_writesTheSplashAgainWithNoEdit() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val written = environment.splashes.size

            preferences.update { prefs -> prefs.copy(appearance = Appearance.Light) }
            runCurrent()

            environment.splashes.size shouldBe written + 1
            environment.splashes.last() shouldBe BootSplash(DEFAULT_SEED, DARK_SPLASH, DEFAULT_SEED, Appearance.Light)

            preferences.update { prefs -> prefs.copy(motion = MotionOverride.Reduce) }
            runCurrent()

            environment.splashes.size shouldBe written + 1
        }
}
