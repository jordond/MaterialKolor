package com.materialkolor.builder.domain.validate

import com.materialkolor.builder.domain.color.Argb
import com.materialkolor.builder.domain.model.Accent
import com.materialkolor.builder.domain.model.Role
import com.materialkolor.builder.domain.model.ThemeDocument
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationTest {
    private val seed = Argb(0xFF00AA00.toInt())

    @Test
    fun validatePackageName_wellFormed_passes() {
        listOf("com.example.app", "a", "io.brand_2.theme", "com.x9").forEach { name ->
            assertEquals(emptyList(), validatePackageName(name), name)
        }
    }

    @Test
    fun validatePackageName_badSegment_isReportedWithItsIndex() {
        val cases = mapOf(
            "Com.example" to ValidationError.PackageSegmentInvalid(0, "Com"),
            "com.9app" to ValidationError.PackageSegmentInvalid(1, "9app"),
            "com._app" to ValidationError.PackageSegmentInvalid(1, "_app"),
            "com.my-app" to ValidationError.PackageSegmentInvalid(1, "my-app"),
            "com..app" to ValidationError.PackageSegmentInvalid(1, ""),
            "com.app." to ValidationError.PackageSegmentInvalid(2, ""),
            "" to ValidationError.PackageSegmentInvalid(0, ""),
            "com.ünï" to ValidationError.PackageSegmentInvalid(1, "ünï"),
        )

        cases.forEach { (name, error) -> assertEquals(listOf(error), validatePackageName(name), name) }
    }

    @Test
    fun validatePackageName_keywordSegment_isReported() {
        assertEquals(
            listOf(
                ValidationError.PackageSegmentKeyword(1, "package"),
                ValidationError.PackageSegmentKeyword(2, "in"),
            ),
            validatePackageName("com.package.in.app"),
        )
    }

    @Test
    fun validatePackageName_everyHardKeyword_isReported() {
        KOTLIN_HARD_KEYWORDS.forEach { keyword ->
            assertEquals(listOf(ValidationError.PackageSegmentKeyword(1, keyword)), validatePackageName("com.$keyword"))
        }
    }

    @Test
    fun validatePackageName_softKeyword_passes() {
        assertEquals(emptyList(), validatePackageName("com.data.value.open.internal"))
    }

    @Test
    fun validateThemeName_identifier_passes() {
        listOf("AppTheme", "_Theme", "Théme", "Theme2", "brand_theme").forEach { name ->
            assertEquals(emptyList(), validateThemeName(name), name)
        }
    }

    @Test
    fun validateThemeName_notAnIdentifier_isReported() {
        listOf("", "2Theme", "App Theme", "App-Theme", "_", "__", "App.Theme", "Theme🎨").forEach { name ->
            assertEquals(listOf(ValidationError.ThemeNameInvalid(name)), validateThemeName(name), name)
        }
    }

    @Test
    fun validateThemeName_keyword_isReported() {
        listOf("class", "object", "typealias").forEach { name ->
            assertEquals(listOf(ValidationError.ThemeNameKeyword(name)), validateThemeName(name), name)
        }
    }

    @Test
    fun validateAccents_eightGoodAccents_passes() {
        val accents = List(MAX_ACCENTS) { index -> Accent(name = "accent$index", seed = seed) }

        assertEquals(emptyList(), validateAccents(accents))
    }

    @Test
    fun validateAccents_nineAccents_isTooMany() {
        val accents = List(MAX_ACCENTS + 1) { index -> Accent(name = "accent$index", seed = seed) }

        assertEquals(listOf(ValidationError.TooManyAccents(9)), validateAccents(accents))
    }

    @Test
    fun validateAccents_badName_isReportedWithItsIndex() {
        val accents = listOf(
            Accent(name = "brand", seed = seed),
            Accent(name = "2brand", seed = seed),
            Accent(name = "when", seed = seed),
            Accent(name = "", seed = seed),
        )

        assertEquals(
            listOf(
                ValidationError.AccentNameInvalid(1, "2brand"),
                ValidationError.AccentNameKeyword(2, "when"),
                ValidationError.AccentNameInvalid(3, ""),
            ),
            validateAccents(accents),
        )
    }

    @Test
    fun validateAccents_nameAt24Bytes_passes() {
        val ascii = "a".repeat(MAX_ACCENT_NAME_BYTES)
        val accented = "é".repeat(MAX_ACCENT_NAME_BYTES / 2)

        assertEquals(emptyList(), validateAccents(listOf(Accent(ascii, seed), Accent(accented, seed))))
    }

    @Test
    fun validateAccents_namePast24Bytes_isTooLong() {
        val ascii = "a".repeat(MAX_ACCENT_NAME_BYTES + 1)
        val accented = "é".repeat(MAX_ACCENT_NAME_BYTES / 2) + "a"

        assertEquals(
            listOf(
                ValidationError.AccentNameTooLong(0, ascii, 25),
                ValidationError.AccentNameTooLong(1, accented, 25),
            ),
            validateAccents(listOf(Accent(ascii, seed), Accent(accented, seed))),
        )
    }

    @Test
    fun validateAccents_repeatedName_isReportedOnEveryLaterCopy() {
        val accents = listOf(
            Accent(name = "brand", seed = seed),
            Accent(name = "sage", seed = seed),
            Accent(name = "brand", seed = seed),
            Accent(name = "brand", seed = seed),
            Accent(name = "Brand", seed = seed),
        )

        assertEquals(
            listOf(
                ValidationError.AccentNameDuplicate(2, "brand"),
                ValidationError.AccentNameDuplicate(3, "brand"),
                ValidationError.AccentNameCaseClash(4, "Brand"), // b-110
            ),
            validateAccents(accents),
        )
    }

    @Test
    fun validateProjectName_at48Bytes_passes() {
        assertEquals(emptyList(), validateProjectName("p".repeat(MAX_PROJECT_NAME_BYTES)))
        assertEquals(emptyList(), validateProjectName("漢".repeat(MAX_PROJECT_NAME_BYTES / 3)))
    }

    @Test
    fun validateProjectName_past48Bytes_isTooLong() {
        val ascii = "p".repeat(MAX_PROJECT_NAME_BYTES + 1)
        val wide = "漢".repeat(MAX_PROJECT_NAME_BYTES / 3) + "p"

        assertEquals(listOf(ValidationError.ProjectNameTooLong(ascii, 49)), validateProjectName(ascii))
        assertEquals(listOf(ValidationError.ProjectNameTooLong(wide, 49)), validateProjectName(wide))
    }

    @Test
    fun validateDocument_default_passes() {
        assertEquals(emptyList(), validateDocument(ThemeDocument.Default))
    }

    @Test
    fun validateDocument_badNameAndAccents_reportsBoth() {
        val document = ThemeDocument.Default.copy(
            themeName = "fun",
            accents = listOf(Accent(name = "x y", seed = seed)),
        )

        assertEquals(
            listOf(ValidationError.ThemeNameKeyword("fun"), ValidationError.AccentNameInvalid(0, "x y")),
            validateDocument(document),
        )
    }

    @Test
    fun validateDocument_anyInput_leavesItAsItWas() {
        val document = ThemeDocument.Default.copy(themeName = "1bad", accents = listOf(Accent(name = "", seed = seed)))
        val untouched = document.copy()

        validateDocument(document)

        assertEquals(untouched, document)
    }

    // b-110

    @Test
    fun validateAccents_namesDifferingOnlyInCase_isACaseClash() {
        val accents = listOf(Accent(name = "brand", seed = seed), Accent(name = "Brand", seed = seed))

        assertEquals(listOf(ValidationError.AccentNameCaseClash(1, "Brand")), validateAccents(accents))
    }

    @Test
    fun validateAccents_caseClashWithAnExactRepeat_isOnlyReportedOnce() {
        val accents = listOf(
            Accent(name = "brand", seed = seed),
            Accent(name = "BRAND", seed = seed),
            Accent(name = "brand", seed = seed),
        )

        assertEquals(
            listOf(
                ValidationError.AccentNameDuplicate(2, "brand"),
                ValidationError.AccentNameCaseClash(1, "BRAND"),
            ),
            validateAccents(accents),
        )
    }

    @Test
    fun validateAccents_roleName_isReportedWithTheRole() {
        val accents = listOf(
            Accent(name = "primary", seed = seed),
            Accent(name = "SurfaceContainerHigh", seed = seed),
            Accent(name = "ONERROR", seed = seed),
            Accent(name = "brand", seed = seed),
        )

        assertEquals(
            listOf(
                ValidationError.AccentNameRole(0, "primary", Role.Primary),
                ValidationError.AccentNameRole(1, "SurfaceContainerHigh", Role.SurfaceContainerHigh),
                ValidationError.AccentNameRole(2, "ONERROR", Role.OnError),
            ),
            validateAccents(accents),
        )
    }

    @Test
    fun validateAccents_nameThatOnlyContainsARole_passes() {
        val accents = listOf(Accent(name = "primaryBrand", seed = seed), Accent(name = "surfaces", seed = seed))

        assertEquals(emptyList(), validateAccents(accents))
    }
}
