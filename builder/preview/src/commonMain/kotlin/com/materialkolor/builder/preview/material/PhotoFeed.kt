package com.materialkolor.builder.preview.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.CloudUpload
import com.composables.icons.lucide.Lucide
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.persist.DeviceWidth
import com.materialkolor.builder.kit.control.foldedChoiceName
import com.materialkolor.builder.kit.control.foldedSelectedName
import com.materialkolor.builder.kit.motion.LocalMotionFrozen
import com.materialkolor.builder.kit.motion.LocalReducedMotion
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose
import androidx.compose.ui.semantics.Role as SemanticsRole

/** The room under the feed the create button needs, so the last row can scroll clear of it. */
private val CreateClearance = 96.dp
private val GridGap = 4.dp
private val GridShape = RoundedCornerShape(12.dp)
private val BackupShape = RoundedCornerShape(24.dp)
private val BackupBadgeSize = 48.dp
private val ViewsMaxWidth = 480.dp

/** How the feed fills a device, the grid's columns and the carousel's largest memory. */
private class FeedMeasures(
    val columns: Int,
    val memoryWidth: Dp,
    val memoryHeight: Dp,
)

private fun measuresFor(deviceWidth: DeviceWidth): FeedMeasures =
    when (deviceWidth) {
        DeviceWidth.Phone -> FeedMeasures(columns = 3, memoryWidth = 200.dp, memoryHeight = 200.dp)
        DeviceWidth.Tablet -> FeedMeasures(columns = 4, memoryWidth = 280.dp, memoryHeight = 220.dp)
        DeviceWidth.Desktop -> FeedMeasures(columns = 6, memoryWidth = 320.dp, memoryHeight = 240.dp)
    }

/**
 * The library the photo app scrolls through. Memories in a carousel, the button group that picks a
 * view, the backup card with its wavy progress, and the grid of the view's photos.
 *
 * @param[state] What the app remembers, shared by both copies.
 * @param[list] The feed's list state, mirrored between the copies.
 * @param[deviceWidth] The device the feed lays itself out for.
 * @param[modifier] Applied to the list.
 */
@Composable
internal fun PhotoFeed(
    state: DemoAppState,
    list: LazyListState,
    deviceWidth: DeviceWidth,
    modifier: Modifier = Modifier,
) {
    val measures = measuresFor(deviceWidth)
    val view = PhotoView.entries[state.choice(PhotoViewChoice, PhotoView.entries.size)]
    LazyColumn(
        state = list,
        modifier = modifier,
        contentPadding = PaddingValues(bottom = CreateClearance),
    ) {
        item(key = "memories") { MemoryCarousel(state, measures) }
        item(key = "views") { PhotoViews(state, view) }
        item(key = "backup") { BackupCard(Modifier.padding(horizontal = SectionGap, vertical = Gap)) }
        photoGrid(view, measures.columns)
    }
}

/**
 * The featured memory up front and the next ones shrinking behind it. A tap features a memory, and
 * each copy scrolls its own carousel to the one the demo state holds, so a split agrees. Dragging
 * is off, since a drag would move one copy only.
 */
