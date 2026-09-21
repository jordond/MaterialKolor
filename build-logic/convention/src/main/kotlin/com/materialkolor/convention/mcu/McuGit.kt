package com.materialkolor.convention.mcu

import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

internal class McuGit(private val execOperations: ExecOperations) {
    fun run(directory: File, vararg arguments: String): String {
        val output = ByteArrayOutputStream()
        val command = listOf("git", "-C", directory.absolutePath) + arguments
        val result = execOperations.exec {
            commandLine(command)
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }

        val text = output.toString(Charsets.UTF_8)
        if (result.exitValue != 0) {
            throw GradleException(
                "MCU Git command failed in $directory: git ${arguments.joinToString(" ")}\n$text",
            )
        }

        return text.trim()
    }
}
