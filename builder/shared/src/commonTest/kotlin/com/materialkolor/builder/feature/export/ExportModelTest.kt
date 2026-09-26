package com.materialkolor.builder.feature.export

import com.materialkolor.builder.ViewModelHarness
import com.materialkolor.builder.codegen.ExportVersions
import com.materialkolor.builder.codegen.generate
import com.materialkolor.builder.core.data.PreferencesRepository
import com.materialkolor.builder.core.session.ProjectSession
import com.materialkolor.builder.core.session.SessionTestBase
import com.materialkolor.builder.domain.capability.forTarget
import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.edit.DocumentChange
import com.materialkolor.builder.domain.edit.EditPhase
import com.materialkolor.builder.domain.link.DecodeResult
import com.materialkolor.builder.domain.link.SHARE_URL_PREFIX
import com.materialkolor.builder.domain.link.ShareCodec
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.domain.model.SeedSource
import com.materialkolor.builder.domain.model.Style
import com.materialkolor.builder.domain.persist.ExportMode
import com.materialkolor.builder.domain.persist.ExportPrefs
import com.materialkolor.builder.domain.persist.ExportTarget
import com.materialkolor.builder.domain.validate.MAX_ACCENTS
import com.materialkolor.builder.engine.resolve.ThemeResolver
import com.materialkolor.builder.fakes.FakeClipboard
import com.materialkolor.builder.fakes.FakeEnvironment
import com.materialkolor.builder.fakes.FakeFileSaver
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val PACKAGE = "com.acme.ui"
private const val PROJECT = "Ocean study"
private const val RENAMED = "Harbor study"
private const val STAGING = "https://staging.materialkolor.com"
private val VERSIONS =
    ExportVersions(
        builder = "2.0.0",
        materialKolor = "6.0.0",
        fluent = "v0.1.0",
        composeUnstyled = "1.0.0",
        composeMaterial3 = "1.12.0-alpha03",
        androidxMaterial3 = "1.5.0-alpha28",
    )

@OptIn(ExperimentalCoroutinesApi::class)
class ExportModelTest : SessionTestBase() {
    private val harness = ViewModelHarness()
    private var generated = 0
    private val counting = ExportGenerator { input ->
        generated++
        generate(input)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun packageName_afterASeedChangeAndAReload_isKept() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences)

            model.handle(ExportAction.SetPackageName(PACKAGE))
            runCurrent()
            session.edit(DocumentChange.SetSeed(Argb(0xFF6A1B9A.toInt()), SeedSource.Picked), EditPhase.Discrete)
            runCurrent()
            val reloaded = exportModel(session, PreferencesRepository(stores, backgroundScope))
            runCurrent()

