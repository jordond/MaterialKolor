package com.materialkolor.builder.preview.unstyled

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.TrendingDown
import com.composables.icons.lucide.TrendingUp
import com.composables.icons.lucide.TriangleAlert
import com.composeunstyled.Indicator
import com.composeunstyled.Tab
import com.composeunstyled.TabList
import com.composeunstyled.Text
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.UnstyledProgress
import com.composeunstyled.UnstyledTabGroup
import com.composeunstyled.focusRing
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.kit.control.foldedTabName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import kotlin.math.min
import kotlin.math.roundToInt

private val CardGap = 12.dp
private val ChartHeight = 160.dp
private val MaxBarWidth = 16.dp
private val LegendDot = 8.dp
private val ProgressHeight = 6.dp
private val MetricIconSize = 32.dp

/** Under this width the metric cards go two to a row instead of four. */
private val FourCardWidth = 600.dp

/** How many lines divide the chart's height. */
private const val GridLines = 4

/**
 * The scrolling page under the top bar, from the range tabs down to the orders table.
 *
 * The table's rows are items of the page's own lazy list, so a long table only composes what shows.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[focus] Where focus goes in this copy as the status menu opens and closes.
 * @param[phone] Whether the page is on a phone, which puts the page actions on it and tightens it.
 * @param[modifier] Applied to the page.
 */
@Composable
internal fun DashboardPage(
    state: DemoAppState,
    focus: DashboardFocus,
    phone: Boolean,
    modifier: Modifier = Modifier,
) {
    val range = DashboardRange.entries[state.choice(DashboardRangeChoice, DashboardRange.entries.size)]
    val filter = OrderFilter.entries[state.choice(DashboardFilterChoice, OrderFilter.entries.size)]
    val report = range.report
    val orders = DashboardOrders.filter { order -> filter.keeps(order) }
    LazyColumn(
        state = state.rememberListState(DashboardPageList),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(if (phone) SectionGap else PageGap),
    ) {
        if (phone) {
            item(key = "actions") { PageActions(Modifier.padding(bottom = SectionGap)) }
        }
        item(key = "tabs") { RangeTabs(state, range, Modifier.padding(bottom = SectionGap)) }
        item(key = "alert") { PayoutAlert(phone, Modifier.padding(bottom = SectionGap)) }
        item(key = "metrics") { MetricCards(report.metrics, range, Modifier.padding(bottom = SectionGap)) }
        item(key = "chart") { RevenueChart(report.chart, Modifier.padding(bottom = PageGap)) }
        item(key = "orders") { OrdersTitle(state, focus, filter, Modifier.padding(bottom = Gap)) }
        item(key = "orders.header") { TableHeader() }
        items(orders, key = { order -> order.id }) { order -> OrderRow(order, last = order === orders.last()) }
    }
}

/** Export and New report, in the top bar when there is room and on the page of a phone. */
@Composable
internal fun PageActions(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Gap)) {
        DashboardButton(DashboardCopy.Export, ButtonStyle.Outlined, onClick = {}, icon = Lucide.Download)
        DashboardButton(DashboardCopy.NewReport, ButtonStyle.Filled, onClick = {}, icon = Lucide.Plus)
    }
}

