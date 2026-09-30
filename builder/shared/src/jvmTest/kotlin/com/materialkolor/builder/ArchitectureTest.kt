package com.materialkolor.builder

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.io.File
import kotlin.test.Test

/**
 * The module rules the compiler cannot see, checked by reading every builder source file.
 *
 * Gradle already keeps UI libraries out of the modules that do not declare them. This catches what
 * slips through anyway, a scheme built outside the engine, browser interop outside the web shell,
 * `Dispatchers.IO`, an animation that would keep running while the tab sleeps, a public declaration
 * in shared or an app, or a JVM test waiting on the default timeout.
 */
class ArchitectureTest {
    @Test
    fun builderSources_asTheyAre_breakNoRule() {
        val sources = builderSources()

        sources.size shouldBeGreaterThan 100
        scan(sources).shouldBeEmpty()
    }

    @Test
    fun scan_schemeGenerationInKit_reportsEachCall() {
        val file = planted(
            "kit/src/commonMain",
            """
            val a = rememberDynamicScheme(seed)
            val b = DynamicScheme(seedColor = seed, isDark = false)
            DynamicMaterialTheme(seedColor = seed) {}
            val c = rememberDynamicMaterialThemeState(seed)
            val d = dynamicColorScheme(seed, false)
            val e = rememberFluentColors(seed)
            val f = com.materialkolor.ktx.DynamicScheme(seed)
            """,
        )

        scan(listOf(file)).map { violation -> violation.rule } shouldBe List(7) { ArchitectureRule.SchemeGeneration }
    }

    @Test
    fun scan_schemeGenerationInEngineOrCodegen_passes() {
        val engine = planted("engine/src/commonMain", "val s = DynamicScheme(seedColor = seed)")
        val codegen = planted("codegen/src/commonMain", "emit(\"DynamicMaterialTheme(seedColor = seed)\")")

        scan(listOf(engine, codegen)).shouldBeEmpty()
    }

    @Test
    fun scan_uiLibraryImportInShared_reportsEachImport() {
        val file = planted(
            "shared/src/commonMain",
            """
            import androidx.compose.material3.Text
            import com.composeunstyled.Button
            import io.github.composefluent.FluentTheme
            """,
        )

        scan(listOf(file)).map { violation -> violation.rule } shouldBe List(3) { ArchitectureRule.UiLibraryUse }
    }

    @Test
    fun scan_fullyQualifiedUiLibraryUseInShared_reportsEachUse() {
        val file = planted(
            "shared/src/commonMain",
            """
            private val scheme = androidx.compose.material3.MaterialTheme.colorScheme
            com.composeunstyled.Text("Hi")
            io.github.composefluent.FluentTheme {}
            """,
        )

        scan(listOf(file)).map { violation -> violation.rule } shouldBe List(3) { ArchitectureRule.UiLibraryUse }
    }

    @Test
    fun scan_uiLibraryNameInCodegen_passes() {
        val file = planted("codegen/src/commonMain", "private const val THEME = \"com.composeunstyled.theme.Theme\"")

        scan(listOf(file)).shouldBeEmpty()
    }

    @Test
    fun scan_uiLibraryImportInKitOrPreview_passes() {
        val kit = planted("kit/src/commonMain", "import androidx.compose.material3.Text")
        val preview = planted("preview/src/commonMain", "import io.github.composefluent.FluentTheme")

        scan(listOf(kit, preview)).shouldBeEmpty()
    }

    @Test
    fun scan_uiLibraryImportInATestSourceSet_passes() {
        val file = planted("engine/src/jvmTest", "import androidx.compose.material3.ColorScheme")

        scan(listOf(file)).shouldBeEmpty()
    }

    @Test
    fun scan_browserInteropOutsideWeb_reportsEachUse() {
        val file = planted(
            "shared/src/wasmJsMain",
            """
            private val now: Double = js("Date.now()")
            @JsFun("() => 1") private external fun one(): Int
            import kotlinx.browser.window
            """,
        )

        scan(listOf(file)).map { violation -> violation.rule } shouldBe List(3) { ArchitectureRule.BrowserInterop }
    }

