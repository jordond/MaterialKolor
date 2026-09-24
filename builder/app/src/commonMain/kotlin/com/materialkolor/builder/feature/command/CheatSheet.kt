package com.materialkolor.builder.feature.command

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.materialkolor.builder.feature.about.ABOUT_HEIGHT_FRACTION
import com.materialkolor.builder.generated.resources.Res
import com.materialkolor.builder.generated.resources.command_cheat_sheet_close
import com.materialkolor.builder.generated.resources.command_group_general
import com.materialkolor.builder.generated.resources.command_group_preview
import com.materialkolor.builder.generated.resources.command_group_project
import com.materialkolor.builder.generated.resources.command_group_theme
import com.materialkolor.builder.generated.resources.command_key_add_image
import com.materialkolor.builder.generated.resources.command_key_appearance
import com.materialkolor.builder.generated.resources.command_key_cheat_sheet
import com.materialkolor.builder.generated.resources.command_key_copy_all
import com.materialkolor.builder.generated.resources.command_key_copy_link
import com.materialkolor.builder.generated.resources.command_key_copy_seed
import com.materialkolor.builder.generated.resources.command_key_device_width
import com.materialkolor.builder.generated.resources.command_key_esc
import com.materialkolor.builder.generated.resources.command_key_escape
import com.materialkolor.builder.generated.resources.command_key_export
import com.materialkolor.builder.generated.resources.command_key_fullscreen
import com.materialkolor.builder.generated.resources.command_key_hue_lock
import com.materialkolor.builder.generated.resources.command_key_inspect
import com.materialkolor.builder.generated.resources.command_key_library_1
import com.materialkolor.builder.generated.resources.command_key_library_2
import com.materialkolor.builder.generated.resources.command_key_library_3
import com.materialkolor.builder.generated.resources.command_key_library_4
import com.materialkolor.builder.generated.resources.command_key_library_5
import com.materialkolor.builder.generated.resources.command_key_new_project
import com.materialkolor.builder.generated.resources.command_key_next_tab
import com.materialkolor.builder.generated.resources.command_key_palette
import com.materialkolor.builder.generated.resources.command_key_poster
import com.materialkolor.builder.generated.resources.command_key_preview_mode
import com.materialkolor.builder.generated.resources.command_key_previous_tab
import com.materialkolor.builder.generated.resources.command_key_projects
import com.materialkolor.builder.generated.resources.command_key_redo
import com.materialkolor.builder.generated.resources.command_key_save
import com.materialkolor.builder.generated.resources.command_key_shuffle
import com.materialkolor.builder.generated.resources.command_key_style_lock
import com.materialkolor.builder.generated.resources.command_key_undo
import com.materialkolor.builder.generated.resources.command_screen_reader_note
import com.materialkolor.builder.generated.resources.command_single_keys
import com.materialkolor.builder.generated.resources.command_single_keys_note
import com.materialkolor.builder.generated.resources.topbar_shortcuts
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderDialog
import com.materialkolor.builder.kit.control.BuilderScrollArea
import com.materialkolor.builder.kit.control.BuilderSwitch
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The shortcut cheat sheet (F-34), every shortcut in the keymap grouped the way the spec's keyboard
 * map runs, written with this platform's Cmd or Ctrl.
 *
 * It holds the switch that turns single-key shortcuts off (WCAG 2.1.4) and one line for screen
 * reader users, whose single keys only reach the page in focus mode. While a panel such as this one
 * is open the dialog owns the keyboard, so Cmd or Ctrl+K does nothing in v1.
 *
 * @param[singleKeys] Whether single-key shortcuts are on.
 * @param[returnFocusTo] Where focus goes once it closes, the overflow button that opened it, or the
 * page's focus holder when `?` did (AR-09).
 */