            reloaded.state.value.prefs.packageName shouldBe PACKAGE
            harness.clearAndJoin()
        }

    @Test
    fun options_perTarget_eachTargetKeepsItsOwn() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences)

            model.handle(ExportAction.SetPackageName(PACKAGE))
            runCurrent()
            session.edit(DocumentChange.SetLibrary(Library.Fluent, expressive = false), EditPhase.Discrete)
            runCurrent()
            model.state.value.prefs.packageName shouldBe ExportPrefs.DEFAULT_PACKAGE_NAME
            model.handle(ExportAction.SetMode(ExportMode.Frozen))
            runCurrent()
            session.edit(DocumentChange.SetLibrary(Library.Material3, expressive = false), EditPhase.Discrete)
            runCurrent()

            model.state.value.prefs.packageName shouldBe PACKAGE
            model.state.value.prefs.mode shouldBe ExportMode.Dynamic
            harness.clearAndJoin()
        }

    @Test
    fun copyAll_equalsTheZipEntriesJoined() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences)

            val ready = model.outcome().shouldBeInstanceOf<ExportOutcome.Ready>()
            val folder = "${session.document.value.themeName}/"
            val joined = storedEntries(ready.zip.bytes).joinToString(separator = "\n") { (path, bytes) ->
                "// ${path.removePrefix(folder)}\n${bytes.decodeToString()}"
            }

            joined shouldBe ready.allText
            ready.zip.name shouldBe "AppTheme.zip"
            harness.clearAndJoin()
        }

    @Test
    fun outcome_sameDocumentOptionsAndVersions_generatesOnce() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences)

            val first = model.outcome()
            val second = model.outcome()
            generated shouldBe 1
            (first === second) shouldBe true
            model.handle(ExportAction.SetMode(ExportMode.Frozen))
            runCurrent()
            model.outcome()
            model.outcome()

            generated shouldBe 2
            harness.clearAndJoin()
        }

    @Test
    fun outcome_invalidPackage_isBlockedWithoutGenerating() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences)

            model.handle(ExportAction.SetPackageName("Com.Example"))
            runCurrent()

            val blocked = model.outcome().shouldBeInstanceOf<ExportOutcome.Blocked>()
            blocked.problems shouldBe listOf(ExportProblem.PackageName("Com.Example"))
            generated shouldBe 0
            harness.clearAndJoin()
        }

    @Test
    fun outcome_themeNameTheTargetAlreadyUses_isBlockedWithoutGenerating() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences)

            session.edit(DocumentChange.SetThemeName("MaterialTheme"), EditPhase.Discrete)
            runCurrent()

            val blocked = model.outcome().shouldBeInstanceOf<ExportOutcome.Blocked>()
            blocked.problems shouldBe listOf(ExportProblem.NameTaken("MaterialTheme"))
            generated shouldBe 0
            harness.clearAndJoin()
        }

    @Test
    fun shareLink_inEveryFile_isTheCodeShareGivesWithTheProjectName() =
        runTest {
            val (session, preferences) = session()
            val id = booted(session)
            session.rename(id, PROJECT) shouldBe null
            val model = exportModel(session, preferences)
            model.handle(ExportAction.SetPackageName(PACKAGE))
            runCurrent()

            val ready = model.outcome().shouldBeInstanceOf<ExportOutcome.Ready>()
            val code = linkCodeIn(ready.allText)

            code shouldBe ShareCodec.encode(session.document.value, PROJECT)
            val decoded = ShareCodec.decode(code)
            decoded shouldBe DecodeResult.Ok(session.document.value, PROJECT)
            decoded.toString() shouldNotContain PACKAGE
            harness.clearAndJoin()
        }

    @Test
    fun shareLink_onStaging_opensOnStagingInEveryFile() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences, FakeEnvironment(siteOrigin = STAGING))

            val ready = model.outcome().shouldBeInstanceOf<ExportOutcome.Ready>()

            ready.allText shouldContain "$STAGING/t/"
            ready.allText shouldNotContain SHARE_URL_PREFIX
            harness.clearAndJoin()
        }

    @Test
    fun shareLink_afterARename_regeneratesOnceUnderTheNewName() =
        runTest {
            val (session, preferences) = session()
            val id = booted(session)
            session.rename(id, PROJECT) shouldBe null
            val model = exportModel(session, preferences)

            model.outcome()
            session.rename(id, RENAMED) shouldBe null
            runCurrent()
            val ready = model.outcome().shouldBeInstanceOf<ExportOutcome.Ready>()
            model.outcome()

            generated shouldBe 2
            val decoded = ShareCodec.decode(linkCodeIn(ready.allText)).shouldBeInstanceOf<DecodeResult.Ok>()
            decoded.projectName shouldBe RENAMED
            harness.clearAndJoin()
        }

    @Test
    fun shareLink_extraColorsTooManyForACode_linksToWhatTheTargetSeesUnderTheProjectName() =
        runTest {
            val (session, preferences) = session()
            val id = booted(session)
            session.rename(id, PROJECT) shouldBe null
            val model = exportModel(session, preferences)
            repeat(MAX_ACCENTS + 1) { index ->
                val accent = Accent(name = "Brand$index", seed = Argb(0xFF6A1B9A.toInt()))
                session.edit(DocumentChange.AddAccent(accent), EditPhase.Discrete)
            }
            session.edit(DocumentChange.SetLibrary(Library.Fluent, expressive = false), EditPhase.Discrete)
            runCurrent()

            val ready = model.outcome().shouldBeInstanceOf<ExportOutcome.Ready>()
            val code = linkCodeIn(ready.allText)

            val document = session.document.value
            runCatching { ShareCodec.encode(document, PROJECT) }.isFailure shouldBe true
            val targeted = document.forTarget(ExportTarget.Fluent)
            code shouldBe ShareCodec.encode(targeted, PROJECT)
            val decoded = ShareCodec.decode(code).shouldBeInstanceOf<DecodeResult.Ok>()
            decoded.projectName shouldBe PROJECT
            decoded.document.accents shouldBe emptyList()
            harness.clearAndJoin()
        }

    @Test
    fun expressiveOn2021_expressiveWithRainbow_warnsAndOnlyThere() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences)

            session.edit(DocumentChange.SetStyle(Style.Rainbow), EditPhase.Discrete)
            runCurrent()
            model.state.value.expressiveOn2021 shouldBe false
            session.edit(DocumentChange.SetLibrary(Library.Material3, expressive = true), EditPhase.Discrete)
            runCurrent()
            model.state.value.expressiveOn2021 shouldBe true
            session.edit(DocumentChange.SetStyle(Style.Vibrant), EditPhase.Discrete)
            runCurrent()

            model.state.value.expressiveOn2021 shouldBe false
            harness.clearAndJoin()
        }

    @Test
    fun exported_theFirstTime_marksTheFirstExportDone() =
        runTest {
            val (session, preferences) = session()
            booted(session)
            val model = exportModel(session, preferences)

            model.handle(ExportAction.Exported)
            runCurrent()

            preferences.current().firstExportDone shouldBe true
            harness.clearAndJoin()
        }

    private fun exportModel(
        session: ProjectSession,
        preferences: PreferencesRepository,
        environment: FakeEnvironment = FakeEnvironment(),
    ): ExportModel =
        harness.own(
            ExportModel(
                session = session,
                preferences = preferences,
                resolver = ThemeResolver(),
                versions = VERSIONS,
                generator = counting,
                clipboard = FakeClipboard(),
                files = FakeFileSaver(),
                environment = environment,
            ),
        )
}

