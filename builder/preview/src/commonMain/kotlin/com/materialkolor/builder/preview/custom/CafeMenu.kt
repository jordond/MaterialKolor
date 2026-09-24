package com.materialkolor.builder.preview.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Stamp
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.kit.control.BuilderButton
import com.materialkolor.builder.kit.control.BuilderCard
import com.materialkolor.builder.kit.control.BuilderChoiceChips
import com.materialkolor.builder.kit.control.BuilderListRow
import com.materialkolor.builder.kit.control.BuilderText
import com.materialkolor.builder.kit.control.BuilderTextStyle
import com.materialkolor.builder.kit.control.Emphasis
import com.materialkolor.builder.kit.icon.IconId
import com.materialkolor.builder.kit.layout.LocalLayout
import com.materialkolor.builder.kit.token.LocalBuilderTokens
import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.inspect.previewRoles

// The categories, the stamp card and the drinks, which every width shows in its own arrangement.

private val ThumbSize = 48.dp
private val BannerGlyphSize = 36.dp
private val HeartSize = 28.dp
private val DotSize = 10.dp
private val StampSize = 12.dp

/** The categories as a list with the stamp card under them, beside the menu on a tablet or desktop. */
@Composable
internal fun CategoryColumn(
    state: DemoAppState,
    colors: CafeColors,
    modifier: Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val picked = state.category
    LazyColumn(
        state = state.rememberListState("cafe.categories"),
        modifier = modifier,
        contentPadding = PaddingValues(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.extraSmall),
    ) {
        item(key = "label") { BuilderText(CafeCopy.Menu, style = BuilderTextStyle.SectionLabel) }
        items(CafeCategory.entries, key = { category -> category.name }) { category ->
            BuilderListRow(
                headline = category.label,
                modifier = Modifier.previewRoles(CustomComponent.ListRow),
                supporting = CafeCopy.drinks(category.items.size),
                onClick = { state.pick(category) },
                selected = category == picked,
                trailing = { CategoryDot(category, colors) },
            )
        }
        item(key = "stamps") { StampCard(colors, Modifier.padding(top = spacing.large)) }
    }
}

/** The categories as chips, above the menu on a phone. */
@Composable
internal fun CategoryChips(state: DemoAppState) {
    BuilderChoiceChips(
        options = CafeCategory.entries,
        selected = state.category,
        onSelect = { category -> state.pick(category) },
        label = CafeCopy.Category,
        modifier = Modifier.previewRoles(CustomComponent.Chip),
    ) { category -> category.label }
}

/** The category's colour as a dot, beside its name in the list. */
@Composable
private fun CategoryDot(
    category: CafeCategory,
    colors: CafeColors,
) {
    val dot = colors.mark(category.accent).fill
    Box(Modifier.previewRoles(dot.ref).size(DotSize).background(dot.color, CircleShape))
}

