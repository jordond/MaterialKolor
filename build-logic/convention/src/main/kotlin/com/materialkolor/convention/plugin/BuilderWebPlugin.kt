package com.materialkolor.convention.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Turns the production wasm distribution of `:builder:web` into the site the host serves.
 *
 * Webpack already names the glue and wasm files by content. `assembleSite` moves them under
 * `/assets/`, where one `_headers` rule marks them immutable, and keeps `index.html`, `boot.js` and
 * `composeResources/` at the root. `checkBudget` holds the result against `budget.json`.
 *
 * `-Psite.env=staging` adds a noindex header and a robots file that turns every crawler away.
 */
class BuilderWebPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val rewriteIndexHtml = tasks.register<RewriteIndexHtml>("rewriteIndexHtml") {
                group = SITE_GROUP
                // b-501
                description = "Fills the asset list in index.html that boot.js boots from."
                dependsOn(DISTRIBUTION_TASK)
                distribution.set(distributionDirectory())
                index.set(layout.buildDirectory.file("site-parts/index.html"))
            }

            val writeHeaders = tasks.register<WriteHeaders>("writeHeaders") {
                group = SITE_GROUP
                description = "Writes the host's _headers file, and robots.txt on staging."
                environment.set(providers.gradleProperty("site.env").orElse("production"))
                outputDirectory.set(layout.buildDirectory.dir("site-parts/host"))
            }

            val assembleSite = tasks.register<AssembleSite>("assembleSite") {
                group = SITE_GROUP
                description = "Lays the production build out as the host serves it, in build/site."
                dependsOn(DISTRIBUTION_TASK)
                distribution.set(distributionDirectory())
                index.set(rewriteIndexHtml.flatMap { task -> task.index })
                host.set(writeHeaders.flatMap { task -> task.outputDirectory })
                site.set(layout.buildDirectory.dir("site"))
            }

            tasks.register<CheckBudget>("checkBudget") {
                group = SITE_GROUP
                description = "Fails when the brotli size of the site is over budget.json."
                site.set(assembleSite.flatMap { task -> task.site })
                script.set(layout.projectDirectory.file("scripts/check-budget.mjs"))
                budget.set(layout.projectDirectory.file("budget.json"))
                stamp.set(layout.buildDirectory.file("site-parts/budget-checked"))
            }
        }
    }

    // The Kotlin plugin registers the distribution task once the build script declares the wasm
    // target, which is after this plugin applies. Site tasks call this when they are configured,
    // by which time the task is there, and read the directory from it so the path cannot drift.
    private fun Project.distributionDirectory(): Provider<Directory> =
        layout.dir(tasks.named(DISTRIBUTION_TASK, Sync::class.java).map(Sync::getDestinationDir))
}

// b-501
/**
 * Fills the `#mk-assets` placeholder in the distribution's `index.html` with the hashed glue and
 * wasm under `/assets/` and the fonts the first frame asks for.
 *
 * `boot.js` reads the list to load the glue, so the placeholder has to come before the tag that loads
 * `boot.js`. The dev page runs on the placeholder's own value.
 */
