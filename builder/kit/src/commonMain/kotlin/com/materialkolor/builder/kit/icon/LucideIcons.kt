package com.materialkolor.builder.kit.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ChevronsDownUp
import com.composables.icons.lucide.ChevronsUpDown
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CircleHelp
import com.composables.icons.lucide.Columns2
import com.composables.icons.lucide.Command
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.Fullscreen
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.Keyboard
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.LockOpen
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Moon
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.Pipette
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Redo2
import com.composables.icons.lucide.ScanSearch
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Share
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Shuffle
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Sun
import com.composables.icons.lucide.Tablet
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.TriangleAlert
import com.composables.icons.lucide.Undo2
import com.composables.icons.lucide.Upload
import com.composables.icons.lucide.X

/**
 * The Lucide set, from `com.composables:icons-lucide`.
 *
 * Several Lucide names differ from the builder's, for example [IconId.Eyedropper] is `Pipette`
 * and [IconId.More] is `Ellipsis`.
 */
public object LucideIcons : BuilderIcons {
    override fun get(id: IconId): ImageVector =
        when (id) {
            IconId.Undo -> Lucide.Undo2
            IconId.Redo -> Lucide.Redo2
            IconId.Share -> Lucide.Share2
            IconId.Export -> Lucide.Share
            IconId.Copy -> Lucide.Copy
            IconId.Check -> Lucide.Check
            IconId.Close -> Lucide.X
            IconId.Search -> Lucide.Search
            IconId.Command -> Lucide.Command
            IconId.Shuffle -> Lucide.Shuffle
            IconId.Eyedropper -> Lucide.Pipette
            IconId.Image -> Lucide.Image
            IconId.Upload -> Lucide.Upload
            IconId.Lock -> Lucide.Lock
            IconId.Unlock -> Lucide.LockOpen
            IconId.Pin -> Lucide.Pin
            IconId.Info -> Lucide.Info
            IconId.ChevronDown -> Lucide.ChevronDown
            IconId.ChevronLeft -> Lucide.ChevronLeft
            IconId.ChevronRight -> Lucide.ChevronRight
            IconId.Sun -> Lucide.Sun
            IconId.Moon -> Lucide.Moon
            IconId.Split -> Lucide.Columns2
            IconId.Phone -> Lucide.Smartphone
            IconId.Tablet -> Lucide.Tablet
            IconId.Desktop -> Lucide.Monitor
            IconId.Inspect -> Lucide.ScanSearch
            IconId.Vision -> Lucide.Eye
            IconId.Fullscreen -> Lucide.Fullscreen
            IconId.More -> Lucide.Ellipsis
            IconId.Plus -> Lucide.Plus
            IconId.Trash -> Lucide.Trash2
            IconId.Folder -> Lucide.Folder
            IconId.Download -> Lucide.Download
            IconId.Warning -> Lucide.TriangleAlert
            IconId.Error -> Lucide.CircleAlert
            IconId.Collapse -> Lucide.ChevronsDownUp
            IconId.Expand -> Lucide.ChevronsUpDown
            IconId.Keyboard -> Lucide.Keyboard
            IconId.Help -> Lucide.CircleHelp
            IconId.ExternalLink -> Lucide.ExternalLink
        }
}
