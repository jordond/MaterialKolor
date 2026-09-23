package com.materialkolor.builder.codegen

/**
 * The CRC-32 of every golden case, as `GoldenDigest` works it out.
 *
 * The JVM golden harness writes this file whenever it updates the goldens, so rather than editing
 * it by hand, run the codegen jvmTest task with -Pgolden.update=true.
 */
internal object GoldenHashes {
    val cases: Map<String, Long> = mapOf(
        "custom-frozen-accents-pins-amoled" to 0x73FABDFEL,
        "custom-frozen-all-contrasts" to 0x95DEE013L,
        "custom-frozen-default" to 0x1467FC7CL,
        "dsl-forms" to 0x373CA3C3L,
        "expressive-dynamic-default" to 0x6130410BL,
        "expressive-dynamic-pins" to 0x8FFC05C6L,
        "expressive-dynamic-tonal-spot-2021" to 0xD5F905D9L,
        "expressive-frozen-all-contrasts" to 0xD8FCCDCFL,
        "expressive-frozen-default" to 0x50315885L,
        "fluent-frozen-all-contrasts" to 0x8D407996L,
        "fluent-frozen-default" to 0x8D407996L,
        "fluent-frozen-vibrant-primary-override" to 0x24ECE5CBL,
        "header-default" to 0xB68DC275L,
        "material3-dynamic-all-overrides" to 0x76F2058EL,
        "material3-dynamic-amoled" to 0x4B42A45FL,
        "material3-dynamic-android-only" to 0x8FE3C2BCL,
        "material3-dynamic-animated" to 0x14FC2328L,
        "material3-dynamic-cmf" to 0x1586783BL,
        "material3-dynamic-default" to 0xC106C8E3L,
        "material3-dynamic-high-contrast" to 0x4B8AF1FFL,
        "material3-dynamic-pins" to 0xFC40E256L,
        "material3-dynamic-primary-override" to 0x6AAEEF2DL,
        "material3-dynamic-reduced-contrast" to 0x3A01E9A9L,
        "material3-dynamic-three-accents" to 0xAC3EEDF8L,
        "material3-dynamic-watch-2025" to 0x92B5CCF4L,
        "material3-frozen-accents-pins-amoled" to 0xE5D431E8L,
        "material3-frozen-all-contrasts" to 0xE0953497L,
        "material3-frozen-default" to 0xF7BE6FF7L,
        "unstyled-frozen-accents-pins-amoled" to 0xE372A610L,
        "unstyled-frozen-all-contrasts" to 0x841D55F4L,
        "unstyled-frozen-default" to 0x22B4252DL,
    )
}
