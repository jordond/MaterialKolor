package com.materialkolor.builder.domain.validate

import androidx.compose.runtime.Immutable
import com.materialkolor.builder.domain.model.Role

/**
 * Something about a theme or its export settings that would stop the export from compiling or
 * from fitting where it has to go.
 *
 * Each error names the field it is about and carries the value that failed, so the UI can point at
 * the right control and say what is wrong with it.
 */
@Immutable
public sealed interface ValidationError {
    /**
     * A segment of the package name is not a lowercase Java package segment.
     *
     * @property[index] Where the segment sits in the package, counting from zero.
     * @property[segment] The segment as it was typed.
     */
    @Immutable
    public data class PackageSegmentInvalid(
        public val index: Int,
        public val segment: String,
    ) : ValidationError

    /**
     * A segment of the package name is a Kotlin keyword.
     *
     * @property[index] Where the segment sits in the package, counting from zero.
     * @property[segment] The keyword.
     */
    @Immutable
    public data class PackageSegmentKeyword(
        public val index: Int,
        public val segment: String,
    ) : ValidationError

    /**
     * The theme name is not a Kotlin identifier.
     *
     * @property[name] The name as it was typed.
     */
    @Immutable
    public data class ThemeNameInvalid(
        public val name: String,
    ) : ValidationError

    /**
     * The theme name is a Kotlin keyword.
     *
     * @property[name] The keyword.
     */
    @Immutable
    public data class ThemeNameKeyword(
        public val name: String,
    ) : ValidationError

    /**
     * The theme has more accents than an export carries.
     *
     * @property[count] How many accents the theme has.
     */
    @Immutable
    public data class TooManyAccents(
        public val count: Int,
    ) : ValidationError

    /**
     * An accent's name is not a Kotlin identifier.
     *
     * @property[index] Where the accent sits in the document's list.
     * @property[name] The name as it was typed.
     */
    @Immutable
    public data class AccentNameInvalid(
        public val index: Int,
        public val name: String,
    ) : ValidationError

    /**
     * An accent's name is a Kotlin keyword.
     *
     * @property[index] Where the accent sits in the document's list.
     * @property[name] The keyword.
     */
    @Immutable
    public data class AccentNameKeyword(
        public val index: Int,
        public val name: String,
    ) : ValidationError

    /**
     * An accent's name is longer than an export allows.
     *
     * @property[index] Where the accent sits in the document's list.
     * @property[name] The name as it was typed.
     * @property[bytes] How long the name is in UTF-8.
     */
    @Immutable
    public data class AccentNameTooLong(
        public val index: Int,
        public val name: String,
        public val bytes: Int,
    ) : ValidationError

    /**
     * An accent has the same name as one listed before it.
     *
     * @property[index] Where the later of the two sits in the document's list.
     * @property[name] The name they share.
     */
    @Immutable
    public data class AccentNameDuplicate(
        public val index: Int,
        public val name: String,
    ) : ValidationError

    /**
     * The project name is longer than an export allows.
     *
     * @property[name] The name as it was typed.
     * @property[bytes] How long the name is in UTF-8.
     */
    @Immutable
    public data class ProjectNameTooLong(
        public val name: String,
        public val bytes: Int,
    ) : ValidationError

    /**
     * An accent's name matches one listed before it once case is ignored, as `brand` and `Brand`
     * do. Both would generate the same `onBrand`.
     *
     * @property[index] Where the later of the two sits in the document's list.
     * @property[name] The name as it was typed.
     */
    @Immutable
    public data class AccentNameCaseClash(
        public val index: Int,
        public val name: String,
    ) : ValidationError

    /**
     * An accent's name is also the name of a scheme role, ignoring case, as `primary` is.
     *
     * @property[index] Where the accent sits in the document's list.
     * @property[name] The name as it was typed.
     * @property[role] The role it would collide with.
     */
    @Immutable
    public data class AccentNameRole(
        public val index: Int,
        public val name: String,
        public val role: Role,
    ) : ValidationError
}
