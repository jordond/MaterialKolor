package com.materialkolor.builder.core.versions

/**
 * The version of a library an export should name, picked from [available] with [floor] as the least
 * it may name.
 *
 * Only versions on the floor's line count, the same major, or the same major and minor while the
 * major is 0, and only those at or above the floor. The newest stable one wins, and a pre-release only
 * when no stable one counts. The floor comes back when nothing counts or the floor does not read.
 * A version in [available] that does not read is passed over. The pick keeps its published spelling,
 * a leading `v` included.
 */
internal fun pickVersion(
    floor: String,
    available: List<String>,
): String {
    val least = Version.parse(floor) ?: return floor
    val counted = available.mapNotNull { spelled ->
        Version.parse(spelled)?.takeIf { version -> version.sameLine(least) && version >= least }?.let { version ->
            spelled to version
        }
    }
    val pool = counted.filter { (_, version) -> version.stable }.ifEmpty { counted }
    return pool.maxWithOrNull(compareBy { (_, version) -> version })?.first ?: floor
}

/**
 * A version as Maven publishes one, `1.12.0-alpha03` or `v0.1.0` for example.
 *
 * @property[preRelease] The rank of its pre-release kind, dev below alpha below beta below rc, or null
 * for a stable version.
 * @property[build] The number after the pre-release kind, 0 when there is none.
 */
private data class Version(
    val major: Long,
    val minor: Long,
    val patch: Long,
    val preRelease: Int?,
    val build: Long,
) : Comparable<Version> {
    val stable: Boolean
        get() = preRelease == null

    fun sameLine(other: Version): Boolean = major == other.major && (major != 0L || minor == other.minor)

    override fun compareTo(other: Version): Int =
        compareValuesBy(
            this,
            other,
            { version -> version.major },
            { version -> version.minor },
            { version -> version.patch },
            // A stable version sorts above every pre-release of the same number.
            { version -> version.preRelease ?: Int.MAX_VALUE },
            { version -> version.build },
        )

    companion object {
        private val PATTERN = Regex(
            pattern = """[vV]?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-(dev|alpha|beta|rc)[.-]?(\d+)?)?""",
            option = RegexOption.IGNORE_CASE,
        )
        private val KINDS = listOf("dev", "alpha", "beta", "rc")

        fun parse(text: String): Version? {
            val groups = PATTERN.matchEntire(text.trim())?.groupValues ?: return null
            val kind = groups[4].lowercase()
            return Version(
                major = groups[1].toLongOrNull() ?: return null,
                minor = groups[2].ifEmpty { "0" }.toLongOrNull() ?: return null,
                patch = groups[3].ifEmpty { "0" }.toLongOrNull() ?: return null,
                preRelease = if (kind.isEmpty()) null else KINDS.indexOf(kind),
                build = groups[5].ifEmpty { "0" }.toLongOrNull() ?: return null,
            )
        }
    }
}
