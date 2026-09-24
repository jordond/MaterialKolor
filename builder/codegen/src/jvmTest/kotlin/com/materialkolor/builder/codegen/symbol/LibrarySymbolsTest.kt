package com.materialkolor.builder.codegen.symbol

import com.materialkolor.builder.codegen.GoldenHarness
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Holds the symbol table and the default table to the library as it is checked in.
 *
 * A rename in the library fails here in seconds, rather than in the compile check or, worse, in a
 * file someone downloaded.
 */
class LibrarySymbolsTest {
    private val root = GoldenHarness.repoRoot()

    @Test
    fun symbols_everyMaterialKolorSymbol_existsInTheAbiDumps() {
        val dumps = abiDumps()
        val missing = librarySymbols()
            .filter { it.packageName.startsWith("com.materialkolor") }
            .filterNot { symbol -> dumps.any { dump -> dump.declares(symbol) } }

        assertEquals(emptyList(), missing, "These symbols are not in any ABI dump")
    }

    @Test
    fun symbols_fluentSymbols_matchTheFluentModuleDump() {
        val fluent = AbiDump.read(File(root, "material-kolor-fluent/api/jvm/material-kolor-fluent.api"))
        val symbols = librarySymbols()

        val missingOwn = symbols.filter { it.packageName == "com.materialkolor.fluent" && !fluent.declares(it) }
        assertEquals(emptyList(), missingOwn, "These symbols are not in the Fluent module's dump")

        val fluentTypes = symbols.filter { it.packageName == "io.github.composefluent" && it.kind == SymbolKind.Class }
        assertTrue(fluentTypes.isNotEmpty())
        val missingTypes = fluentTypes.filterNot { it.qualifiedName in fluent.references }
        assertEquals(emptyList(), missingTypes, "The Fluent module no longer hands out these Compose Fluent types")
    }

    @Test
    fun symbols_otherLibraries_areImportedSomewhereInTheRepo() {
        val imported = importedNames()
        val missing = librarySymbols()
            .filter { it.isImportable && !it.packageName.startsWith("com.materialkolor") }
            .filterNot { it.qualifiedName in imported || it.qualifiedName in AndroidOnly }

        assertEquals(emptyList(), missing, "No source in the repo imports these, so their packages are unverified")
    }

    @Test
    fun symbols_everyEntry_namesADistinctSymbol() {
        val symbols = librarySymbols()

        assertEquals(symbols.size, symbols.map { it.qualifiedName }.toSet().size)
    }

    @Test
    fun defaultArguments_everyDefault_appearsInTheLibrarySignature() {
        val problems = DefaultArguments.all.mapNotNull { (function, defaults) ->
            val overloads = signaturesOf(function)
            val matches = overloads.any { parameters ->
                defaults.all { default -> parameters[default.parameter] == default.source }
            }

            when {
                overloads.isEmpty() -> "$function has no signature in the library source"
                !matches -> "$function has no overload with ${defaults.joinToString()}, found $overloads"
                else -> null
            }
        }

        assertEquals(emptyList(), problems)
    }

    private fun librarySymbols(): List<Symbol> =
        Symbols::class.java.declaredMethods
            .filter { method -> method.parameterCount == 0 && method.returnType == Symbol::class.java }
            .map { method -> method.invoke(Symbols) as Symbol }
            .sortedBy { it.qualifiedName }

    private fun abiDumps(): List<AbiDump> {
        val modules = root
            .listFiles { file ->
                file.isDirectory && (file.name.startsWith("material-kolor-") || file.name == "material-color-utilities")
            }.orEmpty()

        return modules
            .flatMap { module ->
                File(module, "api/jvm").listFiles { file -> file.extension == "api" }.orEmpty().toList()
            }.map(AbiDump::read)
            .also { dumps -> assertTrue(dumps.size >= 5, "Expected the library's ABI dumps under $root") }
    }

