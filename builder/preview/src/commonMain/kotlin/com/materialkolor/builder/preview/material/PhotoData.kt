package com.materialkolor.builder.preview.material

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Album
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.FolderPlus
import com.composables.icons.lucide.Images
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Users
import com.materialkolor.builder.domain.model.Role

// The photo app's sample content. All of it is provisional copy for the owner to replace.

/**
 * The memory the carousel features, a choice in the demo state both copies scroll to.
 */
internal const val PhotoMemoryChoice = "photo.memory"

/**
 * Which photos the button group shows, a choice in the demo state.
 */
internal const val PhotoViewChoice = "photo.view"

/**
 * Whether the create menu is open, a switch in the demo state.
 */
internal const val PhotoCreateSwitch = "photo.create"

/**
 * The photo feed, a list both copies keep at the same place.
 */
internal const val PhotoFeedList = "photo.feed"

/**
 * How far the backup has got, 214 of 356.
 */
internal const val BackupProgress = 214f / 356f

/**
 * The words the photo app shows outside its lists.
 */
internal object PhotoCopy {
    const val Title = "Photos"
    const val Subtitle = "1,248 photos, 36 albums"
    const val Search = "Search photos"
    const val Memories = "Memories"
    const val Create = "Create"
    const val BackingUp = "Backing up"
    const val BackupCount = "214 of 356 photos"
    const val BackupFailed = "2 photos could not upload"
    const val Pause = "Pause"
    const val Retry = "Retry"
}

/**
 * The scheme roles a generated picture is painted in. Its gradient runs from [light] to [strong],
 * and whatever is written on it is in [ink] over the light end.
 */
internal enum class PhotoTint(
    val light: Role,
    val strong: Role,
    val ink: Role,
) {
    Primary(Role.PrimaryContainer, Role.Primary, Role.OnPrimaryContainer),
    Secondary(Role.SecondaryContainer, Role.Secondary, Role.OnSecondaryContainer),
    Tertiary(Role.TertiaryContainer, Role.Tertiary, Role.OnTertiaryContainer),
    Neutral(Role.SurfaceContainerHigh, Role.Outline, Role.OnSurface),
}

/**
 * The Material shape a generated picture shows in its middle, standing in for what was photographed.
 */
internal enum class PhotoMotif {
    Sun,
    Snowflake,
    Flower,
    Moon,
    Clover,
    Arch,
    Gem,
    Heart,
    Burst,
}

/**
 * A memory the carousel can feature.
 */
internal class PhotoMemory(
    val title: String,
    val tint: PhotoTint,
    val motif: PhotoMotif,
)

internal val PhotoMemories: List<PhotoMemory> = listOf(
    PhotoMemory("Lakeside", PhotoTint.Primary, PhotoMotif.Sun),
    PhotoMemory("First snow", PhotoTint.Neutral, PhotoMotif.Snowflake),
    PhotoMemory("Market day", PhotoTint.Tertiary, PhotoMotif.Flower),
    PhotoMemory("Night swim", PhotoTint.Secondary, PhotoMotif.Moon),
    PhotoMemory("Harvest", PhotoTint.Primary, PhotoMotif.Clover),
    PhotoMemory("Old town", PhotoTint.Tertiary, PhotoMotif.Arch),
)

/**
 * One photo of the library grid.
 */
internal class Photo(
    val tint: PhotoTint,
    val motif: PhotoMotif,
    val favourite: Boolean = false,
    val shared: Boolean = false,
)

private val PhotoLibrary: List<Photo> = listOf(
    Photo(PhotoTint.Tertiary, PhotoMotif.Flower, favourite = true),
    Photo(PhotoTint.Primary, PhotoMotif.Sun, shared = true),
    Photo(PhotoTint.Secondary, PhotoMotif.Moon, favourite = true, shared = true),
    Photo(PhotoTint.Neutral, PhotoMotif.Snowflake),
    Photo(PhotoTint.Primary, PhotoMotif.Gem, favourite = true),
    Photo(PhotoTint.Tertiary, PhotoMotif.Heart, favourite = true, shared = true),
    Photo(PhotoTint.Secondary, PhotoMotif.Burst),
    Photo(PhotoTint.Primary, PhotoMotif.Clover, favourite = true),
    Photo(PhotoTint.Neutral, PhotoMotif.Arch, shared = true),
    Photo(PhotoTint.Tertiary, PhotoMotif.Sun, favourite = true),
    Photo(PhotoTint.Secondary, PhotoMotif.Flower, shared = true),
    Photo(PhotoTint.Primary, PhotoMotif.Moon, favourite = true),
)

/**
 * The views the button group picks between, each a slice of the library.
 */
internal enum class PhotoView(
    val label: String,
) {
    Recent("Recent"),
    Favourites("Favourites"),
    Shared("Shared"),
    ;

    /**
     * The photos this view shows, newest first.
     */
    fun photos(): List<Photo> =
        when (this) {
            Recent -> PhotoLibrary
            Favourites -> PhotoLibrary.filter { photo -> photo.favourite }
            Shared -> PhotoLibrary.filter { photo -> photo.shared }
        }
}

/**
 * Where the bottom bar or the rail can go. The library is the one the app has open.
 */
internal enum class PhotoDestination(
    val label: String,
    val icon: ImageVector,
) {
    Photos("Photos", Lucide.Images),
    Albums("Albums", Lucide.Album),
    Sharing("Sharing", Lucide.Users),
}

/**
 * What the create menu offers.
 */
internal enum class PhotoCreate(
    val label: String,
    val icon: ImageVector,
) {
    Camera("Camera", Lucide.Camera),
    Album("New album", Lucide.FolderPlus),
    Collage("Collage", Lucide.LayoutGrid),
}
