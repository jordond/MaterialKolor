package com.materialkolor.builder.preview

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.unit.dp
import com.materialkolor.builder.preview.inspect.PreviewRoles
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wide enough for four columns and tall enough that every card of a gallery composes.
 */
internal val GalleryWhole: Modifier = Modifier
    .wrapContentSize(Alignment.TopStart, unbounded = true)
    .requiredSize(1280.dp, 8000.dp)

/**
 * Anything a user can press, type into or drag.
 */
internal val GalleryInteractive: SemanticsMatcher =
    SemanticsMatcher("is interactive") { node -> node.galleryInteractive() }

/**
 * Anything that takes keyboard focus.
 */
internal val GalleryFocusable: SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsActions.RequestFocus)

internal fun SemanticsNode.galleryInteractive(): Boolean =
    SemanticsActions.OnClick in config || SemanticsActions.SetText in config || SemanticsActions.SetProgress in config

/**
 * Whether the node declares its roles, itself or through the control it is part of. The options of
 * a segmented control or a tab row are nodes of their own under the control, so the nearest node
 * with roles may be an ancestor, as long as it sits inside a card and is not the card's frame among
 * [frames].
 */
internal fun SemanticsNode.galleryDeclaresRoles(frames: Set<Int>): Boolean {
    val holder = generateSequence(this) { node -> node.parent }.firstOrNull { node -> PreviewRoles in node.config }
    return holder != null &&
        holder.id !in frames &&
        generateSequence(holder.parent) { node -> node.parent }.any { node -> node.id in frames }
}

/**
 * Every node under this one in the unmerged tree.
 */
internal fun SemanticsNode.galleryDescendants(): List<SemanticsNode> =
    children.flatMap { child -> listOf(child) + child.galleryDescendants() }

/**
 * The frame of the card called [title], the node its title text sits in.
 */
