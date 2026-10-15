package conformance

/**
 * Explicit raw-reference-only fixture generator, never part of normal test execution.
 * Run through `:mcu-upstream:printMcuQuantizerGoldens`, which passes the upstream lock as argument
 * one so the emitted header records the reviewed revision and the command that produced the file.
 */
object GenerateQuantizerFixtures {
    @JvmStatic
    fun main(args: Array<String>) {
        val random = java.util.Random(987654321L)
        val pixels = IntArray(96) { 0xff000000.toInt() or random.nextInt(0x1000000) }
        val entries = upstream.kotlin.quantize.QuantizerCelebi
            .quantize(pixels, 8)
            .toList()
        println("package com.materialkolor.conformance")
        println()
        val provenance = FixtureProvenance.header(
            generator = "GenerateQuantizerFixtures",
            task = "printMcuQuantizerGoldens",
            fixture = "UpstreamQuantizerGoldenData.kt",
            lockPath = args.firstOrNull(),
        )
        provenance.forEach(::println)
        println("// Expected entries come from raw upstream Kotlin with namespace relocation only.")
        println("// Input is java.util.Random(987654321).nextInt(0x1000000). Preserve entry order/populations.")
        println("internal val quantizerGoldenPixels = intArrayOf(")
        for (chunk in pixels.toList().chunked(4)) {
            println("    " + chunk.joinToString(", ") { "0x${it.toUInt().toString(16)}.toInt()" } + ",")
        }
        println(")")
        println("internal val quantizerGoldenEntries = listOf(")
        for ((argb, population) in entries) println("    0x${argb.toUInt().toString(16)}.toInt() to $population,")
        println(")")
        println("internal val quantizerGoldenScores = listOf(")
        for (argb in upstream.kotlin.score.Score.score(
            entries.toMap(),
        )) {
            println("    0x${argb.toUInt().toString(16)}.toInt(),")
        }
        println(")")
    }
}
