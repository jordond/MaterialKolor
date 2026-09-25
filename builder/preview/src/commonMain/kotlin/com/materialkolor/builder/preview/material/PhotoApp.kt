package com.materialkolor.builder.preview.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.X
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.control.foldedSelectedName
import com.materialkolor.builder.preview.canvas.DemoAppState

/**
 * The bar's height once the feed has scrolled under it.
 */
private val BarCollapsedHeight = TopAppBarDefaults.MediumAppBarCollapsedHeight

/**
 * The bar's height over the top of the feed, with room for the subtitle.
 */
private val BarExpandedHeight = TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight

/**
 * The photo app, the Material 3 Expressive sample app of the App tab.
 *
 * A flexible top bar over the library feed, which opens with a carousel of memories, a button group
 * of toggle buttons, a backup card with wavy progress and a grid of photos drawn from the scheme.
 * A create menu grows out of its floating action button in the layout. A phone gets one column with
 * a bottom bar, a tablet a collapsed wide rail beside the feed and a desktop an expanded one.
 *
 * Everything the app remembers lives in [state] under `photo.` keys, so the two copies of a split
 * agree. The bar collapses with the feed's mirrored scroll position rather than a nested scroll of
 * its own, which only the copy under the pointer would get.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The device the app lays itself out for.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun PhotoApp(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize().previewRoles(Role.Surface, Role.OnSurface)) {
        when (deviceWidth) {
            DeviceWidth.Phone -> PhotoPhone(state)
            DeviceWidth.Tablet -> PhotoPanes(state, deviceWidth, railExpanded = false)
            DeviceWidth.Desktop -> PhotoPanes(state, deviceWidth, railExpanded = true)
        }
    }
}

@Composable
private fun PhotoPhone(state: DemoAppState) {
    val list = state.rememberListState(PhotoFeedList)
    Column(Modifier.fillMaxSize()) {
        PhotoTopBar(list)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            PhotoFeed(state, list, DeviceWidth.Phone, Modifier.fillMaxSize())
            CreateMenu(state, Modifier.align(Alignment.BottomEnd))
        }
        PhotoBottomBar()
    }
}

@Composable
private fun PhotoPanes(
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    railExpanded: Boolean,
) {
    val list = state.rememberListState(PhotoFeedList)
    Row(Modifier.fillMaxSize()) {
        PhotoRail(railExpanded)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize()) {
                PhotoTopBar(list)
                PhotoFeed(state, list, deviceWidth, Modifier.weight(1f).fillMaxWidth())
            }
            CreateMenu(state, Modifier.align(Alignment.BottomEnd))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PhotoTopBar(list: LazyListState) {
    val bar = rememberBarFollowing(list)
    MediumFlexibleTopAppBar(
        title = { Text(PhotoCopy.Title) },
        modifier = Modifier.previewRoles(ExpressiveComponent.FlexibleTopAppBar),
        subtitle = { Text(PhotoCopy.Subtitle) },
        actions = {
            IconButton(onClick = {}, modifier = Modifier.previewRoles(Role.OnSurfaceVariant)) {
                Icon(Lucide.Search, contentDescription = PhotoCopy.Search)
            }
        },
        collapsedHeight = BarCollapsedHeight,
        expandedHeight = BarExpandedHeight,
        windowInsets = WindowInsets(0),
        scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(bar),
    )
}

/**
 * A bar state that collapses as far as [list] has scrolled past its first item, and stays
 * collapsed below it.
 *
 * Both copies of a split read the same mirrored position, so their bars agree. The scroll
 * behavior around it is pinned, which keeps the bar from being dragged in one copy only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberBarFollowing(list: LazyListState): TopAppBarState {
    val range = with(LocalDensity.current) { (BarExpandedHeight - BarCollapsedHeight).toPx() }
    val bar = remember(range) {
        TopAppBarState(
            initialHeightOffsetLimit = -range,
            initialHeightOffset = collapseOf(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset, range),
            initialContentOffset = 0f,
        )
    }
    LaunchedEffect(list, bar, range) {
        snapshotFlow { collapseOf(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset, range) }
            .collect { offset ->
                bar.heightOffsetLimit = -range
                bar.heightOffset = offset
            }
    }
    return bar
}

/**
 * How far the bar collapses, as a negative height offset, for a feed scrolled to [index] and [offset].
 */
private fun collapseOf(
    index: Int,
    offset: Int,
    range: Float,
): Float = if (index > 0) -range else -offset.toFloat().coerceAtMost(range)

@Composable
private fun PhotoBottomBar() {
    ShortNavigationBar(
        modifier = Modifier.previewRoles(ExpressiveComponent.ShortNavigationBar),
        windowInsets = WindowInsets(0),
    ) {
        for (destination in PhotoDestination.entries) {
            val selected = destination == PhotoDestination.Photos
            ShortNavigationBarItem(
                selected = selected,
                onClick = {},
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                modifier = Modifier
                    .previewRoles(ExpressiveComponent.ShortNavigationBarItem)
                    .foldedSelectedName(destination.label, selected),
            )
        }
    }
}

/**
 * A wide rail, never the modal one. Its width never changes while it shows, so both copies agree.
 */
@Composable
private fun PhotoRail(expanded: Boolean) {
    val rail = rememberWideNavigationRailState(
        if (expanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed,
    )
    WideNavigationRail(
        modifier = Modifier.fillMaxHeight().previewRoles(ExpressiveComponent.WideNavigationRail),
        state = rail,
        windowInsets = WindowInsets(0),
    ) {
        for (destination in PhotoDestination.entries) {
            val selected = destination == PhotoDestination.Photos
            WideNavigationRailItem(
                selected = selected,
                onClick = {},
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
                railExpanded = expanded,
                modifier = Modifier
                    .previewRoles(ExpressiveComponent.WideNavigationRailItem)
                    .foldedSelectedName(destination.label, selected),
            )
        }
    }
}

/**
 * The create button, which opens its menu upward in the layout. Picking an item closes it again.
 * Back is the app's own, so the menu listens for no back press.
 *
 * The menu always gets at least a pixel of height. Given none, as on a screen too short for the
 * feed, Material's menu shows and hides its last item every frame and never settles.
 */
@Composable
private fun CreateMenu(
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    val open = state.isOn(PhotoCreateSwitch)
    FloatingActionButtonMenu(
        expanded = open,
        button = {
            ToggleFloatingActionButton(
                checked = open,
                onCheckedChange = { checked -> state.setOn(PhotoCreateSwitch, checked) },
                modifier = Modifier
                    .previewRoles(ExpressiveComponent.ToggleFloatingActionButton)
                    .foldedExpandedName(PhotoCopy.Create, open),
            ) {
                val icon by remember { derivedStateOf { if (checkedProgress > 0.5f) Lucide.X else Lucide.Plus } }
                Icon(icon, contentDescription = null, modifier = Modifier.animateIcon({ checkedProgress }))
            }
        },
        modifier = modifier.requiredHeightIn(min = 1.dp),
    ) {
        for (action in PhotoCreate.entries) {
            FloatingActionButtonMenuItem(
                onClick = { state.setOn(PhotoCreateSwitch, false) },
                text = { Text(action.label) },
                icon = { Icon(action.icon, contentDescription = null) },
                modifier = Modifier.previewRoles(ExpressiveComponent.FloatingActionButtonMenuItem),
            )
        }
    }
}