    @Test
    fun scan_browserInteropInWeb_passes() {
        val file = planted(
            "apps/web/src/wasmJsMain",
            """
            import kotlinx.browser.window
            private val now: Double = js("Date.now()")
            """,
        )

        scan(listOf(file)).shouldBeEmpty()
    }

    @Test
    fun scan_dispatchersIoEvenInWeb_isReported() {
        val file = planted("apps/web/src/wasmJsMain", "withContext(Dispatchers.IO) { load() }")

        scan(listOf(file)).map { violation -> violation.rule } shouldBe listOf(ArchitectureRule.DispatchersIo)
    }

    @Test
    fun scan_infiniteAnimationOutsideLoopPhase_reportsEachUse() {
        val file = planted(
            "kit/src/commonMain",
            """
            val transition = rememberInfiniteTransition()
            val spec = infiniteRepeatable(tween(1000))
            """,
        )

        scan(listOf(file)).map { violation -> violation.rule } shouldBe List(2) { ArchitectureRule.InfiniteAnimation }
    }

    @Test
    fun scan_infiniteAnimationInLoopPhase_passes() {
        val file = SourceFile(
            path = LOOP_PHASE_PATH,
            text = "val transition = rememberInfiniteTransition()\nval spec = infiniteRepeatable(tween(1000))",
        )

        scan(listOf(file)).shouldBeEmpty()
    }

    @Test
    fun scan_waitUntilOnItsDefaultTimeoutInAJvmTest_reportsEachCall() {
        val file = planted(
            "kit/src/jvmTest",
            """
            waitUntil { session != null }
            waitUntil("the toast") { shown }
            """,
        )

        scan(listOf(file)).map { violation -> violation.rule } shouldBe List(2) { ArchitectureRule.WaitTimeout }
    }

    @Test
    fun scan_waitUntilWithItsOwnTimeoutOrOutsideAJvmTest_passes() {
        val test = planted(
            "shared/src/jvmTest",
            """
            waitUntil(timeoutMillis = WAIT_MILLIS) { platform.environment.splashHidden }
            waitUntil("the toast", timeoutMillis = 10_000) { shown }
            waitUntil(
                timeoutMillis = WAIT_MILLIS,
            ) { shown }
            """,
        )
        val common = planted("shared/src/commonTest", "waitUntil { shown }")

        scan(listOf(test, common)).shouldBeEmpty()
    }

    @Test
    fun scan_patternOnlyInComments_passes() {
        val file = planted(
            "shared/src/commonMain",
            """
            // Never call DynamicScheme( here, the engine owns it.
            /**
             * Not Dispatchers.IO either.
             */
            """,
        )

        scan(listOf(file)).shouldBeEmpty()
    }

    @Test
    fun scan_patternInATrailingComment_passes() {
        val file = planted(
            "shared/src/commonMain",
            """
            private val roles = resolver.resolve(document) // never DynamicScheme( here
            private val theme = Theme() // not androidx.compose.material3.MaterialTheme either
            private val home = "https://materialkolor.com" // and no Dispatchers.IO
            """,
        )

        scan(listOf(file)).shouldBeEmpty()
    }

    @Test
    fun scan_codeAfterAUrl_isStillScanned() {
        val file = planted("shared/src/commonMain", "private val s = load(\"https://a.b\", DynamicScheme(seed))")

        scan(listOf(file)).map { violation -> violation.rule } shouldBe listOf(ArchitectureRule.SchemeGeneration)
    }

    @Test
    fun scan_publicTopLevelDeclarationInSharedOrAnApp_reportsEach() {
        val shared = planted(
            "shared/src/commonMain",
            """
            class Leaked
            data class Record(val a: Int)
            fun interface Factory { fun create(): Leaked }
            @Composable fun Screen() {}
            val leakedValue = 1
            """,
        )
        val web = planted(
            "apps/web/src/wasmJsMain",
            """
            public object Stated
            typealias Alias = Int
            """,
        )

        val rules = scan(listOf(shared, web)).map { violation -> violation.rule }

        rules shouldBe List(7) { ArchitectureRule.PublicDeclaration }
    }

