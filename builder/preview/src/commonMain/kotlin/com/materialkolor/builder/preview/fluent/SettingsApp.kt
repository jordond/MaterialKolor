package com.materialkolor.builder.preview.fluent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.control.foldedExpandedName
import com.materialkolor.builder.kit.control.foldedSelectedName
import com.materialkolor.builder.kit.control.foldedSwitchName
import com.materialkolor.builder.kit.control.foldedToggleName
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.split.PaneSpec
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.CardExpanderItem
import io.github.composefluent.component.ExpanderItem
import io.github.composefluent.component.ExpanderItemSeparator
import io.github.composefluent.component.Icon
import io.github.composefluent.component.InfoBar
import io.github.composefluent.component.MenuItem
import io.github.composefluent.component.NavigationDisplayMode
import io.github.composefluent.component.NavigationMenuItemScope
import io.github.composefluent.component.NavigationView
import io.github.composefluent.component.SubtleButton
import io.github.composefluent.component.Switcher
import io.github.composefluent.component.Text
import io.github.composefluent.component.ToggleButton
import io.github.composefluent.component.rememberNavigationState
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.ChevronDown
import io.github.composefluent.icons.regular.Navigation

/** How wide the shade legend is beside the page. */
private val LegendWidth = 300.dp

/**
 * The Settings app, the Fluent sample app of the App tab, a clone of Windows Settings.
 *
 * A navigation view lists the pages, collapsed behind a menu button on a phone, as a rail of icons
 * on a tablet and open on a desktop. The page holds a notice and sections of cards, groups that
 * open to more settings and rows with a switch. Its accent shades button shows the shade legend
 * beside the page, or over it on a phone.
 *
 * Everything the app remembers lives in [state] under `fluent.` keys, so the two copies of a split
 * agree, the phone's open menu included.
 *
 * @param[spec] The pane, for the shade legend.
 * @param[state] What the app remembers, shared by both copies.
 * @param[deviceWidth] The device the app lays itself out for.
 * @param[modifier] Applied to the app.
 */
@Composable
internal fun SettingsApp(
    spec: PaneSpec,
    state: DemoAppState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    val phone = deviceWidth == DeviceWidth.Phone
    val expanded = when (deviceWidth) {
        DeviceWidth.Phone -> state.isOn(FluentMenuSwitch)
        DeviceWidth.Tablet -> false
        DeviceWidth.Desktop -> true
    }
    val navigation = rememberNavigationState(initialExpanded = remember(deviceWidth) { expanded })
    LaunchedEffect(navigation, expanded) { navigation.expanded = expanded }
    if (phone) {
        // A tap on the page closes the open menu inside the library, so the demo state hears of it.
        LaunchedEffect(navigation) {
            snapshotFlow { navigation.expanded }.collect { open -> state.setOn(FluentMenuSwitch, open) }
        }
    }
    Box(modifier.fillMaxSize().background(FluentTheme.colors.background.solid.base)) {
        NavigationView(
            menuItems = {
                for (page in FluentPage.entries) {
                    item(key = page) {
                        PageItem(page, state, onPicked = { state.setOn(FluentMenuSwitch, false) })
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            displayMode = when (deviceWidth) {
                DeviceWidth.Phone -> NavigationDisplayMode.LeftCollapsed
                DeviceWidth.Tablet -> NavigationDisplayMode.LeftCompact
                DeviceWidth.Desktop -> NavigationDisplayMode.Left
            },
            title = { Text(FluentCopy.Title) },
            state = navigation,
            expandedButton = { if (phone) MenuButton(state) },
        ) {
            SettingsPage(spec, state, phone)
        }
    }
}

/**
 * The menu button of the phone's collapsed navigation, which opens the menu over the page. The
 * library's button keeps its clickable inside, so a layer over it takes its place.
 */
@Composable
private fun MenuButton(state: DemoAppState) {
    val open = state.isOn(FluentMenuSwitch)
    val interaction = remember { MutableInteractionSource() }
    FluentOverlaid(
        control = Modifier
            .fluentNeutralRoles()
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = { state.setOn(FluentMenuSwitch, !open) },
            ).foldedExpandedName(FluentCopy.Menu, open),
    ) {
        SubtleButton(onClick = {}, interaction = interaction, iconOnly = true) {
            Icon(Icons.Regular.Navigation, contentDescription = null)
        }
    }
}

/** One page of the navigation, the library's own item under a layer that names it and picks it. */
@Composable
private fun NavigationMenuItemScope.PageItem(
    page: FluentPage,
    state: DemoAppState,
    onPicked: () -> Unit,
) {
    val selected = state.fluentPage() == page
    val interaction = remember { MutableInteractionSource() }
    FluentOverlaid(
        control = Modifier
            .fluentAccentRoles()
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                onClick = {
                    state.openFluentPage(page)
                    onPicked()
                },
            ).foldedSelectedName(page.label, selected),
    ) {
        MenuItem(
            selected = selected,
            onClick = {},
            text = { Text(page.label) },
            icon = { Icon(page.icon, contentDescription = null) },
            interactionSource = interaction,
        )
    }
}

@Composable
private fun SettingsPage(
    spec: PaneSpec,
    state: DemoAppState,
    phone: Boolean,
) {
    val legendShown = state.isOn(FluentShadesSwitch)
    // The phone's menu button and title sit over the top of the page.
    Column(Modifier.fillMaxSize().padding(top = if (phone) 48.dp else 0.dp)) {
        PageHeader(state, phone)
        if (phone) {
            PanelMotion(legendShown) {
                ShadeMapping(spec, Modifier.fillMaxWidth().heightIn(max = 420.dp).padding(horizontal = 16.dp))
            }
            SettingsList(state, phone, Modifier.weight(1f).fillMaxWidth())
        } else {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                SettingsList(state, phone, Modifier.weight(1f).fillMaxHeight())
                PanelMotion(legendShown, horizontal = true) {
                    val legend = Modifier.width(LegendWidth).fillMaxHeight().padding(end = 16.dp, bottom = 16.dp)
                    ShadeMapping(spec, legend)
                }
            }
        }
    }
}

