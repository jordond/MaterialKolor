package com.materialkolor.transformer

import com.materialkolor.transformer.edits.Result
import com.materialkolor.transformer.lock.POLICY_VERSION
import com.materialkolor.transformer.lock.SCHEMA_VERSION
import com.materialkolor.transformer.lock.SourceLock
import com.materialkolor.transformer.lock.sha256

private const val COMMENT = "#"
private const val HEADER_PREFIX = "path\t"
private const val MANIFEST_HEADER = "${HEADER_PREFIX}input_sha256\toutput_sha256\trules"
private const val POLICY_PREAMBLE =
    "$COMMENT Audited eligible files and exact rule counts. Upstream bumps regenerate it, review every change."
private const val POLICY_HEADER = "${HEADER_PREFIX}rules"

/**
 * Renders the reviewed, diffable TSV audit artifact for one generation.
 *
 * Comment rows record the generation identity, then one row per adapted file records its input hash, output hash
 * and sorted rule counts.
 */
internal fun manifest(
    lock: SourceLock,
    sources: Map<String, String>,
    results: Map<String, Result>,
    mode: Mode,
    parserVersion: String,
): String {
    val identity = listOf(
        "schemaVersion" to "$SCHEMA_VERSION",
        "policyVersion" to "$POLICY_VERSION",
        "upstreamRevision" to lock.upstreamRevision,
        "licenseSha256" to lock.licenseSha256,
        "parserVersion" to parserVersion,
        "mode" to mode.name.lowercase(),
    )

    val rows = results.toSortedMap().map { (path, result) ->
        val input = sources.getValue(path).toByteArray().sha256()
        val output = result.text.toByteArray().sha256()

        listOf(path, input, output, ruleCounts(result)).joinToString("\t")
    }

    return buildString {
        identity.forEach { (key, value) -> append("$COMMENT $key\t$value\n") }
        append("$MANIFEST_HEADER\n")
        rows.forEach { row -> append("$row\n") }
    }
}

/**
 * Renders the rule policy that [verifyRulePolicy] would accept for [results].
 *
 * Generation never writes it. The upstream bump commits it on its own so every count change is reviewed in the diff.
 */
internal fun rulePolicy(results: Map<String, Result>): String =
    buildString {
        append("$POLICY_PREAMBLE\n")
        append("$POLICY_HEADER\n")
        results.toSortedMap().forEach { (path, result) -> append("$path\t${ruleCounts(result)}\n") }
    }

/**
 * Renders the per-file rule tally that both the manifest and the reviewed policy lock compare.
 */
private fun ruleCounts(result: Result): String =
    result.edits
        .groupingBy { it.rule.id }
        .eachCount()
        .toSortedMap()
        .entries
        .joinToString(",") { "${it.key}=${it.value}" }

// Resource lookups need a class to resolve against.
private object RulePolicy

/**
 * The hash lock identifies text. This independent policy lock identifies eligible rules and counts.
 */
internal fun verifyRulePolicy(results: Map<String, Result>) {
    val resourceName = "/library-policy-v$POLICY_VERSION.tsv"
    val resource = requireNotNull(RulePolicy::class.java.getResourceAsStream(resourceName)) {
        "rule-policy: missing audited library policy $resourceName"
    }

    val expected = resource.bufferedReader().useLines { lines ->
        lines
            .filter { it.isNotBlank() && !it.startsWith(COMMENT) && !it.startsWith(HEADER_PREFIX) }
            .associate { line ->
                val columns = line.split('\t')
                columns[0] to columns[1]
            }
    }
    require(results.keys == expected.keys) {
        "rule-policy: eligible file inventory changed; " +
            "added=${results.keys - expected.keys}, missing=${expected.keys - results.keys}"
    }

    for ((path, result) in results) {
        val actual = ruleCounts(result)
        require(actual == expected.getValue(path)) {
            "$path:0: rule-policy: rule counts changed; expected=${expected.getValue(path)}, actual=$actual"
        }
    }
}