    /**
     * The parameter defaults of every overload of [function], read off its multi-line signatures.
     * A class's constructor counts as a signature, so constructor defaults are read too.
     *
     * Each overload maps parameter names to the default text, or to null when there is none.
     */
    private fun signaturesOf(function: Symbol): List<Map<String, String?>> {
        val packagePath = function.packageName.replace('.', '/')
        val header = Regex("""^(?:public )?(?:fun (?:[\w.<>?]+\.)?|class )${Regex.escape(function.simpleName)}\($""")

        return root
            .listFiles { file -> file.isDirectory && file.name.startsWith("material-kolor-") }
            .orEmpty()
            .mapNotNull { module -> File(module, "src/commonMain/kotlin/$packagePath").takeIf { it.isDirectory } }
            .flatMap { directory -> directory.listFiles { file -> file.extension == "kt" }.orEmpty().toList() }
            .flatMap { source ->
                val lines = source.readLines()
                lines.indices
                    .filter { index -> header.matches(lines[index]) }
                    .map { index ->
                        lines
                            .drop(index + 1)
                            .takeWhile { !it.startsWith(")") }
                            .mapNotNull { line -> ParameterLine.matchEntire(line) }
                            .associate { match -> match.groupValues[1] to match.groups[3]?.value }
                    }
            }
    }

    /** Every name imported by the library, its samples, the builder and the README. */
    private fun importedNames(): Set<String> {
        val roots = listOf("material-kolor-core", "material-kolor-material3", "material-kolor-unstyled")
            .plus(listOf("material-kolor-fluent", "material-kolor-palette", "samples"))
            .plus(listOf("builder/app", "builder/kit", "builder/preview"))
            .map { File(root, it) }

        val sources = roots.flatMap { dir ->
            dir
                .walkTopDown()
                .onEnter { it.name != "build" }
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        } + File(root, "README.md")

        return sources
            .flatMap { file -> file.readLines() }
            .mapNotNull { line -> ImportLine.matchEntire(line.trim())?.groupValues?.get(1) }
            .toSet()
    }

    private companion object {
        /**
         * Android only names no source here imports since the old builder left. The compile check
         * (`builder/codegen-check`, `compileAndroidMain` in CI) builds exported code that uses them.
         */
        val AndroidOnly = setOf(
            "android.os.Build",
            "androidx.compose.material3.dynamicDarkColorScheme",
            "androidx.compose.material3.dynamicLightColorScheme",
            "androidx.compose.ui.platform.LocalContext",
        )
        val ParameterLine = Regex("""^ {4}(?:(?:private )?val )?(\w+): (.+?)(?: = (.+?))?,$""")
        val ImportLine = Regex("""^import ([\w.]+)$""")
    }
}

/**
 * What one `.api` dump declares, read just far enough to look symbols up.
 *
 * @property[classes] Every class, interface and annotation, by its dotted name.
 * @property[facadeMembers] The functions and property getters of the file facades, by package.
 * @property[references] Every type the dump mentions anywhere, by its dotted name.
 */
private class AbiDump(
    val classes: Set<String>,
    val facadeMembers: Map<String, Set<String>>,
    val references: Set<String>,
) {
    fun declares(symbol: Symbol): Boolean =
        when (symbol.kind) {
            SymbolKind.Class, SymbolKind.Annotation -> {
                symbol.qualifiedName in classes
            }
            SymbolKind.Function -> {
                symbol.simpleName in facadeMembers[symbol.packageName].orEmpty()
            }
            SymbolKind.Property -> {
                val getter = "get" + symbol.simpleName.replaceFirstChar { it.uppercase() }
                getter in facadeMembers[symbol.packageName].orEmpty()
            }
        }

    companion object {
        private val ClassLine = Regex("""^public .*\bclass ([\w/$]+)""")
        private val MemberLine = Regex("""^\tpublic .*\bfun ([A-Za-z_][A-Za-z0-9_]*)""")
        private val Reference = Regex("""L([\w/$]+);""")

        fun read(file: File): AbiDump {
            val classes = mutableSetOf<String>()
            val facadeMembers = mutableMapOf<String, MutableSet<String>>()
            var facadePackage: String? = null

            file.readLines().forEach { line ->
                val classMatch = ClassLine.find(line)
                if (classMatch != null) {
                    val name = classMatch.groupValues[1].dotted()
                    classes += name
                    facadePackage = if (name.endsWith("Kt") && '$' !in classMatch.groupValues[1]) {
                        name.substringBeforeLast('.')
                    } else {
                        null
                    }
                    return@forEach
                }

                val memberPackage = facadePackage ?: return@forEach
                MemberLine.find(line)?.let { match ->
                    facadeMembers.getOrPut(memberPackage) { mutableSetOf() } += match.groupValues[1]
                }
            }

            val text = file.readText()
            val references = Reference.findAll(text).map { it.groupValues[1].dotted() }.toSet()

            return AbiDump(classes, facadeMembers, references)
        }

        private fun String.dotted(): String = replace('/', '.').replace('$', '.')
    }
}
