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
 * `Dispatchers.IO`, or an animation that would keep running while the tab sleeps.
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
    fun scan_uiLibraryImportInApp_reportsEachImport() {
        val file = planted(
            "app/src/commonMain",
            """
            import androidx.compose.material3.Text
            import com.composeunstyled.Button
            import io.github.composefluent.FluentTheme
            """,
        )

        scan(listOf(file)).map { violation -> violation.rule } shouldBe List(3) { ArchitectureRule.UiLibraryImport }
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
            "app/src/wasmJsMain",
            """
            val now: Double = js("Date.now()")
            @JsFun("() => 1") external fun one(): Int
            import kotlinx.browser.window
            """,
        )

        scan(listOf(file)).map { violation -> violation.rule } shouldBe List(3) { ArchitectureRule.BrowserInterop }
    }

    @Test
    fun scan_browserInteropInWeb_passes() {
        val file = planted(
            "web/src/wasmJsMain",
            """
            import kotlinx.browser.window
            val now: Double = js("Date.now()")
            """,
        )

        scan(listOf(file)).shouldBeEmpty()
    }

    @Test
    fun scan_dispatchersIoEvenInWeb_isReported() {
        val file = planted("web/src/wasmJsMain", "withContext(Dispatchers.IO) { load() }")

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
    fun scan_patternOnlyInComments_passes() {
        val file = planted(
            "app/src/commonMain",
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
    fun scan_violation_pointsAtItsLine() {
        val file = planted("app/src/commonMain", "val a = 1\nval b = dynamicColorScheme(seed, false)")

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
     * Only kit and preview import UI libraries. Test source sets are left out because they never
     * ship, and the engine checks its role tables against what Material3 builds.
     */
    UiLibraryImport(
        pattern = Regex(
            """^\s*import\s+(androidx\.compose\.material3|com\.composeunstyled|io\.github\.composefluent)\b""",
        ),
        appliesTo = { file -> file.module != "kit" && file.module != "preview" && file.sourceSet.endsWith("Main") },
    ),

    /** Only the web shell talks to the browser. */
    BrowserInterop(
        pattern = Regex("""(?<![\w.])js\(|@JsFun\b|\bkotlinx\.browser\b"""),
        appliesTo = { file -> file.module != "web" },
    ),

    /** Nothing blocks a thread, wasm has only the one. */
    DispatchersIo(
        pattern = Regex("""\bDispatchers\.IO\b"""),
        appliesTo = { true },
    ),

    /** Endless animation goes through `rememberLoopPhase`, which stops while the tab is hidden. */
    InfiniteAnimation(
        pattern = Regex("""\b(rememberInfiniteTransition|infiniteRepeatable)\b"""),
        appliesTo = { file -> file.path != LOOP_PHASE_PATH },
    ),
}

/**
 * A Kotlin source file, with its path from the repository root.
 */
internal data class SourceFile(
    val path: String,
    val text: String,
) {
    /** The builder module the file belongs to, `kit` for example. */
    val module: String = path.removePrefix("builder/").substringBefore('/')

    /** The source set the file belongs to, `commonMain` for example. */
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
 * Every line of [sources] that breaks a rule, skipping comment lines.
 */
internal fun scan(sources: List<SourceFile>): List<Violation> =
    sources.flatMap { file ->
        val rules = ArchitectureRule.entries.filter { rule -> rule.appliesTo(file) }
        file.text.lines().withIndex().flatMap { (index, line) ->
            if (line.isComment()) return@flatMap emptyList()
            rules.flatMap { rule ->
                rule.pattern
                    .findAll(line)
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

private fun planted(
    sourceRoot: String,
    code: String,
): SourceFile = SourceFile(path = "builder/$sourceRoot/kotlin/Planted.kt", text = code.trimIndent())

private val BUILDER_MODULES = listOf("domain", "codegen", "engine", "kit", "preview", "app", "web")

private const val LOOP_PHASE_PATH =
    "builder/kit/src/commonMain/kotlin/com/materialkolor/builder/kit/motion/LoopPhase.kt"