@Composable
private fun RangeTabs(
    state: DemoAppState,
    range: DashboardRange,
    modifier: Modifier,
) {
    UnstyledTabGroup(
        selectedTab = range,
        onSelectedTabChange = { picked ->
            state.choose(DashboardRangeChoice, DashboardRange.entries.size, picked.ordinal)
        },
        tabs = DashboardRange.entries,
        modifier = modifier,
    ) {
        TabList(
            modifier = Modifier
                .previewRoles(UnstyledComponent.TabList)
                .clip(ControlShape)
                .background(DashboardToken.SurfaceContainerHigh.color)
                .padding(4.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (entry in DashboardRange.entries) {
                    val selected = entry == range
                    val container = if (selected) DashboardToken.SurfaceContainerLowest else null
                    val content = if (selected) DashboardToken.OnSurface else DashboardToken.OnSurfaceVariant
                    val interactions = remember { MutableInteractionSource() }
                    Tab(
                        key = entry,
                        modifier = Modifier
                            .previewRoles(if (selected) UnstyledComponent.SelectedTab else UnstyledComponent.Tab)
                            .foldedTabName(entry.label, selected)
                            .focusRing(interactions, 2.dp, DashboardToken.Primary.color, ControlShape)
                            .clip(ControlShape)
                            .background(container?.color ?: Color.Transparent),
                        interactionSource = interactions,
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = entry.label,
                            modifier = Modifier.padding(horizontal = SectionGap, vertical = 6.dp),
                            style = LabelStyle,
                            color = content.color,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PayoutAlert(
    phone: Boolean,
    modifier: Modifier,
) {
    val content = DashboardToken.OnErrorContainer.color
    Row(
        modifier = modifier
            .fillMaxWidth()
            .previewRoles(UnstyledComponent.Alert)
            .clip(CardShape)
            .background(DashboardToken.ErrorContainer.color)
            .padding(SectionGap),
        horizontalArrangement = Arrangement.spacedBy(CardGap),
        verticalAlignment = if (phone) Alignment.Top else Alignment.CenterVertically,
    ) {
        UnstyledIcon(
            imageVector = Lucide.TriangleAlert,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = DashboardToken.Error.color,
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(DashboardCopy.AlertTitle, style = LabelStyle, color = content)
            Text(DashboardCopy.AlertBody, style = BodyStyle, color = content)
            if (phone) {
                DashboardButton(
                    label = DashboardCopy.AlertAction,
                    style = ButtonStyle.Alert,
                    onClick = {},
                    modifier = Modifier.padding(top = Gap),
                )
            }
        }
        if (!phone) DashboardButton(DashboardCopy.AlertAction, ButtonStyle.Alert, onClick = {})
    }
}

/** The metric cards, four to a row when they fit and two when not. */
@Composable
private fun MetricCards(
    metrics: List<Metric>,
    range: DashboardRange,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val columns = if (maxWidth < FourCardWidth) 2 else 4
        Column(verticalArrangement = Arrangement.spacedBy(CardGap)) {
            for (row in metrics.chunked(columns)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(CardGap),
                ) {
                    for (metric in row) MetricCard(metric, range, Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    metric: Metric,
    range: DashboardRange,
    modifier: Modifier,
) {
    val kind = metric.kind
    val trend = if (metric.good) DashboardToken.Tertiary else DashboardToken.Error
    Column(
        modifier = modifier
            .previewRoles(UnstyledComponent.MetricCard)
            .clip(CardShape)
            .background(DashboardToken.SurfaceContainer.color)
            .padding(SectionGap),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(kind.label, Modifier.weight(1f), style = BodyStyle, color = DashboardToken.OnSurfaceVariant.color)
            Box(
                modifier = Modifier
                    .size(MetricIconSize)
                    .previewRoles(kind.tint.container.role, kind.tint.content.role)
                    .clip(ControlShape)
                    .background(kind.tint.container.color),
                contentAlignment = Alignment.Center,
            ) {
                UnstyledIcon(kind.icon, null, modifier = Modifier.size(IconSize), tint = kind.tint.content.color)
            }
        }
        Text(metric.value, style = ValueStyle, color = DashboardToken.OnSurface.color)
        Row(
            modifier = Modifier.previewRoles(Role.SurfaceContainer, trend.role),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = if (metric.rising) Lucide.TrendingUp else Lucide.TrendingDown
            UnstyledIcon(icon, null, modifier = Modifier.size(14.dp), tint = trend.color)
            Text("${metric.change} ${range.versus}", style = SmallStyle, color = trend.color)
        }
        val goal = metric.goal
        if (goal != null) {
            UnstyledProgress(
                progress = goal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProgressHeight)
                    .previewRoles(UnstyledComponent.Progress)
                    .clip(PillShape)
                    .background(DashboardToken.SurfaceContainerHighest.color),
            ) {
                Indicator(Modifier.clip(PillShape).background(DashboardToken.Primary.color))
            }
            Text(
                text = "${(goal * 100).roundToInt()}% ${DashboardCopy.OfGoal}",
                style = SmallStyle,
                color = DashboardToken.OnSurfaceVariant.color,
            )
        }
    }
}

/** Revenue against the range before, as pairs of bars drawn once. Nothing on it moves. */
@Composable
private fun RevenueChart(
    points: List<ChartPoint>,
    modifier: Modifier,
) {
    val current = DashboardToken.Primary.color
    val previous = DashboardToken.Secondary.color
    val grid = DashboardToken.OutlineVariant.color
    val muted = DashboardToken.OnSurfaceVariant.color
    Column(
        modifier = modifier
            .fillMaxWidth()
            .previewRoles(UnstyledComponent.ChartCard)
            .clip(CardShape)
            .background(DashboardToken.SurfaceContainerLow.color)
            .padding(SectionGap),
        verticalArrangement = Arrangement.spacedBy(CardGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CardGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = DashboardCopy.ChartTitle,
                modifier = Modifier.weight(1f),
                style = HeadingStyle,
                color = DashboardToken.OnSurface.color,
            )
            LegendEntry(DashboardCopy.ChartCurrent, DashboardToken.Primary, muted)
            LegendEntry(DashboardCopy.ChartPrevious, DashboardToken.Secondary, muted)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(ChartHeight)
                .previewRoles(UnstyledComponent.Chart),
        ) {
            drawChart(points, current, previous, grid)
        }
        Row(Modifier.fillMaxWidth()) {
            for (point in points) {
                Text(
                    text = point.label,
                    modifier = Modifier.weight(1f),
                    style = SmallStyle,
                    color = muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A legend's dot, painted from [token] and declaring its role, and its label. */
@Composable
private fun LegendEntry(
    label: String,
    token: DashboardToken,
    textColor: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(LegendDot)
                .previewRoles(token.role)
                .clip(CircleShape)
                .background(token.color),
        )
        Text(label, style = SmallStyle, color = textColor)
    }
}

/** Grid lines, then per slot the previous range's bar on the start side and the current one after it. */
private fun DrawScope.drawChart(
    points: List<ChartPoint>,
    current: Color,
    previous: Color,
    grid: Color,
) {
    val stroke = 1.dp.toPx()
    for (line in 0..GridLines) {
        val y = stroke / 2 + (size.height - stroke) * line / GridLines
        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
    }
    val top = points.maxOf { point -> maxOf(point.current, point.previous) }
    val slot = size.width / points.size
    val bar = min(slot * 0.3f, MaxBarWidth.toPx())
    val space = bar / 4
    val radius = CornerRadius(min(bar / 2, 4.dp.toPx()))
    val rtl = layoutDirection == LayoutDirection.Rtl
    points.forEachIndexed { index, point ->
        val middle = slot * (index + 0.5f)
        val center = if (rtl) size.width - middle else middle
        val before = center - space / 2 - bar
        val after = center + space / 2
        drawBar(previous, if (rtl) after else before, bar, point.previous / top, radius)
        drawBar(current, if (rtl) before else after, bar, point.current / top, radius)
    }
}

private fun DrawScope.drawBar(
    color: Color,
    left: Float,
    width: Float,
    fraction: Float,
    radius: CornerRadius,
) {
    val height = size.height * fraction
    drawRoundRect(color, Offset(left, size.height - height), Size(width, height), radius)
}
