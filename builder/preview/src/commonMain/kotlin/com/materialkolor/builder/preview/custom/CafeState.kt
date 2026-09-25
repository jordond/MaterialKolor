package com.materialkolor.builder.preview.custom

import com.materialkolor.builder.preview.canvas.DemoAppState
import com.materialkolor.builder.preview.canvas.choice
import com.materialkolor.builder.preview.canvas.choose

// What the cafe remembers, all of it in DemoAppState under cafe keys so both copies of a split
// agree. Trips owns tabIndex, selectedItem and text, so the cafe leaves them alone.

internal const val CategoryKey = "cafe.category"
internal const val OrderTypeKey = "cafe.orderType"
internal const val PlacedKey = "cafe.placed"
internal const val PhoneOrderKey = "cafe.phone.order"

/**
 * The category the menu shows, the first until someone picks another.
 */
internal val DemoAppState.category: CafeCategory
    get() = CafeCategory.entries[choice(CategoryKey, CafeCategory.entries.size)]

internal fun DemoAppState.pick(category: CafeCategory) {
    choose(CategoryKey, CafeCategory.entries.size, category.ordinal)
}

/**
 * How the order is had, picked up until someone says otherwise.
 */
internal val DemoAppState.orderType: OrderType
    get() = OrderType.entries[choice(OrderTypeKey, OrderType.entries.size)]

internal fun DemoAppState.pick(type: OrderType) {
    choose(OrderTypeKey, OrderType.entries.size, type.ordinal)
}

/**
 * How many of [item] the order holds.
 */
internal fun DemoAppState.quantity(item: CafeItem): Int =
    choice("cafe.quantity.${item.id}", MaxQuantity + 1, default = item.startQuantity)

private fun DemoAppState.setQuantity(
    item: CafeItem,
    quantity: Int,
) {
    choose("cafe.quantity.${item.id}", MaxQuantity + 1, quantity.coerceIn(0, MaxQuantity))
}

/**
 * Whether [item] is marked as a favourite.
 */
internal fun DemoAppState.isFavourite(item: CafeItem): Boolean =
    choice("cafe.favourite.${item.id}", 2, default = if (item.favourite) 1 else 0) == 1

internal fun DemoAppState.setFavourite(
    item: CafeItem,
    favourite: Boolean,
) {
    choose("cafe.favourite.${item.id}", 2, if (favourite) 1 else 0)
}

/**
 * Put one more [item] in the order, which starts a new order once the last one is placed.
 */
internal fun DemoAppState.addOne(item: CafeItem) {
    setQuantity(item, quantity(item) + 1)
    setOn(PlacedKey, false)
}

internal fun DemoAppState.removeOne(item: CafeItem) {
    setQuantity(item, quantity(item) - 1)
}

/**
 * Empty the order.
 */
internal fun DemoAppState.clearOrder() {
    for (item in CafeMenu) setQuantity(item, 0)
    setOn(PlacedKey, false)
}

/**
 * Send the order to the counter and start an empty one.
 */
internal fun DemoAppState.placeOrder() {
    for (item in CafeMenu) setQuantity(item, 0)
    setOn(PlacedKey, true)
}

/**
 * The drinks in the order, in menu order, with how many of each.
 */
internal fun DemoAppState.orderLines(): List<Pair<CafeItem, Int>> =
    CafeMenu.map { item -> item to quantity(item) }.filter { (_, quantity) -> quantity > 0 }

/**
 * How many drinks the order holds and what they cost together, in cents.
 */
internal fun DemoAppState.orderTotal(): Pair<Int, Int> =
    orderLines().fold(0 to 0) { (count, cents), (item, quantity) -> count + quantity to cents + item.cents * quantity }