@Composable
internal fun CheatSheet(
    visible: Boolean,
    singleKeys: Boolean,
    onSingleKeysChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    returnFocusTo: FocusRequester? = null,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val layout = LocalLayout.current
    val apple = LocalAppleKeys.current
    BuilderDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.topbar_shortcuts),
        modifier = modifier,
        returnFocusTo = returnFocusTo,
        actions = {
            BuilderButton(
                onClick = onDismissRequest,
                label = stringResource(Res.string.command_cheat_sheet_close),
                emphasis = Emphasis.Primary,
            )
        },
    ) {
        BuilderScrollArea(Modifier.heightIn(max = layout.heightDp * ABOUT_HEIGHT_FRACTION)) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                    BuilderSwitch(
                        checked = singleKeys,
                        onCheckedChange = onSingleKeysChange,
                        label = stringResource(Res.string.command_single_keys),
                    )
                    BuilderText(text = stringResource(Res.string.command_single_keys_note), emphasis = Emphasis.Secondary)
                    BuilderText(text = stringResource(Res.string.command_screen_reader_note), emphasis = Emphasis.Secondary)
                }
                ShortcutGroup.entries.forEach { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                        BuilderText(
                            text = stringResource(groupTitle(group)),
                            modifier = Modifier.semantics { heading() },
                            style = BuilderTextStyle.SectionLabel,
                        )
                        Shortcut.entries.filter { shortcut -> shortcut.group == group }.forEach { shortcut ->
                            ShortcutRow(stringResource(shortcutLabel(shortcut)), shortcut.text(apple))
                        }
                        if (group == ShortcutGroup.General) {
                            val esc = stringResource(Res.string.command_key_esc)
                            ShortcutRow(stringResource(Res.string.command_key_escape), esc)
                        }
                    }
                }
            }
        }
    }
}

/** One shortcut, what it does and its keys, read out together. */
@Composable
private fun ShortcutRow(
    label: String,
    keys: String,
) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(LocalBuilderTokens.current.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuilderText(text = label, modifier = Modifier.weight(1f))
        BuilderText(text = keys, emphasis = Emphasis.Secondary)
    }
}

private fun groupTitle(group: ShortcutGroup): StringResource =
    when (group) {
        ShortcutGroup.General -> Res.string.command_group_general
        ShortcutGroup.Theme -> Res.string.command_group_theme
        ShortcutGroup.Preview -> Res.string.command_group_preview
        ShortcutGroup.Project -> Res.string.command_group_project
    }

/** What [shortcut] does, as the cheat sheet says it. */
internal fun shortcutLabel(shortcut: Shortcut): StringResource =
    when (shortcut) {
        Shortcut.Palette -> Res.string.command_key_palette
        Shortcut.CheatSheet -> Res.string.command_key_cheat_sheet
        Shortcut.Shuffle -> Res.string.command_key_shuffle
        Shortcut.HueLock -> Res.string.command_key_hue_lock
        Shortcut.StyleLock -> Res.string.command_key_style_lock
        Shortcut.Library1 -> Res.string.command_key_library_1
        Shortcut.Library2 -> Res.string.command_key_library_2
        Shortcut.Library3 -> Res.string.command_key_library_3
        Shortcut.Library4 -> Res.string.command_key_library_4
        Shortcut.Library5 -> Res.string.command_key_library_5
        Shortcut.PreviewMode -> Res.string.command_key_preview_mode
        Shortcut.Appearance -> Res.string.command_key_appearance
        Shortcut.PreviousTab -> Res.string.command_key_previous_tab
        Shortcut.NextTab -> Res.string.command_key_next_tab
        Shortcut.Export -> Res.string.command_key_export
        Shortcut.CopySeed -> Res.string.command_key_copy_seed
        Shortcut.CopyAll -> Res.string.command_key_copy_all
        Shortcut.CopyLink -> Res.string.command_key_copy_link
        Shortcut.Save -> Res.string.command_key_save
        Shortcut.Undo -> Res.string.command_key_undo
        Shortcut.Redo -> Res.string.command_key_redo
        Shortcut.Projects -> Res.string.command_key_projects
        Shortcut.NewProject -> Res.string.command_key_new_project
        Shortcut.Inspect -> Res.string.command_key_inspect
        Shortcut.AddImage -> Res.string.command_key_add_image
        Shortcut.DeviceWidth -> Res.string.command_key_device_width
        Shortcut.Fullscreen -> Res.string.command_key_fullscreen
        Shortcut.Poster -> Res.string.command_key_poster
    }
