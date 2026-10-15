package com.materialkolor.builder.domain.model

/**
 * An enum whose entries carry a number the share codec writes instead of their name.
 *
 * The number is fixed per entry and has nothing to do with the declaration order, so entries can
 * be reordered, grouped or listed differently without invalidating a link someone already shared.
 * Codes are never reused, even when an entry is dropped.
 *
 * Every enum that implements this is listed by hand in `CodedEnumTest`, because common code cannot
 * go looking for them. A change that adds one adds it to that list too, otherwise its codes go
 * unchecked.
 *
 * @property[code] The number the codec writes for this entry, unique within its enum.
 */
public interface CodedEnum {
    public val code: Int
}

/**
 * The entry whose [CodedEnum.code] is [code], or null when nothing carries it.
 *
 * Decoding a link written by a newer builder lands here, which is why the lenient answer exists
 * alongside [withCode].
 */
public fun <T : CodedEnum> Iterable<T>.withCodeOrNull(code: Int): T? = firstOrNull { entry -> entry.code == code }

/**
 * The entry whose [CodedEnum.code] is [code].
 *
 * @throws[IllegalArgumentException] when nothing carries that code.
 */
public fun <T : CodedEnum> Iterable<T>.withCode(code: Int): T =
    withCodeOrNull(code) ?: throw IllegalArgumentException("No entry with code $code")
