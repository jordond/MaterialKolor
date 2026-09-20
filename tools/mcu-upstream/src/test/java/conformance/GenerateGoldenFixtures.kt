package conformance

/**
 * Explicit maintenance entry point, never invoked by normal builds/tests.
 * Run through `:mcu-upstream:printMcuRoleGoldens`, which puts the namespace-only Kotlin reference
 * and test classes on the JVM classpath and passes the upstream lock as argument one. Redirect
 * stdout to UpstreamRoleGoldenData.kt only after reviewing the upstream pin. The emitted header
 * records that command. Expected colors are read only from raw Kotlin, never the adapted library.
 */
object GenerateGoldenFixtures {
    @JvmStatic
    fun main(args: Array<String>) {
        require(COVERAGE.map(Coverage::variant) == VARIANT_NAMES) {
            "COVERAGE must name every variant in VARIANT_NAMES, in order. Add the new variant with an " +
                "explicit specification and mode; do not renumber the existing entries."
        }
        val raw = ReferenceMcu("upstream.kotlin.")
        println("package com.materialkolor.conformance")
        println()
        val provenance = FixtureProvenance.header(
            generator = "GenerateGoldenFixtures",
            task = "printMcuRoleGoldens",
            fixture = "UpstreamRoleGoldenData.kt",
            lockPath = args.firstOrNull(),
        )
        provenance.forEach(::println)
        println("// Expected colors come from raw upstream Kotlin with namespace relocation only.")
        println("// Review every changed ARGB before adopting a regenerated fixture.")
        println("internal val upstreamRoleGoldens = \"\"\"")
        for ((variant, spec, darkOnPhone) in COVERAGE) {
            emit(raw, SchemeCase(variant, spec, darkOnPhone, 0.0, "PHONE", listOf(0xff6750a4.toInt())))
            emit(
                raw,
                SchemeCase(variant, spec, !darkOnPhone, 1.0, "WATCH", listOf(0xffff0000.toInt(), 0xff3498db.toInt())),
            )
        }
        for (seed in listOf(0xff000000.toInt(), 0xff808080.toInt(), 0xffffffff.toInt())) {
            emit(raw, SchemeCase("Cmf", "SPEC_2026", true, -1.0, "PHONE", listOf(seed)))
        }
        println("\"\"\".trimIndent()")
    }

    private data class Coverage(
        val variant: String,
        val spec: String,
        val darkOnPhone: Boolean,
    )

    // Each variant contributes a PHONE row at contrast 0.0 and a WATCH row at contrast 1.0 in the
    // opposite mode, so every entry here covers both platforms and both modes. The specification and
    // the mode are written out per variant rather than derived from list position. Deriving them
    // meant inserting or reordering a variant silently re-specified every later fixture row, and the
    // resulting diff looked like a color change.
    private val COVERAGE = listOf(
        Coverage("TonalSpot", "SPEC_2021", darkOnPhone = true),
        Coverage("Neutral", "SPEC_2025", darkOnPhone = false),
        Coverage("Vibrant", "SPEC_2026", darkOnPhone = true),
        Coverage("Expressive", "SPEC_2021", darkOnPhone = false),
        Coverage("Fidelity", "SPEC_2025", darkOnPhone = true),
        Coverage("Content", "SPEC_2026", darkOnPhone = false),
        Coverage("Rainbow", "SPEC_2021", darkOnPhone = true),
        Coverage("FruitSalad", "SPEC_2025", darkOnPhone = false),
        Coverage("Monochrome", "SPEC_2026", darkOnPhone = true),
        Coverage("Cmf", "SPEC_2026", darkOnPhone = false),
    )

    private fun emit(
        raw: ReferenceMcu,
        case: SchemeCase,
    ) {
        println(
            listOf(
                case.variant,
                case.spec,
                case.dark,
                case.contrast,
                case.platform,
                case.seeds.joinToString(",") {
                    it.toUInt().toString(16)
                },
            ).joinToString("|"),
        )
        val roles = raw.roles(raw.scheme(case)).entries.map { (name, argb) ->
            "$name=${argb?.toUInt()?.toString(16) ?: "null"}"
        }
        roles.chunked(3).forEach { println(it.joinToString(" ")) }
        println()
    }
}
