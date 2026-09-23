package com.materialkolor.builder.codegen

/**
 * The CRC-32 of every golden case, as `GoldenDigest` works it out.
 *
 * The JVM golden harness writes this file whenever it updates the goldens, so rather than editing
 * it by hand, run the codegen jvmTest task with -Pgolden.update=true.
 */
internal object GoldenHashes {
    val cases: Map<String, Long> = mapOf(
        "header-default" to 0xB68DC275L,
    )
}
