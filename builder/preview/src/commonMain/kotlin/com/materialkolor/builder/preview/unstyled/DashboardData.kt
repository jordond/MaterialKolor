package com.materialkolor.builder.preview.unstyled

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.ChartBar
import com.composables.icons.lucide.DollarSign
import com.composables.icons.lucide.LayoutDashboard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Package
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.ShoppingCart
import com.composables.icons.lucide.Users

// Every word the dashboard shows lives in this file. The copy is a first draft for the owner to
// review, which is why none of it goes through string resources yet.

/** The switch that shows the token side panel, shared by both copies of a split. */
internal const val DashboardDrawerSwitch: String = "dashboard.drawerOpen"

/** The switch that shows the order status menu. */
internal const val DashboardMenuSwitch: String = "dashboard.menuOpen"

/** The switch that shows the navigation under the top bar of a phone. */
internal const val DashboardNavSwitch: String = "dashboard.navOpen"

/** The sidebar pick, one option per [DashboardDestination]. */
internal const val DashboardNavChoice: String = "dashboard.nav"

/** The tab pick, one option per [DashboardRange]. */
internal const val DashboardRangeChoice: String = "dashboard.range"

/** The menu pick, one option per [OrderFilter]. */
internal const val DashboardFilterChoice: String = "dashboard.filter"

/** The scrolling page, the table of orders included. */
internal const val DashboardPageList: String = "dashboard.page"

/** The list of tokens in the side panel. */
internal const val DashboardTokenList: String = "dashboard.tokens"

/** The words on the dashboard that belong to no list of data. */
internal object DashboardCopy {
    const val Brand: String = "Pulse"
    const val Navigation: String = "Navigation"
    const val Export: String = "Export"
    const val NewReport: String = "New report"
    const val ShowTokens: String = "Show tokens"
    const val HideTokens: String = "Hide tokens"
    const val Tokens: String = "Tokens"
    const val CloseTokens: String = "Close tokens"
    const val TokensNote: String = "The MaterialKolor tokens this screen reads"
    const val AlertTitle: String = "Payouts paused"
    const val AlertBody: String = "The bank declined the last transfer. Update the payout account to resume."
    const val AlertAction: String = "Update account"
    const val ChartTitle: String = "Revenue"
    const val ChartCurrent: String = "Current"
    const val ChartPrevious: String = "Previous"
    const val Orders: String = "Recent orders"
    const val ColumnCustomer: String = "Customer"
    const val ColumnAmount: String = "Amount"
    const val ColumnStatus: String = "Status"
    const val OfGoal: String = "of goal"
}

/**
 * Where the sidebar leads. Every destination shows the same sample page under its own title.
 *
 * @property[label] The name in the sidebar and the page title.
 * @property[icon] The sidebar icon, all the rail shows.
 */
internal enum class DashboardDestination(
    val label: String,
    val icon: ImageVector,
) {
    Overview("Overview", Lucide.LayoutDashboard),
    Reports("Reports", Lucide.ChartBar),
    Customers("Customers", Lucide.Users),
    Products("Products", Lucide.Package),
    Settings("Settings", Lucide.Settings),
}

/**
 * The period the tabs pick, which the metric cards and the chart report on.
 *
 * @property[label] The tab's name.
 * @property[versus] What a metric's change is measured against.
 */
internal enum class DashboardRange(
    val label: String,
    val versus: String,
) {
    Week("Week", "vs last week"),
    Month("Month", "vs last month"),
    Year("Year", "vs last year"),
}

/**
 * Which scheme family colors a metric card's icon, so the cards show all four.
 *
 * @property[container] The token the icon sits on.
 * @property[content] The token the icon is drawn in.
 */
