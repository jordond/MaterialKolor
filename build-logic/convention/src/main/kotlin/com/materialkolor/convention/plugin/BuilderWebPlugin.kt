package com.materialkolor.convention.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Turns the production wasm distribution of `:builder:web` into the site the host serves.
 *
 * Webpack already names the glue and wasm files by content. `assembleSite` moves them under
 * `/assets/`, where one `_headers` rule marks them immutable, and keeps `index.html` and
 * `composeResources/` at the root. `checkBudget` holds the result against `budget.json`.
 *
 * `-Psite.env=staging` adds a noindex header and a robots file that turns every crawler away.
 */
class BuilderWebPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val distribution = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")

            val rewriteIndexHtml = tasks.register<RewriteIndexHtml>("rewriteIndexHtml") {
                group = SITE_GROUP
                description = "Points index.html at the hashed glue and lists the assets it boots with."
                dependsOn(DISTRIBUTION_TASK)
                this.distribution.set(distribution)
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
                this.distribution.set(distribution)
                index.set(rewriteIndexHtml.flatMap { task -> task.index })
                host.set(writeHeaders.flatMap { task -> task.outputDirectory })
                site.set(layout.buildDirectory.dir("site"))
            }

            tasks.register<Exec>("checkBudget") {
                group = SITE_GROUP
                description = "Fails when the brotli size of the site is over budget.json."
                dependsOn(assembleSite)
                workingDir = projectDir
                val site = layout.buildDirectory.dir("site").get().asFile.path
                commandLine("node", "scripts/check-budget.mjs", "--site", site)
            }
        }
    }
}

/** Rewrites the distribution's `index.html` for the site layout. */
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
        val fonts = root
            .resolve(RESOURCES)
            .walkTopDown()
            .filter { file -> file.isFile && file.parentFile.name == "font" }
            .filter { file -> file.parentFile.parentFile.name.startsWith(BUILDER_RESOURCES) }
            .map { file -> "/" + file.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()

        val html = root.resolve("index.html").readText()
        if (GLUE_TAG !in html) throw GradleException("index.html no longer loads $GLUE_TAG, update rewriteIndexHtml")
        if ("</head>" !in html) throw GradleException("index.html has no </head> to put the asset list before")

        val assets = buildString {
            append("{\"glue\":").append(jsonString("/$ASSETS/$glue"))
            append(",\"wasm\":").append(wasm.joinToString(",", "[", "]") { name -> jsonString("/$ASSETS/$name") })
            append(",\"fonts\":").append(fonts.joinToString(",", "[", "]", transform = ::jsonString))
            append("}")
        }
        val rewritten = html
            .replace(GLUE_TAG, "src=\"/$ASSETS/$glue\"")
            .replace("</head>", "  <script type=\"application/json\" id=\"mk-assets\">$assets</script>\n  </head>")
        index.get().asFile.writeText(rewritten)
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
private const val GLUE_TAG = "src=\"builder.js\""

/** A name webpack gave a content hash, matching `webpack.config.d/output.js`. */
private val HASHED = Regex("""\.[0-9a-f]{16}\.""")
private val GLUE = Regex("""builder\.[0-9a-f]{16}\.js""")

private const val CONTENT_SECURITY_POLICY =
    "default-src 'self'; script-src 'self' 'wasm-unsafe-eval' https://static.cloudflareinsights.com; " +
        "connect-src 'self' https://fonts.gstatic.com https://cloudflareinsights.com; " +
        "font-src 'self' https://fonts.gstatic.com; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'; " +
        "frame-ancestors 'none'"
