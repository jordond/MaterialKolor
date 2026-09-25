package com.materialkolor.builder.domain.model

/**
 * The address of one color an accent family generates.
 *
 * An accent is a seed the document carries rather than a fixed slot, so its four colors cannot be
 * named by a [CustomSlot]. The engine and the code generator use this address to talk about one of
 * them, and the exported property is named after the accent itself.
 *
 * It is an address and not a stored value, so it never lands in a document. A tone someone moved
 * on an accent lives on the [Accent] as its own tones.
 *
 * @property[index] Which accent of the document this belongs to, counted from its position in the list.
 * @property[part] Which of the family's four colors this names.
 */
public data class AccentSlot(
    public val index: Int,
    public val part: AccentPart,
) {
    init {
        require(index >= 0) { "An accent index is 0 or more, got $index" }
    }
}

/**
 * One of the four colors an accent family generates.
 *
 * The same four an M3 color family has, so an accent reads the way primary or secondary does.
 *
 * @property[code] The number that names this part of a family, fixed per entry.
 */
public enum class AccentPart(
    override val code: Int,
) : CodedEnum {
    /**
     * The family's own color.
     */
    Color(code = 0),

    /**
     * The color that reads on [Color].
     */
    OnColor(code = 1),

    /**
     * The family's container, the quieter surface of the pair.
     */
    Container(code = 2),

    /**
     * The color that reads on [Container].
     */
    OnContainer(code = 3),
}