internal enum class MetricTint(
    val container: DashboardToken,
    val content: DashboardToken,
) {
    Primary(DashboardToken.PrimaryContainer, DashboardToken.OnPrimaryContainer),
    Secondary(DashboardToken.SecondaryContainer, DashboardToken.OnSecondaryContainer),
    Tertiary(DashboardToken.TertiaryContainer, DashboardToken.OnTertiaryContainer),
    Error(DashboardToken.ErrorContainer, DashboardToken.OnErrorContainer),
}

/**
 * What a metric card counts.
 *
 * @property[label] The card's name.
 * @property[icon] The card's icon.
 * @property[tint] The family the icon is colored from.
 * @property[risingIsGood] Whether a rise is good news, false for refunds.
 */
internal enum class MetricKind(
    val label: String,
    val icon: ImageVector,
    val tint: MetricTint,
    val risingIsGood: Boolean = true,
) {
    Revenue("Revenue", Lucide.DollarSign, MetricTint.Primary),
    Customers("Active customers", Lucide.Users, MetricTint.Secondary),
    Orders("Orders", Lucide.ShoppingCart, MetricTint.Tertiary),
    Refunds("Refund rate", Lucide.RotateCcw, MetricTint.Error, risingIsGood = false),
}

/**
 * One metric card over one range.
 *
 * @property[kind] What it counts.
 * @property[value] The figure, formatted.
 * @property[change] How far it moved, formatted with its sign.
 * @property[rising] Whether it went up.
 * @property[goal] How much of the goal it reached, from 0 to 1, or null for a card with no goal.
 */
internal class Metric(
    val kind: MetricKind,
    val value: String,
    val change: String,
    val rising: Boolean,
    val goal: Float? = null,
) {
    /** Whether the move is good news. */
    val good: Boolean get() = rising == kind.risingIsGood
}

/**
 * One slot of the revenue chart.
 *
 * @property[label] What the slot is called under the chart.
 * @property[current] Revenue in the range shown, in thousands.
 * @property[previous] Revenue in the range before it, in thousands.
 */
internal class ChartPoint(
    val label: String,
    val current: Float,
    val previous: Float,
)

/**
 * What the page shows for one range.
 *
 * @property[metrics] The metric cards, one per [MetricKind] in its order.
 * @property[chart] The revenue chart's slots.
 */
internal class RangeReport(
    val metrics: List<Metric>,
    val chart: List<ChartPoint>,
)

private val Reports: Map<DashboardRange, RangeReport> = mapOf(
    DashboardRange.Week to RangeReport(
        metrics = listOf(
            Metric(MetricKind.Revenue, "$12,900", "+8.2%", rising = true, goal = 0.86f),
            Metric(MetricKind.Customers, "1,284", "+3.1%", rising = true),
            Metric(MetricKind.Orders, "342", "-2.4%", rising = false),
            Metric(MetricKind.Refunds, "1.8%", "+0.3%", rising = true),
        ),
        chart = points(
            labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            current = listOf(1.8f, 2.1f, 1.6f, 2.4f, 2.9f, 1.2f, 0.9f),
            previous = listOf(1.5f, 1.9f, 1.7f, 2.0f, 2.3f, 1.4f, 1.0f),
        ),
    ),
    DashboardRange.Month to RangeReport(
        metrics = listOf(
            Metric(MetricKind.Revenue, "$48,400", "+12.4%", rising = true, goal = 0.72f),
            Metric(MetricKind.Customers, "4,920", "+6.0%", rising = true),
            Metric(MetricKind.Orders, "1,386", "+4.8%", rising = true),
            Metric(MetricKind.Refunds, "1.6%", "-0.2%", rising = false),
        ),
        chart = points(
            labels = listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4"),
            current = listOf(11.2f, 12.5f, 10.8f, 13.9f),
            previous = listOf(10.1f, 11.0f, 11.4f, 12.2f),
        ),
    ),
    DashboardRange.Year to RangeReport(
        metrics = listOf(
            Metric(MetricKind.Revenue, "$610k", "+21.5%", rising = true, goal = 0.94f),
            Metric(MetricKind.Customers, "18,240", "+14.2%", rising = true),
            Metric(MetricKind.Orders, "16,905", "+9.7%", rising = true),
            Metric(MetricKind.Refunds, "1.9%", "+0.1%", rising = true),
        ),
        chart = points(
            labels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"),
            current = listOf(38f, 41f, 45f, 43f, 48f, 52f, 50f, 55f, 53f, 58f, 61f, 66f),
            previous = listOf(30f, 33f, 36f, 35f, 39f, 41f, 42f, 44f, 45f, 47f, 50f, 54f),
        ),
    ),
)

