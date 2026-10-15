package com.materialkolor.builder.preview.inspect

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import com.materialkolor.builder.domain.audit.ColorRef
import com.materialkolor.builder.preview.split.LocalPaneSide

/**
 * The colors a preview element declared, kept whether or not Inspect is on.
 *
 * A merged node carries its own colors and then those of everything merged into it.
 */
public val PreviewRoles: SemanticsPropertyKey<List<ColorRef>> =
    SemanticsPropertyKey(name = "PreviewRoles") { parent, child -> parent.orEmpty() + child }

/**
 * Declare the colors this element reads, for the role usage check and for Inspect.
 *
 * The colors are named the way the contrast audit names them, so the Inspect popover can rate a
 * pair of them. They always land in semantics under [PreviewRoles], and with Inspect off that is
 * all the element costs besides noting whether it holds focus. While Inspect is on, meaning
 * [LocalInspectRegistry] holds a registry, the element also records its window bounds there so the
 * overlay can find it under the pointer, and tells the registry when focus is on it or inside it.
 * Put it before the element's clickable or focusable, as a modifier passed to a component is, so
 * it hears that focus.
 *
 * @param[refs] The colors the element reads, most important first.
 */
public fun Modifier.previewRoles(vararg refs: ColorRef): Modifier = this then PreviewRolesElement(refs.toList())

private class PreviewRolesElement(
    private val refs: List<ColorRef>,
) : ModifierNodeElement<PreviewRolesNode>() {
    override fun create(): PreviewRolesNode = PreviewRolesNode(refs)

    override fun update(node: PreviewRolesNode) {
        node.update(refs)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "previewRoles"
        properties["refs"] = refs
    }

    override fun equals(other: Any?): Boolean = this === other || (other is PreviewRolesElement && refs == other.refs)

    override fun hashCode(): Int = refs.hashCode()
}

private class PreviewRolesNode(
    private var refs: List<ColorRef>,
) : DelegatingNode(),
    SemanticsModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    FocusEventModifierNode {
    private var registry: InspectRegistry? = null
    private var coordinates: LayoutCoordinates? = null

    /**
     * This element's key in the registry, made as the node attaches and dropped as it detaches. A
     * lazy list hands a node it no longer needs to another item, so the node itself would carry a pin
     * from one item over to the next. A fresh key makes each stay in the tree a new element. A node
     * the list keeps off screen without placing it gets a fresh key too, since it may come back as
     * another item.
     */
    private var key: Any? = null

    /**
     * Whether focus is on this element or inside it, kept while Inspect is off for when it comes on.
     */
    private var focused = false

    /**
     * Hears about every placement, so it is only delegated to while Inspect is on.
     */
    private var positions: PositionNode? = null

    override fun onAttach() {
        key = Any()
        onObservedReadsChanged()
    }

    override fun onDetach() {
        key?.let { key -> registry?.remove(key) }
        key = null
        registry = null
        coordinates = null
        focused = false
    }

    override fun onFocusEvent(focusState: FocusState) {
        focused = focusState.hasFocus
        val key = key ?: return
        registry?.focus(key, focused)
    }

    override fun onObservedReadsChanged() {
        var current: InspectRegistry? = null
        observeReads { current = currentValueOf(LocalInspectRegistry) }
        val key = key ?: return
        if (current !== registry) {
            registry?.remove(key)
            registry = current
            current?.focus(key, focused)
        }
        val placed = positions
        if (current == null) {
            if (placed != null) undelegate(placed)
            positions = null
            coordinates = null
        } else if (placed == null) {
            // A new position node asks for a placement callback of its own, with no relayout.
            positions = delegate(PositionNode())
        } else {
            record()
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this[PreviewRoles] = refs
    }

    fun update(refs: List<ColorRef>) {
        if (refs == this.refs) return
        this.refs = refs
        record()
    }

    /**
     * Write where this element is into the registry, if Inspect is on and it has been placed.
     */
    private fun record() {
        val registry = registry ?: return
        val key = key ?: return
        val coordinates = coordinates?.takeIf { placed -> placed.isAttached } ?: return
        registry.record(key, InspectEntry(currentValueOf(LocalPaneSide), refs, coordinates.boundsInWindow()))
    }

    /**
     * Forget this element once it is no longer placed, as happens to a lazy list's item the list
     * keeps around off screen. Its last bounds would otherwise stay in the registry, and an outline or
     * pin on it would stay where it was. It records itself again under a fresh key once it is placed.
     */
    private fun unplaced() {
        val old = key ?: return
        val key = Any()
        this.key = key
        coordinates = null
        val registry = registry ?: return
        registry.remove(old)
        if (focused) registry.focus(key, focused = true)
    }

    /**
     * Passes each placement of the element on while Inspect is on, and the moment it stops being placed.
     */
    private inner class PositionNode :
        Modifier.Node(),
        GlobalPositionAwareModifierNode,
        UnplacedAwareModifierNode {
        override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
            this@PreviewRolesNode.coordinates = coordinates
            record()
        }

        override fun onUnplaced() {
            unplaced()
        }
    }
}
