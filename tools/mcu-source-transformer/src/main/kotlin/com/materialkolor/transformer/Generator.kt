package com.materialkolor.transformer

import com.materialkolor.transformer.edits.Result
import com.materialkolor.transformer.lock.POLICY_VERSION
import com.materialkolor.transformer.lock.SourceLock
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.random.Random

internal fun generate(
    input: File,
    output: File,
    report: File,
    lockFile: File,
    mode: Mode,
    parserVersion: String,
) {
    require(parserVersion.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+(?:[-.][A-Za-z0-9]+)*"))) {
        "manifest: invalid parser version"
    }
    require(!Files.isSymbolicLink(output.toPath()) && (!output.exists() || output.isDirectory)) {
        "output-ownership: generated output must be a directory, not a link"
    }
    require(!Files.isSymbolicLink(report.toPath()) && (!report.exists() || report.isFile)) {
        "output-ownership: report must be a regular file, not a link"
    }

    val inputPath = input.canonicalFile.toPath()
    val outputPath = output.canonicalFile.toPath()
    val reportPath = report.canonicalFile.toPath()
    require(!outputPath.startsWith(inputPath) && !inputPath.startsWith(outputPath)) {
        "output-ownership: input/output must be disjoint"
    }
    require(!reportPath.startsWith(outputPath) && !reportPath.startsWith(inputPath)) {
        "output-ownership: report must be separate from the source trees"
    }
    require(reportPath != lockFile.canonicalFile.toPath()) {
        "output-ownership: report cannot replace the reviewed lock"
    }
    require(!lockFile.canonicalFile.toPath().startsWith(outputPath)) {
        "output-ownership: lock cannot be inside generated output"
    }

    val lock = SourceLock.read(lockFile)
    val sources = lock.validate(input)
    val results = Adapter().use { adapter ->
        sources.mapValues { (name, source) ->
            adapter.transform(
                name,
                source,
                mode == Mode.Library,
                mode,
            )
        }
    }

    if (mode == Mode.Library) {
        verifyRulePolicy(results)
    }

    val manifest = manifest(lock, sources, results, mode, parserVersion)
    replaceOutput(outputPath, reportPath, results, manifest)

    val editCount = results.values.sumOf { it.edits.size }
    println(
        "PSI adapted ${results.size} files, $editCount edits, " +
            "mode=${mode.name.lowercase()}, policy=$POLICY_VERSION, parser=$parserVersion",
    )
}

/**
 * Stages a complete validated tree, then swaps it with rollback if either owned output fails.
 */
private fun replaceOutput(
    output: Path,
    report: Path,
    results: Map<String, Result>,
    manifest: String,
) {
    Files.createDirectories(output.parent)
    Files.createDirectories(report.parent)

    val stage = Files.createTempDirectory(output.parent, ".mcu-candidate-")
    val reportStage = Files.createTempFile(report.parent, ".mcu-report-", ".tsv")

    // Backups are named beside their targets and only ever come into existence through a move, so
    // nothing has to reserve and then release the name first.
    val suffix = ".mcu-backup-%016x".format(Random.nextLong())
    val backup = output.resolveSibling("${output.fileName}$suffix")
    val reportBackup = report.resolveSibling("${report.fileName}$suffix")
    var outputBackedUp = false
    var reportBackedUp = false
    var outputInstalled = false
    var reportInstalled = false

    try {
        for ((path, result) in results) {
            val destination = stage.resolve(path)
            Files.createDirectories(destination.parent)
            Files.writeString(destination, result.text)
        }

        Files.writeString(reportStage, manifest)

        if (Files.exists(output)) {
            move(output, backup)
            outputBackedUp = true
        }

        if (Files.exists(report)) {
            move(report, reportBackup)
            reportBackedUp = true
        }

        move(stage, output)
        outputInstalled = true

        move(reportStage, report)
        reportInstalled = true
    } catch (failure: Exception) {
        try {
            if (outputInstalled) output.toFile().deleteRecursively()
            if (outputBackedUp) move(backup, output)
            if (reportBackedUp) move(reportBackup, report)
        } catch (rollback: Exception) {
            failure.addSuppressed(rollback)
            listOf(backup, reportBackup).filter { Files.exists(it) }.forEach { path ->
                System.err.println("PSI rollback failed; the previous tree is still available at $path")
            }
        }

        throw failure
    } finally {
        stage.toFile().deleteRecursively()
        Files.deleteIfExists(reportStage)

        // Leave a backup available if rollback itself failed.
        if (outputInstalled && reportInstalled) {
            backup.toFile().deleteRecursively()
            Files.deleteIfExists(reportBackup)
        }
    }
}

private fun move(
    from: Path,
    to: Path,
) {
    try {
        Files.move(from, to, StandardCopyOption.ATOMIC_MOVE)
    } catch (_: AtomicMoveNotSupportedException) {
        Files.move(from, to)
    }
}
