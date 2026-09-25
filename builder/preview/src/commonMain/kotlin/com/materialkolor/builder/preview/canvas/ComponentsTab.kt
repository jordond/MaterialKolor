package com.materialkolor.builder.preview.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.domain.model.Library
import com.materialkolor.builder.kit.skin.LocalSkin
import com.materialkolor.builder.preview.custom.CustomGalleryEntry
import com.materialkolor.builder.preview.fluent.FluentGalleryEntry
import com.materialkolor.builder.preview.material.MaterialGalleryEntry
import com.materialkolor.builder.preview.split.LocalCompositionProbe
import com.materialkolor.builder.preview.split.PaneSpec
import com.materialkolor.builder.preview.unstyled.UnstyledGalleryEntry

/** The narrowest a gallery card gets (F-21). The grid fits as many columns above it as it can. */
private val MinCardWidth = 280.dp

// b-513

/**
 * The room under the grid's last row the floating dock takes, the dock's 64 dp and the 16 dp it
 * floats above the canvas edge, so the last row scrolls clear of it.
 */
private val DockClearance = 80.dp

/** What each gallery card tells [LocalCompositionProbe] as it composes, followed by its title. */
internal const val GALLERY_CARD: String = "GalleryCard/"

/** What a gallery grid tells [LocalCompositionProbe] each time the scope that calls its lazy column composes. */
internal const val GALLERY_GRID: String = "GalleryGrid"

/**
 * The Components tab, a curated gallery of the library [LocalSkin] names.
 *
 * Call it inside a [PreviewPane] for [spec], once per copy of a split.
 *
 * @param[spec] The pane the gallery is drawn in.
 * @param[state] What the gallery's controls remember, shared by both copies.
 * @param[modifier] Applied to the gallery.
 */
@Composable
public fun ComponentsTab(
    spec: PaneSpec,
    state: DemoAppState,
    modifier: Modifier = Modifier,
) {
    val skin = LocalSkin.current
    when (skin.library) {
        Library.Material3 -> MaterialGalleryEntry(spec, state, skin.expressive, modifier)
        Library.Unstyled -> UnstyledGalleryEntry(spec, state, modifier)
        Library.Fluent -> FluentGalleryEntry(spec, state, modifier)
        Library.Custom -> CustomGalleryEntry(spec, state, modifier)
    }
}

/** The six groups every gallery sorts its cards into (F-21), in the order they show. */
internal enum class GalleryGroup {
    Actions,
    Inputs,
    Selection,
    Containment,
    Navigation,
    Feedback,
}

/**
 * One card of a gallery, a component shown enabled and disabled.
 *
 * @property[title] What the card is called, unique within its gallery.
 * @property[group] The group the card sits under.
 * @property[content] The component, drawn from the state both copies of a split share.
 */
@Immutable
internal class GalleryCard(
    val title: String,
    val group: GalleryGroup,
    val content: @Composable (state: DemoAppState) -> Unit,
)

/**
 * A gallery's cards under their group headers, in as many columns as fit (F-21).
 *
 * The grid is a lazy column of rows, so only the rows on screen compose, and its scroll position
 * can come from [DemoAppState.rememberListState], which keeps both copies of a split at the same
 * place. A lazy grid has no such mirror. Every card takes an equal share of its row and none gets
 * narrower than 280 dp unless the pane itself is. The cards of a row stretch to the tallest one, so
 * a row leaves no holes, and the grid ends clear of the floating dock.
 *
 * It measures its room itself rather than through `BoxWithConstraints`, and the lazy column reads
 * the column count as it builds its rows. So a canvas that changes width every frame, as it does
 * while the poster rail opens or shuts, only lays the grid out again, and the rows are rebuilt
 * only when the count changes.
 *
 * @param[cards] The cards, in the order they show within each group.
 * @param[listState] Where the gallery is scrolled to.
 * @param[gap] The space around the grid and between its cards.
 * @param[modifier] Applied to the gallery.
 * @param[header] Draws the header of a group.
 * @param[card] Draws one card, with the modifier that sizes it to its column and its row.
 */
@Composable
internal fun GalleryGrid(
    cards: List<GalleryCard>,
    listState: LazyListState,
    gap: Dp,
    modifier: Modifier = Modifier,
    header: @Composable (group: GalleryGroup) -> Unit,
    card: @Composable (card: GalleryCard, modifier: Modifier) -> Unit,
) {
    val groups = remember(cards) {
        val byGroup = cards.groupBy { shown -> shown.group }
        GalleryGroup.entries.mapNotNull { group -> byGroup[group]?.let { members -> group to members } }
    }
    val room = remember { GalleryRoom() }
    // Fired right where the lazy column is called, so a scope around the call that recomposes as the
    // width moves, such as a BoxWithConstraints, fires it every frame. The rows' builder would not
    // show that, since it runs again only when the column count changes.
    LocalCompositionProbe.current?.invoke(GALLERY_GRID)
    LazyColumn(
        state = listState,
        modifier = modifier.measureColumns(room, gap).fillMaxSize(),
        contentPadding = PaddingValues(start = gap, top = gap, end = gap, bottom = gap + DockClearance), // b-513
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        // Read here, as the lazy column builds its rows, so a new count rebuilds them without a recomposition.
        val columns = room.columns
        for ((group, members) in groups) {
            item(key = "group.${group.name}", contentType = "header") { header(group) }
            for (row in members.chunked(columns)) {
                item(key = "cards.${row.first().title}", contentType = "cards") {
                    // b-513
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        for (shown in row) {
                            key(shown.title) { GalleryCell(shown, card, Modifier.weight(1f).fillMaxHeight()) }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** How many columns of cards the grid's room fits, noted as the grid measures. */
@Stable
private class GalleryRoom {
    var columns: Int by mutableIntStateOf(1)
}

/**
 * Notes in [room] how many columns fit the widest the grid may be, [gap] around and between them,
 * and otherwise measures as it would. The count only changes, and so only reaches its readers, when
 * a card would drop under 280 dp or a new one fits.
 */
private fun Modifier.measureColumns(
    room: GalleryRoom,
    gap: Dp,
): Modifier =
    layout { measurable, constraints ->
        if (constraints.hasBoundedWidth) {
            val width = constraints.maxWidth.toDp()
            room.columns = ((width - gap) / (MinCardWidth + gap)).toInt().coerceAtLeast(1)
        }
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

@Composable
private fun GalleryCell(
    card: GalleryCard,
    draw: @Composable (card: GalleryCard, modifier: Modifier) -> Unit,
    modifier: Modifier,
) {
    LocalCompositionProbe.current?.invoke(GALLERY_CARD + card.title)
    draw(card, modifier)
}

/**
 * Which of [count] options the gallery control called [group] has picked, [default] until someone
 * picks one.
 *
 * Every option is a switch of its own in [DemoAppState], so both copies of a split agree on it.
 */
internal fun DemoAppState.choice(
    group: String,
    count: Int,
    default: Int = 0,
): Int = (0 until count).firstOrNull { option -> isOn("$group.$option") } ?: default

/** Pick [option] of the [count] the gallery control called [group] offers, and drop the rest. */
internal fun DemoAppState.choose(
    group: String,
    count: Int,
    option: Int,
) {
    require(option in 0 until count) { "Option $option is not one of the $count in $group" }
    for (other in 0 until count) setOn("$group.$other", other == option)
}
