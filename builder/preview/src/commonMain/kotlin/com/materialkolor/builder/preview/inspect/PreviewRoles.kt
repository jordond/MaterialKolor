package com.materialkolor.builder.preview.inspect

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import com.materialkolor.builder.domain.audit.FluentText
import com.materialkolor.builder.domain.model.CustomSlot
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.preview.split.LocalPaneSide

/**
 * One color a preview element reads, in the vocabulary of the library that draws it.
 */
public sealed interface RoleRef {
    /**
     * A role of the Material 3 scheme, which Material 3 and Unstyled screens read.
     *
     * @property[role] The role.
     */
    public data class OfRole(
        public val role: Role,
    ) : RoleRef

    /**
     * A slot of the Custom target.
     *
     * @property[slot] The slot.
     */
    public data class OfSlot(
        public val slot: CustomSlot,
    ) : RoleRef

    /**
     * One of the seven accent shades a Fluent theme is built from.
     *
     * @property[shade] The shade.
     */
    public data class OfFluentShade(
        public val shade: FluentShade,
    ) : RoleRef

    /**
     * A text color Fluent fixes itself.
     *
     * @property[text] The text color.
     */
    public data class OfFluentText(
        public val text: FluentText,
    ) : RoleRef
}

/**
 * The accent shades of a Fluent theme, named the way Fluent's `Shades` names them.
 */
public enum class FluentShade {
    /** The darkest shade. */
    Dark3,

    /** The second darkest shade. */
    Dark2,

    /** The shade just darker than the accent. */
    Dark1,

    /** The accent itself. */
    Base,

    /** The shade just lighter than the accent. */
    Light1,

    /** The second lightest shade. */
    Light2,

    /** The lightest shade. */
    Light3,
}

/**
 * The roles a preview element declared, kept whether or not Inspect is on.
 *
 * A merged node carries its own roles and then those of everything merged into it.
 */
public val PreviewRoles: SemanticsPropertyKey<List<RoleRef>> =
    SemanticsPropertyKey(name = "PreviewRoles") { parent, child -> parent.orEmpty() + child }

/**
 * Declare the colors this element reads, for the role usage check and for Inspect.
 *
 * The roles always land in semantics under [PreviewRoles]. While Inspect is on, meaning
 * [LocalInspectRegistry] holds a registry, the element also records its window bounds there so the
 * overlay can find it under the pointer.
 *
 * @param[roles] The colors the element reads, most important first.
 */
public fun Modifier.previewRoles(vararg roles: RoleRef): Modifier = this then PreviewRolesElement(roles.toList())

private class PreviewRolesElement(
    private val roles: List<RoleRef>,
) : ModifierNodeElement<PreviewRolesNode>() {
    override fun create(): PreviewRolesNode = PreviewRolesNode(roles)

    override fun update(node: PreviewRolesNode) {
        node.update(roles)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "previewRoles"
        properties["roles"] = roles
    }

    override fun equals(other: Any?): Boolean = this === other || (other is PreviewRolesElement && roles == other.roles)

    override fun hashCode(): Int = roles.hashCode()
}

private class PreviewRolesNode(
    private var roles: List<RoleRef>,
) : Modifier.Node(),
    SemanticsModifierNode,
    GlobalPositionAwareModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode {
    private var registry: InspectRegistry? = null
    private var coordinates: LayoutCoordinates? = null

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
            record()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        record()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this[PreviewRoles] = roles
    }

    fun update(roles: List<RoleRef>) {
        if (roles == this.roles) return
        this.roles = roles
        record()
    }

    /** Write where this element is into the registry, if Inspect is on and it has been placed. */
    private fun record() {
        val registry = registry ?: return
        val coordinates = coordinates?.takeIf { placed -> placed.isAttached } ?: return
        registry.record(this, InspectEntry(currentValueOf(LocalPaneSide), roles, coordinates.boundsInWindow()))
    }
}
