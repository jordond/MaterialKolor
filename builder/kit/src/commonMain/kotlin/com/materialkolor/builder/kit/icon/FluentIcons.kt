package com.materialkolor.builder.kit.icon

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.regular.Add
import io.github.composefluent.icons.regular.ArrowDownload
import io.github.composefluent.icons.regular.ArrowExpand
import io.github.composefluent.icons.regular.ArrowSync
import io.github.composefluent.icons.regular.Checkmark
import io.github.composefluent.icons.regular.ChevronDown
import io.github.composefluent.icons.regular.ChevronLeft
import io.github.composefluent.icons.regular.ChevronRight
import io.github.composefluent.icons.regular.Copy
import io.github.composefluent.icons.regular.Delete
import io.github.composefluent.icons.regular.Dismiss
import io.github.composefluent.icons.regular.ErrorCircle
import io.github.composefluent.icons.regular.Eye
import io.github.composefluent.icons.regular.Folder
import io.github.composefluent.icons.regular.Image
import io.github.composefluent.icons.regular.Info
import io.github.composefluent.icons.regular.MoreHorizontal
import io.github.composefluent.icons.regular.Open
import io.github.composefluent.icons.regular.Options
import io.github.composefluent.icons.regular.Pin
import io.github.composefluent.icons.regular.Search
import io.github.composefluent.icons.regular.Share
import io.github.composefluent.icons.regular.Warning

/**
 * The Fluent set, the regular Fluent System Icons from `fluent-icons-core`.
 *
 * That set is the small one Fluent's own components need, so an id it has no glyph for, such as
 * [IconId.Undo] or [IconId.Sun], borrows the Lucide one rather than pull in the whole extended set.
 */
internal object FluentIcons : BuilderIcons {
    override fun get(id: IconId): ImageVector =
        when (id) {
            IconId.Share -> Icons.Regular.Share
            IconId.Copy -> Icons.Regular.Copy
            IconId.Check -> Icons.Regular.Checkmark
            IconId.Close -> Icons.Regular.Dismiss
            IconId.Search -> Icons.Regular.Search
            IconId.Image -> Icons.Regular.Image
            IconId.Pin -> Icons.Regular.Pin
            IconId.Info -> Icons.Regular.Info
            IconId.InfoOutline -> Icons.Regular.Info
            IconId.ChevronDown -> Icons.Regular.ChevronDown
            IconId.ChevronLeft -> Icons.Regular.ChevronLeft
            IconId.ChevronRight -> Icons.Regular.ChevronRight
            IconId.Vision -> Icons.Regular.Eye
            IconId.Fullscreen -> Icons.Regular.ArrowExpand
            IconId.More -> Icons.Regular.MoreHorizontal
            IconId.Plus -> Icons.Regular.Add
            IconId.Trash -> Icons.Regular.Delete
            IconId.Folder -> Icons.Regular.Folder
            IconId.Download -> Icons.Regular.ArrowDownload
            IconId.Warning -> Icons.Regular.Warning
            IconId.Error -> Icons.Regular.ErrorCircle
            IconId.ExternalLink -> Icons.Regular.Open
            IconId.Sliders -> Icons.Regular.Options
            IconId.Progress -> Icons.Regular.ArrowSync
            IconId.Undo,
            IconId.Redo,
            IconId.Export,
            IconId.Command,
            IconId.Shuffle,
            IconId.Eyedropper,
            IconId.Upload,
            IconId.Lock,
            IconId.Unlock,
            IconId.Sun,
            IconId.Moon,
            IconId.Split,
            IconId.Phone,
            IconId.Tablet,
            IconId.Desktop,
            IconId.Inspect,
            IconId.Collapse,
            IconId.Expand,
            IconId.Keyboard,
            IconId.Help,
            IconId.History,
            IconId.Code,
            -> LucideIcons[id]
        }
}