/** What the page shows for this range. */
internal val DashboardRange.report: RangeReport get() = Reports.getValue(this)

private fun points(
    labels: List<String>,
    current: List<Float>,
    previous: List<Float>,
): List<ChartPoint> {
    require(labels.size == current.size && labels.size == previous.size) { "Every chart slot needs both values" }
    return labels.indices.map { index -> ChartPoint(labels[index], current[index], previous[index]) }
}

/**
 * Where an order stands, shown as a small mark in the table.
 *
 * @property[label] The mark's text.
 * @property[container] The token the mark is filled with, or null for an outlined mark.
 * @property[content] The token its text is drawn in.
 */
internal enum class OrderStatus(
    val label: String,
    val container: DashboardToken?,
    val content: DashboardToken,
) {
    Paid("Paid", DashboardToken.TertiaryContainer, DashboardToken.OnTertiaryContainer),
    Pending("Pending", DashboardToken.SecondaryContainer, DashboardToken.OnSecondaryContainer),
    Failed("Failed", DashboardToken.ErrorContainer, DashboardToken.OnErrorContainer),
    Refunded("Refunded", null, DashboardToken.OnSurfaceVariant),
}

/**
 * The picks of the order status menu.
 *
 * @property[label] The pick's name, which the menu button shows once picked.
 * @property[status] The orders it keeps, or null to keep them all.
 */
internal enum class OrderFilter(
    val label: String,
    val status: OrderStatus?,
) {
    All("All statuses", null),
    Paid("Paid", OrderStatus.Paid),
    Pending("Pending", OrderStatus.Pending),
    Failed("Failed", OrderStatus.Failed),
    Refunded("Refunded", OrderStatus.Refunded),
    ;

    /** Whether the table shows [order] under this pick. */
    fun keeps(order: Order): Boolean = status == null || order.status == status
}

/**
 * A row of the orders table.
 *
 * @property[id] The order number.
 * @property[customer] Who placed it.
 * @property[amount] What it came to, formatted.
 * @property[status] Where it stands.
 */
internal class Order(
    val id: String,
    val customer: String,
    val amount: String,
    val status: OrderStatus,
)

/** The orders table, newest first. */
internal val DashboardOrders: List<Order> = listOf(
    Order("#3210", "Amara Okafor", "$1,999.00", OrderStatus.Paid),
    Order("#3209", "Jonas Berg", "$39.00", OrderStatus.Pending),
    Order("#3208", "Priya Raman", "$299.00", OrderStatus.Paid),
    Order("#3207", "Mateo Silva", "$99.00", OrderStatus.Failed),
    Order("#3206", "Hana Sato", "$450.00", OrderStatus.Refunded),
    Order("#3205", "Leo Fischer", "$250.00", OrderStatus.Paid),
    Order("#3204", "Chloe Martin", "$120.00", OrderStatus.Pending),
    Order("#3203", "Omar Haddad", "$89.00", OrderStatus.Paid),
    Order("#3202", "Ines Duarte", "$640.00", OrderStatus.Failed),
    Order("#3201", "Theo Novak", "$75.00", OrderStatus.Paid),
    Order("#3200", "Maya Cohen", "$310.00", OrderStatus.Refunded),
    Order("#3199", "Arjun Mehta", "$58.00", OrderStatus.Paid),
)
