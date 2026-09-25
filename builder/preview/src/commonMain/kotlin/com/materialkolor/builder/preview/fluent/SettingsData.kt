package com.materialkolor.builder.preview.fluent

import androidx.compose.ui.graphics.vector.ImageVector
import com.materialkolor.builder.domain.audit.FluentShade
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Alert
import io.github.composefluent.icons.regular.ArrowSync
import io.github.composefluent.icons.regular.Folder
import io.github.composefluent.icons.regular.Home
import io.github.composefluent.icons.regular.Image
import io.github.composefluent.icons.regular.Key
import io.github.composefluent.icons.regular.Person
import io.github.composefluent.icons.regular.Settings
import io.github.composefluent.icons.regular.Star

// The Settings app's sample content. All of it is provisional copy for the owner to replace.

/**
 * The page the navigation shows as current, a choice in the demo state.
 */
internal const val FluentPageChoice = "fluent.page"

/**
 * Whether the phone's navigation menu is open, a switch in the demo state.
 */
internal const val FluentMenuSwitch = "fluent.menu"

/**
 * Whether the shade legend shows beside or over the page, a switch in the demo state.
 */
internal const val FluentShadesSwitch = "fluent.shades"

/**
 * The settings page, a list both copies keep at the same place.
 */
internal const val FluentSettingsList = "fluent.settings"

/**
 * The words the Settings app shows outside its settings.
 */
internal object FluentCopy {
    const val Title = "Settings"
    const val Menu = "Navigation"
    const val Shades = "Accent shades"
    const val On = "On"
    const val Off = "Off"
    const val NoticeTitle = "Your theme, your colors"
    const val NoticeMessage = "Accent color follows the seed of this theme on every page."
    const val LegendTitle = "Where your accent goes"
    const val LegendNote = "Only the accent follows your theme. Fluent keeps its neutrals, text colors and " +
        "system colors fixed."
}

/**
 * The pages the navigation lists, in order.
 */
internal enum class FluentPage(
    val label: String,
    val icon: ImageVector,
) {
    Home("Home", Icons.Regular.Home),
    System("System", Icons.Regular.Settings),
    Personalization("Personalization", Icons.Regular.Image),
    Apps("Apps", Icons.Regular.Folder),
    Accounts("Accounts", Icons.Regular.Person),
    Privacy("Privacy & security", Icons.Regular.Key),
    Update("Windows Update", Icons.Regular.ArrowSync),
}

/**
 * The page the app opens on.
 */
internal val FluentStartPage: FluentPage = FluentPage.Personalization

/**
 * The page [DemoAppState] has as current.
 */
internal fun DemoAppState.fluentPage(): FluentPage =
    FluentPage.entries[choice(FluentPageChoice, FluentPage.entries.size, FluentStartPage.ordinal)]

/**
 * Make [page] the current one.
 */
internal fun DemoAppState.openFluentPage(page: FluentPage) {
    choose(FluentPageChoice, FluentPage.entries.size, page.ordinal)
}

/**
 * One setting that turns on and off.
 *
 * @property[key] Where [DemoAppState] keeps whether it has been flipped from [onAtFirst].
 * @property[onAtFirst] Whether it is on before anyone flips it.
 */
internal enum class FluentSetting(
    val title: String,
    val caption: String,
    val onAtFirst: Boolean,
) {
    Transparency("Transparency effects", "Windows and surfaces appear translucent", onAtFirst = true),
    AccentOnStart("Show accent color on Start and taskbar", "Uses the accent for the taskbar", onAtFirst = false),
    AccentOnTitleBars("Show accent color on title bars", "And on window borders", onAtFirst = true),
    Badges("Show badges on taskbar apps", "Counts for unread mail and messages", onAtFirst = true),
    AutoHide("Automatically hide the taskbar", "It slides back when you point at it", onAtFirst = false),
    Flashing("Show flashing on taskbar apps", "When an app needs you", onAtFirst = true),
    ;

    val key: String get() = "fluent.flipped.$name"
}

/**
 * Whether [setting] is on.
 */
internal fun DemoAppState.isOn(setting: FluentSetting): Boolean = isOn(setting.key) != setting.onAtFirst

/**
 * Turn [setting] on or off.
 */
internal fun DemoAppState.setOn(
    setting: FluentSetting,
    on: Boolean,
) {
    setOn(setting.key, on != setting.onAtFirst)
}

/**
 * A setting group that opens to more settings.
 *
 * @property[key] Where [DemoAppState] keeps whether it is open.
 */
internal enum class FluentGroup(
    val title: String,
    val caption: String,
    val icon: ImageVector,
    val settings: List<FluentSetting>,
) {
    AccentColor(
        title = "Accent color",
        caption = "Picked from your theme",
        icon = Icons.Regular.Star,
        settings = listOf(FluentSetting.AccentOnStart, FluentSetting.AccentOnTitleBars),
    ),
    TaskbarBehaviors(
        title = "Taskbar behaviors",
        caption = "Badges, flashing and hiding",
        icon = Icons.Regular.Alert,
        settings = listOf(FluentSetting.AutoHide, FluentSetting.Flashing),
    ),
    ;

    val key: String get() = "fluent.open.$name"
}

/**
 * One entry of the settings page, a heading, a notice, a group or a single setting.
 */
internal sealed interface FluentEntry {
    /**
     * The words over a section.
     */
    data class Heading(
        val title: String,
    ) : FluentEntry

    /**
     * The informational bar at the top of the page.
     */
    data object Notice : FluentEntry

    /**
     * A group that opens.
     */
    data class Group(
        val group: FluentGroup,
    ) : FluentEntry

    /**
     * A row with a switch.
     */
    data class Toggle(
        val setting: FluentSetting,
    ) : FluentEntry
}

/**
 * The settings page from the top, a notice and then two sections of cards.
 */
internal val FluentEntries: List<FluentEntry> = listOf(
    FluentEntry.Notice,
    FluentEntry.Heading("Colors"),
    FluentEntry.Toggle(FluentSetting.Transparency),
    FluentEntry.Group(FluentGroup.AccentColor),
    FluentEntry.Heading("Taskbar"),
    FluentEntry.Toggle(FluentSetting.Badges),
    FluentEntry.Group(FluentGroup.TaskbarBehaviors),
)

/**
 * Where the real Fluent controls paint each shade in each mode, from how compose-fluent builds its
 * colors out of the seven shades.
 */
internal fun FluentShade.usage(isDark: Boolean): String =
    when (this) {
        FluentShade.Dark3 -> if (isDark) "Not used in dark mode" else "Secondary accent text"
        FluentShade.Dark2 -> if (isDark) "Accent acrylic backdrop" else "Accent text and links"
        FluentShade.Dark1 -> if (isDark) "Accent acrylic fill" else AccentFillUse
        FluentShade.Base -> "Selected text highlight"
        FluentShade.Light1 -> "Not drawn by any control today"
        FluentShade.Light2 -> if (isDark) AccentFillUse else "Not used in light mode"
        FluentShade.Light3 -> if (isDark) "Accent text and links" else "Accent acrylic fill"
    }

private const val AccentFillUse = "Switches that are on, accent buttons, the current page"