    @Test
    fun scan_entryPointsAndHiddenDeclarationsInSharedOrAnApp_pass() {
        val shared = planted(
            "shared/src/commonMain",
            """
            @Composable
            fun BuilderApp(platform: PlatformServices) {}
            class InMemoryStoreFactory : StoreFactory
            internal class Graph {
                val nested = 1
                fun member() = Unit
            }
            @Inject internal class Model
            private fun helper() = Unit
            internal data class Record(val a: Int)
            """,
        )
        val web = planted("apps/web/src/wasmJsMain", "fun main() {}\nprivate object Hidden")

        scan(listOf(shared, web)).shouldBeEmpty()
    }

    @Test
    fun scan_publicDeclarationInThePlatformContract_passes() {
        val file = SourceFile(path = PLATFORM_CONTRACT_PATH, text = "interface Router\nenum class StoreError")

        scan(listOf(file)).shouldBeEmpty()
    }

    @Test
    fun scan_publicDeclarationOutsideSharedAndTheAppsOrInATest_passes() {
        val kit = planted("kit/src/commonMain", "public class Skin")
        val test = planted("shared/src/jvmTest", "class AppGraphTest")

        scan(listOf(kit, test)).shouldBeEmpty()
    }

    @Test
    fun scan_violation_pointsAtItsLine() {
        val file = planted(
            "shared/src/commonMain",
            "private val a = 1\nprivate val b = dynamicColorScheme(seed, false)",
        )

        val violations = scan(listOf(file))

        violations shouldHaveSize 1
        violations.single().line shouldBe 2
    }
}

/**
 * One rule and where it holds.
 *
 * @property[pattern] What breaks the rule on a line of code.
 * @property[appliesTo] Whether the rule holds in a file.
 */
internal enum class ArchitectureRule(
    val pattern: Regex,
    val appliesTo: (SourceFile) -> Boolean,
) {
    /**
     * Only the engine builds schemes. Codegen is left out because it has no core on its classpath,
     * so every match there is text it writes into an export.
     */
    SchemeGeneration(
        pattern = Regex(
            """\b(rememberDynamicScheme|rememberDynamicMaterialThemeState|rememberFluentColors)\b""" +
                """|(?<!\w)(DynamicScheme|DynamicMaterialTheme|dynamicColorScheme)\(""",
        ),
        appliesTo = { file -> file.module != "engine" && file.module != "codegen" },
    ),

    /**
     * Only kit and preview use UI libraries, by import or by fully qualified name. Test source sets
     * are left out because they never ship, and the engine checks its role tables against what
     * Material3 builds. Codegen is left out because it has no UI library on its classpath, so every
     * match there is a package name it writes into an export.
     */
    UiLibraryUse(
        pattern = Regex(
            """(?<![\w.])(androidx\.compose\.material3|com\.composeunstyled|io\.github\.composefluent)\.""",
        ),
        appliesTo = { file -> file.module !in UI_LIBRARY_EXEMPT_MODULES && file.sourceSet.endsWith("Main") },
    ),

    /**
     * Only the web shell talks to the browser.
     */
    BrowserInterop(
        pattern = Regex("""(?<![\w.])js\(|@JsFun\b|\bkotlinx\.browser\b"""),
        appliesTo = { file -> file.module != "apps/web" },
    ),

    /**
     * Nothing blocks a thread, wasm has only the one.
     */
    DispatchersIo(
        pattern = Regex("""\bDispatchers\.IO\b"""),
        appliesTo = { true },
    ),

    /**
     * Endless animation goes through `rememberLoopPhase`, which stops while the tab is hidden.
     */
    InfiniteAnimation(
        pattern = Regex("""\b(rememberInfiniteTransition|infiniteRepeatable)\b"""),
        appliesTo = { file -> file.path != LOOP_PHASE_PATH },
    ),

    /**
     * A Compose test on the JVM names the time it waits for a condition. The one second default runs
     * out on a busy machine long before the test's clock does. A call whose arguments the formatter
     * wrapped onto lines of their own is left to review.
     */
    WaitTimeout(
        pattern = Regex("""\bwaitUntil\s*(?:\{|\((?!\s*$)(?![^)]*\btimeoutMillis\b))"""),
        appliesTo = { file -> file.sourceSet == "jvmTest" },
    ),

    /**
     * Shared and the apps keep every top level declaration `internal` or `private`, apart from the
     * entry points, the in-memory stores the apps borrow and the platform contract they implement.
     * Explicit API mode is off in all of them, so this is what holds the line. It reads
     * declarations that start at the first column, which is where ktlint leaves every top level one.
     */
    PublicDeclaration(
        pattern = Regex(
            """^(?:@[\w.:]+(?:\([^)]*\))?\s+)*(?:(?!internal\b|private\b)[a-z]+\s+)*""" +
                """(?:class|interface|object|fun|val|var|typealias)\s+""" +
                """(?!(?:BuilderApp|main|InMemoryStoreFactory)\b)""",
        ),
        appliesTo = { file ->
            file.module in INTERNAL_ONLY_MODULES &&
                file.sourceSet.endsWith("Main") &&
                file.path != PLATFORM_CONTRACT_PATH
        },
    ),
}