/** The drinks of the chosen category under its banner, in [columns] columns. */
@Composable
internal fun MenuPane(
    state: DemoAppState,
    colors: CafeColors,
    columns: Int,
    modifier: Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val category = state.category
    LazyColumn(
        state = state.rememberListState("cafe.menu"),
        modifier = modifier,
        contentPadding = PaddingValues(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        item(key = "banner") { CategoryBanner(category, colors) }
        items(category.items.chunked(columns), key = { row -> row.first().id }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                for (item in row) MenuItemCard(item, state, colors, Modifier.weight(1f))
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The chosen category's name and line on its accent's container. */
@Composable
internal fun CategoryBanner(
    category: CafeCategory,
    colors: CafeColors,
) {
    val spacing = LocalBuilderTokens.current.spacing
    val banner = colors.container(category.accent)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .previewRoles(banner)
            .background(banner.fill.color, cafePanelShape())
            .padding(spacing.large),
        horizontalArrangement = Arrangement.spacedBy(spacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CafeGlyph(category.icon, banner.ink.color, BannerGlyphSize)
        Column(Modifier.weight(1f)) {
            BuilderText(
                text = category.label,
                modifier = Modifier.semantics { heading() },
                style = BuilderTextStyle.Title,
                color = banner.ink.color,
            )
            BuilderText(category.blurb, color = banner.ink.color)
        }
    }
}

/** The stamps collected towards a free drink, on the secondary container. */
@Composable
internal fun StampCard(
    colors: CafeColors,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalBuilderTokens.current
    val card = colors.pair(CustomSlot.SecondaryContainer, CustomSlot.OnSecondaryContainer)
    val stamp = colors.slot(CustomSlot.Secondary)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .previewRoles(card)
            .background(card.fill.color, cafePanelShape())
            .padding(tokens.spacing.large),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.small),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CafeGlyph(Lucide.Stamp, card.ink.color)
            BuilderText(CafeCopy.StampCard, style = BuilderTextStyle.Label, color = card.ink.color)
        }
        BuilderText(CafeCopy.stampsLeft(StampsCollected, StampsNeeded), color = card.ink.color)
        Row(
            modifier = Modifier.previewRoles(stamp.ref),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        ) {
            repeat(StampsNeeded) { index ->
                val collected = index < StampsCollected
                Box(
                    Modifier
                        .size(StampSize)
                        .background(if (collected) stamp.color else Color.Transparent, CircleShape)
                        .border(tokens.outlineWidth, stamp.color, CircleShape),
                )
            }
        }
    }
}

/** One drink on a card, with its tags, its favourite mark, its price and a button to add it. */
@Composable
internal fun MenuItemCard(
    item: CafeItem,
    state: DemoAppState,
    colors: CafeColors,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalBuilderTokens.current.spacing
    BuilderCard(modifier.fillMaxWidth().previewRoles(CustomComponent.SampleCard)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
            ItemThumb(item.category, colors)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BuilderText(item.name, Modifier.weight(1f), style = BuilderTextStyle.Label, maxLines = 1)
                    FavouriteToggle(item, state, colors)
                }
                BuilderText(item.detail, emphasis = Emphasis.Secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
                    CafeTag(item.served.label, item.served.icon, colors.container(item.served.accent))
                    if (item.soldOut) {
                        val soldOut = colors.pair(CustomSlot.ErrorContainer, CustomSlot.OnErrorContainer)
                        CafeTag(CafeCopy.SoldOut, null, soldOut)
                    }
                }
                Row(
                    modifier = Modifier.padding(top = spacing.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BuilderText(CafeCopy.price(item.cents), Modifier.weight(1f), style = BuilderTextStyle.Label)
                    BuilderButton(
                        onClick = { state.addOne(item) },
                        label = CafeCopy.Add,
                        modifier = Modifier.previewRoles(Emphasis.Primary.component),
                        emphasis = Emphasis.Primary,
                        icon = IconId.Plus,
                        enabled = !item.soldOut && state.quantity(item) < MaxQuantity,
                    )
                }
            }
        }
    }
}

/** The drink's category glyph on the category's container. */
@Composable
private fun ItemThumb(
    category: CafeCategory,
    colors: CafeColors,
) {
    val thumb = colors.container(category.accent)
    Box(
        modifier = Modifier
            .previewRoles(thumb)
            .size(ThumbSize)
            .background(thumb.fill.color, RoundedCornerShape(LocalBuilderTokens.current.radius.small)),
        contentAlignment = Alignment.Center,
    ) {
        CafeGlyph(category.icon, thumb.ink.color)
    }
}

/** A short label on [pair]'s fill, with a glyph in front when there is one. */
@Composable
private fun CafeTag(
    label: String,
    icon: ImageVector?,
    pair: CafePair,
) {
    val tokens = LocalBuilderTokens.current
    Row(
        modifier = Modifier
            .previewRoles(pair)
            .background(pair.fill.color, CircleShape)
            .padding(horizontal = tokens.spacing.small, vertical = tokens.spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) CafeGlyph(icon, pair.ink.color, tokens.spacing.medium)
        BuilderText(label, style = BuilderTextStyle.Label, color = pair.ink.color, maxLines = 1)
    }
}

/**
 * The heart that marks a favourite, filled with the Love accent while on and a muted outline while
 * off. The kit has no heart, so the app draws it and rings it in the focus slot while it has focus.
 */
@Composable
private fun FavouriteToggle(
    item: CafeItem,
    state: DemoAppState,
    colors: CafeColors,
) {
    val tokens = LocalBuilderTokens.current
    val favourite = state.isFavourite(item)
    val mark = colors.mark(CafeAccent.Love)
    val muted = colors.slot(CustomSlot.TextMuted)
    val ring = colors.slot(CustomSlot.FocusRing)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val painted = buildList {
        if (favourite) addAll(listOf(mark.fill.ref, mark.ink.ref)) else add(muted.ref)
        if (focused) add(ring.ref)
    }
    Box(
        modifier = Modifier
            .previewRoles(*painted.toTypedArray())
            .toggleable(
                value = favourite,
                interactionSource = interaction,
                indication = null,
                role = Role.Checkbox,
            ) { on -> state.setFavourite(item, on) }
            .semantics { contentDescription = CafeCopy.Favourite }
            .size(LocalLayout.current.minTouchTarget.coerceAtLeast(HeartSize)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(HeartSize)
                .background(if (favourite) mark.fill.color else Color.Transparent, CircleShape)
                .then(if (focused) Modifier.border(tokens.highlightWidth, ring.color, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            CafeGlyph(Lucide.Heart, if (favourite) mark.ink.color else muted.color, tokens.spacing.large)
        }
    }
}
