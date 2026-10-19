package com.materialkolor.transformer

import java.io.File

fun main(args: Array<String>) {
    if (args.firstOrNull() == "policy") {
        require(args.size == 4) { "Usage: policy <inputDir> <lockFile> <candidateFile>" }
        candidateRulePolicy(File(args[1]), File(args[2]), File(args[3]))
        return
    }

    require(args.size == 6) {
        "Usage: <inputDir> <outputDir> <reportFile> <lockFile> <library|reference> <parserVersion>"
    }
    val mode = when (args[4]) {
        "library" -> Mode.Library
        "reference" -> Mode.Reference
        else -> error("Unsupported transformation mode: ${args[4]}")
    }

    generate(File(args[0]), File(args[1]), File(args[2]), File(args[3]), mode, args[5])
}