/**
 * A Kotlin source file, with its path from the repository root.
 */
internal data class SourceFile(
    val path: String,
    val text: String,
) {
    /**
     * The builder module the file belongs to, as its directory under `builder`, `kit` or `apps/web`
     * for example.
     */
    val module: String = path.removePrefix("builder/").substringBefore("/src/")

    /**
     * The source set the file belongs to, `commonMain` for example.
     */
    val sourceSet: String = path.substringAfter("/src/").substringBefore('/')
}

/**
 * A line that breaks [rule].
 */
internal data class Violation(
    val rule: ArchitectureRule,
    val path: String,
    val line: Int,
    val code: String,
) {
    override fun toString(): String = "$path:$line breaks $rule, $code"
}

/**
 * Every line of [sources] that breaks a rule, skipping comment lines and trailing comments.
 */
internal fun scan(sources: List<SourceFile>): List<Violation> =
    sources.flatMap { file ->
        val rules = ArchitectureRule.entries.filter { rule -> rule.appliesTo(file) }
        file.text.lines().withIndex().flatMap { (index, line) ->
            if (line.isComment()) return@flatMap emptyList()
            val code = line.withoutTrailingComment()
            rules.flatMap { rule ->
                rule.pattern
                    .findAll(code)
                    .map { Violation(rule, file.path, index + 1, line.trim()) }
                    .toList()
            }
        }
    }

/**
 * Every Kotlin file under the builder modules' `src/<set>/kotlin` roots, apart from this test.
 */
private fun builderSources(): List<SourceFile> {
    val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { dir -> dir.parentFile }
        .first { dir -> File(dir, "settings.gradle.kts").isFile && File(dir, "builder").isDirectory }
    return BUILDER_MODULES.flatMap { module ->
        val sourceSets = File(root, "builder/$module/src").listFiles().orEmpty()
        sourceSets.flatMap { sourceSet ->
            File(sourceSet, "kotlin")
                .walkTopDown()
                .filter { file -> file.isFile && file.extension == "kt" && file.name != "ArchitectureTest.kt" }
                .map { file -> SourceFile(file.relativeTo(root).invariantSeparatorsPath, file.readText()) }
                .toList()
        }
    }
}

private fun String.isComment(): Boolean {
    val code = trimStart()
    return code.startsWith("//") || code.startsWith("*") || code.startsWith("/*")
}

/**
 * The line up to its `//` comment. A `//` right after a colon stays, since that is a URL in a
 * string far more often than a comment.
 */
private fun String.withoutTrailingComment(): String = replace(TRAILING_COMMENT, "")

private fun planted(
    sourceRoot: String,
    code: String,
): SourceFile = SourceFile(path = "builder/$sourceRoot/kotlin/Planted.kt", text = code.trimIndent())

private val BUILDER_MODULES =
    listOf("domain", "codegen", "engine", "kit", "preview", "shared", "apps/web", "apps/desktop")

private val UI_LIBRARY_EXEMPT_MODULES = setOf("kit", "preview", "codegen")

private val INTERNAL_ONLY_MODULES = setOf("shared", "apps/web", "apps/desktop")

private val TRAILING_COMMENT = Regex("""(?<!:)//.*""")

private const val LOOP_PHASE_PATH =
    "builder/kit/src/commonMain/kotlin/com/materialkolor/builder/kit/motion/LoopPhase.kt"

private const val PLATFORM_CONTRACT_PATH =
    "builder/shared/src/commonMain/kotlin/com/materialkolor/builder/core/platform/PlatformServices.kt"
