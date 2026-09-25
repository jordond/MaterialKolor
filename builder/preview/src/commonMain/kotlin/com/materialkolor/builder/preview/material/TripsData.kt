package com.materialkolor.builder.preview.material

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Bookmark
import com.composables.icons.lucide.Compass
import com.composables.icons.lucide.Landmark
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Sailboat
import com.composables.icons.lucide.Snowflake
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.User
import com.materialkolor.builder.domain.model.Role

/**
 * The switch that keeps maps on the device, shared by both copies of a split.
 */
internal const val OfflineMapsSwitch: String = "trips.offlineMaps"

/**
 * Which scheme family colors a trip's thumbnail, so the list shows more than one.
 *
 * @property[container] The role the thumbnail is filled with.
 * @property[content] The role its icon is drawn in.
 */
internal enum class TripTint(
    val container: Role,
    val content: Role,
) {
    /**
     * The primary container, for the trip coming up next.
     */
    Primary(Role.PrimaryContainer, Role.OnPrimaryContainer),

    /**
     * The tertiary container.
     */
    Tertiary(Role.TertiaryContainer, Role.OnTertiaryContainer),

    /**
     * The highest surface container, for trips further out.
     */
    Neutral(Role.SurfaceContainerHighest, Role.OnSurfaceVariant),
}

/**
 * One stop of a day's plan.
 *
 * @property[what] What happens there.
 * @property[time] When, on a 24 hour clock.
 */
internal class PlanStop(
    val what: String,
    val time: String,
)

/**
 * A trip in the list.
 *
 * @property[name] The city and country.
 * @property[dates] When the trip runs.
 * @property[countdown] How long until it starts.
 * @property[summary] The line under the name when the trip is open.
 * @property[icon] What its thumbnail shows, since the app carries no photos.
 * @property[tint] The family its thumbnail is colored from.
 * @property[shared] Whether other travellers share the trip.
 * @property[plan] The first day's plan, empty when nothing is planned yet.
 */
internal class Trip(
    val name: String,
    val dates: String,
    val countdown: String,
    val summary: String,
    val icon: ImageVector,
    val tint: TripTint,
    val shared: Boolean,
    val plan: List<PlanStop> = emptyList(),
)

/**
 * Every trip, the one coming up next first.
 */
internal val Trips: List<Trip> = listOf(
    Trip(
        name = "Lisbon, Portugal",
        dates = "Oct 3 to 6",
        countdown = "9 days",
        summary = "Oct 3 to 6 · 4 travellers",
        icon = Lucide.Sailboat,
        tint = TripTint.Primary,
        shared = true,
        plan = listOf(
            PlanStop("Land at LIS", "10:40"),
            PlanStop("Check in at Casa do Largo", "15:00"),
            PlanStop("Dinner in Alfama", "20:30"),
        ),
    ),
    Trip("Kyoto, Japan", "Nov 12 to 20", "49 days", "Nov 12 to 20", Lucide.Landmark, TripTint.Tertiary, shared = false),
    Trip("Oaxaca, Mexico", "Jan 8 to 15", "3 mo", "Jan 8 to 15", Lucide.Sun, TripTint.Neutral, shared = false),
    Trip("Tromsø, Norway", "Feb 2 to 7", "4 mo", "Feb 2 to 7", Lucide.Snowflake, TripTint.Neutral, shared = false),
)

/**
 * The filters over the trip list, picked by the demo's tab index.
 *
 * @property[label] What the chip says.
 */
internal enum class TripFilter(
    val label: String,
) {
    /**
     * Every trip still to come, which is all of them.
     */
    Upcoming("Upcoming"),

    /**
     * Trips already taken, none yet.
     */
    Past("Past"),

    /**
     * Trips other travellers share.
     */
    Shared("Shared"),
    ;

    /**
     * The trips this filter keeps, paired with their place in [Trips].
     */
    fun trips(): List<IndexedValue<Trip>> =
        Trips.withIndex().filter { (_, trip) ->
            when (this) {
                Upcoming -> true
                Past -> false
                Shared -> trip.shared
            }
        }

    companion object {
        /**
         * The filter at [index], the first one for an index out of range.
         */
        fun at(index: Int): TripFilter = entries.getOrElse(index) { Upcoming }
    }
}

/**
 * Where the rail goes. Only Trips has a screen in the demo.
 *
 * @property[label] What the rail says under the icon.
 * @property[icon] The icon.
 */
internal enum class TripsDestination(
    val label: String,
    val icon: ImageVector,
) {
    Trips("Trips", Lucide.MapPin),
    Explore("Explore", Lucide.Compass),
    Saved("Saved", Lucide.Bookmark),
    Profile("Profile", Lucide.User),
}

/**
 * Something to pack, ticked in the checklist.
 *
 * @property[label] What it says.
 * @property[key] The checkbox it is ticked with, shared by both copies of a split.
 */
internal enum class PackingItem(
    val label: String,
) {
    Passports("Passports"),
    PlugAdapters("Plug adapters"),
    RainJackets("Rain jackets"),
    ;

    val key: String = "trips.packing.$name"
}

/**
 * The trip scene's own canvas, which the drawing stretches to fit.
 */
internal val SceneSize: Size = Size(480f, 184f)

/**
 * Where the sun sits on [SceneSize].
 */
internal val SceneSun: Offset = Offset(360f, 70f)

/**
 * How big the sun is on [SceneSize].
 */
internal const val SceneSunRadius: Float = 34f

/**
 * The hills of the trip scene as x and y pairs on [SceneSize], farthest first.
 *
 * Each ridge runs from the left edge to the right one and closes along the bottom.
 */
internal val SceneRidges: List<FloatArray> = listOf(
    floatArrayOf(0f, 150f, 90f, 88f, 160f, 128f, 250f, 70f, 340f, 136f, 420f, 100f, 480f, 124f),
    floatArrayOf(0f, 168f, 70f, 130f, 150f, 160f, 240f, 118f, 330f, 166f, 410f, 140f, 480f, 158f),
    floatArrayOf(0f, 176f, 120f, 160f, 260f, 178f, 380f, 164f, 480f, 176f),
)
