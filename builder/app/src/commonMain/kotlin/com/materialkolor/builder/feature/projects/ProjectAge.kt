package com.materialkolor.builder.feature.projects

/**
 * How long ago a project was saved, in the one unit the drawer shows.
 */
internal sealed interface ProjectAge {
    /** Less than a minute ago, or a clock that reads earlier than the save. */
    data object JustNow : ProjectAge

    data class Minutes(
        val count: Int,
    ) : ProjectAge

    data class Hours(
        val count: Int,
    ) : ProjectAge

    data class Days(
        val count: Int,
    ) : ProjectAge

    data class Weeks(
        val count: Int,
    ) : ProjectAge

    data class Months(
        val count: Int,
    ) : ProjectAge

    data class Years(
        val count: Int,
    ) : ProjectAge

    companion object {
        /** The age of a save made at [savedAt] when it is [now], both in milliseconds since the epoch. */
        fun of(
            savedAt: Long,
            now: Long,
        ): ProjectAge {
            val minutes = (now - savedAt) / MINUTE
            val hours = minutes / MINUTES_PER_HOUR
            val days = hours / HOURS_PER_DAY
            return when {
                minutes < 1 -> JustNow
                hours < 1 -> Minutes(minutes.toInt())
                days < 1 -> Hours(hours.toInt())
                days < DAYS_PER_WEEK -> Days(days.toInt())
                days < DAYS_PER_MONTH -> Weeks((days / DAYS_PER_WEEK).toInt())
                days < DAYS_PER_YEAR -> Months((days / DAYS_PER_MONTH).toInt())
                else -> Years((days / DAYS_PER_YEAR).toInt())
            }
        }
    }
}

private const val MINUTE: Long = 60_000
private const val MINUTES_PER_HOUR: Long = 60
private const val HOURS_PER_DAY: Long = 24
private const val DAYS_PER_WEEK: Long = 7
private const val DAYS_PER_MONTH: Long = 30
private const val DAYS_PER_YEAR: Long = 365
