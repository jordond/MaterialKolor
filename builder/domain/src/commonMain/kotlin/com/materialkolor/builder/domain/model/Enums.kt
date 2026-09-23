package com.materialkolor.builder.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Which Material spec the scheme is generated against.
 *
 * @property[code] The number the share codec writes for this spec.
 */
@Serializable
public enum class SpecVersion(
    override val code: Int,
) : CodedEnum {
    /** The original Material 3 spec. */
    @SerialName("Spec2021")
    Spec2021(code = 0),

    /** The 2025 expressive revision. */
    @SerialName("Spec2025")
    Spec2025(code = 1),

    /** The 2026 revision, the one the color, material and finish style belongs to. */
    @SerialName("Spec2026")
    Spec2026(code = 2),
}

/**
 * The device the scheme is tuned for.
 *
 * @property[code] The number the share codec writes for this platform.
 */
@Serializable
public enum class SchemePlatform(
    override val code: Int,
) : CodedEnum {
    /** The everyday platform, what every scheme uses until someone asks otherwise. */
    @SerialName("Phone")
    Phone(code = 0),

    /** Higher contrast throughout, for a small always on display. */
    @SerialName("Watch")
    Watch(code = 1),
}

/**
 * Which MaterialKolor module the exported theme is written against.
 *
 * @property[code] The number the share codec writes for this library.
 */
@Serializable
public enum class Library(
    override val code: Int,
) : CodedEnum {
    /** A Compose Material 3 `ColorScheme`. */
    @SerialName("Material3")
    Material3(code = 0),

    /** The plain color holder, for apps that bring their own design system. */
    @SerialName("Unstyled")
    Unstyled(code = 1),

    /** A Fluent color set. */
    @SerialName("Fluent")
    Fluent(code = 2),

    /** A hand shaped set of slots, the target with the most room in it. */
    @SerialName("Custom")
    Custom(code = 3),
}

/**
 * Which motion scheme the exported theme carries.
 *
 * @property[code] The number the share codec writes for this choice.
 */
@Serializable
public enum class MotionSchemeChoice(
    override val code: Int,
) : CodedEnum {
    /** Restrained springs, the calmer of the two. */
    @SerialName("Standard")
    Standard(code = 0),

    /** Livelier springs, the default the expressive spec asks for. */
    @SerialName("Expressive")
    Expressive(code = 1),
}