@Composable
private fun MemoryCarousel(
    state: DemoAppState,
    measures: FeedMeasures,
) {
    val count = PhotoMemories.size
    val featured = state.choice(PhotoMemoryChoice, count)
    val carousel = rememberCarouselState(initialItem = featured) { count }
    val still = LocalMotionFrozen.current || LocalReducedMotion.current
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(carousel, featured, still) {
        if (still) carousel.scrollToItem(featured) else carousel.animateScrollToItem(featured, spatial)
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = PhotoCopy.Memories,
            modifier = Modifier.padding(horizontal = SectionGap, vertical = PaneGap),
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalMultiBrowseCarousel(
            state = carousel,
            preferredItemWidth = measures.memoryWidth,
            modifier = Modifier.fillMaxWidth().height(measures.memoryHeight),
            itemSpacing = Gap,
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = SectionGap),
        ) { index ->
            val memory = PhotoMemories[index]
            val selected = index == featured
            val info = carouselItemDrawInfo
            PhotoArt(
                tint = memory.tint,
                motif = memory.motif,
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .previewRoles(memory.tint.light, memory.tint.strong, memory.tint.ink)
                    .selectable(selected = selected) { state.choose(PhotoMemoryChoice, count, index) }
                    .foldedSelectedName(memory.title, selected),
            ) {
                Text(
                    text = memory.title,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(SectionGap)
                        .graphicsLayer {
                            // The title fades out as the memory shrinks toward the edge.
                            val range = info.maxSize - info.minSize
                            alpha = if (range > 0f) ((info.size - info.minSize) / range).coerceIn(0f, 1f) else 1f
                        },
                    color = MaterialTheme.colorScheme.tile(memory.tint).ink,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

/** A connected row of toggle buttons that picks the view, each turning round as it turns on. */
@Composable
private fun PhotoViews(
    state: DemoAppState,
    current: PhotoView,
) {
    val count = PhotoView.entries.size
    Row(
        modifier = Modifier
            .padding(horizontal = SectionGap, vertical = Gap)
            .widthIn(max = ViewsMaxWidth)
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        for (view in PhotoView.entries) {
            val checked = view == current
            ToggleButton(
                checked = checked,
                onCheckedChange = { state.choose(PhotoViewChoice, count, view.ordinal) },
                modifier = Modifier
                    .weight(1f)
                    .previewRoles(ExpressiveComponent.ToggleButton)
                    .semantics { role = SemanticsRole.RadioButton }
                    .foldedChoiceName(view.label, checked),
                shapes = when (view.ordinal) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(view.label)
            }
        }
    }
}

/** The backup under way on a low container, its wavy progress holding still, and the two photos that failed. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackupCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(BackupShape)
            .background(colors.surfaceContainerLow)
            .previewRoles(Role.SurfaceContainerLow, Role.OnSurface, Role.OnSurfaceVariant)
            .padding(SectionGap),
        verticalArrangement = Arrangement.spacedBy(PaneGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PaneGap)) {
            Box(
                modifier = Modifier
                    .size(BackupBadgeSize)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(colors.surfaceContainerHighest)
                    .previewRoles(Role.SurfaceContainerHighest, Role.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.CloudUpload, contentDescription = null, tint = colors.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(PhotoCopy.BackingUp, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = PhotoCopy.BackupCount,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedButton(
                onClick = {},
                modifier = Modifier.previewRoles(Role.Outline, Role.OnSurfaceVariant),
                border = BorderStroke(1.dp, colors.outline),
            ) {
                Text(PhotoCopy.Pause)
            }
        }
        LinearWavyProgressIndicator(
            progress = { BackupProgress },
            modifier = Modifier.fillMaxWidth().previewRoles(ExpressiveComponent.LinearWavyProgressIndicator),
            waveSpeed = 0.dp,
        )
        Row(
            modifier = Modifier.previewRoles(Role.Error),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Gap),
        ) {
            Icon(Lucide.CircleAlert, contentDescription = null, tint = colors.error)
            Text(
                text = PhotoCopy.BackupFailed,
                modifier = Modifier.weight(1f),
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = {}, modifier = Modifier.previewRoles(MaterialComponent.TextButton)) {
                Text(PhotoCopy.Retry)
            }
        }
    }
}

/** The photos [view] shows, [columns] to a row. */
private fun LazyListScope.photoGrid(
    view: PhotoView,
    columns: Int,
) {
    val rows = view.photos().chunked(columns)
    items(count = rows.size, key = { row -> "photos.${view.name}.$row" }) { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = SectionGap, vertical = GridGap / 2),
            horizontalArrangement = Arrangement.spacedBy(GridGap),
        ) {
            for (photo in rows[row]) {
                PhotoArt(
                    tint = photo.tint,
                    motif = photo.motif,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(GridShape)
                        .previewRoles(photo.tint.light, photo.tint.strong),
                )
            }
            repeat(columns - rows[row].size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/**
 * A picture drawn from the scheme, no image behind it. A gradient from the light role of [tint] to
 * its strong one, with the Material shape of [motif] in the middle in the light role, so it moves
 * with the theme and Inspect can name every color.
 */
@Composable
private fun PhotoArt(
    tint: PhotoTint,
    motif: PhotoMotif,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme.tile(tint)
    Box(modifier.background(Brush.linearGradient(listOf(colors.light, colors.strong)))) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxHeight(0.45f)
                .aspectRatio(1f)
                .clip(motif.shape())
                .background(colors.light),
        )
        content()
    }
}

/** The colors of a [PhotoTint] in one scheme. */
@Immutable
private class TileColors(
    val light: Color,
    val strong: Color,
    val ink: Color,
)

private fun ColorScheme.tile(tint: PhotoTint): TileColors =
    when (tint) {
        PhotoTint.Primary -> TileColors(primaryContainer, primary, onPrimaryContainer)
        PhotoTint.Secondary -> TileColors(secondaryContainer, secondary, onSecondaryContainer)
        PhotoTint.Tertiary -> TileColors(tertiaryContainer, tertiary, onTertiaryContainer)
        PhotoTint.Neutral -> TileColors(surfaceContainerHigh, outline, onSurface)
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PhotoMotif.shape(): Shape =
    when (this) {
        PhotoMotif.Sun -> MaterialShapes.Sunny
        PhotoMotif.Snowflake -> MaterialShapes.Cookie12Sided
        PhotoMotif.Flower -> MaterialShapes.Flower
        PhotoMotif.Moon -> MaterialShapes.SemiCircle
        PhotoMotif.Clover -> MaterialShapes.Clover4Leaf
        PhotoMotif.Arch -> MaterialShapes.Arch
        PhotoMotif.Gem -> MaterialShapes.Gem
        PhotoMotif.Heart -> MaterialShapes.Heart
        PhotoMotif.Burst -> MaterialShapes.SoftBurst
    }.toShape()
