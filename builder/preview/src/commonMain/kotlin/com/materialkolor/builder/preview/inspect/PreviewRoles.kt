package com.materialkolor.builder.preview.inspect

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
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
 * all the element costs. While Inspect is on, meaning [LocalInspectRegistry] holds a registry, the
 * element also records its window bounds there so the overlay can find it under the pointer.
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
    ObserverModifierNode {
    private var registry: InspectRegistry? = null
    private var coordinates: LayoutCoordinates? = null

    /** Hears about every placement, so it is only delegated to while Inspect is on. */
    private var positions: PositionNode? = null

    override fun onAttach() {
        onObservedReadsChanged()
    }

    override fun onDetach() {
        registry?.remove(this)
        registry = null
        coordinates = null
    }

    override fun onObservedReadsChanged() {
        var current: InspectRegistry? = null
        observeReads { current = currentValueOf(LocalInspectRegistry) }
        if (current !== registry) {
            registry?.remove(this)
            registry = current
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

    /** Write where this element is into the registry, if Inspect is on and it has been placed. */
    private fun record() {
        val registry = registry ?: return
        val coordinates = coordinates?.takeIf { placed -> placed.isAttached } ?: return
        registry.record(this, InspectEntry(currentValueOf(LocalPaneSide), refs, coordinates.boundsInWindow()))
    }

    /** Passes each placement of the element on while Inspect is on. */
    private inner class PositionNode :
        Modifier.Node(),
        GlobalPositionAwareModifierNode {
        override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
            this@PreviewRolesNode.coordinates = coordinates
            record()
        }
    }
}