@Composable
private fun PageHeader(
    state: DemoAppState,
    phone: Boolean,
) {
    val legendShown = state.isOn(FluentShadesSwitch)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (phone) 16.dp else 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.fluentPage().label,
            style = FluentTheme.typography.title,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        ToggleButton(
            checked = legendShown,
            onCheckedChanged = { checked -> state.setOn(FluentShadesSwitch, checked) },
            modifier = Modifier
                .fluentAccentRoles()
                .foldedToggleName(FluentCopy.Shades, legendShown)
                .semantics { toggleableState = ToggleableState(legendShown) },
        ) {
            Text(FluentCopy.Shades)
        }
    }
}

@Composable
private fun SettingsList(
    state: DemoAppState,
    phone: Boolean,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = state.rememberListState(FluentSettingsList),
        contentPadding = PaddingValues(start = if (phone) 16.dp else 24.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(FluentEntries) { entry ->
            when (entry) {
                FluentEntry.Notice -> InfoBar(
                    title = { Text(FluentCopy.NoticeTitle) },
                    message = { Text(FluentCopy.NoticeMessage) },
                    modifier = Modifier.fillMaxWidth(),
                )
                is FluentEntry.Heading -> Text(
                    text = entry.title,
                    style = FluentTheme.typography.bodyStrong,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                )
                is FluentEntry.Toggle -> CardExpanderItem(
                    heading = { Text(entry.setting.title) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = null,
                    caption = { Text(entry.setting.caption) },
                    trailing = { SettingSwitch(entry.setting, state) },
                )
                is FluentEntry.Group -> SettingsGroup(entry.group, state)
            }
        }
    }
}

/**
 * A group of settings that opens, built from the library's expander parts.
 *
 * Its header is the expander's clickable card, so it carries the colors it declares and the web's
 * expanded fold itself, which the one piece `Expander` keeps out of reach. The settings under it
 * open with the skin's panel motion, and at once while motion is frozen.
 */
@Composable
private fun SettingsGroup(
    group: FluentGroup,
    state: DemoAppState,
) {
    val open = state.isOn(group.key)
    Column(Modifier.fillMaxWidth()) {
        CardExpanderItem(
            onClick = { state.setOn(group.key, !open) },
            heading = { Text(group.title) },
            modifier = Modifier
                .fillMaxWidth()
                .fluentAccentRoles()
                .foldedExpandedName(group.title, open),
            icon = { AccentTile(group.icon) },
            caption = { Text(group.caption) },
            dropdown = {
                Icon(
                    imageVector = Icons.Regular.ChevronDown,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp).graphicsLayer { rotationZ = if (open) 180f else 0f },
                )
            },
        )
        PanelMotion(open) {
            Column {
                for (setting in group.settings) {
                    ExpanderItemSeparator()
                    ExpanderItem(
                        heading = { Text(setting.title) },
                        modifier = Modifier.fillMaxWidth(),
                        caption = { Text(setting.caption) },
                        trailing = { SettingSwitch(setting, state) },
                        dropdown = null,
                    )
                }
            }
        }
    }
}

/** A group's icon on the accent fill, the way Windows marks a page's own settings. */
@Composable
private fun AccentTile(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(FluentTheme.colors.fillAccent.default, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = FluentTheme.colors.text.onAccent.primary,
        )
    }
}

/**
 * The library's switch, under a layer that toggles it with the switch role, names it for the web
 * and declares its accent fill. `Switcher` takes no modifier and reports itself as a plain button.
 */
@Composable
private fun SettingSwitch(
    setting: FluentSetting,
    state: DemoAppState,
) {
    val on = state.isOn(setting)
    val interaction = remember { MutableInteractionSource() }
    FluentOverlaid(
        control = Modifier
            .fluentAccentRoles()
            .toggleable(
                value = on,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = { checked -> state.setOn(setting, checked) },
            ).foldedSwitchName(setting.title, on),
    ) {
        Switcher(
            checked = on,
            onCheckStateChange = { checked -> state.setOn(setting, checked) },
            text = if (on) FluentCopy.On else FluentCopy.Off,
            textBefore = true,
            interactionSource = interaction,
        )
    }
}