abstract class RewriteIndexHtml : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distribution: DirectoryProperty

    @get:OutputFile
    abstract val index: RegularFileProperty

    @TaskAction
    fun rewrite() {
        val root = distribution.get().asFile
        val names = root.list().orEmpty().sorted()
        val glue = names.singleOrNull { name -> GLUE.matches(name) }
            ?: throw GradleException("Expected one builder.<hash>.js in $root, found ${names.filter(GLUE::matches)}")
        val wasm = names.filter { name -> HASHED.containsMatchIn(name) && name.endsWith(".wasm") }
        // The builder's own fonts only. Libraries ship fallback fonts the first frame never asks for.
        // The initial fonts role in budget.json names the same files.
        val fonts = root
            .resolve(RESOURCES)
            .walkTopDown()
            .filter { file -> file.isFile && file.parentFile.name == "font" }
            .filter { file -> file.parentFile.parentFile.name.startsWith(BUILDER_RESOURCES) }
            .map { file -> "/" + file.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()

        // b-501
        val html = root.resolve("index.html").readText()
        val placeholder = ASSETS_ELEMENT.findAll(html).singleOrNull()
            ?: throw GradleException("index.html needs one #mk-assets placeholder, update rewriteIndexHtml")
        val boot = html.indexOf(BOOT_TAG)
        if (boot < 0) throw GradleException("index.html no longer has $BOOT_TAG, update rewriteIndexHtml")
        if (placeholder.range.first > boot) throw GradleException("#mk-assets has to come before $BOOT_TAG")

        val assets = buildString {
            append("{\"glue\":").append(jsonString("/$ASSETS/$glue"))
            append(",\"wasm\":").append(wasm.joinToString(",", "[", "]") { name -> jsonString("/$ASSETS/$name") })
            append(",\"fonts\":").append(fonts.joinToString(",", "[", "]", transform = ::jsonString))
            append("}")
        }
        val filled = "<script type=\"application/json\" id=\"mk-assets\">$assets</script>"
        index.get().asFile.writeText(html.replaceRange(placeholder.range, filled))
    }

    private fun jsonString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

/** Writes the host files that sit beside `index.html`. */
abstract class WriteHeaders : DefaultTask() {
    @get:Input
    abstract val environment: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun write() {
        val staging = when (val name = environment.get()) {
            "production" -> false
            "staging" -> true
            else -> throw GradleException("site.env is '$name', expected production or staging")
        }
        val directory = outputDirectory.get().asFile
        directory.deleteRecursively()
        directory.mkdirs()
        directory.resolve("_headers").writeText(headers(staging))
        if (staging) directory.resolve("robots.txt").writeText("User-agent: *\nDisallow: /\n")
    }

    private fun headers(staging: Boolean): String =
        buildString {
            appendLine("/*")
            appendLine("  X-Content-Type-Options: nosniff")
            appendLine("  Referrer-Policy: strict-origin-when-cross-origin")
            appendLine("  Content-Security-Policy: $CONTENT_SECURITY_POLICY")
            if (staging) appendLine("  X-Robots-Tag: noindex")
            appendLine("/$ASSETS/*")
            appendLine("  Cache-Control: public, max-age=31536000, immutable")
            appendLine("/boot.js")
            appendLine("  Cache-Control: no-cache")
            appendLine("/index.html")
            appendLine("  Cache-Control: no-cache")
            appendLine("/$RESOURCES/*")
            appendLine("  Cache-Control: public, max-age=86400, stale-while-revalidate=604800")
        }
}

/**
 * Runs `check-budget.mjs` on the assembled site. The site, the script and the budget file are its
 * inputs and a stamp its output, so it is up to date until one of them changes.
 */
abstract class CheckBudget : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val site: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val script: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val budget: RegularFileProperty

    @get:OutputFile
    abstract val stamp: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun check() {
        execOperations.exec {
            commandLine(
                "node",
                script.get().asFile.path,
                "--site",
                site.get().asFile.path,
                "--budget",
                budget.get().asFile.path,
            )
        }
        stamp.get().asFile.writeText("Within budget.\n")
    }
}

/** Copies the distribution into the site layout, hashed files under `/assets/`. */
abstract class AssembleSite : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distribution: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val index: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val host: DirectoryProperty

    @get:OutputDirectory
    abstract val site: DirectoryProperty

    @TaskAction
    fun assemble() {
        val root = distribution.get().asFile
        val output = site.get().asFile
        output.deleteRecursively()
        output.mkdirs()

        root.walkTopDown().filter(File::isFile).forEach { file ->
            val path = file.relativeTo(root).invariantSeparatorsPath
            val target = when {
                path == "index.html" -> return@forEach
                '/' !in path && HASHED.containsMatchIn(path) -> "$ASSETS/$path"
                else -> path
            }
            file.copyTo(output.resolve(target))
        }
        index.get().asFile.copyTo(output.resolve("index.html"))
        host.get().asFile.listFiles().orEmpty().forEach { file -> file.copyTo(output.resolve(file.name)) }
    }
}

private const val SITE_GROUP = "site"
private const val DISTRIBUTION_TASK = "wasmJsBrowserDistribution"
private const val ASSETS = "assets"
private const val RESOURCES = "composeResources"
private const val BUILDER_RESOURCES = "com.materialkolor."

// b-501
private const val BOOT_TAG = "<script src=\"/boot.js\"></script>"
private val ASSETS_ELEMENT = Regex("""<script type="application/json" id="mk-assets">[^<]*</script>""")

/** A name webpack gave a content hash, matching `webpack.config.d/output.js`. */
private val HASHED = Regex("""\.[0-9a-f]{16}\.""")
private val GLUE = Regex("""builder\.[0-9a-f]{16}\.js""")

private const val CONTENT_SECURITY_POLICY =
    "default-src 'self'; script-src 'self' 'wasm-unsafe-eval' https://static.cloudflareinsights.com; " +
        "connect-src 'self' https://fonts.gstatic.com https://cloudflareinsights.com; " +
        "font-src 'self' https://fonts.gstatic.com; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'; " +
        "frame-ancestors 'none'"