internal fun SemanticsNodeInteractionsProvider.galleryFrame(title: String): SemanticsNode =
    onAllNodes(hasText(title), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .mapNotNull { text -> text.parent }
        .first { frame -> PreviewRoles in frame.config }

/**
 * Whether anything in the card called [title] declares roles, its frame aside.
 */
internal fun SemanticsNodeInteractionsProvider.galleryCardDeclaresRoles(title: String): Boolean =
    galleryFrame(title).galleryDescendants().any { node -> PreviewRoles in node.config }

/**
 * How many rows of the [composed] cards show at least partly on screen. The cards of a row share
 * their top edge, so each distinct top is a row.
 */
internal fun SemanticsNodeInteractionsProvider.galleryRowsOnScreen(composed: Set<String>): Int {
    val screen = onRoot().fetchSemanticsNode().boundsInRoot
    return composed
        .map { title -> galleryFrame(title) }
        .filter { frame -> frame.layoutInfo.isPlaced && frame.boundsInRoot.overlaps(screen) }
        .map { frame -> frame.boundsInRoot.top }
        .distinct()
        .size
}

/**
 * Every colour painted on screen, as ARGB.
 */
internal fun SemanticsNodeInteractionsProvider.screenColors(): Set<Int> {
    val map = onRoot().captureToImage().toPixelMap()
    return buildSet { for (x in 0 until map.width) for (y in 0 until map.height) add(map[x, y].toArgb()) }
}

/**
 * Checks the scene holds one window, the gallery's own, and no popup, naming the step [after] which
 * one opened.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.assertOneWindow(after: String) {
    withClue("Windows after $after") { onAllNodes(isRoot()).fetchSemanticsNodes().size shouldBe 1 }
}

/**
 * Presses every enabled control through its click action, then checks each press was handled, that
 * together they flipped at least one toggle or selection, and that no popup or window opened.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.pressEveryControl() {
    val pressable = onAllNodes(hasClickAction(), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .filter { node -> SemanticsProperties.Disabled !in node.config }
    pressable.shouldNotBeEmpty()
    val before = pickedStates()
    val unhandled = runOnIdle {
        pressable.filterNot { node -> node.config[SemanticsActions.OnClick].action?.invoke() == true }
    }
    waitForIdle()
    assertOneWindow("pressing every control")
    unhandled.map { node -> node.config.toString() }.shouldBeEmpty()
    pickedStates() shouldNotBe before
}

/**
 * Moves the pointer onto every interactive control in turn, and with [pressAndDrag] presses it and
 * drags it 8 px before letting go, checking after each that no popup or window opened.
 *
 * The controls are fetched once and their bounds read afresh at each step, so a layout that moved is
 * followed. They are fetched again only once one of them has left the tree.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.hoverEveryControl(pressAndDrag: Boolean = false) {
    var controls = onAllNodes(GalleryInteractive, useUnmergedTree = true).fetchSemanticsNodes()
    controls.shouldNotBeEmpty()
    var index = 0
    while (index < controls.size) {
        if (!controls[index].layoutInfo.isAttached) {
            controls = onAllNodes(GalleryInteractive, useUnmergedTree = true).fetchSemanticsNodes()
            if (index >= controls.size) break
        }
        val target = controls[index].boundsInRoot.center
        onRoot().performMouseInput {
            moveTo(target)
            if (pressAndDrag) {
                press()
                moveBy(Offset(8f, 0f))
            }
        }
        waitForIdle()
        assertOneWindow("hovering control $index")
        if (pressAndDrag) {
            onRoot().performMouseInput { release() }
            waitForIdle()
        }
        index++
    }
    onRoot().performMouseInput { exit() }
}

/**
 * Focuses every focusable control in turn, checking after each that no popup or window opened.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.focusEveryControl() {
    val focusable = onAllNodes(GalleryFocusable, useUnmergedTree = true).fetchSemanticsNodes()
    focusable.shouldNotBeEmpty()
    for ((index, node) in focusable.withIndex()) {
        if (!node.layoutInfo.isAttached) continue
        runOnIdle { node.config[SemanticsActions.RequestFocus].action?.invoke() }
        waitForIdle()
        assertOneWindow("focusing control $index")
    }
}

/**
 * Types [word] into the first of [fields], so a field has a word to select, then right clicks and
 * long presses every field, checking after each that no popup or window opened.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.rightClickAndLongPressEveryField(
    fields: SemanticsNodeInteractionCollection,
    word: String,
) {
    val count = fields.fetchSemanticsNodes().size
    count shouldBeGreaterThan 0
    fields[0].performTextReplacement(word)
    for (index in 0 until count) {
        fields[index].performMouseInput { rightClick() }
        waitForIdle()
        assertOneWindow("right clicking text field $index")
        fields[index].performTouchInput { longClick() }
        waitForIdle()
        assertOneWindow("long pressing text field $index")
    }
}

/**
 * The whole sweep of a gallery. Presses, hovers and focuses every control, then right clicks and
 * long presses every text field, and no step may open a popup or a window, which on the web take
 * the mirror over.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.sweepEveryControl(pressAndDrag: Boolean = false) {
    pressEveryControl()
    hoverEveryControl(pressAndDrag)
    focusEveryControl()
    rightClickAndLongPressEveryField(onAllNodes(hasSetTextAction()), "Harbour")
}

/**
 * A text toolbar that counts how often it is asked to show, where the web's would open a popup.
 */
internal class TextToolbarProbe : TextToolbar {
    var shown: Int = 0
        private set

    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        shown++
    }

    override fun hide() = Unit
}

/**
 * Every toggle and selection in the scene by node, to tell whether pressing changed any.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.pickedStates(): Map<Int, Pair<Any?, Boolean?>> =
    onAllNodes(isToggleable() or isSelectable(), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .associate { node ->
            node.id to (
                node.config.getOrNull(SemanticsProperties.ToggleableState) to
                    node.config.getOrNull(SemanticsProperties.Selected)
            )
        }
