package com.materialkolor.builder.preview.custom

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Bean
import com.composables.icons.lucide.Coffee
import com.composables.icons.lucide.CupSoda
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Leaf
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Snowflake
import com.composables.icons.lucide.Sprout

// The cafe's words and its menu. All of it is placeholder copy until the owner signs it off.

/** Every line of copy the cafe shows that is not part of the menu. */
internal object CafeCopy {
    const val StoreName = "Corner Cup"
    const val OpenUntil = "Open until 6 pm"
    const val ReadyIn = "Ready in 8 min"
    const val StampCard = "Stamp card"
    const val Menu = "Menu"
    const val Category = "Category"
    const val Add = "Add"
    const val SoldOut = "Sold out"
    const val YourOrder = "Your order"
    const val OrderType = "Order type"
    const val EmptyOrder = "Nothing in your order yet"
    const val Clear = "Clear"
    const val PlaceOrder = "Place order"
    const val Placed = "Order placed. We will call your name when it is ready."
    const val Dismiss = "Dismiss"
    const val ViewOrder = "View order"
    const val BackToMenu = "Menu"

    /** How the stamp card reads with [collected] of [needed] stamps. */
    fun stampsLeft(
        collected: Int,
        needed: Int,
    ): String = "$collected of $needed, ${needed - collected} more for a free drink"

    /** A count of drinks, one drink or several. */
    fun drinks(count: Int): String = if (count == 1) "1 drink" else "$count drinks"

    /** A count of items in the order, one item or several. */
    fun items(count: Int): String = if (count == 1) "1 item" else "$count items"

    /** What the heart of the drink [name] reads out. */
    fun favourite(name: String): String = "Favourite $name"

    /** What the remove button of an order line reads out. */
    fun removeOne(name: String): String = "Remove one $name"

    /** A price in cents as dollars and cents. */
    fun price(cents: Int): String = "$" + "${cents / 100}." + "${cents % 100}".padStart(2, '0')
}

/** How to have an order. */
internal enum class OrderType(
    val label: String,
) {
    PickUp("Pick up"),
    DineIn("Dine in"),
}

/** How a drink is served, which says which accent tags it. */
internal enum class Served(
    val label: String,
    val icon: ImageVector,
    val accent: CafeAccent,
) {
    Hot("Hot", Lucide.Flame, CafeAccent.Warm),
    Cold("Cold", Lucide.Snowflake, CafeAccent.Cold),
}

/**
 * A part of the menu, one per drink seed of the sample.
 *
 * @property[blurb] The line under the category's name.
 * @property[accent] The accent the category is drawn in.
 */
internal enum class CafeCategory(
    val label: String,
    val blurb: String,
    val icon: ImageVector,
    val accent: CafeAccent,
) {
    Coffee("Coffee", "Pulled from the house roast", Lucide.Coffee, CafeAccent.Coffee),
    Matcha("Matcha", "Stone ground and whisked to order", Lucide.Sprout, CafeAccent.Matcha),
    Iced("Iced", "Poured over ice, made to linger", Lucide.CupSoda, CafeAccent.Iced),
    Tea("Tea", "Loose leaf, steeped by the pot", Lucide.Leaf, CafeAccent.Tea),
    Chocolate("Chocolate", "Single origin cocoa, melted slowly", Lucide.Bean, CafeAccent.Chocolate),
}

/**
 * A drink on the menu.
 *
 * @property[id] What the drink is keyed by in [com.materialkolor.builder.preview.canvas.DemoAppState].
 * @property[cents] The price.
 * @property[favourite] Whether the drink starts out marked as a favourite.
 * @property[soldOut] Whether the drink cannot be ordered today.
 * @property[startQuantity] How many the order holds before anyone changes it.
 */
internal class CafeItem(
    val id: String,
    val name: String,
    val detail: String,
    val cents: Int,
    val category: CafeCategory,
    val served: Served,
    val favourite: Boolean = false,
    val soldOut: Boolean = false,
    val startQuantity: Int = 0,
)

/** The whole menu, in the order each category lists it. */
internal val CafeMenu: List<CafeItem> = listOf(
    CafeItem(
        id = "flatWhite",
        name = "Flat white",
        detail = "Double ristretto, velvet milk",
        cents = 420,
        category = CafeCategory.Coffee,
        served = Served.Hot,
        favourite = true,
        startQuantity = 1,
    ),
    CafeItem(
        id = "cortado",
        name = "Cortado",
        detail = "Equal parts espresso and milk",
        cents = 380,
        category = CafeCategory.Coffee,
        served = Served.Hot,
        soldOut = true,
    ),
    CafeItem("coldBrew", "Cold brew", "Steeped for eighteen hours", 450, CafeCategory.Coffee, Served.Cold),
    CafeItem("mapleLatte", "Maple latte", "House maple and a pinch of salt", 520, CafeCategory.Coffee, Served.Hot),
    CafeItem("matchaLatte", "Matcha latte", "Ceremonial grade with oat milk", 520, CafeCategory.Matcha, Served.Hot),
    CafeItem("icedMatcha", "Iced matcha", "Over ice with a honey swirl", 540, CafeCategory.Matcha, Served.Cold),
    CafeItem("matchaTonic", "Matcha tonic", "Tonic water and a yuzu twist", 560, CafeCategory.Matcha, Served.Cold),
    CafeItem(
        id = "icedLatte",
        name = "Iced latte",
        detail = "Espresso over cold milk",
        cents = 480,
        category = CafeCategory.Iced,
        served = Served.Cold,
        startQuantity = 1,
    ),
    CafeItem("icedMocha", "Iced mocha", "Dark chocolate, espresso and ice", 540, CafeCategory.Iced, Served.Cold),
    CafeItem("espressoTonic", "Espresso tonic", "A single shot over lemon tonic", 500, CafeCategory.Iced, Served.Cold),
    CafeItem("earlGrey", "Earl grey", "Bergamot black tea by the pot", 380, CafeCategory.Tea, Served.Hot),
    CafeItem(
        id = "chaiLatte",
        name = "Chai latte",
        detail = "Spiced black tea and steamed milk",
        cents = 460,
        category = CafeCategory.Tea,
        served = Served.Hot,
        favourite = true,
    ),
    CafeItem("peachTea", "Iced peach tea", "Brewed cold with white peach", 420, CafeCategory.Tea, Served.Cold),
    CafeItem("hotChocolate", "Hot chocolate", "Dark cocoa and whole milk", 450, CafeCategory.Chocolate, Served.Hot),
    CafeItem("mocha", "Mocha", "Chocolate, espresso and steamed milk", 500, CafeCategory.Chocolate, Served.Hot),
    CafeItem("frozenCocoa", "Frozen cocoa", "Blended with ice and cream", 560, CafeCategory.Chocolate, Served.Cold),
)

/** The drinks of this category, in menu order. */
internal val CafeCategory.items: List<CafeItem>
    get() = CafeMenu.filter { item -> item.category == this }

/** The most of one drink an order holds. */
internal const val MaxQuantity = 9

/** How many stamps the stamp card holds, and how many are on it already. */
internal const val StampsNeeded = 10
internal const val StampsCollected = 7
