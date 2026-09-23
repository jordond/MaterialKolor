package com.materialkolor.builder.domain.validate

import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument

/** The most accents an export carries. */
public const val MAX_ACCENTS: Int = 8

/** The longest an accent name may be, in UTF-8 bytes. */
public const val MAX_ACCENT_NAME_BYTES: Int = 24

/** The longest a project name may be, in UTF-8 bytes. */
public const val MAX_PROJECT_NAME_BYTES: Int = 48

/**
 * Everything wrong with [document] that would get in the way of an export, the theme name and the
 * accents. An empty list means there is nothing to fix.
 *
 * Nothing here corrects anything. It only says what is wrong, and the person editing decides what
 * to do about it.
 */
public fun validateDocument(document: ThemeDocument): List<ValidationError> =
    validateThemeName(document.themeName) + validateAccents(document.accents)

/**
 * Whether [name] can be the name of the exported theme, which has to be a Kotlin identifier that
 * is not a keyword.
 */
public fun validateThemeName(name: String): List<ValidationError> =
    when {
        !name.isKotlinIdentifier() -> listOf(ValidationError.ThemeNameInvalid(name))
        name in KOTLIN_HARD_KEYWORDS -> listOf(ValidationError.ThemeNameKeyword(name))
        else -> emptyList()
    }

/**
 * Whether [accents] fit in an export. There can be at most [MAX_ACCENTS] of them, and their names
 * have to be distinct Kotlin identifiers of at most [MAX_ACCENT_NAME_BYTES] UTF-8 bytes each.
 */
public fun validateAccents(accents: List<Accent>): List<ValidationError> =
    buildList {
        if (accents.size > MAX_ACCENTS) add(ValidationError.TooManyAccents(accents.size))
        val seen = mutableSetOf<String>()
        accents.forEachIndexed { index, accent ->
            val name = accent.name
            when {
                !name.isKotlinIdentifier() -> add(ValidationError.AccentNameInvalid(index, name))
                name in KOTLIN_HARD_KEYWORDS -> add(ValidationError.AccentNameKeyword(index, name))
            }
            val bytes = name.utf8Size()
            if (bytes > MAX_ACCENT_NAME_BYTES) add(ValidationError.AccentNameTooLong(index, name, bytes))
            if (!seen.add(name)) add(ValidationError.AccentNameDuplicate(index, name))
        }
        addAll(accentNameClashes(accents)) // b-110
    }

/**
 * Whether [packageName] can be the package of the exported theme.
 *
 * Every dot separated segment has to start with a lowercase letter, hold only lowercase letters,
 * digits and underscores, and not be a Kotlin keyword. An empty name, or one with an empty
 * segment, fails on that segment.
 */
public fun validatePackageName(packageName: String): List<ValidationError> =
    packageName.split('.').mapIndexedNotNull { index, segment ->
        when {
            !PACKAGE_SEGMENT.matches(segment) -> ValidationError.PackageSegmentInvalid(index, segment)
            segment in KOTLIN_HARD_KEYWORDS -> ValidationError.PackageSegmentKeyword(index, segment)
            else -> null
        }
    }

/**
 * Whether [projectName] fits, which means at most [MAX_PROJECT_NAME_BYTES] UTF-8 bytes.
 */
public fun validateProjectName(projectName: String): List<ValidationError> {
    val bytes = projectName.utf8Size()
    if (bytes <= MAX_PROJECT_NAME_BYTES) return emptyList()
    return listOf(ValidationError.ProjectNameTooLong(projectName, bytes))
}

/**
 * Whether this is something Kotlin accepts as a name without backticks.
 *
 * It starts with a letter or an underscore and goes on with letters, digits and underscores. A name
 * made of underscores alone is reserved, so it does not count.
 */
internal fun String.isKotlinIdentifier(): Boolean {
    if (isEmpty() || all { char -> char == '_' }) return false
    val head = first()
    return (head.isLetter() || head == '_') && all { char -> char.isLetterOrDigit() || char == '_' }
}

private fun String.utf8Size(): Int = encodeToByteArray().size

// b-110
// An export names each accent's family after it with the first letter lowered, and its on colors with
// "on" in front, so two names that only differ in case, or a name that is also a scheme role, collide.
private fun accentNameClashes(accents: List<Accent>): List<ValidationError> =
    buildList {
        val exact = mutableSetOf<String>()
        val folded = mutableSetOf<String>()
        accents.forEachIndexed { index, accent ->
            val name = accent.name
            val role = Role.entries.firstOrNull { role -> role.name.equals(name, ignoreCase = true) }
            if (role != null) add(ValidationError.AccentNameRole(index, name, role))

            // An exact repeat is already reported as a duplicate, so only a clash in case is new here.
            val repeated = !exact.add(name)
            val clashes = !folded.add(name.lowercase())
            if (clashes && !repeated) add(ValidationError.AccentNameCaseClash(index, name))
        }
    }

private val PACKAGE_SEGMENT = Regex("[a-z][a-z0-9_]*")

/**
 * The words Kotlin never accepts as a name, whatever the context.
 */
internal val KOTLIN_HARD_KEYWORDS: Set<String> =
    setOf(
        "as",
        "break",
        "class",
        "continue",
        "do",
        "else",
        "false",
        "for",
        "fun",
        "if",
        "in",
        "interface",
        "is",
        "null",
        "object",
        "package",
        "return",
        "super",
        "this",
        "throw",
        "true",
        "try",
        "typealias",
        "typeof",
        "val",
        "var",
        "when",
        "while",
    )