/**
 * The share code every link back in [text] carries, which has to be one and the same.
 */
private fun linkCodeIn(text: String): String {
    val link = Regex(Regex.escape(SHARE_URL_PREFIX) + "([A-Za-z0-9_-]+)")
    val codes = link.findAll(text).map { match -> match.groupValues[1] }.toSet()
    codes shouldHaveSize 1
    return codes.single()
}

/**
 * The path and bytes of every entry in a zip written with STORE, read from the local headers.
 */
private fun storedEntries(zip: ByteArray): List<Pair<String, ByteArray>> =
    buildList {
        var at = 0
        while (zip.int(at) == LOCAL_HEADER) {
            val size = zip.int(at + 18)
            val nameLength = zip.short(at + 26)
            val extraLength = zip.short(at + 28)
            val nameStart = at + 30
            val dataStart = nameStart + nameLength + extraLength
            val path = zip.copyOfRange(nameStart, nameStart + nameLength).decodeToString()
            add(path to zip.copyOfRange(dataStart, dataStart + size))
            at = dataStart + size
        }
    }

private const val LOCAL_HEADER = 0x04034b50

private fun ByteArray.short(at: Int): Int = (this[at].toInt() and 0xFF) or ((this[at + 1].toInt() and 0xFF) shl 8)

private fun ByteArray.int(at: Int): Int = short(at) or (short(at + 2) shl 16)
